package com.rdf112.sweep.domain.model

import java.util.Collections

/** Pedido limitado de uma página; a ordem e a estabilidade são responsabilidades do adaptador. */
data class MediaPageRequest(
    val sessionId: String,
    val kind: MediaKind,
    val volumeName: String,
    val offset: Int,
    val pageSize: Int = DEFAULT_PAGE_SIZE,
    /** Skip per-page duplicate hashing when a global mission will hash cross-page groups. */
    val hashSameSizeCandidates: Boolean = true,
) {
    init {
        require(sessionId.isNotBlank()) { "sessionId must not be blank" }
        require(volumeName.isNotBlank()) { "volumeName must not be blank" }
        require(offset >= 0) { "offset must be non-negative" }
        require(pageSize in 1..MAX_PAGE_SIZE) { "pageSize must be between 1 and $MAX_PAGE_SIZE" }
        require(offset <= Int.MAX_VALUE - pageSize) { "offset plus pageSize must not overflow" }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 100
        const val MAX_PAGE_SIZE = 500
    }
}

enum class MediaPageOutcome {
    MORE_AVAILABLE,
    END_OF_SOURCE,
    FAILED,
    SESSION_STALE,
    PAGING_UNSUPPORTED,
    CANCELLED,
}

enum class PageIssueCode {
    PROVIDER_FAILURE,
    NULL_CURSOR,
    INVALID_ROW,
    PAGING_UNSUPPORTED,
    SESSION_STALE,
    ACCESS_CHANGED,
    VOLUME_CHANGED,
    REVISION_UNVERIFIABLE,
    PARTIAL_ACCESS,
    PERMISSION_MISSING,
    CANCELLED,
}

data class PageIssue(
    val code: PageIssueCode,
    val kind: MediaKind? = null,
    val volumeName: String? = null,
    val detail: String? = null,
)

/** Immutable page response. rowsRead counts provider rows, including rows rejected as invalid. */
class MediaPage(
    val request: MediaPageRequest,
    records: Collection<MediaRecord>,
    val rowsRead: Int,
    val providerTotalCount: Long?,
    val nextOffset: Int?,
    val outcome: MediaPageOutcome,
    issues: Collection<PageIssue> = emptyList(),
    /** URIs observed in raw provider rows, including system-filtered rows. Bounded by rowsRead. */
    observedRowKeys: Collection<String> = records.map { it.uri },
) {
    val records: List<MediaRecord> = immutableCopy(records)
    val issues: List<PageIssue> = immutableCopy(issues)
    val observedRowKeys: List<String> = immutableCopy(observedRowKeys)

    init {
        require(rowsRead in 0..request.pageSize) { "rowsRead must not exceed the requested page size" }
        require(this.records.size <= rowsRead) { "records cannot exceed rowsRead" }
        require(this.records.all { it.kind == request.kind }) { "page records must match the requested media kind" }
        require(this.records.all { it.volumeName == request.volumeName }) { "page records must match the requested volume" }
        require(this.records.map { it.uri }.toSet().size == this.records.size) { "page record URIs must be unique" }
        require(this.observedRowKeys.size <= rowsRead) { "observed row keys cannot exceed provider rows read" }
        require(this.observedRowKeys.all { it.startsWith("content://") }) { "observed row keys must be content URIs" }
        require(this.observedRowKeys.toSet().size == this.observedRowKeys.size) { "observed row keys must be unique within a page" }
        require(providerTotalCount == null || providerTotalCount >= 0) { "providerTotalCount must be non-negative" }

        when (outcome) {
            MediaPageOutcome.MORE_AVAILABLE -> {
                require(rowsRead > 0) { "a page with more results must advance the provider cursor" }
                val expectedNextOffset = Math.addExact(request.offset, rowsRead)
                require(nextOffset == expectedNextOffset) { "nextOffset must advance by rowsRead" }
            }
            MediaPageOutcome.END_OF_SOURCE -> require(nextOffset == null) {
                "a terminal page cannot have a nextOffset"
            }
            MediaPageOutcome.FAILED -> {
                require(nextOffset == null && rowsRead == 0 && this.records.isEmpty()) {
                    "a failed page cannot expose partial provider rows"
                }
                require(this.issues.isNotEmpty()) { "a failed page must explain the failure" }
            }
            MediaPageOutcome.SESSION_STALE -> {
                require(nextOffset == null && rowsRead == 0 && this.records.isEmpty()) {
                    "a stale session cannot expose page records"
                }
                require(this.issues.any {
                    it.code in setOf(PageIssueCode.SESSION_STALE, PageIssueCode.ACCESS_CHANGED, PageIssueCode.VOLUME_CHANGED)
                }) { "a stale session must identify why it was invalidated" }
            }
            MediaPageOutcome.PAGING_UNSUPPORTED -> {
                require(nextOffset == null && rowsRead == 0 && this.records.isEmpty()) {
                    "unsupported paging cannot expose an oversized or ambiguous page"
                }
                require(this.issues.any { it.code == PageIssueCode.PAGING_UNSUPPORTED }) {
                    "unsupported paging must be identified explicitly"
                }
            }
            MediaPageOutcome.CANCELLED -> require(nextOffset == null && rowsRead == 0 && this.records.isEmpty()) {
                "a cancelled page cannot expose incomplete provider rows"
            }
        }
    }
}

data class MediaSourceRevision(
    val version: String,
    val generation: Long?,
) {
    companion object {
        const val UNAVAILABLE_VERSION = "unavailable"
    }

    init {
        require(version.isNotBlank()) { "version must not be blank" }
        require(generation == null || generation >= 0) { "generation must be non-negative when present" }
    }
}

enum class SessionFreshness {
    REVISION_UNCHANGED,
    ACCESS_CHANGED,
    VOLUMES_CHANGED,
    SOURCE_CHANGED,
    UNVERIFIABLE,
}

/** Captures permissions and per-volume revision markers without claiming a transactional snapshot. */
class MediaScanSession(
    val id: String,
    val startedAtEpochMillis: Long,
    accessByKind: Map<MediaKind, AccessLevel>,
    volumeNames: Set<String>,
    revisionsByVolume: Map<String, MediaSourceRevision>,
) {
    val accessByKind: Map<MediaKind, AccessLevel> = immutableMap(accessByKind)
    val volumeNames: Set<String> = Collections.unmodifiableSet(LinkedHashSet(volumeNames))
    val revisionsByVolume: Map<String, MediaSourceRevision> = immutableMap(revisionsByVolume)

    init {
        require(id.isNotBlank()) { "session id must not be blank" }
        require(startedAtEpochMillis >= 0) { "startedAtEpochMillis must be non-negative" }
        require(this.volumeNames.isNotEmpty()) { "a session must capture at least one volume" }
        require(this.volumeNames.all(String::isNotBlank)) { "volume names must not be blank" }
        require(this.revisionsByVolume.keys.all { it in this.volumeNames }) {
            "revision keys must belong to captured volumes"
        }
    }

    /** API 29 or missing generation metadata yields UNVERIFIABLE, never an assumed stable snapshot. */
    fun assessFreshness(
        currentAccessByKind: Map<MediaKind, AccessLevel>,
        currentVolumeNames: Set<String>,
        currentRevisionsByVolume: Map<String, MediaSourceRevision>,
    ): SessionFreshness {
        if (this.accessByKind != currentAccessByKind) return SessionFreshness.ACCESS_CHANGED
        if (this.volumeNames != currentVolumeNames) return SessionFreshness.VOLUMES_CHANGED
        if (this.revisionsByVolume.keys != currentRevisionsByVolume.keys) return SessionFreshness.UNVERIFIABLE

        for ((volume, original) in revisionsByVolume) {
            val current = currentRevisionsByVolume[volume] ?: return SessionFreshness.UNVERIFIABLE
            // A placeholder or missing provider version is not evidence of stability.
            if (original.version == MediaSourceRevision.UNAVAILABLE_VERSION || current.version == MediaSourceRevision.UNAVAILABLE_VERSION) {
                return SessionFreshness.UNVERIFIABLE
            }
            if (original.version != current.version) return SessionFreshness.SOURCE_CHANGED
            if (original.generation == null || current.generation == null) {
                return SessionFreshness.UNVERIFIABLE
            }
            if (original.generation != current.generation) return SessionFreshness.SOURCE_CHANGED
        }

        if (revisionsByVolume.keys != volumeNames) return SessionFreshness.UNVERIFIABLE
        return SessionFreshness.REVISION_UNCHANGED
    }
}

private fun <T> immutableCopy(items: Collection<T>): List<T> =
    Collections.unmodifiableList(ArrayList(items))

private fun <K, V> immutableMap(items: Map<K, V>): Map<K, V> =
    Collections.unmodifiableMap(LinkedHashMap(items))
