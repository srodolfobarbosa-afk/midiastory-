package com.rdf112.sweep.presentation.ui

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

internal suspend fun playLocalizedMariaSpeech(
    context: Context,
    @RawRes rawAudioResId: Int,
    text: String,
    onAudioPlayerCreated: (MediaPlayer) -> Unit,
): Boolean {
    if (MariaI18n.isPt()) {
        val player = runCatching { MediaPlayer.create(context, rawAudioResId) }.getOrNull() ?: return false
        onAudioPlayerCreated(player)
        return suspendCancellableCoroutine { continuation ->
            player.setOnCompletionListener { if (continuation.isActive) continuation.resume(true) }
            player.setOnErrorListener { _, _, _ ->
                if (continuation.isActive) continuation.resume(false)
                true
            }
            continuation.invokeOnCancellation {
                runCatching { player.stop() }
                runCatching { player.release() }
            }
            runCatching { player.start() }.onFailure {
                if (continuation.isActive) continuation.resume(false)
            }
        }.also { runCatching { player.release() } }
    }

    val requestedLocale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()
    return suspendCancellableCoroutine { continuation ->
        var engine: TextToSpeech? = null
        val utteranceId = "maria-${UUID.randomUUID()}"
        continuation.invokeOnCancellation { engine?.let { runCatching { it.stop() }; it.shutdown() } }
        engine = TextToSpeech(context) { status ->
            val tts = engine ?: return@TextToSpeech
            if (!continuation.isActive) {
                tts.shutdown()
                return@TextToSpeech
            }
            if (status != TextToSpeech.SUCCESS) {
                continuation.resume(false)
                tts.shutdown()
                return@TextToSpeech
            }
            tts.language = requestedLocale
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                private fun finish(success: Boolean) {
                    if (continuation.isActive) continuation.resume(success)
                    tts.shutdown()
                }
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) = finish(true)
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) = finish(false)
                override fun onError(id: String?, errorCode: Int) = finish(false)
            })
            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.ERROR) {
                if (continuation.isActive) continuation.resume(false)
                tts.shutdown()
            }
        }
    }
}
