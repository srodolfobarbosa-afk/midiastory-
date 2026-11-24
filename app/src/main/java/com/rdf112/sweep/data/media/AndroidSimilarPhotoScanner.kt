package com.rdf112.sweep.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Size
import com.rdf112.sweep.domain.analysis.BlurVarianceEstimator
import com.rdf112.sweep.domain.analysis.PerceptualImageHasher
import com.rdf112.sweep.domain.analysis.PhotoVisualFingerprint
import com.rdf112.sweep.domain.analysis.SimilarPhotoGroup
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class SimilarPhotosScanResult(
    val photosInspected: Int,
    val cacheHits: Int,
    val groups: List<SimilarPhotoGroup>,
    val totalRecoverableBytes: Long,
    val capReached: Boolean,
)

/**
 * High-speed on-device scanner for Similar / Burst Photos backed by [AndroidMediaFingerprintStore].
 *
 * - First run: decodes a 96x96 thumbnail once per photo, computing BOTH 64-bit perceptual dHash
 *   and Laplacian sharpness variance in a single pass, then persists to SQLite WAL.
 * - Subsequent runs: serves unchanged photos directly from SQLite in milliseconds (< 0.2s warm scan).
 * - Never deletes automatically; protects the sharpest shot (`isProtected = true, isSelectable = false`).
 */
object AndroidSimilarPhotoScanner {
    private const val SAMPLE_SIZE_PX = 96
    const val DEFAULT_MAX_PHOTOS = 1_500

    suspend fun scanSimilarPhotos(
        context: Context,
        maxPhotos: Int = DEFAULT_MAX_PHOTOS,
        maxHammingDistance: Int = PerceptualImageHasher.DEFAULT_MAX_HAMMING_DISTANCE,
        onProgress: ((inspected: Int, cacheHits: Int) -> Unit)? = null,
    ): SimilarPhotosScanResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val store = AndroidMediaFingerprintStore.getInstance(context)
        val volumes = runCatching { MediaStore.getExternalVolumeNames(context) }
            .getOrElse { setOf(MediaStore.VOLUME_EXTERNAL_PRIMARY) }
            .sortedWith(compareBy<String> { it != MediaStore.VOLUME_EXTERNAL_PRIMARY }.thenBy { it })

        val projection = arrayOf(
            BaseColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.IS_PENDING,
        ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(MediaStore.MediaColumns.IS_TRASHED)
        } else {
            emptyArray()
        }

        val discoveredRecords = ArrayList<MediaRecord>(maxPhotos.coerceAtMost(2_048))
        var capReached = false

        volumeLoop@ for (volume in volumes) {
            currentCoroutineContext().ensureActive()
            val collection = MediaStore.Images.Media.getContentUri(volume)
            val queryArgs = Bundle().apply {
                putInt(ContentResolver.QUERY_ARG_LIMIT, maxPhotos)
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.MediaColumns.DATE_MODIFIED, BaseColumns._ID),
                )
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            }
            val signal = CancellationSignal()
            val cursor = try {
                resolver.query(collection, projection, queryArgs, signal)
            } catch (cancelled: CancellationException) {
                signal.cancel()
                throw cancelled
            } catch (_: Exception) {
                runCatching {
                    resolver.query(collection, projection, null, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")
                }.getOrNull()
            } ?: continue

            cursor.use { c ->
                val idCol = c.getColumnIndex(BaseColumns._ID)
                val nameCol = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeCol = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val modCol = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val relPathCol = c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                val pendingCol = c.getColumnIndex(MediaStore.MediaColumns.IS_PENDING)
                val trashedCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                } else {
                    -1
                }
                if (idCol < 0 || sizeCol < 0 || mimeCol < 0) return@use

                while (c.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    if (discoveredRecords.size >= maxPhotos) {
                        capReached = true
                        break@volumeLoop
                    }
                    val id = runCatching { c.getLong(idCol) }.getOrNull() ?: continue
                    if (id <= 0L) continue
                    val pending = pendingCol >= 0 && !c.isNull(pendingCol) && runCatching { c.getInt(pendingCol) }.getOrDefault(0) != 0
                    val trashed = trashedCol >= 0 && !c.isNull(trashedCol) && runCatching { c.getInt(trashedCol) }.getOrDefault(0) != 0
                    if (pending || trashed) continue

                    val size = if (!c.isNull(sizeCol)) runCatching { c.getLong(sizeCol) }.getOrDefault(0L) else 0L
                    val mime = if (!c.isNull(mimeCol)) runCatching { c.getString(mimeCol) }.getOrNull() else null
                    if (size <= 0L || mime.isNullOrBlank() || !mime.startsWith("image/", ignoreCase = true)) continue

                    val displayName = if (nameCol >= 0 && !c.isNull(nameCol)) runCatching { c.getString(nameCol) }.getOrNull() else null
                    val modified = if (modCol >= 0 && !c.isNull(modCol)) runCatching { c.getLong(modCol) }.getOrNull() else null
                    val relPath = if (relPathCol >= 0 && !c.isNull(relPathCol)) runCatching { c.getString(relPathCol) }.getOrNull() else null

                    val uri = ContentUris.withAppendedId(collection, id)
                    discoveredRecords += MediaRecord(
                        uri = uri.toString(),
                        volumeName = volume,
                        kind = MediaKind.IMAGE,
                        displayName = displayName,
                        mimeType = mime,
                        sizeBytes = size,
                        dateModifiedEpochSeconds = modified,
                        relativePath = relPath,
                    )
                }
            }
        }

        if (discoveredRecords.isEmpty()) {
            return@withContext SimilarPhotosScanResult(
                photosInspected = 0,
                cacheHits = 0,
                groups = emptyList(),
                totalRecoverableBytes = 0L,
                capReached = false,
            )
        }

        val expectedMeta = HashMap<String, Pair<Long, Long>>(discoveredRecords.size)
        for (rec in discoveredRecords) {
            expectedMeta[rec.uri] = Pair(rec.sizeBytes, rec.dateModifiedEpochSeconds ?: -1L)
        }
        val cachedMap = store.getValidBatch(expectedMeta)

        val fingerprints = ArrayList<PhotoVisualFingerprint>(discoveredRecords.size)
        val pendingWrites = ArrayList<CachedMediaFingerprint>(128)
        var pixelBuffer = IntArray(SAMPLE_SIZE_PX * SAMPLE_SIZE_PX * 2)
        var lumaBuffer = IntArray(SAMPLE_SIZE_PX * SAMPLE_SIZE_PX * 2)
        var inspected = 0
        var cacheHits = 0

        for (rec in discoveredRecords) {
            currentCoroutineContext().ensureActive()
            val cached = cachedMap[rec.uri]
            if (cached?.dHash64 != null && cached.laplacianVariance != null) {
                fingerprints += PhotoVisualFingerprint(
                    record = rec,
                    dHash64 = cached.dHash64,
                    sharpnessScore = cached.laplacianVariance.coerceAtLeast(0.0),
                )
                inspected++
                cacheHits++
                if (inspected % 50 == 0) {
                    withContext(Dispatchers.Main) { onProgress?.invoke(inspected, cacheHits) }
                }
                continue
            }

            val parsedUri = android.net.Uri.parse(rec.uri)
            val thumbSignal = CancellationSignal()
            val bitmap: Bitmap? = try {
                resolver.loadThumbnail(parsedUri, Size(SAMPLE_SIZE_PX, SAMPLE_SIZE_PX), thumbSignal)
            } catch (cancelled: CancellationException) {
                thumbSignal.cancel()
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (bitmap == null) continue

            var softBitmap: Bitmap? = null
            try {
                val w = bitmap.width
                val h = bitmap.height
                val pixelCount = w.toLong() * h.toLong()
                if (w >= BlurVarianceEstimator.MIN_SAMPLE_SIDE &&
                    h >= BlurVarianceEstimator.MIN_SAMPLE_SIDE &&
                    pixelCount in 1L..(512L * 512L)
                ) {
                    val countInt = pixelCount.toInt()
                    if (countInt > pixelBuffer.size) {
                        pixelBuffer = IntArray(countInt)
                        lumaBuffer = IntArray(countInt)
                    }
                    softBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) {
                        bitmap
                    } else {
                        bitmap.copy(Bitmap.Config.ARGB_8888, false)
                    }
                    if (softBitmap != null) {
                        softBitmap.getPixels(pixelBuffer, 0, w, 0, 0, w, h)
                        for (i in 0 until countInt) {
                            lumaBuffer[i] = BlurVarianceEstimator.rgbPixelToLuma(pixelBuffer[i])
                        }
                        val blur = BlurVarianceEstimator.analyzeLumaSample(lumaBuffer, w, h)
                        val dHash = PerceptualImageHasher.computeDHash64FromLuma(lumaBuffer, w, h)
                        inspected++

                        if (blur != null && dHash != null) {
                            fingerprints += PhotoVisualFingerprint(
                                record = rec,
                                dHash64 = dHash,
                                sharpnessScore = blur.laplacianVariance,
                            )
                            pendingWrites += CachedMediaFingerprint(
                                uri = rec.uri,
                                sizeBytes = rec.sizeBytes,
                                dateModifiedEpochSeconds = rec.dateModifiedEpochSeconds ?: -1L,
                                dHash64 = dHash,
                                laplacianVariance = blur.laplacianVariance,
                            )
                            if (pendingWrites.size >= 100) {
                                store.upsertBatch(pendingWrites)
                                pendingWrites.clear()
                            }
                        }
                        if (inspected % 20 == 0) {
                            withContext(Dispatchers.Main) { onProgress?.invoke(inspected, cacheHits) }
                        }
                    }
                }
            } finally {
                if (softBitmap != null && softBitmap !== bitmap) {
                    softBitmap.recycle()
                }
                bitmap.recycle()
            }
        }

        if (pendingWrites.isNotEmpty()) {
            store.upsertBatch(pendingWrites)
            pendingWrites.clear()
        }

        val groups = PerceptualImageHasher.detectSimilarGroups(
            items = fingerprints,
            maxHammingDistance = maxHammingDistance,
        )
        val totalRecoverable = groups.fold(0L) { acc, g -> Math.addExact(acc, g.recoverableBytes) }

        SimilarPhotosScanResult(
            photosInspected = inspected,
            cacheHits = cacheHits,
            groups = groups,
            totalRecoverableBytes = totalRecoverable,
            capReached = capReached,
        )
    }
}
