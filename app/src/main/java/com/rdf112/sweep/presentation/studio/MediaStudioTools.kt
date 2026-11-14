package com.rdf112.sweep.presentation.studio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * ESTÚDIO DE MÍDIA & FERRAMENTAS AVANÇADAS (Midiastore v2.4.0)
 *
 * Funcionalidades nativas de alto desempenho:
 * (a) Filtros de Foto com ColorMatrix (Cinema, Nitidez, Vintage, P&B, Vibrante)
 * (b) Extrator de Áudio de Vídeo (M4A/AAC) e Limpador de Privacidade EXIF (remove GPS e metadados)
 * (c) Gerador de QR Code (PIX, Links, WhatsApp, Wi-Fi) e Conversor de Fotos para PDF A4
 * (d) Importador de Vídeo Direto (MP4/WebM) com validação binária anti-HTML
 */
object MediaStudioTools {

    data class ExifSummary(
        val hasGps: Boolean,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val camera: String? = null,
        val dateTime: String? = null
    )

    private val PRIVACY_EXIF_TAGS = listOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_IMAGE_UNIQUE_ID,
        "CameraOwnerName",
        "BodySerialNumber",
        "LensMake",
        "LensModel",
        "LensSerialNumber",
    )

    fun readExifSummary(context: Context, uri: Uri): ExifSummary {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val latLong = FloatArray(2)
                val hasLatLong = exif.getLatLong(latLong)
                val hasRawGps = !exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE).isNullOrBlank() ||
                    !exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE).isNullOrBlank()
                val hasGps = hasLatLong || hasRawGps
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()?.takeIf { it.isNotEmpty() }
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()?.takeIf { it.isNotEmpty() }
                val camera = when {
                    make != null && model != null -> if (model.startsWith(make, ignoreCase = true)) model else "$make $model"
                    model != null -> model
                    make != null -> make
                    else -> null
                }
                val dt = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.takeIf { it.isNotBlank() }
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)?.takeIf { it.isNotBlank() }
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)?.takeIf { it.isNotBlank() }
                ExifSummary(
                    hasGps = hasGps,
                    latitude = if (hasLatLong) latLong[0].toDouble() else null,
                    longitude = if (hasLatLong) latLong[1].toDouble() else null,
                    camera = camera,
                    dateTime = dt
                )
            } ?: ExifSummary(false)
        } catch (e: Exception) {
            ExifSummary(false)
        }
    }

    fun loadSampledBitmap(context: Context, uri: Uri, maxDim: Int = 1280): Bitmap? {
        return try {
            val safeMaxDim = maxDim.coerceAtLeast(128)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null
            var sampleSize = 1
            while (options.outWidth / sampleSize > safeMaxDim || options.outHeight / sampleSize > safeMaxDim) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val orientation = try {
                context.contentResolver.openInputStream(uri)?.use { exifStream ->
                    ExifInterface(exifStream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            applyExifOrientation(decoded, orientation)
        } catch (e: Exception) {
            null
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (oriented !== bitmap) {
                bitmap.recycle()
            }
            oriented
        } catch (_: Exception) {
            bitmap
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, titlePrefix: String): Uri? {
        val resolver = context.contentResolver
        val filename = "${titlePrefix}_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Midiastore")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            val compressed = resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            } ?: false
            if (!compressed) {
                resolver.delete(uri, null, null)
                return null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    fun cleanExifAndSave(context: Context, sourceUri: Uri): Uri? {
        val bitmap = loadSampledBitmap(context, sourceUri, 2048) ?: return null
        return try {
            val savedUri = saveBitmapToGallery(context, bitmap, "Privacidade_EXIF_Limpo") ?: return null
            try {
                context.contentResolver.openFileDescriptor(savedUri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    for (tag in PRIVACY_EXIF_TAGS) {
                        exif.setAttribute(tag, null)
                    }
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                    exif.saveAttributes()
                }
            } catch (_: Exception) {
                // Re-encoded JPEG via Bitmap.compress is already free of original source EXIF metadata.
            }
            savedUri
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    fun getShareIntent(context: Context, file: File, mimeType: String, title: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // =========================================================================
    // 1. ESTÚDIO DE FILTROS DE FOTO (ColorMatrix)
    // =========================================================================
    enum class PhotoFilter(val displayName: String) {
        NORMAL("Normal"),
        CINEMA("Cinema"),
        SHARP("Nitidez"),
        VINTAGE("Vintage"),
        BLACK_AND_WHITE("Preto e Branco"),
        VIBRANT("Vibrante")
    }

    fun buildFilterMatrix(filter: PhotoFilter): ColorMatrix {
        val matrix = ColorMatrix()
        when (filter) {
            PhotoFilter.NORMAL -> matrix.reset()
            PhotoFilter.CINEMA -> matrix.set(floatArrayOf(
                1.2f, 0.0f, 0.0f, 0.0f, 10f,
                0.0f, 1.0f, 0.0f, 0.0f, 0f,
                0.0f, 0.0f, 0.8f, 0.0f, 20f,
                0.0f, 0.0f, 0.0f, 1.0f, 0f
            ))
            PhotoFilter.SHARP -> matrix.set(floatArrayOf(
                1.4f, 0.0f, 0.0f, 0.0f, -20f,
                0.0f, 1.4f, 0.0f, 0.0f, -20f,
                0.0f, 0.0f, 1.4f, 0.0f, -20f,
                0.0f, 0.0f, 0.0f, 1.0f, 0f
            ))
            PhotoFilter.VINTAGE -> {
                val sepia = ColorMatrix()
                sepia.setScale(1f, 0.95f, 0.82f, 1.0f)
                val sat = ColorMatrix()
                sat.setSaturation(0.6f)
                sepia.postConcat(sat)
                matrix.set(sepia)
            }
            PhotoFilter.BLACK_AND_WHITE -> matrix.setSaturation(0.0f)
            PhotoFilter.VIBRANT -> matrix.setSaturation(1.6f)
        }
        return matrix
    }

    fun buildCompositeColorMatrix(
        filter: PhotoFilter = PhotoFilter.NORMAL,
        brightness: Float = 0f,
        contrast: Float = 1.0f,
        saturation: Float = 1.0f
    ): ColorMatrix {
        val cm = buildFilterMatrix(filter)
        if (contrast != 1.0f || brightness != 0f) {
            val scale = contrast
            val translate = brightness + (128f * (1.0f - scale))
            val adj = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(adj)
        }
        if (saturation != 1.0f) {
            val sat = ColorMatrix()
            sat.setSaturation(saturation)
            cm.postConcat(sat)
        }
        return cm
    }

    fun applyFilter(source: Bitmap, filter: PhotoFilter): Bitmap {
        return applyTransformations(source, filter = filter)
    }

    fun applyTransformations(
        source: Bitmap,
        filter: PhotoFilter = PhotoFilter.NORMAL,
        brightness: Float = 0f,
        contrast: Float = 1.0f,
        saturation: Float = 1.0f,
        rotationDeg: Int = 0
    ): Bitmap {
        val normalizedRotation = ((rotationDeg % 360) + 360) % 360
        val matrix = Matrix()
        if (normalizedRotation != 0) {
            matrix.postRotate(normalizedRotation.toFloat(), source.width / 2f, source.height / 2f)
        }
        val rotated = if (normalizedRotation != 0) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        return try {
            val result = Bitmap.createBitmap(
                rotated.width,
                rotated.height,
                rotated.config ?: Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val colorMatrix = buildCompositeColorMatrix(filter, brightness, contrast, saturation)
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(rotated, 0f, 0f, paint)
            result
        } finally {
            if (rotated !== source && !rotated.isRecycled) {
                rotated.recycle()
            }
        }
    }

    // =========================================================================
    // 2. EXTRATOR DE VÍDEO PARA ÁUDIO (M4A)
    // =========================================================================
    fun extractAudioTrack(context: Context, videoUri: Uri, outputFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        return try {
            extractor = MediaExtractor().apply { setDataSource(context, videoUri, null) }
            val trackCount = extractor.trackCount
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || audioFormat == null) return false

            extractor.selectTrack(audioTrackIndex)
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(256 * 1024)
            } else {
                256 * 1024
            }
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            var wroteAnySample = false

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                wroteAnySample = true
                extractor.advance()
            }
            wroteAnySample
        } catch (e: Exception) {
            outputFile.delete()
            false
        } finally {
            try { muxer?.stop(); muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    fun extractAudioToCache(context: Context, videoUri: Uri): File? {
        val outputDir = File(context.cacheDir, "extracted_audio").apply { mkdirs() }
        val outputFile = File(outputDir, "Audio_${System.currentTimeMillis()}.m4a")
        val success = extractAudioTrack(context, videoUri, outputFile)
        return if (success && outputFile.exists() && outputFile.length() > 0) outputFile else null
    }

    fun saveAudioToMusic(context: Context, audioFile: File, displayName: String = audioFile.name): Uri? {
        if (!audioFile.exists() || audioFile.length() <= 0L) return null
        val resolver = context.contentResolver
        val safeName = if (displayName.endsWith(".m4a", ignoreCase = true)) displayName else "$displayName.m4a"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, safeName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Midiastore")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            val bytesCopied = resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(audioFile).use { input ->
                    input.copyTo(out)
                }
            } ?: 0L
            if (bytesCopied <= 0L) {
                resolver.delete(uri, null, null)
                return null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    // =========================================================================
    // 3. LIMPADOR DE PRIVACIDADE EXIF (Remove coordenadas GPS e metadados)
    // =========================================================================
    fun stripExifPrivacyData(filePath: String): Boolean {
        return try {
            val exif = ExifInterface(filePath)
            for (tag in PRIVACY_EXIF_TAGS) {
                exif.setAttribute(tag, null)
            }
            exif.saveAttributes()
            true
        } catch (e: Exception) {
            false
        }
    }

    // =========================================================================
    // 4. CRIADOR DE FOTOS PARA PDF (PdfDocument A4)
    // =========================================================================
    fun convertBitmapsToPdf(bitmaps: List<Bitmap>, outputFile: File): Boolean {
        if (bitmaps.isEmpty()) return false
        val document = PdfDocument()
        return try {
            val pageWidth = 595 // A4 width em points (72 dpi)
            val pageHeight = 842 // A4 height em points
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            bitmaps.forEachIndexed { index, bitmap ->
                drawBitmapPageToPdf(document, bitmap, index + 1, pageWidth, pageHeight, bitmapPaint)
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            outputFile.exists() && outputFile.length() > 0L
        } catch (e: Exception) {
            outputFile.delete()
            false
        } finally {
            document.close()
        }
    }

    private fun drawBitmapPageToPdf(
        document: PdfDocument,
        bitmap: Bitmap,
        pageNumber: Int,
        pageWidth: Int,
        pageHeight: Int,
        bitmapPaint: Paint,
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val margin = 30f
        val availableWidth = pageWidth - (margin * 2)
        val availableHeight = pageHeight - (margin * 2)

        val safeWidth = bitmap.width.coerceAtLeast(1).toFloat()
        val safeHeight = bitmap.height.coerceAtLeast(1).toFloat()
        val scale = minOf(availableWidth / safeWidth, availableHeight / safeHeight)
        val destWidth = safeWidth * scale
        val destHeight = safeHeight * scale
        val left = margin + (availableWidth - destWidth) / 2f
        val top = margin + (availableHeight - destHeight) / 2f

        val destRect = android.graphics.RectF(left, top, left + destWidth, top + destHeight)
        canvas.drawBitmap(bitmap, null, destRect, bitmapPaint)

        document.finishPage(page)
    }

    fun createPdfFromUris(context: Context, uris: List<Uri>, outputFile: File): Boolean {
        if (uris.isEmpty()) return false
        val document = PdfDocument()
        return try {
            val pageWidth = 595
            val pageHeight = 842
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            var pagesAdded = 0

            for (uri in uris) {
                val bitmap = loadSampledBitmap(context, uri, 1600) ?: continue
                try {
                    pagesAdded++
                    drawBitmapPageToPdf(document, bitmap, pagesAdded, pageWidth, pageHeight, bitmapPaint)
                } finally {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }

            if (pagesAdded == 0) {
                outputFile.delete()
                return false
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            outputFile.exists() && outputFile.length() > 0L
        } catch (e: Exception) {
            outputFile.delete()
            false
        } finally {
            document.close()
        }
    }

    fun savePdfToDownloads(context: Context, pdfFile: File, displayName: String = pdfFile.name): Uri? {
        if (!pdfFile.exists() || pdfFile.length() <= 0L) return null
        val resolver = context.contentResolver
        val safeName = if (displayName.endsWith(".pdf", ignoreCase = true)) displayName else "$displayName.pdf"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, safeName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/Midiastore")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val uri = resolver.insert(collection, values) ?: return null
        return try {
            val copiedBytes = resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(pdfFile).use { input ->
                    input.copyTo(out)
                }
            } ?: 0L
            if (copiedBytes <= 0L) {
                resolver.delete(uri, null, null)
                return null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    // =========================================================================
    // 5. GERADOR NATIVO DE QR CODE ESTILIZADO (PADRÃO OFICIAL ZXING ISO/IEC 18004)
    // =========================================================================
    enum class QrVisualTheme(
        val cardBgColor: Int,
        val moduleStartColor: Int,
        val moduleEndColor: Int,
        val borderColor: Int,
        val captionColor: Int,
    ) {
        CLASSIC_DARK(0xFFFFFFFF.toInt(), 0xFF0F172A.toInt(), 0xFF334155.toInt(), 0xFFCBD5E1.toInt(), 0xFF1E293B.toInt()),
        ROYAL_GOLD(0xFF130E03.toInt(), 0xFFFFD700.toInt(), 0xFFFF9800.toInt(), 0xFFFFB300.toInt(), 0xFFFDE68A.toInt()),
        CYBER_MINT(0xFF070B14.toInt(), 0xFF00E5B0.toInt(), 0xFF38BDF8.toInt(), 0xFF00E5B0.toInt(), 0xFF5DF5D3.toInt()),
        STUDIO_NEON(0xFF120E1E.toInt(), 0xFFE040FB.toInt(), 0xFFFF4081.toInt(), 0xFFE040FB.toInt(), 0xFFF5D0FE.toInt()),
        OCEAN_BLUE(0xFF0A192F.toInt(), 0xFF38BDF8.toInt(), 0xFF818CF8.toInt(), 0xFF38BDF8.toInt(), 0xFFBAE6FD.toInt()),
    }

    fun generateDesignerQrBitmap(
        content: String,
        theme: QrVisualTheme = QrVisualTheme.CYBER_MINT,
        caption: String? = null,
        size: Int = 720,
    ): Bitmap {
        val safeSize = size.coerceIn(256, 2048)
        val normalizedContent = content.trim()
            .ifEmpty { "https://midiastory-df6a0.web.app" }
            .take(MAX_QR_CONTENT_CHARS)
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        )
        val qrCode = try {
            Encoder.encode(normalizedContent, ErrorCorrectionLevel.M, hints)
        } catch (_: Exception) {
            Encoder.encode(normalizedContent.take(512), ErrorCorrectionLevel.M, hints)
        }
        val matrix = qrCode.matrix
        val gridSize = matrix.width.coerceAtLeast(1)
        val footerHeight = if (!caption.isNullOrBlank()) (safeSize * 0.12f).toInt() else (safeSize * 0.04f).toInt()
        val totalHeight = safeSize + footerHeight
        val bitmap = Bitmap.createBitmap(safeSize, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer rounded luxury card
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.cardBgColor }
        val cardRadius = safeSize * 0.06f
        val outerRect = RectF(0f, 0f, safeSize.toFloat(), totalHeight.toFloat())
        canvas.drawRoundRect(outerRect, cardRadius, cardRadius, bgPaint)

        val borderStroke = (safeSize * 0.012f).coerceAtLeast(3f)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderStroke
            color = theme.borderColor
        }
        canvas.drawRoundRect(
            RectF(borderStroke, borderStroke, safeSize - borderStroke, totalHeight - borderStroke),
            cardRadius - borderStroke,
            cardRadius - borderStroke,
            borderPaint,
        )

        val margin = safeSize * 0.10f
        val drawArea = safeSize - margin * 2f
        val cellSize = drawArea / gridSize.toFloat()

        val modulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                margin,
                margin,
                margin + drawArea,
                margin + drawArea,
                theme.moduleStartColor,
                theme.moduleEndColor,
                Shader.TileMode.CLAMP,
            )
        }

        val dotInset = cellSize * 0.08f
        val dotCorner = cellSize * 0.30f

        fun isFinderArea(x: Int, y: Int): Boolean {
            val inTopLeft = x < 7 && y < 7
            val inTopRight = x >= gridSize - 7 && y < 7
            val inBottomLeft = x < 7 && y >= gridSize - 7
            return inTopLeft || inTopRight || inBottomLeft
        }

        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                if (matrix.get(x, y).toInt() == 1 && !isFinderArea(x, y)) {
                    val left = margin + x * cellSize + dotInset
                    val top = margin + y * cellSize + dotInset
                    val right = margin + (x + 1) * cellSize - dotInset
                    val bottom = margin + (y + 1) * cellSize - dotInset
                    canvas.drawRoundRect(RectF(left, top, right, bottom), dotCorner, dotCorner, modulePaint)
                }
            }
        }

        // Stylized high-contrast rounded finder eyes (100% readable by scanners)
        fun drawFinderEye(startX: Int, startY: Int) {
            val fx = margin + startX * cellSize
            val fy = margin + startY * cellSize
            val box = 7f * cellSize
            val outerEyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = cellSize
                color = theme.moduleStartColor
            }
            canvas.drawRoundRect(
                RectF(fx + cellSize * 0.5f, fy + cellSize * 0.5f, fx + box - cellSize * 0.5f, fy + box - cellSize * 0.5f),
                cellSize * 1.4f,
                cellSize * 1.4f,
                outerEyePaint,
            )
            val innerEyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.moduleEndColor
            }
            canvas.drawRoundRect(
                RectF(fx + cellSize * 2f, fy + cellSize * 2f, fx + box - cellSize * 2f, fy + box - cellSize * 2f),
                cellSize * 0.8f,
                cellSize * 0.8f,
                innerEyePaint,
            )
        }
        drawFinderEye(0, 0)
        drawFinderEye(gridSize - 7, 0)
        drawFinderEye(0, gridSize - 7)

        val cleanCaption = caption?.trim()?.take(36)
        if (!cleanCaption.isNullOrEmpty()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.captionColor
                textSize = safeSize * 0.048f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                cleanCaption,
                safeSize / 2f,
                safeSize + footerHeight * 0.45f,
                textPaint,
            )
        }
        return bitmap
    }

    fun generateQrCodeBitmap(content: String, size: Int = 512): Bitmap {
        val safeSize = size.coerceIn(128, 2048)
        val normalizedContent = content.trim()
            .ifEmpty { "https://midiastory-df6a0.web.app" }
            .take(MAX_QR_CONTENT_CHARS)
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
        )
        val writer = QRCodeWriter()
        val bitMatrix: BitMatrix = try {
            writer.encode(normalizedContent, BarcodeFormat.QR_CODE, safeSize, safeSize, hints)
        } catch (_: Exception) {
            writer.encode(normalizedContent.take(512), BarcodeFormat.QR_CODE, safeSize, safeSize, hints)
        }
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                pixels[rowOffset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    // =========================================================================
    // 6. BAIXADOR REAL DE VÍDEOS POR LINK (MP4 & MÍDIA PÚBLICA)
    // =========================================================================
    data class DownloadResult(
        val success: Boolean,
        val savedUri: Uri? = null,
        val fileName: String? = null,
        val bytesDownloaded: Long = 0L,
        val errorMessage: String? = null
    )

    internal fun isLikelyHtmlOrInvalidMediaStream(
        prefixBytes: ByteArray,
        length: Int,
        contentType: String?,
    ): Boolean {
        if (length <= 0) return true
        val normalizedType = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        if (
            normalizedType.startsWith("text/") ||
            normalizedType == "application/xhtml+xml" ||
            normalizedType == "application/json" ||
            normalizedType == "application/xml" ||
            normalizedType == "application/javascript"
        ) {
            return true
        }
        val inspectLen = minOf(length, prefixBytes.size, 512)
        val sample = String(prefixBytes, 0, inspectLen, Charsets.ISO_8859_1).trimStart().lowercase()
        if (
            sample.startsWith("<!doctype") ||
            sample.startsWith("<html") ||
            sample.startsWith("<head") ||
            sample.startsWith("<body") ||
            sample.startsWith("<script") ||
            sample.startsWith("<meta") ||
            sample.startsWith("<?xml") ||
            sample.startsWith("{\"error\"") ||
            sample.contains("<!doctype html") ||
            sample.contains("<html")
        ) {
            return true
        }
        return false
    }

    internal fun extractDirectVideoUrlFromHtml(html: String, baseUrl: String? = null): String? {
        val patterns = listOf(
            Regex("""<meta[^>]+property=["']og:video(?::secure_url|:url)?["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:video(?::secure_url|:url)?["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+name=["']twitter:player:stream["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex(""""video_url"\s*:\s*"([^"]+)""""),
            Regex(""""playable_url(?:_quality_hd)?"\s*:\s*"([^"]+)""""),
            Regex(""""contentUrl"\s*:\s*"([^"]+\.mp4[^"]*)"""", RegexOption.IGNORE_CASE),
            Regex("""<source[^>]+src=["']([^"']+\.(?:mp4|webm|m4v)(?:\?[^"']*)?)["']""", RegexOption.IGNORE_CASE),
            Regex("""<video[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
        )
        for (regex in patterns) {
            val match = regex.find(html) ?: continue
            val raw = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (raw.isEmpty()) continue
            val decoded = unescapeHtml(
                raw.replace("\\u0026", "&")
                    .replace("\\/", "/")
                    .replace("&amp;", "&")
            ).trim()
            val resolved = resolveRelativeUrl(baseUrl, decoded)
            if (resolved.startsWith("http://") || resolved.startsWith("https://")) {
                return resolved
            }
        }
        return null
    }

    private fun resolveRelativeUrl(baseUrl: String?, candidate: String): String {
        if (candidate.startsWith("http://") || candidate.startsWith("https://")) return candidate
        if (baseUrl.isNullOrBlank()) return candidate
        return try {
            java.net.URL(java.net.URL(baseUrl), candidate).toString()
        } catch (_: Exception) {
            candidate
        }
    }

    private fun openConnectionFollowingRedirects(
        initialUrl: String,
        userAgent: String,
        connectTimeoutMs: Int = 15000,
        readTimeoutMs: Int = 30000,
    ): java.net.HttpURLConnection {
        var currentUrl = initialUrl
        repeat(MAX_HTTP_REDIRECTS) {
            val url = java.net.URL(currentUrl)
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "video/*,application/octet-stream;q=0.9,*/*;q=0.8")
            }
            conn.connect()
            val code = conn.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = conn.getHeaderField("Location")?.trim()
                conn.disconnect()
                if (location.isNullOrEmpty()) {
                    throw IllegalStateException("Redirecionamento HTTP $code sem cabeçalho Location.")
                }
                currentUrl = resolveRelativeUrl(currentUrl, location)
            } else {
                return conn
            }
        }
        throw IllegalStateException("Limite máximo de redirecionamentos HTTP atingido.")
    }

    fun resolveAndDownloadVideo(
        context: Context,
        rawUrl: String,
        shouldCancel: () -> Boolean = { false },
        onProgress: (percent: Int, bytesRead: Long, totalBytes: Long) -> Unit
    ): DownloadResult {
        return try {
            if (shouldCancel() || Thread.currentThread().isInterrupted) {
                return DownloadResult(false, errorMessage = "Download cancelado.")
            }
            val trimmedUrl = rawUrl.trim()
            if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                return DownloadResult(false, errorMessage = "URL inválida. Informe um link completo começando com https://")
            }
            val parsedHost = try {
                java.net.URL(trimmedUrl).host?.trim().orEmpty()
            } catch (_: Exception) {
                ""
            }
            if (parsedHost.isEmpty() || !parsedHost.contains('.')) {
                return DownloadResult(false, errorMessage = "URL incompleta. Informe um endereço válido (ex: https://exemplo.com/video.mp4).")
            }

            var directVideoUrl = trimmedUrl

            // Se for link conhecido de página social, tentar extrair a URL de vídeo direta primeiro
            if (trimmedUrl.contains("instagram.com") || trimmedUrl.contains("tiktok.com") ||
                trimmedUrl.contains("facebook.com") || trimmedUrl.contains("fb.watch") ||
                trimmedUrl.contains("threads.net")
            ) {
                val extracted = extractDirectVideoUrlFromPage(trimmedUrl)
                if (extracted != null) {
                    directVideoUrl = extracted
                } else {
                    return DownloadResult(
                        false,
                        errorMessage = "Não foi possível extrair a mídia deste link. Verifique se o conteúdo é público ou use o link direto MP4."
                    )
                }
            }

            if (shouldCancel() || Thread.currentThread().isInterrupted) {
                return DownloadResult(false, errorMessage = "Download cancelado.")
            }

            val mobileUa = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
            var connection = openConnectionFollowingRedirects(directVideoUrl, mobileUa)
            try {
                var responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    return DownloadResult(false, errorMessage = "Servidor retornou código HTTP $responseCode")
                }

                var contentType = connection.contentType?.lowercase() ?: ""
                if (contentType.contains("text/html") || contentType.contains("application/xhtml+xml")) {
                    val pageHtml = connection.inputStream.bufferedReader().use { reader ->
                        val sb = StringBuilder()
                        val charBuf = CharArray(4096)
                        var totalChars = 0
                        while (totalChars < 262_144) {
                            val r = reader.read(charBuf)
                            if (r <= 0) break
                            sb.append(charBuf, 0, r)
                            totalChars += r
                        }
                        sb.toString()
                    }
                    connection.disconnect()
                    val extractedFromHtml = extractDirectVideoUrlFromHtml(pageHtml, directVideoUrl)
                        ?: return DownloadResult(
                            false,
                            errorMessage = "O link fornecido retornou uma página web (HTML) sem arquivo de vídeo público direto."
                        )
                    directVideoUrl = extractedFromHtml
                    connection = openConnectionFollowingRedirects(directVideoUrl, mobileUa)
                    responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        return DownloadResult(false, errorMessage = "Servidor de mídia retornou código HTTP $responseCode")
                    }
                    contentType = connection.contentType?.lowercase() ?: ""
                }

                if (contentType.contains("text/html") || contentType.contains("text/plain")) {
                    return DownloadResult(false, errorMessage = "O link fornecido retornou uma página web (HTML/texto) em vez de um arquivo de vídeo direto.")
                }

                val totalBytes = connection.contentLengthLong
                val timestamp = System.currentTimeMillis()
                val fileName = "Midiastore_Video_$timestamp.mp4"
                val resolver = context.contentResolver

                connection.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    var firstRead = 0
                    while (firstRead < 512) {
                        val r = input.read(buffer, firstRead, buffer.size - firstRead)
                        if (r <= 0) break
                        firstRead += r
                    }

                    if (isLikelyHtmlOrInvalidMediaStream(buffer, firstRead, contentType)) {
                        return DownloadResult(
                            false,
                            errorMessage = "O servidor retornou HTML/texto ou mídia vazia em vez de um vídeo MP4 válido."
                        )
                    }

                    val contentValues = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Midiastore")
                            put(MediaStore.Video.Media.IS_PENDING, 1)
                        }
                    }

                    val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: return DownloadResult(false, errorMessage = "Erro ao alocar espaço na Galeria de Vídeos.")

                    var bytesReadTotal = 0L
                    try {
                        val outputStream = resolver.openOutputStream(videoUri)
                            ?: throw IllegalStateException("Não foi possível abrir o arquivo de vídeo na Galeria.")
                        outputStream.use { output ->
                            output.write(buffer, 0, firstRead)
                            bytesReadTotal += firstRead
                            val initialPercent = if (totalBytes > 0) ((bytesReadTotal * 100) / totalBytes).toInt() else -1
                            onProgress(initialPercent, bytesReadTotal, totalBytes)

                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                if (shouldCancel() || Thread.currentThread().isInterrupted) {
                                    throw java.util.concurrent.CancellationException("Download cancelado pelo usuário.")
                                }
                                output.write(buffer, 0, read)
                                bytesReadTotal += read
                                val percent = if (totalBytes > 0) ((bytesReadTotal * 100) / totalBytes).toInt() else -1
                                onProgress(percent, bytesReadTotal, totalBytes)
                            }
                            output.flush()
                        }

                        if (bytesReadTotal <= 0L) {
                            resolver.delete(videoUri, null, null)
                            return DownloadResult(false, errorMessage = "O download retornou 0 bytes.")
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                            resolver.update(videoUri, contentValues, null, null)
                        }

                        return DownloadResult(
                            success = true,
                            savedUri = videoUri,
                            fileName = fileName,
                            bytesDownloaded = bytesReadTotal
                        )
                    } catch (e: java.util.concurrent.CancellationException) {
                        resolver.delete(videoUri, null, null)
                        return DownloadResult(false, errorMessage = "Download cancelado.")
                    } catch (e: Exception) {
                        resolver.delete(videoUri, null, null)
                        return DownloadResult(false, errorMessage = "Falha durante o download: ${e.message}")
                    }
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            DownloadResult(false, errorMessage = "Erro ao conectar: ${e.message}")
        }
    }

    private fun extractDirectVideoUrlFromPage(pageUrl: String): String? {
        var connection: java.net.HttpURLConnection? = null
        return try {
            val desktopUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            connection = openConnectionFollowingRedirects(pageUrl, desktopUa, connectTimeoutMs = 10000, readTimeoutMs = 15000)
            if (connection.responseCode in 200..299) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                return extractDirectVideoUrlFromHtml(html, pageUrl)
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun unescapeHtml(input: String): String {
        return input.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }

    private const val MAX_QR_CONTENT_CHARS = 1800
    private const val MAX_HTTP_REDIRECTS = 5
}
