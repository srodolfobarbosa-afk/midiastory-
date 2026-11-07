package com.rdf112.sweep

import android.app.Activity
import android.content.IntentSender
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Debug-only host for Android's real media-operation consent UI in the target app UID. */
class MediaOperationConsentHostActivity : ComponentActivity() {
    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        consentResultCode = result.resultCode
        consentCompleted.countDown()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun requestConsent(intentSender: IntentSender) {
        consentLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
    }

    companion object {
        @Volatile
        var consentResultCode: Int? = null
            private set

        private var consentCompleted = CountDownLatch(1)

        fun resetConsentResult() {
            consentResultCode = null
            consentCompleted = CountDownLatch(1)
        }

        fun awaitConsentResult(timeout: Long, unit: TimeUnit): Boolean =
            consentCompleted.await(timeout, unit)
    }
}
