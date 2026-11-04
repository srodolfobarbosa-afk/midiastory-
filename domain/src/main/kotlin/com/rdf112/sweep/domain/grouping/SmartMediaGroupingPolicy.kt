package com.rdf112.sweep.domain.grouping

import com.rdf112.sweep.domain.model.MediaRecord

enum class SmartFilterMode {
    ALL_ITEMS,
    LARGE_FILES_10MB,
    SAME_SIZE_CANDIDATES,
    OLDER_THAN_30_DAYS,
}

/**
 * Motor determinístico de análise e proteção de originais do Sweep:
 * 1. Filtra mídias grandes (>= 10 MB).
 * 2. Verifica duplicatas exatas por tamanho (> 0) + hash criptográfico SHA-256 em fluxo:
 *    - Se os arquivos tiverem contentSha256 preenchido, exige igualdade de (sizeBytes, contentSha256.lowercase()).
 *      Arquivos de mesmo tamanho mas hash diferente (falso positivo) NUNCA são classificados como duplicados.
 *    - Protege automaticamente 1 referência original por grupo confirmado (isProtected = true, isSelectable = false).
 * 3. Filtra mídias antigas (>= 30 dias).
 */
object SmartMediaGroupingPolicy {
    const val LARGE_FILE_THRESHOLD_BYTES: Long = 10L * 1024L * 1024L // 10 MB
    const val THIRTY_DAYS_SECONDS: Long = 30L * 24L * 3600L

    fun applyFilter(
        records: List<MediaRecord>,
        mode: SmartFilterMode,
        nowEpochSeconds: Long,
    ): List<MediaRecord> = when (mode) {
        SmartFilterMode.ALL_ITEMS -> records
        SmartFilterMode.LARGE_FILES_10MB -> records.filter { it.sizeBytes >= LARGE_FILE_THRESHOLD_BYTES }
            .sortedByDescending { it.sizeBytes }
        SmartFilterMode.SAME_SIZE_CANDIDATES -> markProtectedReferencesForSameSizeGroups(records)
        SmartFilterMode.OLDER_THAN_30_DAYS -> {
            val cutoff = (nowEpochSeconds - THIRTY_DAYS_SECONDS).coerceAtLeast(0L)
            records.filter { record ->
                val mod = record.dateModifiedEpochSeconds
                mod != null && mod in 1..cutoff
            }
        }
    }

    /**
     * Agrupa registros que possuem o mesmo tamanho em bytes (> 0) e, quando houver hash SHA-256,
     * exige comprovação de conteúdo idêntico.
     * Rejeita falsos positivos (mesmo tamanho com SHA-256 divergente) e protege 1 referência original por grupo.
     */
    fun markProtectedReferencesForSameSizeGroups(records: List<MediaRecord>): List<MediaRecord> {
        val bySize = records.filter { it.sizeBytes > 0L }.groupBy { it.sizeBytes }
        val result = ArrayList<MediaRecord>()
        for ((_, sizeGroup) in bySize) {
            if (sizeGroup.size < 2) continue
            val confirmedSubgroups = sizeGroup
                .filter { !it.contentSha256.isNullOrBlank() }
                .groupBy { it.contentSha256!!.lowercase() }
            for ((_, hashGroup) in confirmedSubgroups) {
                if (hashGroup.size < 2) continue
                appendProtectedGroup(hashGroup, result)
            }
        }
        return result.sortedByDescending { it.sizeBytes }
    }

    /**
     * Identifica itens que possuem mesmo tamanho em bytes na página, mas não puderam ter seu hash SHA-256
     * confirmado ou apresentaram conteúdo divergente (falsos positivos excluídos da seleção automática).
     */
    fun findUnconfirmedSameSizeRecords(records: List<MediaRecord>): List<MediaRecord> {
        val bySize = records.filter { it.sizeBytes > 0L }.groupBy { it.sizeBytes }
        val confirmedUris = markProtectedReferencesForSameSizeGroups(records).map { it.uri }.toSet()
        val unconfirmed = ArrayList<MediaRecord>()
        for ((_, sizeGroup) in bySize) {
            if (sizeGroup.size < 2) continue
            for (item in sizeGroup) {
                if (item.uri !in confirmedUris) {
                    unconfirmed += item
                }
            }
        }
        return unconfirmed
    }

    private fun appendProtectedGroup(group: List<MediaRecord>, out: MutableList<MediaRecord>) {
        val sorted = group.sortedWith(
            compareBy<MediaRecord> { it.dateModifiedEpochSeconds ?: Long.MAX_VALUE }
                .thenBy { it.uri }
        )
        sorted.forEachIndexed { index, item ->
            if (index == 0) {
                out += item.copy(isProtected = true, isSelectable = false)
            } else {
                out += item
            }
        }
    }
}
