package com.rdf112.sweep.data.media

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Cached visual & cryptographic metadata keyed by `(uri, sizeBytes, dateModifiedEpochSeconds)`.
 * If a file's size or modification timestamp changes on disk, the entry automatically misses and refreshes.
 */
data class CachedMediaFingerprint(
    val uri: String,
    val sizeBytes: Long,
    val dateModifiedEpochSeconds: Long,
    val dHash64: Long?,
    val laplacianVariance: Double?,
    val sha256: String? = null,
)

/**
 * Ultra-lightweight native SQLite fingerprint cache (WAL mode, zero external libraries / 0 KB APK bloat).
 * Eliminates repeated thumbnail decoding and re-hashing on subsequent app launches (< 0.2s warm scan).
 */
class AndroidMediaFingerprintStore private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        runCatching { db.enableWriteAheadLogging() }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                uri TEXT PRIMARY KEY NOT NULL,
                size_bytes INTEGER NOT NULL,
                date_modified_sec INTEGER NOT NULL,
                dhash64 INTEGER,
                laplacian_variance REAL,
                sha256 TEXT,
                updated_at_ms INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_${TABLE_NAME}_updated ON $TABLE_NAME(updated_at_ms DESC)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    /**
     * Looks up a batch of URIs and returns only entries whose [sizeBytes] and [dateModifiedEpochSeconds]
     * still match MediaStore's current metadata.
     */
    fun getValidBatch(
        expectedMetaByUri: Map<String, Pair<Long, Long>>,
    ): Map<String, CachedMediaFingerprint> {
        if (expectedMetaByUri.isEmpty()) return emptyMap()
        val result = HashMap<String, CachedMediaFingerprint>(expectedMetaByUri.size)
        val db = runCatching { readableDatabase }.getOrNull() ?: return emptyMap()

        val uris = expectedMetaByUri.keys.toList()
        val chunkSize = 400
        for (offset in uris.indices step chunkSize) {
            val slice = uris.subList(offset, (offset + chunkSize).coerceAtMost(uris.size))
            val placeholders = slice.joinToString(",") { "?" }
            val sql =
                "SELECT uri, size_bytes, date_modified_sec, dhash64, laplacian_variance, sha256 " +
                    "FROM $TABLE_NAME WHERE uri IN ($placeholders)"
            runCatching {
                db.rawQuery(sql, slice.toTypedArray()).use { c ->
                    while (c.moveToNext()) {
                        val uri = c.getString(0) ?: continue
                        val size = c.getLong(1)
                        val modified = c.getLong(2)
                        val expected = expectedMetaByUri[uri] ?: continue
                        if (expected.first != size || expected.second != modified) continue

                        val dHash = if (c.isNull(3)) null else c.getLong(3)
                        val lapVar = if (c.isNull(4)) null else c.getDouble(4)
                        val sha = if (c.isNull(5)) null else c.getString(5)
                        result[uri] = CachedMediaFingerprint(
                            uri = uri,
                            sizeBytes = size,
                            dateModifiedEpochSeconds = modified,
                            dHash64 = dHash,
                            laplacianVariance = lapVar,
                            sha256 = sha,
                        )
                    }
                }
            }
        }
        return result
    }

    /**
     * Atomically writes or updates visual fingerprints in a single WAL transaction and enforces
     * a bounded row cap ([MAX_CACHED_ROWS]) so the local database never grows unbounded.
     */
    fun upsertBatch(entries: Collection<CachedMediaFingerprint>) {
        if (entries.isEmpty()) return
        val db = runCatching { writableDatabase }.getOrNull() ?: return
        val nowMs = System.currentTimeMillis()

        runCatching {
            db.beginTransactionNonExclusive()
            try {
                val stmt = db.compileStatement(
                    """
                    INSERT INTO $TABLE_NAME (uri, size_bytes, date_modified_sec, dhash64, laplacian_variance, sha256, updated_at_ms)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uri) DO UPDATE SET
                        size_bytes = excluded.size_bytes,
                        date_modified_sec = excluded.date_modified_sec,
                        dhash64 = COALESCE(excluded.dhash64, $TABLE_NAME.dhash64),
                        laplacian_variance = COALESCE(excluded.laplacian_variance, $TABLE_NAME.laplacian_variance),
                        sha256 = COALESCE(excluded.sha256, $TABLE_NAME.sha256),
                        updated_at_ms = excluded.updated_at_ms
                    """.trimIndent(),
                )
                for (entry in entries) {
                    stmt.clearBindings()
                    stmt.bindString(1, entry.uri)
                    stmt.bindLong(2, entry.sizeBytes)
                    stmt.bindLong(3, entry.dateModifiedEpochSeconds)
                    if (entry.dHash64 != null) stmt.bindLong(4, entry.dHash64) else stmt.bindNull(4)
                    if (entry.laplacianVariance != null) stmt.bindDouble(5, entry.laplacianVariance) else stmt.bindNull(5)
                    if (entry.sha256 != null) stmt.bindString(6, entry.sha256) else stmt.bindNull(6)
                    stmt.bindLong(7, nowMs)
                    stmt.executeInsert()
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            // Bounded storage hygiene
            db.execSQL(
                """
                DELETE FROM $TABLE_NAME
                WHERE uri NOT IN (
                    SELECT uri FROM $TABLE_NAME ORDER BY updated_at_ms DESC LIMIT $MAX_CACHED_ROWS
                )
                """.trimIndent(),
            )
        }
    }

    companion object {
        private const val DB_NAME = "midiastore_fingerprints.db"
        private const val DB_VERSION = 1
        private const val TABLE_NAME = "media_visual_fingerprints"
        const val MAX_CACHED_ROWS = 100_000

        @Volatile
        private var instance: AndroidMediaFingerprintStore? = null

        fun getInstance(context: Context): AndroidMediaFingerprintStore =
            instance ?: synchronized(this) {
                instance ?: AndroidMediaFingerprintStore(context).also { instance = it }
            }
    }
}
