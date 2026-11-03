package com.rdf112.sweep.domain.model

import java.util.Collections

/** Classificação de mídia reconhecida pelo domínio. */
enum class MediaKind { IMAGE, VIDEO, AUDIO }

enum class AccessLevel { FULL, PARTIAL, DENIED, UNKNOWN }

enum class ScanIssueCode {
    PERMISSION_MISSING,
    QUERY_RETURNED_NULL,
    ACCESS_REVOKED,
    INVALID_ROW,
    VOLUME_UNAVAILABLE,
    PROVIDER_FAILURE,
    RESULT_LIMIT_REACHED,
}

data class MediaRecord(
    val uri: String,
    val volumeName: String,
    val kind: MediaKind,
    val displayName: String?,
    val mimeType: String?,
    val sizeBytes: Long,
    val dateModifiedEpochSeconds: Long?,
    val isProtected: Boolean = false,
    val isSelectable: Boolean = true,
    val contentSha256: String? = null,
) {
    init {
        require(uri.startsWith("content://")) { "uri must use the content:// scheme" }
        require(volumeName.isNotBlank()) { "volumeName must not be empty" }
        require(sizeBytes >= 0) { "sizeBytes must be non-negative" }
    }
}

data class ScanIssue(
    val code: ScanIssueCode,
    val kind: MediaKind? = null,
    val volumeName: String? = null,
    val detail: String? = null,
)

/** Resultado independente de plataforma; todas as coleções expostas são cópias não modificáveis. */
class ScanReport(
    records: Collection<MediaRecord>,
    accessByKind: Map<MediaKind, AccessLevel>,
    querySucceededKinds: Set<MediaKind>,
    issues: Collection<ScanIssue>,
    val elapsedMillis: Long,
    requestedKinds: Set<MediaKind> = MediaKind.entries.toSet(),
) {
    val requestedKinds: Set<MediaKind> =
        Collections.unmodifiableSet(LinkedHashSet(requestedKinds))
    val records: List<MediaRecord> = immutableList(records)
    val accessByKind: Map<MediaKind, AccessLevel> =
        Collections.unmodifiableMap(LinkedHashMap(accessByKind))
    val querySucceededKinds: Set<MediaKind> =
        Collections.unmodifiableSet(LinkedHashSet(querySucceededKinds))
    val issues: List<ScanIssue> = immutableList(issues)

    init {
        require(elapsedMillis >= 0) { "elapsedMillis must be non-negative" }
        require(this.requestedKinds.isNotEmpty()) { "requestedKinds must not be empty" }
        require(this.records.map { it.uri }.toSet().size == this.records.size) { "record URIs must be unique" }
        require(this.records.all { it.kind in this.requestedKinds }) { "records must belong to requestedKinds" }
        require(this.querySucceededKinds.all { it in this.requestedKinds }) { "querySucceededKinds must belong to requestedKinds" }
        require(this.issues.all { it.kind == null || it.kind in this.requestedKinds }) { "issue kinds must belong to requestedKinds" }
    }

    /** True whenever any requested media kind was not completely and successfully scanned with full access. */
    val isPartial: Boolean
        get() = requestedKinds.any { kind ->
            kind !in querySucceededKinds || accessByKind[kind] != AccessLevel.FULL
        } || issues.isNotEmpty()

    /** Empty is definitive only if every requested kind was queried successfully with full access. */
    val isCompleteEmpty: Boolean
        get() = records.isEmpty() && !isPartial
}

/** Seleção persistida por URI, nunca pela posição dos registros retornados pelo scan. */
class SelectionLedger private constructor(selectedUris: Set<String>) {
    private val selectedUris: Set<String> = Collections.unmodifiableSet(LinkedHashSet(selectedUris))

    val isEmpty: Boolean get() = selectedUris.isEmpty()
    val size: Int get() = selectedUris.size

    fun isSelected(uri: String): Boolean = uri in selectedUris

    fun setSelected(record: MediaRecord, selected: Boolean): SelectionLedger {
        val next = LinkedHashSet(selectedUris)
        if (!selected || !record.isSelectable || record.isProtected) {
            next.remove(record.uri)
        } else {
            next.add(record.uri)
        }
        return SelectionLedger(next)
    }

    fun toggle(record: MediaRecord): SelectionLedger {
        val next = LinkedHashSet(selectedUris)
        if (!record.isSelectable || record.isProtected) {
            if (!next.remove(record.uri)) return this
            return SelectionLedger(next)
        }
        if (!next.add(record.uri)) next.remove(record.uri)
        return SelectionLedger(next)
    }

    /** Mantém somente URIs presentes e ainda elegíveis na lista atual. */
    fun reconcile(currentRecords: Collection<MediaRecord>): SelectionLedger {
        val eligible = currentRecords.asSequence()
            .filter { it.isSelectable && !it.isProtected }
            .map { it.uri }
            .toSet()
        return SelectionLedger(selectedUris.intersect(eligible))
    }

    /** Cria um snapshot ordenado e atual, ignorando seleções ausentes ou inelegíveis. */
    fun snapshot(currentRecords: Collection<MediaRecord>): OperationDraft {
        val selected = currentRecords.asSequence()
            .filter { it.uri in selectedUris && it.isSelectable && !it.isProtected }
            .distinctBy { it.uri }
            .sortedBy { it.uri }
            .toList()
        require(selected.isNotEmpty()) { "cannot create an OperationDraft from an empty selection" }
        var total = 0L
        for (record in selected) total = Math.addExact(total, record.sizeBytes)
        return OperationDraft(selected, total)
    }

    companion object {
        fun empty(): SelectionLedger = SelectionLedger(emptySet())
    }
}

/** Imutável e validado conjunto de registros que será entregue a uma operação. */
class OperationDraft(records: Collection<MediaRecord>, val totalBytes: Long) {
    val records: List<MediaRecord> = immutableList(records)

    init {
        require(this.records.isNotEmpty()) { "OperationDraft must not be empty" }
        require(this.records.all { it.isSelectable && !it.isProtected }) { "OperationDraft cannot contain protected or ineligible records" }
        require(this.records.map { it.uri }.toSet().size == this.records.size) { "record URIs must be unique" }
        var expected = 0L
        for (record in this.records) expected = Math.addExact(expected, record.sizeBytes)
        require(totalBytes == expected) { "totalBytes must equal the sum of record sizes" }
    }
}

private fun <T> immutableList(items: Collection<T>): List<T> =
    Collections.unmodifiableList(ArrayList(items))
