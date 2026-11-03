package com.rdf112.sweep.domain.oaas

import com.rdf112.sweep.domain.grouping.SmartFilterMode
import com.rdf112.sweep.domain.grouping.SmartMediaGroupingPolicy
import com.rdf112.sweep.domain.model.MediaRecord

/**
 * Motor OaaS (Outcome-as-a-Service) do Sweep:
 * "Você diz o resultado. O Sweep prepara o trabalho. Você revisa e autoriza."
 *
 * Processo de 6 etapas auditáveis:
 *   1. BUSCAR — consultar mídias autorizadas no Android.
 *   2. CONFERIR — analisar os registros e identificar oportunidades reais (incluindo SHA-256 para duplicatas).
 *   3. PREPARAR — montar uma proposta de ação automaticamente.
 *   4. RECONCILIAR — validar seleção, permissões, referências protegidas e bytes envolvidos.
 *   5. EXECUTAR — solicitar autorização do usuário pelo mecanismo oficial Android.
 *   6. ACOMPANHAR — verificar o resultado real pós-operação e atualizar os indicadores.
 */
enum class DesiredOutcomeMission(
    val id: String,
    val minimum10xDimension: String,
) {
    FREE_MAX_SPACE_NOW(
        id = "free_max_space_now",
        minimum10xDimension = "Identificar arquivos grandes (>= 10 MB) e preparar proposta de liberação de espaço",
    ),
    ELIMINATE_DUPLICATE_COPIES(
        id = "eliminate_duplicate_copies",
        minimum10xDimension = "Encontrar duplicatas exatas comprovadas, protegendo 1 referência original por grupo",
    ),
    ARCHIVE_OLD_MEDIA_30D(
        id = "archive_old_media_30d",
        minimum10xDimension = "Encontrar mídias antigas (>= 30 dias) e preparar lote de revisão",
    ),
}

enum class ProcessExecutionStage {
    BUSCAR,
    CONFERIR,
    PREPARAR,
    RECONCILIAR,
    EXECUTAR,
    ACOMPANHAR;

    companion object {
        // Alias de compatibilidade para código/testes existentes
        val PREENCHER: ProcessExecutionStage get() = PREPARAR
        val LIGAR: ProcessExecutionStage get() = EXECUTAR
    }
}

/**
 * Diferenciação rigorosa dos 4 estados de espaço exigidos pela arquitetura OaaS do Sweep:
 * - potentiallyRecoverableBytes: espaço potencialmente recuperável identificado na página/análise.
 * - preparedForReviewBytes: espaço preparado para revisão na seleção atual.
 * - authorizedBytes: espaço autorizado pelo usuário e submetido ao fluxo oficial do Android.
 * - confirmedFreedBytes: espaço efetivamente liberado e comprovado no MediaStore após a operação.
 */
data class SpaceAccountingMetrics(
    val potentiallyRecoverableBytes: Long = 0L,
    val preparedForReviewBytes: Long = 0L,
    val authorizedBytes: Long = 0L,
    val confirmedFreedBytes: Long = 0L,
)

data class OutcomeExecutionPlan(
    val mission: DesiredOutcomeMission,
    val completedStages: List<ProcessExecutionStage>,
    val autoSelectedRecords: List<MediaRecord>,
    val protectedReferencesKept: Int,
    val unconfirmedRecordsCount: Int = 0,
    val estimatedDeliverableBytes: Long,
) {
    val isReadyToDeliver: Boolean
        get() = autoSelectedRecords.isNotEmpty()
}

object OutcomeServiceEngine {
    fun buildAutomatedOutcomePlan(
        mission: DesiredOutcomeMission,
        pageRecords: List<MediaRecord>,
        nowEpochSeconds: Long,
        maxSelectionLimit: Int = 500,
    ): OutcomeExecutionPlan {
        // Etapa 1: BUSCAR & Etapa 2: CONFERIR
        val candidateRecords = when (mission) {
            DesiredOutcomeMission.FREE_MAX_SPACE_NOW ->
                SmartMediaGroupingPolicy.applyFilter(pageRecords, SmartFilterMode.LARGE_FILES_10MB, nowEpochSeconds)
            DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES ->
                SmartMediaGroupingPolicy.applyFilter(pageRecords, SmartFilterMode.SAME_SIZE_CANDIDATES, nowEpochSeconds)
            DesiredOutcomeMission.ARCHIVE_OLD_MEDIA_30D ->
                SmartMediaGroupingPolicy.applyFilter(pageRecords, SmartFilterMode.OLDER_THAN_30_DAYS, nowEpochSeconds)
        }

        val unconfirmedCount = if (mission == DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES) {
            SmartMediaGroupingPolicy.findUnconfirmedSameSizeRecords(pageRecords).size
        } else {
            0
        }

        val protectedCount = candidateRecords.count { it.isProtected }

        // Etapa 3: PREPARAR (seleciona apenas itens elegíveis e não protegidos, ordenados por maior impacto)
        val autoSelected = candidateRecords
            .filter { it.isSelectable && !it.isProtected && it.sizeBytes > 0L }
            .sortedByDescending { it.sizeBytes }
            .take(maxSelectionLimit.coerceAtLeast(1))

        // Etapa 4: RECONCILIAR (soma segura de bytes preparados para revisão)
        val deliverableBytes = safeSumBytes(autoSelected)

        return OutcomeExecutionPlan(
            mission = mission,
            completedStages = listOf(
                ProcessExecutionStage.BUSCAR,
                ProcessExecutionStage.CONFERIR,
                ProcessExecutionStage.PREPARAR,
                ProcessExecutionStage.RECONCILIAR,
            ),
            autoSelectedRecords = autoSelected,
            protectedReferencesKept = protectedCount,
            unconfirmedRecordsCount = unconfirmedCount,
            estimatedDeliverableBytes = deliverableBytes,
        )
    }

    fun calculatePotentiallyRecoverableBytes(pageRecords: List<MediaRecord>, nowEpochSeconds: Long): Long {
        val large = SmartMediaGroupingPolicy.applyFilter(pageRecords, SmartFilterMode.LARGE_FILES_10MB, nowEpochSeconds)
            .filter { it.isSelectable && !it.isProtected }
        val dups = SmartMediaGroupingPolicy.applyFilter(pageRecords, SmartFilterMode.SAME_SIZE_CANDIDATES, nowEpochSeconds)
            .filter { it.isSelectable && !it.isProtected }
        val old = SmartMediaGroupingPolicy.applyFilter(pageRecords, SmartFilterMode.OLDER_THAN_30_DAYS, nowEpochSeconds)
            .filter { it.isSelectable && !it.isProtected }
        val combinedUnique = (large + dups + old).distinctBy { it.uri }
        return safeSumBytes(combinedUnique)
    }

    fun safeSumBytes(records: Iterable<MediaRecord>): Long =
        records.fold(0L) { acc, item ->
            try {
                Math.addExact(acc, item.sizeBytes.coerceAtLeast(0L))
            } catch (_: ArithmeticException) {
                Long.MAX_VALUE
            }
        }
}
