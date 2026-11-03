package com.rdf112.sweep.data.media

import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import com.rdf112.sweep.domain.model.MediaRecord
import com.rdf112.sweep.domain.operations.ItemRevalidationReason
import com.rdf112.sweep.domain.operations.MediaOperationMode
import com.rdf112.sweep.domain.operations.ObservedMediaPostState
import com.rdf112.sweep.domain.operations.RestoreReconciliationResult
import com.rdf112.sweep.domain.operations.RevalidatedMediaItem
import com.rdf112.sweep.domain.operations.RevalidatedOperationPlan
import com.rdf112.sweep.domain.operations.TrashRecoveryLedger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.security.MessageDigest

/**
 * Coordenador Android para revalidação item a item, Lixeira oficial de 30 dias (`createTrashRequest(..., true)`),
 * Exclusão Permanente (`createDeleteRequest`) e Restauração com reconciliação parcial (`createTrashRequest(..., false)`).
 */
class AndroidTrashOperationCoordinator(
    context: Context,
    private val resolver: ContentResolver = context.contentResolver,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun supportsSystemTrash(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

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
        val evaluated = selectedRecords.map { record ->
            if (!record.isSelectable || record.isProtected) {
                RevalidatedMediaItem(record, ItemRevalidationReason.PROTECTED_OR_INELIGIBLE)
            } else {
                val reason = revalidateSingleRecord(record)
                RevalidatedMediaItem(record, reason)
            }
        }
        RevalidatedOperationPlan(
            mode = mode,
            evaluatedItems = evaluated,
        )
    }

    private suspend fun revalidateSingleRecord(record: MediaRecord): ItemRevalidationReason {
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
            val uri = Uri.parse(record.uri)
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

                val snapshotReason = RevalidatedOperationPlan.isSnapshotUnchanged(
                    expectedSizeBytes = record.sizeBytes,
                    expectedMimeType = record.mimeType,
                    expectedDateModifiedSec = record.dateModifiedEpochSeconds,
                    actualSizeBytes = actualSize,
                    actualMimeType = actualMime,
                    actualDateModifiedSec = actualMod,
                    actualIsPending = actualPending,
                    actualIsTrashed = actualTrashed,
                )
                if (snapshotReason != ItemRevalidationReason.VALID_UNCHANGED) return@use snapshotReason

                val expectedHash = record.contentSha256?.takeIf(String::isNotBlank)
                    ?: return@use ItemRevalidationReason.VALID_UNCHANGED
                val actualHash = computeStreamingSha256(uri)
                    ?: return@use ItemRevalidationReason.PROVIDER_ERROR
                if (RevalidatedOperationPlan.contentHashMatches(expectedHash, actualHash)) {
                    ItemRevalidationReason.VALID_UNCHANGED
                } else {
                    ItemRevalidationReason.MODIFIED_SINCE_SCAN
                }
            }
        } catch (_: SecurityException) {
            ItemRevalidationReason.PERMISSION_DENIED
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ItemRevalidationReason.PROVIDER_ERROR
        }
    }

    private suspend fun computeStreamingSha256(uri: Uri): String? {
        val stream = resolver.openInputStream(uri) ?: return null
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(stream, 8192).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
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

    suspend fun verifyPostDelete(targetUris: Collection<String>): Int =
        withContext(Dispatchers.IO) {
            val states = inspectPostOperationStates(targetUris)
            states.values.count { it == ObservedMediaPostState.DELETED_OR_ABSENT }
        }

    suspend fun verifyPostRestoreAndReconcile(): RestoreReconciliationResult =
        withContext(Dispatchers.IO) {
            val currentLedger = loadPersistedTrashLedger()
            val states = inspectPostOperationStates(currentLedger.trackedUris)
            val result = currentLedger.reconcileAfterRestoreAttempt(states)
            saveTrashLedger(result.updatedLedger)
            result
        }

    private fun inspectPostOperationStates(uris: Collection<String>): Map<String, ObservedMediaPostState> {
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
                            if (idx < 0 || c.isNull(idx)) {
                                ObservedMediaPostState.UNVERIFIED_ERROR
                            } else if (c.getInt(idx) == 1) {
                                ObservedMediaPostState.IN_TRASH
                            } else {
                                ObservedMediaPostState.RESTORED_ACTIVE
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                ObservedMediaPostState.UNVERIFIED_ERROR
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "sweep_trash_recovery_prefs"
        private const val KEY_LAST_TRASH_URIS = "last_trash_uris"
    }
}
