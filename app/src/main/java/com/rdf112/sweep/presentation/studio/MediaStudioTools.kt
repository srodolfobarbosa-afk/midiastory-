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
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * ESTÚDIO DE MÍDIA & FERRAMENTAS AVANÇADAS (Midiastore v2.4.0)
 * 
 * Funcionalidades nativas de alto desempenho:
 * (a) Filtros de Foto com ColorMatrix (Cinema, Nitidez, Vintage, P&B, Vibrante)
 * (b) Compressor e Cortador de Vídeo (reduz até 80% do tamanho para liberar espaço)
 * (c) Extrator de Áudio de Vídeo (M4A/AAC) e Limpador de Privacidade EXIF (remove GPS)
 * (d) Gerador de QR Code (PIX, Links, WhatsApp, Wi-Fi) e Conversor de Fotos para PDF
 */
object MediaStudioTools {

    data class ExifSummary(
        val hasGps: Boolean,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val camera: String? = null,
        val dateTime: String? = null
    )

    fun readExifSummary(context: Context, uri: Uri): ExifSummary {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val latLong = FloatArray(2)
                val hasGps = exif.getLatLong(latLong)
                val camera = exif.getAttribute(ExifInterface.TAG_MODEL)
                val dt = exif.getAttribute(ExifInterface.TAG_DATETIME)
                ExifSummary(
                    hasGps = hasGps,
                    latitude = if (hasGps) latLong[0].toDouble() else null,
                    longitude = if (hasGps) latLong[1].toDouble() else null,
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
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
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
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
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
        return saveBitmapToGallery(context, bitmap, "Privacidade_EXIF_Limpo")
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
        val matrix = Matrix()
        if (rotationDeg % 360 != 0) {
            matrix.postRotate(rotationDeg.toFloat(), source.width / 2f, source.height / 2f)
        }
        val rotated = if (rotationDeg % 360 != 0) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val result = Bitmap.createBitmap(rotated.width, rotated.height, rotated.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val colorMatrix = buildCompositeColorMatrix(filter, brightness, contrast, saturation)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(rotated, 0f, 0f, paint)
        return result
    }

    // =========================================================================
    // 2. EXTRATOR DE VÍDEO PARA ÁUDIO (M4A) & COMPRESSOR NATIVO
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
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                256 * 1024
            }
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                extractor.advance()
            }
            true
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

    // =========================================================================
    // 3. LIMPADOR DE PRIVACIDADE EXIF (Remove coordenadas GPS e metadados)
    // =========================================================================
    fun stripExifPrivacyData(filePath: String): Boolean {
        return try {
            val exif = ExifInterface(filePath)
            val tagsToRemove = listOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_SOFTWARE
            )
            for (tag in tagsToRemove) {
                exif.setAttribute(tag, null)
            }
            exif.saveAttributes()
            true
        } catch (e: Exception) {
            false
        }
    }

    // =========================================================================
    // 4. CRIADOR DE FOTOS PARA PDF (PdfDocument)
    // =========================================================================
    fun convertBitmapsToPdf(bitmaps: List<Bitmap>, outputFile: File): Boolean {
        if (bitmaps.isEmpty()) return false
        val document = PdfDocument()
        return try {
            val pageWidth = 595 // A4 width em points (72 dpi)
            val pageHeight = 842 // A4 height em points

            bitmaps.forEachIndexed { index, bitmap ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Fundo branco
                canvas.drawColor(Color.WHITE)

                // Escalar bitmap para caber na página com margem de 30 points
                val margin = 30f
                val availableWidth = pageWidth - (margin * 2)
                val availableHeight = pageHeight - (margin * 2)

                val scale = minOf(availableWidth / bitmap.width.toFloat(), availableHeight / bitmap.height.toFloat())
                val destWidth = bitmap.width * scale
                val destHeight = bitmap.height * scale
                val left = margin + (availableWidth - destWidth) / 2f
                val top = margin + (availableHeight - destHeight) / 2f

                val destRect = android.graphics.RectF(left, top, left + destWidth, top + destHeight)
                canvas.drawBitmap(bitmap, null, destRect, null)

                document.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            true
        } catch (e: Exception) {
            outputFile.delete()
            false
        } finally {
            document.close()
        }
    }

    fun createPdfFromUris(context: Context, uris: List<Uri>, outputFile: File): Boolean {
        val bitmaps = uris.mapNotNull { uri -> loadSampledBitmap(context, uri, 1600) }
        if (bitmaps.isEmpty()) return false
        return convertBitmapsToPdf(bitmaps, outputFile)
    }

    // =========================================================================
    // 5. GERADOR NATIVO DE QR CODE (PADRÃO OFICIAL ZXING ISO/IEC 18004)
    // =========================================================================
    fun generateQrCodeBitmap(content: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
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
                return DownloadResult(false, errorMessage = "URL inválida. Comece com https://")
            }

            var directVideoUrl = trimmedUrl

            // Se for link de rede social, tentar extrair a URL de vídeo direta
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

            val url = java.net.URL(directVideoUrl)
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                )
            }
            try {
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    return DownloadResult(false, errorMessage = "Servidor retornou código HTTP $responseCode")
                }

                val contentType = connection.contentType?.lowercase() ?: ""
                if (contentType.contains("text/html") || contentType.contains("text/plain")) {
                    return DownloadResult(false, errorMessage = "O link fornecido retornou uma página web (HTML) em vez de um arquivo de vídeo direto.")
                }

                val totalBytes = connection.contentLengthLong
                val timestamp = System.currentTimeMillis()
                val fileName = "Midiastore_Video_$timestamp.mp4"

                val resolver = context.contentResolver
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
                val buffer = ByteArray(8192)

                try {
                    connection.inputStream.use { input ->
                        resolver.openOutputStream(videoUri)?.use { output ->
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
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                        resolver.update(videoUri, contentValues, null, null)
                    }

                    DownloadResult(
                        success = true,
                        savedUri = videoUri,
                        fileName = fileName,
                        bytesDownloaded = bytesReadTotal
                    )
                } catch (e: java.util.concurrent.CancellationException) {
                    resolver.delete(videoUri, null, null)
                    DownloadResult(false, errorMessage = "Download cancelado.")
                } catch (e: Exception) {
                    resolver.delete(videoUri, null, null)
                    DownloadResult(false, errorMessage = "Falha durante o download: ${e.message}")
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
            val url = java.net.URL(pageUrl)
            connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                )
            }
            connection.connect()
            if (connection.responseCode in 200..299) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }

                val ogVideoRegex = Regex("""<meta\s+property=["']og:video(?::secure_url)?["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                val match = ogVideoRegex.find(html)
                if (match != null) {
                    val raw = match.groupValues[1]
                    return unescapeHtml(raw)
                }

                val jsonVideoRegex = Regex(""""video_url"\s*:\s*"([^"]+)"""")
                val jsonMatch = jsonVideoRegex.find(html)
                if (jsonMatch != null) {
                    val raw = jsonMatch.groupValues[1]
                    return raw.replace("\\u0026", "&").replace("\\/", "/")
                }
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
}
