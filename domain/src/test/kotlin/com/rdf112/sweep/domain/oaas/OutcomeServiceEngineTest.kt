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
}
