package com.rdf112.sweep.data.media

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.rdf112.sweep.domain.model.AccessLevel
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaPage
import com.rdf112.sweep.domain.model.MediaPageOutcome
import com.rdf112.sweep.domain.model.MediaPageRequest
import com.rdf112.sweep.domain.model.MediaRecord
import com.rdf112.sweep.domain.model.MediaScanSession
import com.rdf112.sweep.domain.model.MediaSourceRevision
import com.rdf112.sweep.domain.model.PageIssue
import com.rdf112.sweep.domain.model.PageIssueCode
import com.rdf112.sweep.domain.model.SessionFreshness
import com.rdf112.sweep.domain.permissions.MediaPermissionPolicy
import com.rdf112.sweep.domain.repository.MediaPageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.BufferedInputStream
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Adaptador MediaStore compatível com Samsung OneUI, Motorola, Xiaomi e Google Pixel (API 29–36).
 * Corrige o bloqueio de fabricantes que omitem EXTRA_TOTAL_COUNT ou EXTRA_HONORED_ARGS no Bundle
 * do Cursor, garantindo paginação real por cursor sem travar a tela com erro de provedor.
 */
class AndroidMediaPageRepository(
    context: Context,
    private val resolver: ContentResolver = context.contentResolver,
) : MediaPageRepository {
    private val appContext = context.applicationContext

    override suspend fun createSession(): MediaScanSession = withContext(Dispatchers.IO) {
        val access = readAccessSnapshot()
        if (MediaKind.entries.none { kind ->
                access[kind] == AccessLevel.FULL || access[kind] == AccessLevel.PARTIAL
            }
        ) {
            throw NoMediaAccessException()
        }

        val volumes = readVolumeNames()
        if (volumes.isEmpty()) throw MediaVolumeUnavailableException()
        MediaScanSession(
            id = UUID.randomUUID().toString(),
            startedAtEpochMillis = System.currentTimeMillis(),
            accessByKind = access,
            volumeNames = volumes,
            revisionsByVolume = readRevisions(volumes),
        )
    }

    override suspend fun assessSession(session: MediaScanSession): SessionFreshness = withContext(Dispatchers.IO) {
        val currentAccess = readAccessSnapshot()
        val currentVolumes = try {
            readVolumeNames()
        } catch (_: Exception) {
            return@withContext SessionFreshness.UNVERIFIABLE
        }
        val currentRevisions = try {
            readRevisions(currentVolumes)
        } catch (_: Exception) {
            emptyMap()
        }
        session.assessFreshness(currentAccess, currentVolumes, currentRevisions)
    }

    override suspend fun loadPage(
        session: MediaScanSession,
        request: MediaPageRequest,
    ): MediaPage = withContext(Dispatchers.IO) {
        if (request.sessionId != session.id || request.volumeName !in session.volumeNames) {
            return@withContext stalePage(request, PageIssueCode.SESSION_STALE)
        }
        val initialFreshness = assessSession(session)
        when (initialFreshness) {
            SessionFreshness.ACCESS_CHANGED -> return@withContext stalePage(request, PageIssueCode.ACCESS_CHANGED)
            SessionFreshness.VOLUMES_CHANGED -> return@withContext stalePage(request, PageIssueCode.VOLUME_CHANGED)
            SessionFreshness.SOURCE_CHANGED -> return@withContext stalePage(request, PageIssueCode.SESSION_STALE)
            SessionFreshness.REVISION_UNCHANGED, SessionFreshness.UNVERIFIABLE -> Unit
        }

        val access = readAccessSnapshot()[request.kind]
        if (access != AccessLevel.FULL && access != AccessLevel.PARTIAL) {
            return@withContext failedPage(request, PageIssueCode.PERMISSION_MISSING)
        }

        val sessionIssues = buildList {
            if (access == AccessLevel.PARTIAL) {
                add(PageIssue(PageIssueCode.PARTIAL_ACCESS, kind = request.kind, volumeName = request.volumeName))
            }
            if (initialFreshness == SessionFreshness.UNVERIFIABLE) {
                add(PageIssue(PageIssueCode.REVISION_UNVERIFIABLE, kind = request.kind, volumeName = request.volumeName))
            }
        }

        val page = try {
            queryPage(request, sessionIssues)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            return@withContext stalePage(request, PageIssueCode.ACCESS_CHANGED)
        } catch (_: Exception) {
            return@withContext failedPage(request, PageIssueCode.PROVIDER_FAILURE, sessionIssues)
        }

        if (page.outcome != MediaPageOutcome.MORE_AVAILABLE &&
            page.outcome != MediaPageOutcome.END_OF_SOURCE
        ) {
            return@withContext page
        }

        when (assessSession(session)) {
            SessionFreshness.ACCESS_CHANGED -> stalePage(request, PageIssueCode.ACCESS_CHANGED)
            SessionFreshness.VOLUMES_CHANGED -> stalePage(request, PageIssueCode.VOLUME_CHANGED)
            SessionFreshness.SOURCE_CHANGED -> stalePage(request, PageIssueCode.SESSION_STALE)
            SessionFreshness.REVISION_UNCHANGED -> page
            SessionFreshness.UNVERIFIABLE -> {
                if (page.issues.any { it.code == PageIssueCode.REVISION_UNVERIFIABLE }) page
                else copyWithIssue(page, PageIssue(PageIssueCode.REVISION_UNVERIFIABLE, kind = request.kind, volumeName = request.volumeName))
            }
        }
    }

    private fun readAccessSnapshot(): Map<MediaKind, AccessLevel> {
        val granted = buildSet {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ).filterTo(this) { isGranted(it) }
        }
        return MediaKind.entries.associateWith { kind ->
            MediaPermissionPolicy.accessLevel(Build.VERSION.SDK_INT, kind, granted)
        }
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    private fun readVolumeNames(): Set<String> = MediaStore.getExternalVolumeNames(appContext).toSortedSet()

    private fun readRevisions(volumes: Set<String>): Map<String, MediaSourceRevision> =
        volumes.associateWith { volume ->
            val version = runCatching { MediaStore.getVersion(appContext, volume) }
                .getOrNull()
                ?.takeIf(String::isNotBlank)
                ?: MediaSourceRevision.UNAVAILABLE_VERSION
            val generation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching { MediaStore.getGeneration(appContext, volume) }.getOrNull()
            } else {
                null
            }
            MediaSourceRevision(version = version, generation = generation)
        }

    private suspend fun queryPage(
        request: MediaPageRequest,
        inheritedIssues: List<PageIssue>,
    ): MediaPage {
        val collection = collectionFor(request.kind, request.volumeName)
        val projection = arrayOf(
            BaseColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.IS_PENDING,
        ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(MediaStore.MediaColumns.IS_TRASHED)
        } else {
            emptyArray()
        }
        val cursor = queryWithCancellation(collection, projection, createQueryArgs(request))
            ?: return failedPage(request, PageIssueCode.NULL_CURSOR, inheritedIssues)

        cursor.use { c ->
            val extras = runCatching { c.extras }.getOrNull()
            val honored = extras?.getStringArray(ContentResolver.EXTRA_HONORED_ARGS)?.toSet().orEmpty()
            val limitHonoredByProvider = ContentResolver.QUERY_ARG_LIMIT in honored &&
                ContentResolver.QUERY_ARG_OFFSET in honored

            val rawTotal = if (extras != null && extras.containsKey(ContentResolver.EXTRA_TOTAL_COUNT)) {
                runCatching { extras.getLong(ContentResolver.EXTRA_TOTAL_COUNT) }.getOrNull()
            } else {
                null
            }
            val totalCount = rawTotal?.takeIf { it >= 0L }

            val idColumn = c.getColumnIndex(BaseColumns._ID)
            val nameColumn = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val mimeColumn = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val modifiedColumn = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val pendingColumn = c.getColumnIndex(MediaStore.MediaColumns.IS_PENDING)
            val trashedColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
            } else {
                -1
            }
            if (idColumn < 0 || sizeColumn < 0 || mimeColumn < 0) {
                return pagingUnsupported(request, inheritedIssues)
            }

            // Se o provedor (como em alguns Samsung/Xiaomi) não honrar QUERY_ARG_OFFSET no Bundle,
            // avançamos o cursor até request.offset de forma controlada para garantir a paginação real.
            if (!limitHonoredByProvider && request.offset > 0) {
                val targetPos = request.offset - 1
                if (!c.moveToPosition(targetPos)) {
                    return MediaPage(
                        request = request,
                        records = emptyList(),
                        rowsRead = 0,
                        providerTotalCount = totalCount ?: c.count.toLong().coerceAtLeast(0L),
                        nextOffset = null,
                        outcome = MediaPageOutcome.END_OF_SOURCE,
                        issues = inheritedIssues,
                        observedRowKeys = emptyList(),
                    )
                }
            }

            val records = ArrayList<MediaRecord>(request.pageSize)
            val observedRowKeys = ArrayList<String>(request.pageSize)
            val issues = inheritedIssues.toMutableList()
            val ids = HashSet<Long>(request.pageSize)
            var rowsRead = 0

            while (rowsRead < request.pageSize && c.moveToNext()) {
                rowsRead++
                val id = runCatching { c.getLong(idColumn) }.getOrNull()
                val size = runCatching { c.getLong(sizeColumn) }.getOrNull()
                val mime = runCatching { c.getString(mimeColumn) }.getOrNull()
                val expectedPrefix = when (request.kind) {
                    MediaKind.IMAGE -> "image/"
                    MediaKind.VIDEO -> "video/"
                    MediaKind.AUDIO -> "audio/"
                }
                if (id == null || id <= 0L || !ids.add(id)) {
                    issues += PageIssue(PageIssueCode.INVALID_ROW, kind = request.kind, volumeName = request.volumeName)
                    continue
                }
                val rowUri = ContentUris.withAppendedId(collection, id).toString()
                observedRowKeys += rowUri

                val pendingValue = if (pendingColumn < 0 || c.isNull(pendingColumn)) 0 else {
                    runCatching { c.getInt(pendingColumn) }.getOrDefault(0)
                }
                val trashedValue = if (trashedColumn < 0 || c.isNull(trashedColumn)) 0 else {
                    runCatching { c.getInt(trashedColumn) }.getOrDefault(0)
                }
                if (pendingValue != 0 || trashedValue != 0) continue
                if (size == null || size <= 0L || mime.isNullOrBlank() ||
                    !mime.startsWith(expectedPrefix, ignoreCase = true)
                ) {
                    issues += PageIssue(PageIssueCode.INVALID_ROW, kind = request.kind, volumeName = request.volumeName)
                    continue
                }

                val displayName = if (nameColumn >= 0) {
                    runCatching { c.getString(nameColumn) }.getOrNull()?.takeIf(String::isNotBlank)
                } else {
                    null
                }
                val modified = if (modifiedColumn >= 0 && !c.isNull(modifiedColumn)) {
                    runCatching { c.getLong(modifiedColumn) }.getOrNull()
                } else {
                    null
                }
                records += MediaRecord(
                    uri = rowUri,
                    volumeName = request.volumeName,
                    kind = request.kind,
                    displayName = displayName,
                    mimeType = mime,
                    sizeBytes = size,
                    dateModifiedEpochSeconds = modified,
                )
            }

            val absoluteEnd = request.offset.toLong() + rowsRead.toLong()
            if (absoluteEnd > Int.MAX_VALUE.toLong()) {
                return pagingUnsupported(request, issues)
            }
            val effectiveTotal = totalCount ?: if (!limitHonoredByProvider) c.count.toLong().coerceAtLeast(absoluteEnd) else null
            val hasMore = when {
                rowsRead == 0 -> false
                effectiveTotal != null -> absoluteEnd < effectiveTotal
                else -> rowsRead == request.pageSize
            }
            val nextOffset = if (hasMore) absoluteEnd.toInt() else null
            val enrichedRecords = if (request.hashSameSizeCandidates) {
                populateStreamingSha256ForSameSizeCandidates(records)
            } else {
                records
            }
            return MediaPage(
                request = request,
                records = enrichedRecords,
                rowsRead = rowsRead,
                providerTotalCount = effectiveTotal,
                nextOffset = nextOffset,
                outcome = if (hasMore) MediaPageOutcome.MORE_AVAILABLE else MediaPageOutcome.END_OF_SOURCE,
                issues = issues,
                observedRowKeys = observedRowKeys,
            )
        }
    }

    private fun createQueryArgs(request: MediaPageRequest): Bundle = Bundle().apply {
        putInt(ContentResolver.QUERY_ARG_LIMIT, request.pageSize)
        putInt(ContentResolver.QUERY_ARG_OFFSET, request.offset)
        putStringArray(
            ContentResolver.QUERY_ARG_SORT_COLUMNS,
            arrayOf(MediaStore.MediaColumns.DATE_MODIFIED, BaseColumns._ID),
        )
        putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
    }

    private fun collectionFor(kind: MediaKind, volumeName: String): Uri = when (kind) {
        MediaKind.IMAGE -> MediaStore.Images.Media.getContentUri(volumeName)
        MediaKind.VIDEO -> MediaStore.Video.Media.getContentUri(volumeName)
        MediaKind.AUDIO -> MediaStore.Audio.Media.getContentUri(volumeName)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun queryWithCancellation(
        uri: Uri,
        projection: Array<String>,
        args: Bundle,
    ): Cursor? = suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        if (!continuation.isActive) return@suspendCancellableCoroutine
        try {
            val cursor = resolver.query(uri, projection, args, signal)
            continuation.resume(cursor) { _, value, _ -> value?.close() }
        } catch (cancelled: OperationCanceledException) {
            if (continuation.isActive) {
                continuation.resumeWithException(
                    CancellationException("MediaStore query cancelled").apply { initCause(cancelled) }
                )
            }
        } catch (error: Exception) {
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }

    private fun copyWithIssue(page: MediaPage, issue: PageIssue): MediaPage = MediaPage(
        request = page.request,
        records = page.records,
        rowsRead = page.rowsRead,
        providerTotalCount = page.providerTotalCount,
        nextOffset = page.nextOffset,
        outcome = page.outcome,
        issues = page.issues + issue,
        observedRowKeys = page.observedRowKeys,
    )

    private fun failedPage(
        request: MediaPageRequest,
        code: PageIssueCode,
        inheritedIssues: List<PageIssue> = emptyList(),
    ): MediaPage = MediaPage(
        request = request,
        records = emptyList(),
        rowsRead = 0,
        providerTotalCount = null,
        nextOffset = null,
        outcome = MediaPageOutcome.FAILED,
        issues = inheritedIssues + PageIssue(code, kind = request.kind, volumeName = request.volumeName),
    )

    private fun stalePage(request: MediaPageRequest, code: PageIssueCode): MediaPage = MediaPage(
        request = request,
        records = emptyList(),
        rowsRead = 0,
        providerTotalCount = null,
        nextOffset = null,
        outcome = MediaPageOutcome.SESSION_STALE,
        issues = listOf(PageIssue(code, kind = request.kind, volumeName = request.volumeName)),
    )

    private fun pagingUnsupported(
        request: MediaPageRequest,
        inheritedIssues: List<PageIssue> = emptyList(),
    ): MediaPage = MediaPage(
        request = request,
        records = emptyList(),
        rowsRead = 0,
        providerTotalCount = null,
        nextOffset = null,
        outcome = MediaPageOutcome.PAGING_UNSUPPORTED,
        issues = inheritedIssues + PageIssue(PageIssueCode.PAGING_UNSUPPORTED, kind = request.kind, volumeName = request.volumeName),
    )
    /**
     * Calcula o hash criptográfico SHA-256 em fluxo (buffers de 8 KB, sem carregar o arquivo inteiro na RAM)
     * exclusivamente para registros que possuem tamanho idêntico (> 0) na página atual, permitindo
     * comprovar duplicatas exatas sem falsos positivos.
     */
    private suspend fun populateStreamingSha256ForSameSizeCandidates(records: List<MediaRecord>): List<MediaRecord> {
        val candidateSizes = records
            .filter { it.sizeBytes > 0L }
            .groupBy { it.sizeBytes }
            .filterValues { it.size >= 2 }
            .keys
        if (candidateSizes.isEmpty()) return records
        return records.map { record ->
            if (record.sizeBytes in candidateSizes) {
                val hash = computeStreamingSha256(Uri.parse(record.uri))
                if (hash != null) record.copy(contentSha256 = hash) else record
            } else {
                record
            }
        }
    }

    override suspend fun computeSha256(uri: String): String? =
        withContext(Dispatchers.IO) {
            computeStreamingSha256(Uri.parse(uri))
        }

    private suspend fun computeStreamingSha256(uri: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val stream = resolver.openInputStream(uri) ?: return null
            BufferedInputStream(stream, 8192).use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read > 0) digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
}

class NoMediaAccessException : IllegalStateException("Photo and video access has not been granted.")
class MediaVolumeUnavailableException : IllegalStateException("No external media volume is currently available.")
