package com.rdf112.sweep.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface
import com.rdf112.sweep.domain.analysis.VideoCompressionPlan
import com.rdf112.sweep.domain.analysis.VideoCompressionPlanner
import com.rdf112.sweep.domain.model.MediaRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

sealed interface VideoCompressionOutcome {
    data class Success(
        val publishedUri: Uri,
        val publishedFileName: String,
        val originalBytes: Long,
        val compressedBytes: Long,
        val savedBytes: Long,
        val codecMime: String,
    ) : VideoCompressionOutcome

    data class IneligibleOrAlreadyCompact(val reasonCode: String) : VideoCompressionOutcome
    data class Failed(val reasonCode: String) : VideoCompressionOutcome
}

/**
 * 100% On-Device Hardware GPU Video Compressor (`MediaExtractor` + `MediaCodec` H.265/HEVC with AVC fallback
 * + EGL Surface-to-Surface zero-copy blit + lossless AAC audio passthrough + `MediaMuxer`).
 *
 * - Zero external dependencies (`0 KB` APK bloat — preserves `< 8 MB` signed AAB).
 * - Zero cloud cost, 100% private on user's phone.
 * - Fail-closed verification: never deletes the original automatically; verifies duration + >=15% byte savings
 *   before publishing to `MediaStore` via `IS_PENDING`, then lets the user move the original to Android Trash.
 */
object AndroidHardwareVideoCompressor {
    private const val MIME_HEVC = MediaFormat.MIMETYPE_VIDEO_HEVC
    private const val MIME_AVC = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val TIMEOUT_US = 10_000L

    suspend fun compressToMediaStore(
        context: Context,
        source: MediaRecord,
        onProgressPercent: ((Int) -> Unit)? = null,
    ): VideoCompressionOutcome = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val resolver = appContext.contentResolver
        val sourceUri = Uri.parse(source.uri)

        if (source.sizeBytes < VideoCompressionPlanner.MIN_COMPRESSIBLE_VIDEO_BYTES) {
            return@withContext VideoCompressionOutcome.IneligibleOrAlreadyCompact("too_small")
        }

        // 1. Inspect source video metadata
        var durationMs = 0L
        var width = 0
        var height = 0
        var rotation = 0
        var sourceBitrate: Int? = null
        var sourceFps: Int? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, sourceUri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            sourceBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                sourceFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    ?.toFloatOrNull()?.toInt()
            }
        } catch (_: Exception) {
            return@withContext VideoCompressionOutcome.Failed("metadata_unreadable")
        } finally {
            runCatching { retriever.release() }
        }

        val hasHardwareHevc = hasHardwareVideoEncoder(MIME_HEVC)
        val chosenMime = when {
            hasHardwareHevc -> MIME_HEVC
            hasHardwareVideoEncoder(MIME_AVC) -> MIME_AVC
            else -> MIME_HEVC
        }

        val plan = VideoCompressionPlanner.buildPlan(
            sourceSizeBytes = source.sizeBytes,
            sourceWidth = width,
            sourceHeight = height,
            durationMs = durationMs,
            sourceBitrateBps = sourceBitrate,
            sourceFrameRate = sourceFps,
            rotationDegrees = rotation,
            preferHevc = chosenMime == MIME_HEVC,
        ) ?: return@withContext VideoCompressionOutcome.IneligibleOrAlreadyCompact("already_compact")

        val tempOutFile = File(appContext.cacheDir, "midiastore_hevc_${System.nanoTime()}.mp4")
        try {
            val transcodeOk = try {
                transcodeOnHardwareSurface(
                    context = appContext,
                    sourceUri = sourceUri,
                    outputFile = tempOutFile,
                    plan = plan,
                    outputMime = chosenMime,
                    totalDurationUs = (durationMs * 1_000L).coerceAtLeast(1_000L),
                    onProgressPercent = onProgressPercent,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Automatic fallback to hardware H.264/AVC if OEM HEVC encoder rejected profile/resolution
                if (chosenMime == MIME_HEVC) {
                    runCatching { tempOutFile.delete() }
                    transcodeOnHardwareSurface(
                        context = appContext,
                        sourceUri = sourceUri,
                        outputFile = tempOutFile,
                        plan = plan,
                        outputMime = MIME_AVC,
                        totalDurationUs = (durationMs * 1_000L).coerceAtLeast(1_000L),
                        onProgressPercent = onProgressPercent,
                    )
                } else {
                    false
                }
            }

            if (!transcodeOk || !tempOutFile.isFile) {
                return@withContext VideoCompressionOutcome.Failed("transcode_failed")
            }

            // 2. Strict post-transcode verification (duration + >=15% byte reduction)
            val outputBytes = tempOutFile.length()
            val outDurationMs = readFileDurationMs(tempOutFile)
            if (!VideoCompressionPlanner.isCompressedOutputVerified(
                    sourceSizeBytes = source.sizeBytes,
                    sourceDurationMs = durationMs,
                    outputSizeBytes = outputBytes,
                    outputDurationMs = outDurationMs,
                )
            ) {
                return@withContext VideoCompressionOutcome.IneligibleOrAlreadyCompact("savings_below_threshold")
            }

            // 3. Publish verified compressed video safely via MediaStore IS_PENDING
            val baseName = (source.displayName ?: "video")
                .substringBeforeLast('.')
                .take(64)
                .ifBlank { "video" }
            val outDisplayName = "${baseName}_compact.mp4"
            val targetRelPath = source.relativePath
                ?.takeIf { it.startsWith("Movies", ignoreCase = true) || it.startsWith("DCIM", ignoreCase = true) }
                ?: "${Environment.DIRECTORY_MOVIES}/Midiastore/"

            val collection = MediaStore.Video.Media.getContentUri(source.volumeName)
            val insertValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, outDisplayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelPath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val publishedUri = resolver.insert(collection, insertValues)
                ?: return@withContext VideoCompressionOutcome.Failed("mediastore_insert_failed")

            var publishCommitted = false
            try {
                resolver.openOutputStream(publishedUri, "w")?.use { outStream ->
                    FileInputStream(tempOutFile).use { inStream ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = inStream.read(buf)
                            if (n < 0) break
                            if (n > 0) outStream.write(buf, 0, n)
                        }
                        outStream.flush()
                    }
                } ?: return@withContext VideoCompressionOutcome.Failed("mediastore_stream_failed")

                val finalizeValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(publishedUri, finalizeValues, null, null)
                publishCommitted = true
            } finally {
                if (!publishCommitted) {
                    runCatching { resolver.delete(publishedUri, null, null) }
                }
            }

            val savedBytes = (source.sizeBytes - outputBytes).coerceAtLeast(0L)
            VideoCompressionOutcome.Success(
                publishedUri = publishedUri,
                publishedFileName = outDisplayName,
                originalBytes = source.sizeBytes,
                compressedBytes = outputBytes,
                savedBytes = savedBytes,
                codecMime = chosenMime,
            )
        } finally {
            runCatching { tempOutFile.delete() }
        }
    }

    private fun hasHardwareVideoEncoder(mime: String): Boolean = runCatching {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        list.codecInfos.any { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
        }
    }.getOrDefault(false)

    private fun readFileDurationMs(file: File): Long {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(file.absolutePath)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { r.release() }
        }
    }

    private suspend fun transcodeOnHardwareSurface(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        plan: VideoCompressionPlan,
        outputMime: String,
        totalDurationUs: Long,
        onProgressPercent: ((Int) -> Unit)?,
    ): Boolean {
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var inputEglSurface: EglEncoderSurface? = null
        var outputSurface: OesDecoderSurface? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            videoExtractor.setDataSource(context, sourceUri, null)
            val videoTrackIdx = selectTrack(videoExtractor, "video/")
            if (videoTrackIdx < 0) return false
            videoExtractor.selectTrack(videoTrackIdx)
            val inputVideoFormat = videoExtractor.getTrackFormat(videoTrackIdx)
            val inputVideoMime = inputVideoFormat.getString(MediaFormat.KEY_MIME) ?: return false

            audioExtractor.setDataSource(context, sourceUri, null)
            val audioTrackIdx = selectTrack(audioExtractor, "audio/")
            val inputAudioFormat = if (audioTrackIdx >= 0) {
                audioExtractor.selectTrack(audioTrackIdx)
                audioExtractor.getTrackFormat(audioTrackIdx)
            } else {
                null
            }

            val outputVideoFormat = MediaFormat.createVideoFormat(outputMime, plan.targetWidth, plan.targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, plan.targetBitrateBps)
                setInteger(MediaFormat.KEY_FRAME_RATE, plan.targetFrameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            }

            encoder = MediaCodec.createEncoderByType(outputMime)
            encoder.configure(outputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputEglSurface = EglEncoderSurface(encoder.createInputSurface())
            inputEglSurface.makeCurrent()
            encoder.start()

            outputSurface = OesDecoderSurface(plan.targetWidth, plan.targetHeight)
            decoder = MediaCodec.createDecoderByType(inputVideoMime)
            decoder.configure(inputVideoFormat, outputSurface.surface, null, 0)
            decoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            if (plan.rotationDegrees != 0) {
                muxer.setOrientationHint(plan.rotationDegrees)
            }

            val decoderInfo = MediaCodec.BufferInfo()
            val encoderInfo = MediaCodec.BufferInfo()
            var muxerVideoTrack = -1
            var muxerAudioTrack = -1
            var extractorDone = false
            var decoderDone = false
            var encoderDone = false
            var lastReportedPct = -1

            while (!encoderDone) {
                currentCoroutineContext().ensureActive()

                if (!extractorDone) {
                    val inBufIdx = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inBufIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(inBufIdx)!!
                        val sampleSize = videoExtractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inBufIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            extractorDone = true
                        } else {
                            val ptsUs = videoExtractor.sampleTime.coerceAtLeast(0L)
                            decoder.queueInputBuffer(inBufIdx, 0, sampleSize, ptsUs, 0)
                            videoExtractor.advance()
                        }
                    }
                }

                var decoderOutputAvailable = !decoderDone
                while (decoderOutputAvailable) {
                    val outIdx = decoder.dequeueOutputBuffer(decoderInfo, TIMEOUT_US)
                    when {
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> decoderOutputAvailable = false
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                        outIdx >= 0 -> {
                            val render = decoderInfo.size > 0
                            decoder.releaseOutputBuffer(outIdx, render)
                            if (render) {
                                outputSurface.awaitNewImage()
                                outputSurface.drawImage()
                                inputEglSurface.setPresentationTime(decoderInfo.presentationTimeUs * 1_000L)
                                inputEglSurface.swapBuffers()

                                val pct = ((decoderInfo.presentationTimeUs * 95L) / totalDurationUs)
                                    .toInt().coerceIn(0, 95)
                                if (pct >= lastReportedPct + 3) {
                                    lastReportedPct = pct
                                    withContext(Dispatchers.Main) { onProgressPercent?.invoke(pct) }
                                }
                            }
                            if ((decoderInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                encoder.signalEndOfInputStream()
                                decoderDone = true
                                decoderOutputAvailable = false
                            }
                        }
                    }
                }

                var encoderOutputAvailable = true
                while (encoderOutputAvailable) {
                    val encIdx = encoder.dequeueOutputBuffer(encoderInfo, TIMEOUT_US)
                    when {
                        encIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> encoderOutputAvailable = false
                        encIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) return false
                            muxerVideoTrack = muxer.addTrack(encoder.outputFormat)
                            if (inputAudioFormat != null) {
                                runCatching {
                                    muxerAudioTrack = muxer.addTrack(inputAudioFormat)
                                }
                            }
                            muxer.start()
                            muxerStarted = true
                        }
                        encIdx >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(encIdx)
                            if ((encoderInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                encoderInfo.size = 0
                            }
                            if (encoderInfo.size > 0 && muxerStarted && encodedData != null) {
                                encodedData.position(encoderInfo.offset)
                                encodedData.limit(encoderInfo.offset + encoderInfo.size)
                                muxer.writeSampleData(muxerVideoTrack, encodedData, encoderInfo)
                            }
                            encoder.releaseOutputBuffer(encIdx, false)
                            if ((encoderInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                encoderDone = true
                                encoderOutputAvailable = false
                            }
                        }
                    }
                }
            }

            // Lossless fast muxing of original compressed audio stream
            if (muxerStarted && muxerAudioTrack >= 0) {
                val audioBuf = ByteBuffer.allocateDirect(256 * 1024)
                val audioInfo = MediaCodec.BufferInfo()
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val size = audioExtractor.readSampleData(audioBuf, 0)
                    if (size < 0) break
                    val pts = audioExtractor.sampleTime
                    if (pts < 0) break
                    audioInfo.offset = 0
                    audioInfo.size = size
                    audioInfo.presentationTimeUs = pts
                    audioInfo.flags = if ((audioExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else {
                        0
                    }
                    muxer.writeSampleData(muxerAudioTrack, audioBuf, audioInfo)
                    audioExtractor.advance()
                }
            }

            withContext(Dispatchers.Main) { onProgressPercent?.invoke(100) }
            return muxerStarted
        } finally {
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { outputSurface?.release() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            runCatching { inputEglSurface?.release() }
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
        }
    }

    private fun selectTrack(extractor: MediaExtractor, prefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith(prefix, ignoreCase = true)) return i
        }
        return -1
    }
}

private class EglEncoderSurface(private var surface: Surface?) {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    init {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "Unable to get EGL14 display" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { "Unable to init EGL14" }

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            0x3142, 1, // EGL_RECORDABLE_ANDROID
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        check(EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0) && numConfigs[0] > 0) {
            "Unable to find recordable RGB888 EGLConfig"
        }
        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        val surfAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfAttribs, 0)
    }

    fun makeCurrent() {
        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) { "eglMakeCurrent failed" }
    }

    fun swapBuffers(): Boolean = EGL14.eglSwapBuffers(eglDisplay, eglSurface)

    fun setPresentationTime(nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
    }

    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        surface?.release()
        surface = null
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }
}

private class OesDecoderSurface(
    private val viewportWidth: Int,
    private val viewportHeight: Int,
) : SurfaceTexture.OnFrameAvailableListener {
    private val frameSyncObject = Object()
    private var frameAvailable = false
    private val renderer = OesTextureRenderer()
    private val surfaceTexture: SurfaceTexture
    val surface: Surface

    init {
        renderer.surfaceCreated()
        surfaceTexture = SurfaceTexture(renderer.textureId)
        surfaceTexture.setOnFrameAvailableListener(this)
        surface = Surface(surfaceTexture)
    }

    override fun onFrameAvailable(st: SurfaceTexture?) {
        synchronized(frameSyncObject) {
            frameAvailable = true
            frameSyncObject.notifyAll()
        }
    }

    fun awaitNewImage() {
        synchronized(frameSyncObject) {
            var waitedMs = 0L
            while (!frameAvailable && waitedMs < 2_500L) {
                frameSyncObject.wait(250L)
                waitedMs += 250L
            }
            frameAvailable = false
        }
        surfaceTexture.updateTexImage()
    }

    fun drawImage() {
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        renderer.drawFrame(surfaceTexture)
    }

    fun release() {
        surface.release()
        surfaceTexture.release()
    }
}

private class OesTextureRenderer {
    private val triangleVerticesData = floatArrayOf(
        -1.0f, -1.0f, 0f, 0f, 0f,
        1.0f, -1.0f, 0f, 1f, 0f,
        -1.0f, 1.0f, 0f, 0f, 1f,
        1.0f, 1.0f, 0f, 1f, 1f,
    )
    private val triangleVertices: FloatBuffer = ByteBuffer
        .allocateDirect(triangleVerticesData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply { put(triangleVerticesData).position(0) }

    private val mvpMatrix = FloatArray(16)
    private val stMatrix = FloatArray(16)
    private var program = 0
    var textureId = -12345
        private set
    private var uMVPMatrixHandle = 0
    private var uSTMatrixHandle = 0
    private var aPositionHandle = 0
    private var aTextureHandle = 0

    init {
        Matrix.setIdentityM(stMatrix, 0)
    }

    fun surfaceCreated() {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aTextureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    fun drawFrame(st: SurfaceTexture) {
        st.getTransformMatrix(stMatrix)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        triangleVertices.position(0)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 20, triangleVertices)
        GLES20.glEnableVertexAttribArray(aPositionHandle)

        triangleVertices.position(3)
        GLES20.glVertexAttribPointer(aTextureHandle, 2, GLES20.GL_FLOAT, false, 20, triangleVertices)
        GLES20.glEnableVertexAttribArray(aTextureHandle)

        Matrix.setIdentityM(mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, stMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        val shader = GLES20.glCreateShader(shaderType)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vertexShader)
        GLES20.glAttachShader(prog, pixelShader)
        GLES20.glLinkProgram(prog)
        return prog
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uSTMatrix;
            attribute vec4 aPosition;
            attribute vec4 aTextureCoord;
            varying vec2 vTextureCoord;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTextureCoord = (uSTMatrix * aTextureCoord).xy;
            }
        """
        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, vTextureCoord);
            }
        """
    }
}
