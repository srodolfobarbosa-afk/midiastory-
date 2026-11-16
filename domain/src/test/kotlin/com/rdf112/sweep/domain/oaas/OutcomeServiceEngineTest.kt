package com.rdf112.sweep.domain.oaas

import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutcomeServiceEngineTest {
    private fun rec(id: String, size: Long, modSec: Long, sha256: String? = "e".repeat(64)) = MediaRecord(
        uri = "content://media/external_primary/images/media/$id",
        volumeName = "external_primary",
        kind = MediaKind.IMAGE,
        displayName = "item_$id.jpg",
        mimeType = "image/jpeg",
        sizeBytes = size,
        dateModifiedEpochSeconds = modSec,
        contentSha256 = sha256,
    )

    @Test
    fun buildsAutomatedOutcomePlanForConfirmedDuplicatesAndSeparatesUnconfirmed() {
        val dup1 = rec("1", 20L * 1024L * 1024L, 100L, "c".repeat(64))
        val dup2 = rec("2", 20L * 1024L * 1024L, 200L, "c".repeat(64))
        val dup3 = rec("3", 20L * 1024L * 1024L, 300L, "c".repeat(64))
        val falsePositive = rec("4", 20L * 1024L * 1024L, 400L, "d".repeat(64))

        val plan = OutcomeServiceEngine.buildAutomatedOutcomePlan(
            mission = DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES,
            pageRecords = listOf(dup1, dup2, dup3, falsePositive),
            nowEpochSeconds = 1000L,
        )

        assertTrue(plan.isReadyToDeliver)
        assertEquals(1, plan.protectedReferencesKept)
        assertEquals(2, plan.autoSelectedRecords.size)
        assertEquals(1, plan.unconfirmedRecordsCount)
        assertEquals(40L * 1024L * 1024L, plan.estimatedDeliverableBytes)
        assertTrue(plan.completedStages.containsAll(listOf(
            ProcessExecutionStage.BUSCAR,
            ProcessExecutionStage.CONFERIR,
            ProcessExecutionStage.PREPARAR,
            ProcessExecutionStage.RECONCILIAR,
        )))
    }

    @Test
    fun buildsAutomatedOutcomePlanForOldScreenshots30d() {
        val now = 60L * 24L * 3600L
        val oldShot = MediaRecord(
            uri = "content://media/external_primary/images/media/201",
            volumeName = "external_primary",
            kind = MediaKind.IMAGE,
            displayName = "Screenshot_20260901.png",
            mimeType = "image/png",
            sizeBytes = 3L * 1024L * 1024L,
            dateModifiedEpochSeconds = 10L * 24L * 3600L,
            relativePath = "Pictures/Screenshots/",
        )
        val regularPhoto = rec("202", 5L * 1024L * 1024L, 10L * 24L * 3600L)

        val plan = OutcomeServiceEngine.buildAutomatedOutcomePlan(
            mission = DesiredOutcomeMission.CLEAN_OLD_SCREENSHOTS_30D,
            pageRecords = listOf(oldShot, regularPhoto),
            nowEpochSeconds = now,
        )

        assertTrue(plan.isReadyToDeliver)
        assertEquals(listOf(oldShot.uri), plan.autoSelectedRecords.map { it.uri })
        assertEquals(3L * 1024L * 1024L, plan.estimatedDeliverableBytes)
    }
}
