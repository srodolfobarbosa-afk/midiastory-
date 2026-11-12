package com.rdf112.sweep.util

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Gatilho Inteligente do Google Play In-App Review API:
 * Dispara o diálogo oficial de avaliação do Google Play sem tirar o usuário do app,
 * especificamente no momento de pico de alívio e satisfação (após confirmação de espaço liberado).
 */
class InAppReviewCoordinator(private val activity: Activity) {
    private val reviewManager = ReviewManagerFactory.create(activity)
    private var hasRequestedInCurrentSession = false

    fun requestReviewIfAppropriate(confirmedFreedBytes: Long) {
        // Dispara apenas quando o usuário liberou espaço real e apenas uma vez por sessão para não cansar o usuário
        if (confirmedFreedBytes <= 0L || hasRequestedInCurrentSession) return
        hasRequestedInCurrentSession = true

        val requestTask = reviewManager.requestReviewFlow()
        requestTask.addOnCompleteListener { task ->
            if (task.isSuccessful && !activity.isFinishing && !activity.isDestroyed) {
                val reviewInfo = task.result
                reviewManager.launchReviewFlow(activity, reviewInfo)
            }
        }
    }
}
