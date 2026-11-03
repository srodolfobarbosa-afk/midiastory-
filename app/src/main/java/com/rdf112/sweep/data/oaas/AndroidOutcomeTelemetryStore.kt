package com.rdf112.sweep.data.oaas

import android.content.Context
import com.rdf112.sweep.domain.oaas.DesiredOutcomeMission

data class OutcomeFlywheelSnapshot(
    val missionsExecutedCount: Int,
    val totalVerifiedItemsDelivered: Int,
    val totalConfirmedFreedBytes: Long,
    val mostRequestedMissionId: String?,
    val customUserOutcomeRequests: List<String>,
    val isRadarConsentGranted: Boolean,
)

/**
 * Radar OaaS local (100% no dispositivo):
 * - Respeita o princípio de privacidade por padrão (opt-in explícito para registrar sugestões do Radar OaaS).
 * - Nunca coleta nomes de arquivos, fotos, vídeos, áudios ou URIs.
 * - Mantém contabilidade local auditável de bytes efetivamente liberados pós-confirmação do Android.
 */
class AndroidOutcomeTelemetryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isRadarConsentGranted(): Boolean = prefs.getBoolean(KEY_RADAR_CONSENT, false)

    fun setRadarConsentGranted(granted: Boolean) {
        prefs.edit().putBoolean(KEY_RADAR_CONSENT, granted).apply()
    }

    fun recordMissionRequested(mission: DesiredOutcomeMission, itemsPrepared: Int) {
        val totalMissions = prefs.getInt(KEY_MISSIONS_COUNT, 0) + 1
        val totalItems = prefs.getInt(KEY_ITEMS_DELIVERED, 0) + itemsPrepared.coerceAtLeast(0)
        val missionCountKey = "mission_count_${mission.id}"
        val thisMissionCount = prefs.getInt(missionCountKey, 0) + 1

        val currentTopId = prefs.getString(KEY_TOP_MISSION_ID, null)
        val currentTopCount = if (currentTopId != null) prefs.getInt("mission_count_$currentTopId", 0) else 0
        val nextTopId = if (thisMissionCount >= currentTopCount) mission.id else currentTopId

        prefs.edit()
            .putInt(KEY_MISSIONS_COUNT, totalMissions)
            .putInt(KEY_ITEMS_DELIVERED, totalItems)
            .putInt(missionCountKey, thisMissionCount)
            .putString(KEY_TOP_MISSION_ID, nextTopId)
            .apply()
    }

    fun recordConfirmedFreedBytes(freedBytes: Long) {
        if (freedBytes <= 0L) return
        val existing = prefs.getLong(KEY_CONFIRMED_FREED_BYTES, 0L).coerceAtLeast(0L)
        val updated = try {
            Math.addExact(existing, freedBytes)
        } catch (_: ArithmeticException) {
            Long.MAX_VALUE
        }
        prefs.edit().putLong(KEY_CONFIRMED_FREED_BYTES, updated).apply()
    }

    fun recordCustomOutcomeRequest(userGoalText: String): Boolean {
        if (!isRadarConsentGranted()) return false
        val cleaned = userGoalText.trim().take(120)
        if (cleaned.isBlank()) return false
        val existing = prefs.getStringSet(KEY_CUSTOM_REQUESTS, emptySet()).orEmpty().toMutableSet()
        existing.add(cleaned)
        prefs.edit().putStringSet(KEY_CUSTOM_REQUESTS, existing).apply()
        return true
    }

    fun clearAllTelemetry() {
        prefs.edit().clear().apply()
    }

    fun loadSnapshot(): OutcomeFlywheelSnapshot {
        return OutcomeFlywheelSnapshot(
            missionsExecutedCount = prefs.getInt(KEY_MISSIONS_COUNT, 0),
            totalVerifiedItemsDelivered = prefs.getInt(KEY_ITEMS_DELIVERED, 0),
            totalConfirmedFreedBytes = prefs.getLong(KEY_CONFIRMED_FREED_BYTES, 0L).coerceAtLeast(0L),
            mostRequestedMissionId = prefs.getString(KEY_TOP_MISSION_ID, null),
            customUserOutcomeRequests = prefs.getStringSet(KEY_CUSTOM_REQUESTS, emptySet()).orEmpty().sorted(),
            isRadarConsentGranted = isRadarConsentGranted(),
        )
    }

    companion object {
        private const val PREFS_NAME = "sweep_oaas_flywheel_prefs"
        private const val KEY_RADAR_CONSENT = "radar_consent_granted"
        private const val KEY_MISSIONS_COUNT = "missions_executed_count"
        private const val KEY_ITEMS_DELIVERED = "total_verified_items_delivered"
        private const val KEY_CONFIRMED_FREED_BYTES = "total_confirmed_freed_bytes"
        private const val KEY_TOP_MISSION_ID = "most_requested_mission_id"
        private const val KEY_CUSTOM_REQUESTS = "custom_user_outcome_requests"
    }
}
