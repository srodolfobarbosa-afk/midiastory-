package com.rdf112.sweep.data.maria

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.annotation.RawRes
import com.rdf112.sweep.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/**
 * Lightweight Firebase Hosting CDN client + bounded local disk cache (<= 24 MB) for Maria 2.0
 * MoDA Diffusion H.265/HEVC neural talking-head micro-videos.
 *
 * Design guarantees:
 * - Zero third-party SDK bloat (0 KB added to AAB).
 * - Verifies ISO-BMFF (`ftyp`) signature and optional SHA-256 checksum from `manifest.json`
 *   before exposing any cached `.mp4` file to `MediaPlayer`.
 * - Strict offline resilience: if offline or clip not yet published for a sub-tile, returns `null`
 *   immediately without crashing so `MariaHeroBanner` uses its local `maria2_idle.mp4` + studio audio fallback.
 */
object MariaCdnVideoCache {
    private const val CDN_BASE_URL = "https://midiastory-df6a0.web.app/cdn/maria2"
    private const val MANIFEST_URL = "$CDN_BASE_URL/manifest.json"
    private const val CACHE_FOLDER = "maria2_cdn_cache"
    private const val MANIFEST_CACHE_FILE = "manifest_v291.json"
    private const val UNIVERSAL_VISUAL_LANG = "pt"
    private const val UNIVERSAL_VISUAL_CLIP = "clean"
    private const val MANIFEST_TTL_MS = 20L * 60L * 1000L // 20 minutes
    private const val MAX_CACHE_BYTES = 24L * 1024L * 1024L // 24 MB cap
    private const val CONNECT_TIMEOUT_MS = 3500
    private const val READ_TIMEOUT_MS = 6000

    private data class RemoteClipMeta(
        val url: String,
        val bytes: Long,
        val sha256: String?,
    )

    private val downloadMutex = Mutex()

    @Volatile
    private var cachedManifestAtMs: Long = 0L

    @Volatile
    private var cachedManifestMap: Map<String, Map<String, RemoteClipMeta>>? = null

    fun normalizedLang(locale: Locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()): String {
        return when (val raw = locale.language.lowercase(Locale.ROOT)) {
            "in", "id" -> "id"
            "pt", "en", "es", "de", "fr", "hi", "ja", "zh", "ar", "ru", "bn", "ko", "it" -> raw
            else -> "en"
        }
    }

    /** Fast non-blocking check if a valid neural MP4 clip is already on disk. */
    fun peekCachedClip(context: Context, lang: String, clipKey: String): File? {
        val file = clipFile(context, lang, clipKey)
        return if (isValidMp4File(file)) {
            runCatching { file.setLastModified(System.currentTimeMillis()) }
            file
        } else {
            null
        }
    }

    /**
     * Resolves a verified local MoDA H.265 `.mp4` clip from disk or downloads it within [timeoutMs].
     * Returns `null` cleanly if offline or if the specific clip is not yet listed in CDN `manifest.json`.
     */
    suspend fun resolveOrFetchClip(
        context: Context,
        lang: String,
        clipKey: String,
        timeoutMs: Long = 3800L,
    ): File? = withContext(Dispatchers.IO) {
        peekCachedClip(context, lang, clipKey)?.let { return@withContext it }
        withTimeoutOrNull(timeoutMs) {
            downloadMutex.withLock {
                peekCachedClip(context, lang, clipKey)?.let { return@withLock it }
                val manifest = loadManifest(context)
                val resolvedKey = if (manifest?.get(lang)?.containsKey(clipKey) == true) clipKey else if (lang != "pt" && manifest?.get(lang)?.containsKey("clean") == true) "clean" else clipKey
                if (resolvedKey != clipKey) { peekCachedClip(context, lang, resolvedKey)?.let { return@withLock it } }
                val meta = manifest?.get(lang)?.get(resolvedKey) ?: return@withLock null
                downloadAndVerifyClip(context, lang, resolvedKey, meta)
            }
        }
    }

    /** One shared visual animation for Maria speech in every tab and app locale. */
    suspend fun resolveUniversalVisualClip(context: Context, timeoutMs: Long = 12_000L): File? =
        resolveOrFetchClip(context, UNIVERSAL_VISUAL_LANG, UNIVERSAL_VISUAL_CLIP, timeoutMs)

    /**
     * Pre-warms the single shared Maria speech animation, independent of the app locale.
     */
    suspend fun prefetchUniversalVisualClip(context: Context) = withContext(Dispatchers.IO) {
        pruneUnusedCachedVideos(context)
        runCatching { resolveUniversalVisualClip(context) }
    }

    private fun pruneUnusedCachedVideos(context: Context) {
        val universalName = "${UNIVERSAL_VISUAL_LANG}_${UNIVERSAL_VISUAL_CLIP}_v291.mp4"
        cacheDir(context).listFiles()?.forEach { file ->
            val isUnusedVideo = file.isFile && file.extension.equals("mp4", ignoreCase = true) && file.name != universalName
            val isAbandonedDownload = file.isFile && file.name.endsWith(".part", ignoreCase = true)
            if (isUnusedVideo || isAbandonedDownload) file.delete()
        }
    }

    private fun cacheDir(context: Context): File =
        File(context.applicationContext.filesDir, CACHE_FOLDER).apply { mkdirs() }

    private fun clipFile(context: Context, lang: String, clipKey: String): File =
        File(cacheDir(context), "${lang}_${clipKey}_v291.mp4")

    private fun loadManifest(context: Context): Map<String, Map<String, RemoteClipMeta>>? {
        val now = System.currentTimeMillis()
        val mem = cachedManifestMap
        if (mem != null && (now - cachedManifestAtMs) < MANIFEST_TTL_MS) {
            return mem
        }

        val diskFile = File(cacheDir(context), MANIFEST_CACHE_FILE)
        val fetchedText = runCatching {
            val conn = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = CONNECT_TIMEOUT_MS
                useCaches = false
            }
            try {
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()

        val rawJson = if (!fetchedText.isNullOrBlank()) {
            runCatching { diskFile.writeText(fetchedText, Charsets.UTF_8) }
            fetchedText
        } else if (diskFile.isFile && diskFile.length() > 16L) {
            runCatching { diskFile.readText(Charsets.UTF_8) }.getOrNull()
        } else {
            null
        } ?: return mem

        val parsed = parseManifestJson(rawJson)
        if (parsed != null) {
            cachedManifestMap = parsed
            cachedManifestAtMs = now
        }
        return parsed ?: mem
    }

    private fun parseManifestJson(rawJson: String): Map<String, Map<String, RemoteClipMeta>>? = runCatching {
        val root = JSONObject(rawJson)
        val localesObj = root.optJSONObject("locales") ?: return null
        val out = LinkedHashMap<String, Map<String, RemoteClipMeta>>()
        val langIter = localesObj.keys()
        while (langIter.hasNext()) {
            val lang = langIter.next()
            val clipsObj = localesObj.optJSONObject(lang) ?: continue
            val clipMap = LinkedHashMap<String, RemoteClipMeta>()
            val clipIter = clipsObj.keys()
            while (clipIter.hasNext()) {
                val clipKey = clipIter.next()
                val item = clipsObj.optJSONObject(clipKey) ?: continue
                val url = item.optString("url", "").trim()
                val bytes = item.optLong("bytes", 0L)
                val sha = item.optString("sha256", "").trim().takeIf { it.length == 64 }
                if (url.startsWith("https://") && bytes >= 8_000L) {
                    clipMap[clipKey] = RemoteClipMeta(url = url, bytes = bytes, sha256 = sha)
                }
            }
            if (clipMap.isNotEmpty()) {
                out[lang] = clipMap
            }
        }
        out
    }.getOrNull()

    private fun downloadAndVerifyClip(
        context: Context,
        lang: String,
        clipKey: String,
        meta: RemoteClipMeta,
    ): File? = runCatching {
        val dir = cacheDir(context)
        val dest = clipFile(context, lang, clipKey)
        val temp = File(dir, "${dest.name}.part")
        if (temp.exists()) temp.delete()

        val conn = (URL(meta.url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            conn.inputStream.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > 4L * 1024L * 1024L) {
                            // Safety guard: reject unexpectedly large response (> 4 MB)
                            temp.delete()
                            return null
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }

        if (!isValidMp4File(temp)) {
            temp.delete()
            return null
        }
        if (meta.sha256 != null) {
            val actualHex = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actualHex.equals(meta.sha256, ignoreCase = true)) {
                temp.delete()
                return null
            }
        }
        if (dest.exists()) dest.delete()
        if (!temp.renameTo(dest)) {
            temp.copyTo(dest, overwrite = true)
            temp.delete()
        }
        trimCacheIfNeeded(dir, preserve = dest)
        dest
    }.getOrNull()

    private fun isValidMp4File(file: File): Boolean {
        if (!file.isFile || file.length() < 8_192L) return false
        return runCatching {
            val header = ByteArray(12)
            val read = file.inputStream().use { it.read(header) }
            read >= 8 &&
                header[4] == 'f'.code.toByte() &&
                header[5] == 't'.code.toByte() &&
                header[6] == 'y'.code.toByte() &&
                header[7] == 'p'.code.toByte()
        }.getOrDefault(false)
    }

    private fun trimCacheIfNeeded(dir: File, preserve: File) {
        runCatching {
            val mp4Files = dir.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }
                ?.sortedBy { it.lastModified() }
                ?: return
            var totalBytes = mp4Files.sumOf { it.length() }
            for (candidate in mp4Files) {
                if (totalBytes <= MAX_CACHE_BYTES) break
                if (candidate.absolutePath == preserve.absolutePath) continue
                val len = candidate.length()
                if (candidate.delete()) {
                    totalBytes -= len
                }
            }
        }
    }
}
