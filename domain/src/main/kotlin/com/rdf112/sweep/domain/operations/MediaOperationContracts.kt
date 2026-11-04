package com.rdf112.sweep.domain.operations

import com.rdf112.sweep.domain.model.MediaRecord
import java.util.Collections

/**
 * Contratos puros de domínio para revalidação pré-operação e reconciliação de restauração parcial
 * da Lixeira, respeitando a Planta Técnica v1.9.
 *
 * IMPORTANTE: As operações destrutivas na UI permanecem desabilitadas até que os testes
 * instrumentados de paginação, seleção e revalidação sejam homologados em dispositivos reais.
 */
enum class MediaOperationMode {
    MOVE_TO_TRASH,
    RESTORE_FROM_TRASH,
    PERMANENT_DELETE,
}

enum class ItemRevalidationReason {
    VALID_UNCHANGED,
    MISSING_OR_DELETED,
    MODIFIED_SINCE_SCAN,
    ALREADY_TRASHED,
    PROTECTED_OR_INELIGIBLE,
    PERMISSION_DENIED,
    PROVIDER_ERROR,
    DUPLICATE_HASH_UNVERIFIABLE,
    DUPLICATE_CONTENT_CHANGED,
    ORIGINAL_REFERENCE_CHANGED,
}

data class RevalidatedMediaItem(
    val record: MediaRecord,
    val reason: ItemRevalidationReason,
) {
    val isEligibleForTrashOrDelete: Boolean
        get() = reason == ItemRevalidationReason.VALID_UNCHANGED && record.isSelectable && !record.isProtected
}

class RevalidatedOperationPlan(
    val mode: MediaOperationMode,
    evaluatedItems: Collection<RevalidatedMediaItem>,
    val maxBatchSize: Int = DEFAULT_BATCH_SIZE,
) {
    init {
        require(maxBatchSize in 1..ABSOLUTE_MAX_BATCH_SIZE) {
            "maxBatchSize must be between 1 and $ABSOLUTE_MAX_BATCH_SIZE"
        }
    }

    val evaluatedItems: List<RevalidatedMediaItem> =
        Collections.unmodifiableList(ArrayList(evaluatedItems))

    val validRecords: List<MediaRecord> = Collections.unmodifiableList(
        this.evaluatedItems.filter { it.isEligibleForTrashOrDelete }.map { it.record }
    )

    val rejectedItems: List<RevalidatedMediaItem> = Collections.unmodifiableList(
        this.evaluatedItems.filterNot { it.isEligibleForTrashOrDelete }
    )

    val uriBatches: List<List<String>> = Collections.unmodifiableList(
        validRecords.map { it.uri }.distinct().chunked(maxBatchSize).map { Collections.unmodifiableList(it) }
    )

    val totalValidBytes: Long = validRecords.fold(0L) { acc, item -> Math.addExact(acc, item.sizeBytes) }

    companion object {
        const val DEFAULT_BATCH_SIZE = 500
        const val ABSOLUTE_MAX_BATCH_SIZE = 2000

        fun isSnapshotUnchanged(
            expectedSizeBytes: Long,
            expectedMimeType: String?,
            expectedDateModifiedSec: Long?,
            actualSizeBytes: Long?,
            actualMimeType: String?,
            actualDateModifiedSec: Long?,
            actualIsPending: Int,
            actualIsTrashed: Int,
        ): ItemRevalidationReason {
            if (actualIsTrashed != 0) return ItemRevalidationReason.ALREADY_TRASHED
            if (actualIsPending != 0) return ItemRevalidationReason.MODIFIED_SINCE_SCAN
            if (actualSizeBytes == null || actualSizeBytes != expectedSizeBytes) {
                return ItemRevalidationReason.MODIFIED_SINCE_SCAN
            }
            if (!expectedMimeType.isNullOrBlank() &&
                !expectedMimeType.equals(actualMimeType, ignoreCase = true)
            ) {
                return ItemRevalidationReason.MODIFIED_SINCE_SCAN
            }
            if (expectedDateModifiedSec != null && actualDateModifiedSec != expectedDateModifiedSec) {
                return ItemRevalidationReason.MODIFIED_SINCE_SCAN
            }
            return ItemRevalidationReason.VALID_UNCHANGED
        }
    }
}

enum class ObservedMediaPostState {
    IN_TRASH,
    RESTORED_ACTIVE,
    DELETED_OR_ABSENT,
    STILL_ACTIVE_UNCHANGED,
    UNVERIFIED_ERROR,
}

/**
 * Ledger imutável de URIs enviadas para a Lixeira do Android.
 * Cumpre a exigência P1 da Planta v1.9: após uma tentativa de restauração, NUNCA limpa
 * cegamente todas as referências (`last_trash_uris`); remove apenas os itens
 * confirmados como restaurados (`RESTORED_ACTIVE`), preservando as referências ainda na
 * lixeira (`IN_TRASH`) ou não confirmadas (`UNVERIFIED_ERROR`) para nova tentativa.
 */
class TrashRecoveryLedger private constructor(
    trackedUris: Set<String>,
) {
    val trackedUris: Set<String> = Collections.unmodifiableSet(LinkedHashSet(trackedUris))
    val size: Int get() = this.trackedUris.size
    val isEmpty: Boolean get() = this.trackedUris.isEmpty()

    init {
        require(this.trackedUris.all { it.startsWith("content://") }) {
            "All tracked trash URIs must use content:// scheme"
        }
    }

    fun recordConfirmedTrashed(postStates: Map<String, ObservedMediaPostState>): TrashRecoveryLedger {
        val next = LinkedHashSet(trackedUris)
        for ((uri, state) in postStates) {
            if (uri.startsWith("content://") && state == ObservedMediaPostState.IN_TRASH) {
                next.add(uri)
            }
        }
        return TrashRecoveryLedger(next)
    }

    fun reconcileAfterRestoreAttempt(
        postStates: Map<String, ObservedMediaPostState>,
        dropDeletedOrAbsent: Boolean = false,
    ): RestoreReconciliationResult {
        val remaining = LinkedHashSet<String>()
        val confirmedRestored = LinkedHashSet<String>()
        val unconfirmedOrStillTrashed = LinkedHashSet<String>()

        for (uri in trackedUris) {
            when (postStates[uri]) {
                ObservedMediaPostState.RESTORED_ACTIVE -> {
                    confirmedRestored.add(uri)
                }
                ObservedMediaPostState.DELETED_OR_ABSENT -> {
                    if (!dropDeletedOrAbsent) {
                        remaining.add(uri)
                        unconfirmedOrStillTrashed.add(uri)
                    }
                }
                ObservedMediaPostState.IN_TRASH,
                ObservedMediaPostState.STILL_ACTIVE_UNCHANGED,
                ObservedMediaPostState.UNVERIFIED_ERROR,
                null -> {
                    remaining.add(uri)
                    unconfirmedOrStillTrashed.add(uri)
                }
            }
        }
        return RestoreReconciliationResult(
            updatedLedger = TrashRecoveryLedger(remaining),
            confirmedRestoredUris = Collections.unmodifiableSet(confirmedRestored),
            unresolvedUris = Collections.unmodifiableSet(unconfirmedOrStillTrashed),
        )
    }

    companion object {
        fun empty(): TrashRecoveryLedger = TrashRecoveryLedger(emptySet())
        fun fromUris(uris: Collection<String>): TrashRecoveryLedger =
            TrashRecoveryLedger(uris.filter { it.startsWith("content://") }.toSet())
    }
}

data class RestoreReconciliationResult(
    val updatedLedger: TrashRecoveryLedger,
    val confirmedRestoredUris: Set<String>,
    val unresolvedUris: Set<String>,
) {
    val isCompleteSuccess: Boolean
        get() = unresolvedUris.isEmpty() && confirmedRestoredUris.isNotEmpty()
    val isPartialOrUnresolved: Boolean
        get() = unresolvedUris.isNotEmpty()
}
