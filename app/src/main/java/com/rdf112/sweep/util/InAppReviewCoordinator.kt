package com.rdf112.sweep.util

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Gatilho Inteligente do Google Play In-App Review API:
 * Dispara o diálogo oficial 5 estrelas do Google Play sem tirar o usuário do app,
 * exclusivamente no momento de alívio real pós-limpeza confirmada, respeitando cooldown
 * de 7 dias para maximizar conversão sem fadiga.
 */
class InAppReviewCoordinator(private val activity: Activity) {
    private val reviewManager = ReviewManagerFactory.create(activity)
    private val prefs = activity.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var hasRequestedInCurrentSession = false

    fun requestReviewIfAppropriate(
        confirmedFreedBytes: Long,
        lifetimeConfirmedFreedBytes: Long = confirmedFreedBytes,
    ): Boolean {
        if (!shouldTriggerReviewPrompt(
                confirmedFreedBytes = confirmedFreedBytes,
                lifetimeConfirmedFreedBytes = lifetimeConfirmedFreedBytes,
                alreadyRequestedInSession = hasRequestedInCurrentSession,
                lastPromptEpochMs = prefs.getLong(KEY_LAST_PROMPT_MS, 0L),
                nowEpochMs = System.currentTimeMillis(),
            )
        ) {
            return false
        }
        hasRequestedInCurrentSession = true
        prefs.edit().putLong(KEY_LAST_PROMPT_MS, System.currentTimeMillis()).apply()

        val requestTask = reviewManager.requestReviewFlow()
        requestTask.addOnCompleteListener { task ->
            if (task.isSuccessful && !activity.isFinishing && !activity.isDestroyed) {
                val reviewInfo = task.result
                reviewManager.launchReviewFlow(activity, reviewInfo)
            }
        }
        return true
    }

    companion object {
        private const val PREFS_NAME = "midiastore_in_app_review_prefs"
        private const val KEY_LAST_PROMPT_MS = "last_review_prompt_ms"
        const val MIN_SINGLE_CLEANUP_BYTES: Long = 5L * 1024L * 1024L // 5 MB
        const val MIN_LIFETIME_CLEANUP_BYTES: Long = 25L * 1024L * 1024L // 25 MB
        const val COOLDOWN_MILLIS: Long = 7L * 24L * 60L * 60L * 1_000L // 7 days

        fun shouldTriggerReviewPrompt(
            confirmedFreedBytes: Long,
            lifetimeConfirmedFreedBytes: Long,
            alreadyRequestedInSession: Boolean,
            lastPromptEpochMs: Long,
            nowEpochMs: Long,
        ): Boolean {
            if (confirmedFreedBytes <= 0L || alreadyRequestedInSession) return false
            val meetsThreshold = confirmedFreedBytes >= MIN_SINGLE_CLEANUP_BYTES ||
                lifetimeConfirmedFreedBytes >= MIN_LIFETIME_CLEANUP_BYTES
            if (!meetsThreshold) return false
            if (lastPromptEpochMs > 0L && (nowEpochMs - lastPromptEpochMs) < COOLDOWN_MILLIS) {
                return false
            }
            return true
        }
    }
}
