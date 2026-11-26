package com.rdf112.sweep.presentation.ui

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/** One application-scoped, pre-warmed TTS engine shared by every Maria tab. */
internal object MariaLocalizedSpeech {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = CompletableDeferred<Boolean>()
    private var activeId: String? = null
    private var activeContinuation: kotlinx.coroutines.CancellableContinuation<Boolean>? = null
    private var activeOnStart: (() -> Unit)? = null
    private var activeOnFinish: (() -> Unit)? = null

    fun warm(context: Context) {
        main.post { initialize(context.applicationContext) }
    }

    fun stop() {
        val stopNow = {
            val continuation = activeContinuation
            val onFinish = activeOnFinish
            clearActive()
            runCatching { tts?.stop() }
            onFinish?.invoke()
            if (continuation?.isActive == true) continuation.resume(false)
        }
        if (Looper.myLooper() == Looper.getMainLooper()) stopNow() else main.post(stopNow)
    }

    suspend fun speak(context: Context, text: String, onStart: () -> Unit, onFinish: () -> Unit): Boolean {
        val contextReady = withContext(Dispatchers.Main.immediate) {
            initializeResult(context)
        }.await()
        if (!contextReady) return false

        val requestedLocale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()
        return withContext(Dispatchers.Main.immediate) {
            val engine = tts ?: return@withContext false
            suspendCancellableCoroutine { continuation ->
                val id = "maria-${UUID.randomUUID()}"
                activeId = id
                activeContinuation = continuation
                activeOnStart = onStart
                activeOnFinish = onFinish
                continuation.invokeOnCancellation {
                    main.post {
                        if (activeId == id) stop()
                    }
                }

                // Resolve the active app locale immediately before each utterance.
                runCatching { engine.setLanguage(requestedLocale) }
                val result = runCatching {
                    engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
                }.getOrDefault(TextToSpeech.ERROR)
                if (result == TextToSpeech.ERROR) finish(id, false)
            }
        }
    }

    private fun initializeResult(context: Context): CompletableDeferred<Boolean> {
        if (tts == null && !ready.isCompleted) initialize(context.applicationContext)
        return ready
    }

    private fun initialize(context: Context?) {
        if (tts != null || ready.isCompleted) return
        if (context == null) return
        ready = CompletableDeferred()
        val engine = runCatching {
            TextToSpeech(context.applicationContext) { status ->
                main.post {
                    if (status == TextToSpeech.SUCCESS) {
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                utteranceId?.let { id -> main.post { if (activeId == id) activeOnStart?.invoke() } }
                            }

                            override fun onDone(utteranceId: String?) {
                                utteranceId?.let { id -> main.post { finish(id, true) } }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                utteranceId?.let { id -> main.post { finish(id, false) } }
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                utteranceId?.let { id -> main.post { finish(id, false) } }
                            }

                            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                                utteranceId?.let { id -> main.post { finish(id, false) } }
                            }
                        })
                        ready.complete(true)
                    } else {
                        ready.complete(false)
                    }
                }
            }
        }.getOrNull()
        if (engine == null) {
            ready.complete(false)
        } else {
            tts = engine
        }
    }

    private fun finish(id: String, success: Boolean) {
        if (activeId != id) return
        val continuation = activeContinuation
        val onFinish = activeOnFinish
        clearActive()
        onFinish?.invoke()
        if (continuation?.isActive == true) continuation.resume(success)
    }

    private fun clearActive() {
        activeId = null
        activeContinuation = null
        activeOnStart = null
        activeOnFinish = null
    }
}

internal suspend fun playLocalizedMariaSpeech(
    context: Context,
    @RawRes rawAudioResId: Int,
    text: String,
    onAudioPlayerCreated: (MediaPlayer) -> Unit,
    onAudioStarted: () -> Unit,
    onAudioFinished: () -> Unit,
): Boolean {
    if (MariaI18n.isPt()) {
        val player = runCatching { MediaPlayer.create(context, rawAudioResId) }.getOrNull() ?: return false
        onAudioPlayerCreated(player)
        return suspendCancellableCoroutine { continuation ->
            var completed = false
            fun finish(success: Boolean) {
                if (completed) return
                completed = true
                onAudioFinished()
                if (continuation.isActive) continuation.resume(success)
            }
            player.setOnCompletionListener { finish(true) }
            player.setOnErrorListener { _, _, _ ->
                finish(false)
                true
            }
            continuation.invokeOnCancellation {
                runCatching { player.stop() }
                runCatching { player.release() }
                onAudioFinished()
            }
            runCatching {
                player.start()
                onAudioStarted()
            }.onFailure { finish(false) }
        }.also { runCatching { player.release() } }
    }

    return MariaLocalizedSpeech.speak(context, text, onAudioStarted, onAudioFinished)
}
