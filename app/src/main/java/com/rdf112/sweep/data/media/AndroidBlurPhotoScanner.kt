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
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class BlurryPhotoCandidate(
    val record: MediaRecord,
    val laplacianVariance: Double,
)

data class BlurScanResult(
    val photosAnalyzed: Int,
    val candidates: List<BlurryPhotoCandidate>,
    val capReached: Boolean,
)

/**
 * Local on-device scanner that evaluates sharpness on downsampled 96x96 thumbnails
 * using discrete Laplacian variance. Produces reviewable candidates only (never auto-deletes).
 */
object AndroidBlurPhotoScanner {
    private const val SAMPLE_SIZE_PX = 96
    private const val DEFAULT_MAX_PHOTOS = 300

    suspend fun scanRecentPhotos(
        context: Context,
        maxPhotos: Int = DEFAULT_MAX_PHOTOS,
        onProgress: ((analyzed: Int, candidatesFound: Int) -> Unit)? = null,
    ): BlurScanResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
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

        val candidates = ArrayList<BlurryPhotoCandidate>()
        var analyzed = 0
        var capReached = false
        var pixelBuffer = IntArray(SAMPLE_SIZE_PX * SAMPLE_SIZE_PX * 2)

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
                runCatching { resolver.query(collection, projection, null, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC") }.getOrNull()
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
                    if (analyzed >= maxPhotos) {
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
                    val record = MediaRecord(
                        uri = uri.toString(),
                        volumeName = volume,
                        kind = MediaKind.IMAGE,
                        displayName = displayName,
                        mimeType = mime,
                        sizeBytes = size,
                        dateModifiedEpochSeconds = modified,
                        relativePath = relPath,
                    )

                    val thumbSignal = CancellationSignal()
                    val bitmap: Bitmap? = try {
                        resolver.loadThumbnail(uri, Size(SAMPLE_SIZE_PX, SAMPLE_SIZE_PX), thumbSignal)
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
                            if (pixelCount > pixelBuffer.size) {
                                pixelBuffer = IntArray(pixelCount.toInt())
                            }
                            softBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) {
                                bitmap
                            } else {
                                bitmap.copy(Bitmap.Config.ARGB_8888, false)
                            }
                            if (softBitmap != null) {
                                softBitmap.getPixels(pixelBuffer, 0, w, 0, 0, w, h)
                                analyzed++
                                val analysis = BlurVarianceEstimator.analyzeArgbPixels(pixelBuffer, w, h)
                                if (analysis != null && analysis.isBlurryCandidate) {
                                    candidates += BlurryPhotoCandidate(record, analysis.laplacianVariance)
                                }
                                if (analyzed % 15 == 0) {
                                    withContext(Dispatchers.Main) {
                                        onProgress?.invoke(analyzed, candidates.size)
                                    }
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
            }
        }

        val sorted = candidates.sortedWith(
            compareBy<BlurryPhotoCandidate> { it.laplacianVariance }
                .thenByDescending { it.record.sizeBytes },
        )
        BlurScanResult(
            photosAnalyzed = analyzed,
            candidates = sorted,
            capReached = capReached,
        )
    }
}
