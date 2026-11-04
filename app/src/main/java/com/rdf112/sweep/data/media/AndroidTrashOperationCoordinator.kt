package com.rdf112.sweep.data.media

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import com.rdf112.sweep.domain.model.MediaRecord
import com.rdf112.sweep.domain.repository.MediaPageRepository
import com.rdf112.sweep.domain.operations.ItemRevalidationReason
import com.rdf112.sweep.domain.operations.MediaOperationMode
import com.rdf112.sweep.domain.operations.ObservedMediaPostState
import com.rdf112.sweep.domain.operations.RestoreReconciliationResult
import com.rdf112.sweep.domain.operations.RevalidatedMediaItem
import com.rdf112.sweep.domain.operations.RevalidatedOperationPlan
import com.rdf112.sweep.domain.operations.TrashRecoveryLedger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coordenador Android para revalidação item a item, Lixeira oficial de 30 dias (`createTrashRequest(..., true)`),
 * Exclusão Permanente (`createDeleteRequest`) e Restauração com reconciliação parcial (`createTrashRequest(..., false)`).
 */
sealed interface Api29DeleteAttempt {
    data object Deleted : Api29DeleteAttempt
    data class ConsentRequired(val intentSender: IntentSender) : Api29DeleteAttempt
    data object NotDeleted : Api29DeleteAttempt
}

class AndroidTrashOperationCoordinator(
    context: Context,
    private val resolver: ContentResolver = context.contentResolver,
) {
    private val appContext = context.applicationContext
    private val hashRepository: MediaPageRepository = AndroidMediaPageRepository(appContext, resolver)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun supportsSystemTrash(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** Android 10 supports permanent deletion through RecoverableSecurityException consent. */
    fun supportsPermanentDelete(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * Attempts one API 29 deletion. If the provider requires user consent, the caller must launch
     * the returned system IntentSender and retry this same URI only after RESULT_OK.
     */
    suspend fun deleteApi29Item(record: MediaRecord): Api29DeleteAttempt = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q ||
            !record.isSelectable || record.isProtected ||
            revalidateSingleRecord(record) != ItemRevalidationReason.VALID_UNCHANGED
        ) {
            return@withContext Api29DeleteAttempt.NotDeleted
        }
        try {
            val deletedRows = resolver.delete(Uri.parse(record.uri), null, null)
            if (deletedRows > 0) Api29DeleteAttempt.Deleted else Api29DeleteAttempt.NotDeleted
        } catch (recoverable: RecoverableSecurityException) {
            Api29DeleteAttempt.ConsentRequired(recoverable.userAction.actionIntent.intentSender)
        } catch (_: Exception) {
            Api29DeleteAttempt.NotDeleted
        }
    }

    fun loadPersistedTrashLedger(): TrashRecoveryLedger {
        val stored = prefs.getStringSet(KEY_LAST_TRASH_URIS, emptySet()).orEmpty()
        return TrashRecoveryLedger.fromUris(stored)
    }

    private fun saveTrashLedger(ledger: TrashRecoveryLedger) {
        prefs.edit().putStringSet(KEY_LAST_TRASH_URIS, LinkedHashSet(ledger.trackedUris)).apply()
    }

    suspend fun prepareAndRevalidatePlan(
        selectedRecords: Collection<MediaRecord>,
        mode: MediaOperationMode = MediaOperationMode.MOVE_TO_TRASH,
    ): RevalidatedOperationPlan = withContext(Dispatchers.IO) {
        val evaluated = ArrayList<RevalidatedMediaItem>(selectedRecords.size)
        for (record in selectedRecords) {
            val reason = if (!record.isSelectable || record.isProtected) {
                ItemRevalidationReason.PROTECTED_OR_INELIGIBLE
            } else {
                revalidateSingleRecord(record)
            }
            evaluated += RevalidatedMediaItem(record, reason)
        }
        RevalidatedOperationPlan(
            mode = mode,
            evaluatedItems = evaluated,
        )
    }

    private suspend fun revalidateSingleRecord(record: MediaRecord): ItemRevalidationReason {
        val candidateMetadata = revalidateMetadataSnapshot(
            uriString = record.uri,
            expectedSizeBytes = record.sizeBytes,
            expectedMimeType = record.mimeType,
            expectedDateModifiedSec = record.dateModifiedEpochSeconds,
        )
        if (candidateMetadata != ItemRevalidationReason.VALID_UNCHANGED) return candidateMetadata

        val reference = record.duplicateReferenceSnapshot ?: return ItemRevalidationReason.VALID_UNCHANGED
        val expectedHash = record.contentSha256?.takeIf(String::isNotBlank)?.lowercase()
            ?: return ItemRevalidationReason.DUPLICATE_HASH_UNVERIFIABLE
        if (!expectedHash.equals(reference.sha256, ignoreCase = true)) {
            return ItemRevalidationReason.DUPLICATE_CONTENT_CHANGED
        }
        if (record.sizeBytes > MAX_DUPLICATE_REVALIDATION_BYTES) {
            return ItemRevalidationReason.DUPLICATE_HASH_UNVERIFIABLE
        }

        val originalMetadata = revalidateMetadataSnapshot(
            uriString = reference.uri,
            expectedSizeBytes = reference.sizeBytes,
            expectedMimeType = reference.mimeType,
            expectedDateModifiedSec = reference.dateModifiedEpochSeconds,
        )
        if (originalMetadata != ItemRevalidationReason.VALID_UNCHANGED) {
            return ItemRevalidationReason.ORIGINAL_REFERENCE_CHANGED
        }

        val candidateHash = hashRepository.computeSha256(
            record.uri,
            maxBytes = record.sizeBytes,
            expectedBytes = record.sizeBytes,
        )?.lowercase() ?: return ItemRevalidationReason.DUPLICATE_HASH_UNVERIFIABLE
        val originalHash = hashRepository.computeSha256(
            reference.uri,
            maxBytes = reference.sizeBytes,
            expectedBytes = reference.sizeBytes,
        )?.lowercase() ?: return ItemRevalidationReason.DUPLICATE_HASH_UNVERIFIABLE

        if (candidateHash != expectedHash || originalHash != expectedHash) {
            return ItemRevalidationReason.DUPLICATE_CONTENT_CHANGED
        }
        return ItemRevalidationReason.VALID_UNCHANGED
    }

    private fun revalidateMetadataSnapshot(
        uriString: String,
        expectedSizeBytes: Long,
        expectedMimeType: String?,
        expectedDateModifiedSec: Long?,
    ): ItemRevalidationReason {
        val projection = arrayOf(
            BaseColumns._ID,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.IS_PENDING,
        ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(MediaStore.MediaColumns.IS_TRASHED)
        } else {
            emptyArray()
        }
        return try {
            val uri = Uri.parse(uriString)
            val args = Bundle().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                }
            }
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                resolver.query(uri, projection, args, null)
            } else {
                resolver.query(uri, projection, null, null, null)
            } ?: return ItemRevalidationReason.MISSING_OR_DELETED

            cursor.use { c ->
                if (!c.moveToFirst()) return ItemRevalidationReason.MISSING_OR_DELETED
                val sizeIdx = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeIdx = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val modIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val pendIdx = c.getColumnIndex(MediaStore.MediaColumns.IS_PENDING)
                val trashIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                } else -1

                val actualSize = if (sizeIdx >= 0 && !c.isNull(sizeIdx)) c.getLong(sizeIdx) else null
                val actualMime = if (mimeIdx >= 0 && !c.isNull(mimeIdx)) c.getString(mimeIdx) else null
                val actualMod = if (modIdx >= 0 && !c.isNull(modIdx)) c.getLong(modIdx) else null
                val actualPending = if (pendIdx >= 0 && !c.isNull(pendIdx)) c.getInt(pendIdx) else 0
                val actualTrashed = if (trashIdx >= 0 && !c.isNull(trashIdx)) c.getInt(trashIdx) else 0

                RevalidatedOperationPlan.isSnapshotUnchanged(
                    expectedSizeBytes = expectedSizeBytes,
                    expectedMimeType = expectedMimeType,
                    expectedDateModifiedSec = expectedDateModifiedSec,
                    actualSizeBytes = actualSize,
                    actualMimeType = actualMime,
                    actualDateModifiedSec = actualMod,
                    actualIsPending = actualPending,
                    actualIsTrashed = actualTrashed,
                )
            }
        } catch (_: SecurityException) {
            ItemRevalidationReason.PERMISSION_DENIED
        } catch (_: Exception) {
            ItemRevalidationReason.PROVIDER_ERROR
        }
    }

    fun createTrashIntentSender(uriBatch: List<String>, moveToTrash: Boolean): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uriBatch.isEmpty()) return null
        return runCatching {
            val uris = uriBatch.map(Uri::parse)
            MediaStore.createTrashRequest(resolver, uris, moveToTrash).intentSender
        }.getOrNull()
    }

    fun createPermanentDeleteIntentSender(uriBatch: List<String>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uriBatch.isEmpty()) return null
        return runCatching {
            val uris = uriBatch.map(Uri::parse)
            MediaStore.createDeleteRequest(resolver, uris).intentSender
        }.getOrNull()
    }

    suspend fun verifyPostTrashAndRecord(targetUris: Collection<String>): Map<String, ObservedMediaPostState> =
        withContext(Dispatchers.IO) {
            val states = inspectPostOperationStates(targetUris)
            val updated = loadPersistedTrashLedger().recordConfirmedTrashed(states)
            saveTrashLedger(updated)
            states
        }

    suspend fun verifyPostDeleteUris(targetUris: Collection<String>): Set<String> =
        withContext(Dispatchers.IO) {
            inspectPostOperationStates(targetUris)
                .filterValues { it == ObservedMediaPostState.DELETED_OR_ABSENT }
                .keys
        }

    suspend fun verifyPostDelete(targetUris: Collection<String>): Int =
        verifyPostDeleteUris(targetUris).size

    suspend fun verifyPostRestoreAndReconcile(): RestoreReconciliationResult =
        withContext(Dispatchers.IO) {
            val currentLedger = loadPersistedTrashLedger()
            val states = inspectPostOperationStates(currentLedger.trackedUris)
            val result = currentLedger.reconcileAfterRestoreAttempt(states)
            saveTrashLedger(result.updatedLedger)
            result
        }

    private fun inspectPostOperationStates(uris: Collection<String>): Map<String, ObservedMediaPostState> {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            return uris.associateWith { rawUri ->
                try {
                    val cursor = resolver.query(Uri.parse(rawUri), arrayOf(BaseColumns._ID), null, null, null)
                    if (cursor == null) {
                        ObservedMediaPostState.UNVERIFIED_ERROR
                    } else {
                        cursor.use { c ->
                            if (c.moveToFirst()) ObservedMediaPostState.RESTORED_ACTIVE
                            else ObservedMediaPostState.DELETED_OR_ABSENT
                        }
                    }
                } catch (_: Exception) {
                    ObservedMediaPostState.UNVERIFIED_ERROR
                }
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return uris.associateWith { ObservedMediaPostState.UNVERIFIED_ERROR }
        }
        val projection = arrayOf(BaseColumns._ID, MediaStore.MediaColumns.IS_TRASHED)
        val args = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
        }
        return uris.associateWith { rawUri ->
            try {
                val cursor = resolver.query(Uri.parse(rawUri), projection, args, null)
                if (cursor == null) {
                    ObservedMediaPostState.UNVERIFIED_ERROR
                } else {
                    cursor.use { c ->
                        if (!c.moveToFirst()) {
                            ObservedMediaPostState.DELETED_OR_ABSENT
                        } else {
                            val idx = c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                            val trashed = if (idx >= 0 && !c.isNull(idx)) c.getInt(idx) else 0
                            if (trashed == 1) ObservedMediaPostState.IN_TRASH
                            else ObservedMediaPostState.RESTORED_ACTIVE
                        }
                    }
                }
            } catch (_: Exception) {
                ObservedMediaPostState.UNVERIFIED_ERROR
            }
        }
    }

    companion object {
        private const val MAX_DUPLICATE_REVALIDATION_BYTES = 2L * 1024L * 1024L * 1024L
        private const val PREFS_NAME = "sweep_trash_recovery_prefs"
        private const val KEY_LAST_TRASH_URIS = "last_trash_uris"
    }
}
