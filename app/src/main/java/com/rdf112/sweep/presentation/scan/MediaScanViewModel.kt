package com.rdf112.sweep.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rdf112.sweep.data.media.NoMediaAccessException
import com.rdf112.sweep.domain.grouping.SmartFilterMode
import com.rdf112.sweep.domain.oaas.DesiredOutcomeMission
import com.rdf112.sweep.domain.oaas.OutcomeExecutionPlan
import com.rdf112.sweep.domain.oaas.OutcomeServiceEngine
import com.rdf112.sweep.domain.grouping.SmartMediaGroupingPolicy
import com.rdf112.sweep.domain.model.AccessLevel
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaPage
import com.rdf112.sweep.domain.model.MediaPageOutcome
import com.rdf112.sweep.domain.model.MediaPageRequest
import com.rdf112.sweep.domain.model.MediaRecord
import com.rdf112.sweep.domain.model.MediaScanSession
import com.rdf112.sweep.domain.model.SessionFreshness
import com.rdf112.sweep.domain.paging.MediaPageSequenceGuard
import com.rdf112.sweep.domain.paging.PageSequenceDecision
import com.rdf112.sweep.domain.selection.MediaSelectionLedger
import com.rdf112.sweep.domain.selection.SelectionUpdate
import com.rdf112.sweep.domain.repository.MediaPageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

const val SWEEP_PAGE_SIZE = 100
const val SWEEP_PAGE_HISTORY_LIMIT = 50
const val SWEEP_GLOBAL_SCAN_MAX_ROWS = 250_000
const val SWEEP_GLOBAL_DUPLICATE_CANDIDATES_LIMIT = 20_000
const val SWEEP_GLOBAL_UNIQUE_SIZE_LIMIT = 100_000

enum class ScanUiStatus {
    IDLE,
    STARTING,
    LOADING_PAGE,
    READY,
    PARTIAL,
    FAILED,
    PAGING_UNSUPPORTED,
    NO_ACCESS,
    SESSION_STALE,
    CANCELLED,
}

data class GlobalMissionProgress(
    val mission: DesiredOutcomeMission,
    val pagesProcessed: Int = 0,
    val recordsExamined: Long = 0L,
    val sourcesProcessed: Int = 0,
    val sourceCount: Int = 0,
    val isRunning: Boolean = true,
    val plan: OutcomeExecutionPlan? = null,
    val coverageComplete: Boolean = false,
    val warningCount: Int = 0,
    val message: String? = null,
)

data class MediaScanUiState(
    val status: ScanUiStatus = ScanUiStatus.IDLE,
    val session: MediaScanSession? = null,
    val selectedKind: MediaKind = MediaKind.IMAGE,
    val selectedVolume: String? = null,
    val smartFilter: SmartFilterMode = SmartFilterMode.ALL_ITEMS,
    val currentPage: MediaPage? = null,
    val pageHistory: List<Int> = emptyList(),
    val selection: MediaSelectionLedger = MediaSelectionLedger.empty(),
    val message: String? = null,
    val isLoading: Boolean = false,
    val missionProgress: GlobalMissionProgress? = null,
) {
    val accessLevel: AccessLevel
        get() = session?.accessByKind?.get(selectedKind) ?: AccessLevel.DENIED
    val availableVolumes: List<String>
        get() = session?.volumeNames?.sortedWith(compareBy<String> { it != "external_primary" }.thenBy { it }) ?: emptyList()
    val canLoadNext: Boolean
        get() = !isLoading && status in setOf(ScanUiStatus.READY, ScanUiStatus.PARTIAL) &&
            currentPage?.outcome == MediaPageOutcome.MORE_AVAILABLE
    val canLoadPrevious: Boolean
        get() = !isLoading && status in setOf(ScanUiStatus.READY, ScanUiStatus.PARTIAL) && pageHistory.isNotEmpty()
    val shouldShowReviewAccessAction: Boolean
        get() {
            if (status == ScanUiStatus.FAILED) {
                val codes = currentPage?.issues?.map { it.code }.orEmpty()
                val providerOnly = codes.isNotEmpty() && codes.all {
                    it == com.rdf112.sweep.domain.model.PageIssueCode.PROVIDER_FAILURE ||
                        it == com.rdf112.sweep.domain.model.PageIssueCode.NULL_CURSOR
                }
                if (providerOnly) return false
            }
            return true
        }
}

/** Orchestrates bounded pages and URI-based selection; no Activity or Android Context is retained. */
class MediaScanViewModel(
    private val repository: MediaPageRepository,
    private val missionDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MediaScanUiState())
    val state: StateFlow<MediaScanUiState> = mutableState.asStateFlow()
    private var pageJob: Job? = null
    private var sessionJob: Job? = null
    private var missionJob: Job? = null
    private data class PageSourceKey(val sessionId: String, val kind: MediaKind, val volumeName: String)
    private val sequenceGuards = mutableMapOf<PageSourceKey, MediaPageSequenceGuard>()

    fun startSession() {
        missionJob?.cancel()
        sessionJob?.cancel()
        pageJob?.cancel()
        sequenceGuards.clear()
        mutableState.value = MediaScanUiState(status = ScanUiStatus.STARTING, isLoading = true)
        sessionJob = viewModelScope.launch {
            try {
                val session = repository.createSession()
                coroutineContext.ensureActive()
                val allowedKind = listOf(MediaKind.IMAGE, MediaKind.VIDEO, MediaKind.AUDIO).firstOrNull { kind ->
                    val access = session.accessByKind[kind]
                    access == AccessLevel.FULL || access == AccessLevel.PARTIAL
                } ?: run {
                    mutableState.value = MediaScanUiState(status = ScanUiStatus.NO_ACCESS)
                    return@launch
                }
                val volume = session.volumeNames.sortedWith(
                    compareBy<String> { it != "external_primary" }.thenBy { it }
                ).first()
                mutableState.value = MediaScanUiState(
                    status = ScanUiStatus.LOADING_PAGE,
                    session = session,
                    selectedKind = allowedKind,
                    selectedVolume = volume,
                    isLoading = true,
                )
                loadPageAt(offset = 0, history = emptyList())
            } catch (cancelled: CancellationException) {
                // A replacement session or explicit cancel owns the UI state transition.
                throw cancelled
            } catch (error: Exception) {
                coroutineContext.ensureActive()
                mutableState.value = MediaScanUiState(
                    status = if (error is NoMediaAccessException) ScanUiStatus.NO_ACCESS else ScanUiStatus.FAILED,
                    message = null,
                    isLoading = false,
                )
            }
        }
    }

    fun cancelLoading() {
        missionJob?.cancel()
        pageJob?.cancel()
        sessionJob?.cancel()
        val current = mutableState.value
        mutableState.value = current.copy(
            status = ScanUiStatus.CANCELLED,
            isLoading = false,
            missionProgress = current.missionProgress?.copy(
                isRunning = false,
                plan = null,
                coverageComplete = false,
                message = "cancelled",
            ),
        )
    }

    fun selectKind(kind: MediaKind) {
        val current = mutableState.value
        val session = current.session ?: return
        val access = session.accessByKind[kind]
        if (access != AccessLevel.FULL && access != AccessLevel.PARTIAL) {
            mutableState.value = current.copy(message = null)
            return
        }
        if (current.selectedKind == kind) return
        mutableState.value = current.copy(selectedKind = kind, currentPage = null, pageHistory = emptyList(), message = null)
        loadPageAt(0, emptyList())
    }

    fun selectVolume(volumeName: String) {
        val current = mutableState.value
        if (volumeName !in current.availableVolumes || current.selectedVolume == volumeName) return
        mutableState.value = current.copy(selectedVolume = volumeName, currentPage = null, pageHistory = emptyList(), message = null)
        loadPageAt(0, emptyList())
    }

    fun selectSmartFilter(filter: SmartFilterMode) {
        val current = mutableState.value
        if (current.smartFilter == filter) return
        mutableState.value = current.copy(smartFilter = filter, message = null)
    }

    /**
     * Executa automaticamente as etapas OaaS (Buscar -> Conferir -> Preencher -> Reconciliar)
     * para entregar o resultado pronto na seleção de revisão em 1 toque.
     */
    fun executeOutcomeMission(mission: DesiredOutcomeMission, nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): OutcomeExecutionPlan? {
        val current = mutableState.value
        if (current.isLoading) return null
        val page = current.currentPage ?: return null
        val targetFilter = when (mission) {
            DesiredOutcomeMission.FREE_MAX_SPACE_NOW -> SmartFilterMode.LARGE_FILES_10MB
            DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES -> SmartFilterMode.SAME_SIZE_CANDIDATES
            DesiredOutcomeMission.ARCHIVE_OLD_MEDIA_30D -> SmartFilterMode.OLDER_THAN_30_DAYS
        }
        val plan = OutcomeServiceEngine.buildAutomatedOutcomePlan(
            mission = mission,
            pageRecords = page.records,
            nowEpochSeconds = nowEpochSeconds,
        )
        var nextLedger = MediaSelectionLedger.empty()
        for (record in plan.autoSelectedRecords) {
            val update = nextLedger.setSelected(record, true)
            if (update is SelectionUpdate.Applied) {
                nextLedger = update.ledger
            }
        }
        mutableState.value = current.copy(
            smartFilter = targetFilter,
            selection = nextLedger,
            message = null,
        )
        return plan
    }

    /**
     * Runs one mission across every accessible media kind, volume and MediaStore page.
     * Results are labeled partial whenever permissions, providers, revisions or safety limits
     * prevent the app from proving complete coverage.
     */
    fun executeGlobalOutcomeMission(
        mission: DesiredOutcomeMission,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L,
    ) {
        missionJob?.cancel()
        pageJob?.cancel()
        sessionJob?.cancel()

        val startingState = mutableState.value
        mutableState.value = startingState.copy(
            status = ScanUiStatus.LOADING_PAGE,
            isLoading = true,
            message = null,
            missionProgress = GlobalMissionProgress(mission = mission),
        )

        missionJob = viewModelScope.launch(missionDispatcher) {
            try {
                val session = startingState.session ?: repository.createSession()
                coroutineContext.ensureActive()

                val allKinds = MediaKind.entries
                val accessibleKinds = allKinds.filter { kind ->
                    session.accessByKind[kind] == AccessLevel.FULL ||
                        session.accessByKind[kind] == AccessLevel.PARTIAL
                }
                if (accessibleKinds.isEmpty()) {
                    throw NoMediaAccessException()
                }

                val volumes = session.volumeNames.sortedWith(
                    compareBy<String> { it != "external_primary" }.thenBy { it }
                )
                val sources = accessibleKinds.flatMap { kind -> volumes.map { volume -> kind to volume } }
                var coverageComplete = allKinds.all { session.accessByKind[it] == AccessLevel.FULL }
                var warningCount = allKinds.count { session.accessByKind[it] != AccessLevel.FULL }
                var pagesProcessed = 0
                var recordsExamined = 0L
                var sourcesProcessed = 0
                var fatalStaleness = false
                var safetyLimitReached = false
                val sequenceGuards = mutableMapOf<Pair<MediaKind, String>, MediaPageSequenceGuard>()

                // For large/old missions, retain only the highest-impact 500 candidates.
                val rankedCandidates = ArrayList<MediaRecord>(500)

                // For exact duplicates, retain one record per observed size and expand only repeated sizes.
                // A hard cap prevents an extremely large library from exhausting application memory.
                val firstBySize = LinkedHashMap<Long, MutableList<MediaRecord>>()
                var duplicateCandidateRows = 0

                fun publishProgress(message: String? = null) {
                    val latest = mutableState.value
                    mutableState.value = latest.copy(
                        status = ScanUiStatus.LOADING_PAGE,
                        isLoading = true,
                        missionProgress = GlobalMissionProgress(
                            mission = mission,
                            pagesProcessed = pagesProcessed,
                            recordsExamined = recordsExamined,
                            sourcesProcessed = sourcesProcessed,
                            sourceCount = sources.size,
                            isRunning = true,
                            coverageComplete = false,
                            warningCount = warningCount,
                            message = message,
                        ),
                    )
                }

                if (mission == DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES) {
                    // No candidate-size map is shared with other missions.
                }

                sourceLoop@ for ((kind, volume) in sources) {
                    var offset = 0
                    val guard = sequenceGuards.getOrPut(kind to volume) { MediaPageSequenceGuard() }
                    while (true) {
                        coroutineContext.ensureActive()
                        val request = MediaPageRequest(
                            sessionId = session.id,
                            kind = kind,
                            volumeName = volume,
                            offset = offset,
                            pageSize = SWEEP_PAGE_SIZE,
                        )
                        val page = repository.loadPage(session, request)
                        coroutineContext.ensureActive()
                        pagesProcessed++
                        recordsExamined += page.rowsRead.toLong()

                        if (page.issues.isNotEmpty()) {
                            warningCount += page.issues.size
                            coverageComplete = false
                        }

                        when (page.outcome) {
                            MediaPageOutcome.SESSION_STALE -> {
                                fatalStaleness = true
                                coverageComplete = false
                                warningCount++
                                break@sourceLoop
                            }
                            MediaPageOutcome.PAGING_UNSUPPORTED,
                            MediaPageOutcome.FAILED -> {
                                coverageComplete = false
                                warningCount++
                                break
                            }
                            MediaPageOutcome.CANCELLED -> throw CancellationException("MediaStore page cancelled")
                            MediaPageOutcome.MORE_AVAILABLE,
                            MediaPageOutcome.END_OF_SOURCE -> Unit
                        }

                        when (guard.observe(page)) {
                            PageSequenceDecision.OVERLAPPING_ITEMS -> {
                                coverageComplete = false
                                warningCount++
                                break
                            }
                            PageSequenceDecision.SAME_OFFSET_CHANGED_CONTENT -> {
                                fatalStaleness = true
                                coverageComplete = false
                                warningCount++
                                break@sourceLoop
                            }
                            PageSequenceDecision.ACCEPTED,
                            PageSequenceDecision.SAME_OFFSET_SAME_CONTENT -> Unit
                        }

                        for (record in page.records) {
                            when (mission) {
                                DesiredOutcomeMission.FREE_MAX_SPACE_NOW -> {
                                    if (record.sizeBytes >= SmartMediaGroupingPolicy.LARGE_FILE_THRESHOLD_BYTES) {
                                        rankedCandidates += record
                                        if (rankedCandidates.size > 500) {
                                            rankedCandidates.sortByDescending { it.sizeBytes }
                                            rankedCandidates.removeAt(rankedCandidates.lastIndex)
                                        }
                                    }
                                }
                                DesiredOutcomeMission.ARCHIVE_OLD_MEDIA_30D -> {
                                    val modified = record.dateModifiedEpochSeconds
                                    val cutoff = (nowEpochSeconds - SmartMediaGroupingPolicy.THIRTY_DAYS_SECONDS).coerceAtLeast(0L)
                                    if (modified != null && modified in 1..cutoff) {
                                        rankedCandidates += record
                                        if (rankedCandidates.size > 500) {
                                            rankedCandidates.sortByDescending { it.sizeBytes }
                                            rankedCandidates.removeAt(rankedCandidates.lastIndex)
                                        }
                                    }
                                }
                                DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES -> {
                                    val bucket = firstBySize[record.sizeBytes]
                                    if (bucket == null) {
                                        if (firstBySize.size >= SWEEP_GLOBAL_UNIQUE_SIZE_LIMIT) {
                                            safetyLimitReached = true
                                            coverageComplete = false
                                            warningCount++
                                            break
                                        }
                                        firstBySize[record.sizeBytes] = mutableListOf(record)
                                    } else {
                                        bucket += record
                                        duplicateCandidateRows++
                                        if (duplicateCandidateRows > SWEEP_GLOBAL_DUPLICATE_CANDIDATES_LIMIT) {
                                            safetyLimitReached = true
                                            coverageComplete = false
                                            warningCount++
                                            break
                                        }
                                    }
                                }
                            }
                        }

                        if (safetyLimitReached) break@sourceLoop
                        if (recordsExamined >= SWEEP_GLOBAL_SCAN_MAX_ROWS) {
                            safetyLimitReached = true
                            coverageComplete = false
                            warningCount++
                            break@sourceLoop
                        }

                        if (page.outcome == MediaPageOutcome.END_OF_SOURCE) break
                        val next = page.nextOffset
                        if (next == null || next <= offset) {
                            coverageComplete = false
                            warningCount++
                            break
                        }
                        offset = next
                        if (pagesProcessed % 5 == 0) publishProgress()
                    }
                    sourcesProcessed++
                    publishProgress()
                }

                if (fatalStaleness) {
                    invalidateSession("session_source_changed")
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        missionProgress = GlobalMissionProgress(
                            mission = mission,
                            pagesProcessed = pagesProcessed,
                            recordsExamined = recordsExamined,
                            sourcesProcessed = sourcesProcessed,
                            sourceCount = sources.size,
                            isRunning = false,
                            coverageComplete = false,
                            warningCount = warningCount,
                            message = "session_stale",
                        ),
                    )
                    return@launch
                }

                val missionRecords = when (mission) {
                    DesiredOutcomeMission.FREE_MAX_SPACE_NOW,
                    DesiredOutcomeMission.ARCHIVE_OLD_MEDIA_30D -> rankedCandidates
                    DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES -> {
                        val groups = firstBySize.values.filter { it.size >= 2 }
                        val candidateRows = groups.sumOf { it.size }
                        if (candidateRows > SWEEP_GLOBAL_DUPLICATE_CANDIDATES_LIMIT) {
                            coverageComplete = false
                            warningCount++
                            emptyList()
                        } else {
                            val hashed = ArrayList<MediaRecord>(candidateRows)
                            for (group in groups) {
                                for (record in group) {
                                    coroutineContext.ensureActive()
                                    val hash = record.contentSha256?.takeIf(String::isNotBlank)
                                        ?: repository.computeSha256(record.uri)
                                    if (hash.isNullOrBlank()) {
                                        coverageComplete = false
                                        warningCount++
                                        hashed += record.copy(contentSha256 = null)
                                    } else {
                                        hashed += record.copy(contentSha256 = hash)
                                    }
                                }
                            }
                            hashed
                        }
                    }
                }

                val freshness = runCatching { repository.assessSession(session) }
                    .getOrElse { SessionFreshness.UNVERIFIABLE }
                if (freshness in setOf(
                        SessionFreshness.ACCESS_CHANGED,
                        SessionFreshness.VOLUMES_CHANGED,
                        SessionFreshness.SOURCE_CHANGED,
                    )
                ) {
                    invalidateSession("session_source_changed")
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        missionProgress = GlobalMissionProgress(
                            mission = mission,
                            pagesProcessed = pagesProcessed,
                            recordsExamined = recordsExamined,
                            sourcesProcessed = sourcesProcessed,
                            sourceCount = sources.size,
                            isRunning = false,
                            coverageComplete = false,
                            warningCount = warningCount + 1,
                            message = "session_stale",
                        ),
                    )
                    return@launch
                }
                if (freshness == SessionFreshness.UNVERIFIABLE) {
                    coverageComplete = false
                    warningCount++
                }

                val generatedPlan = OutcomeServiceEngine.buildAutomatedOutcomePlan(
                    mission = mission,
                    pageRecords = missionRecords,
                    nowEpochSeconds = nowEpochSeconds,
                )
                // A duplicate group cannot safely choose the original unless every accessible
                // source was scanned and every relevant hash was verified.
                val plan = if (
                    mission == DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES && !coverageComplete
                ) {
                    generatedPlan.copy(
                        autoSelectedRecords = emptyList(),
                        estimatedDeliverableBytes = 0L,
                    )
                } else {
                    generatedPlan
                }
                var nextLedger = MediaSelectionLedger.empty()
                for (record in plan.autoSelectedRecords) {
                    val update = nextLedger.setSelected(record, true)
                    if (update is SelectionUpdate.Applied) nextLedger = update.ledger
                }

                val latest = mutableState.value
                mutableState.value = latest.copy(
                    status = if (coverageComplete) ScanUiStatus.READY else ScanUiStatus.PARTIAL,
                    session = session,
                    smartFilter = when (mission) {
                        DesiredOutcomeMission.FREE_MAX_SPACE_NOW -> SmartFilterMode.LARGE_FILES_10MB
                        DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES -> SmartFilterMode.SAME_SIZE_CANDIDATES
                        DesiredOutcomeMission.ARCHIVE_OLD_MEDIA_30D -> SmartFilterMode.OLDER_THAN_30_DAYS
                    },
                    selection = nextLedger,
                    message = null,
                    isLoading = false,
                    missionProgress = GlobalMissionProgress(
                        mission = mission,
                        pagesProcessed = pagesProcessed,
                        recordsExamined = recordsExamined,
                        sourcesProcessed = sourcesProcessed,
                        sourceCount = sources.size,
                        isRunning = false,
                        plan = plan,
                        coverageComplete = coverageComplete,
                        warningCount = warningCount,
                        message = if (safetyLimitReached) "safety_limit_reached" else null,
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                coroutineContext.ensureActive()
                val current = mutableState.value
                mutableState.value = current.copy(
                    status = ScanUiStatus.FAILED,
                    isLoading = false,
                    missionProgress = current.missionProgress?.copy(
                        isRunning = false,
                        plan = null,
                        coverageComplete = false,
                        warningCount = current.missionProgress.warningCount + 1,
                        message = "scan_failed",
                    ),
                )
            }
        }
    }

    fun loadNextPage() {
        val current = mutableState.value
        val currentPage = current.currentPage ?: return
        val next = currentPage.nextOffset ?: return
        val currentOffset = currentPage.request.offset
        loadPageAt(next, (current.pageHistory + currentOffset).takeLast(SWEEP_PAGE_HISTORY_LIMIT))
    }

    fun loadPreviousPage() {
        val current = mutableState.value
        val previous = current.pageHistory.lastOrNull() ?: return
        loadPageAt(previous, current.pageHistory.dropLast(1))
    }

    fun retryCurrentPage() {
        val current = mutableState.value
        val offset = current.currentPage?.request?.offset ?: 0
        loadPageAt(offset, current.pageHistory)
    }

    fun setSelected(record: MediaRecord, selected: Boolean) {
        val current = mutableState.value
        if (current.isLoading) return
        // Removing an item from the review list must work even when a global mission
        // selected it from a page that is no longer visible.
        val candidate = if (!selected) {
            current.selection.records.firstOrNull { it.uri == record.uri } ?: return
        } else {
            val visibleRecord = current.currentPage?.records?.firstOrNull { it.uri == record.uri } ?: return
            if (visibleRecord != record) return
            visibleRecord
        }
        val update = current.selection.setSelected(candidate, selected)
        when (update) {
            is SelectionUpdate.Applied -> mutableState.value = current.copy(selection = update.ledger, message = null)
            is SelectionUpdate.LimitReached -> mutableState.value = current.copy(message = "selection_limit_reached")
            is SelectionUpdate.NotSelectable -> mutableState.value = current.copy(message = "item_not_selectable")
        }
    }

    fun clearSelection() {
        mutableState.value = mutableState.value.copy(selection = MediaSelectionLedger.empty())
    }

    /** Generates a plain-text summary of the current review selection for verified Pro export. */
    fun buildReviewSummaryExportText(): String? {
        val current = mutableState.value
        val selected = current.selection.records
        if (selected.isEmpty()) return null
        val totalBytes = current.selection.totalBytesOrNull() ?: 0L
        return buildString {
            appendLine("Midiastore — Resumo de Revisão de Mídias")
            appendLine("Itens selecionados: ${selected.size}")
            appendLine("Bytes somados: $totalBytes")
            appendLine("---")
            selected.forEachIndexed { idx, item ->
                val name = item.displayName?.takeIf(String::isNotBlank) ?: "sem_nome"
                appendLine("${idx + 1}. [$name] (${item.kind}) - ${item.sizeBytes} bytes - volume=${item.volumeName}")
            }
        }.trimEnd()
    }

    /** Called from Activity.onResume; API 29 is explicitly unverifiable, not assumed stable. */
    fun checkSessionFreshness() {
        val currentState = mutableState.value
        if (currentState.isLoading && currentState.missionProgress?.isRunning == true) return
        val session = currentState.session ?: return
        // Android does not expose a stable, complete snapshot of the user-selected media set.
        // After the app resumes, discard partial-access pages and their review selection rather
        // than assuming the same subset is still authorized.
        if (session.accessByKind.values.any { it == AccessLevel.PARTIAL }) {
            invalidateSession("partial_access_requires_refresh")
            return
        }
        viewModelScope.launch {
            val freshness = try {
                repository.assessSession(session).also { coroutineContext.ensureActive() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SessionFreshness.UNVERIFIABLE
            }
            if (mutableState.value.session?.id != session.id) return@launch
            when (freshness) {
                SessionFreshness.ACCESS_CHANGED,
                SessionFreshness.VOLUMES_CHANGED,
                SessionFreshness.SOURCE_CHANGED -> invalidateSession("session_source_changed")
                SessionFreshness.REVISION_UNCHANGED -> Unit
                SessionFreshness.UNVERIFIABLE -> {
                    val current = mutableState.value
                    if (current.session?.id == session.id) {
                        val page = current.currentPage?.let { existing ->
                            if (existing.issues.any { it.code == com.rdf112.sweep.domain.model.PageIssueCode.REVISION_UNVERIFIABLE }) {
                                existing
                            } else {
                                MediaPage(
                                    request = existing.request,
                                    records = existing.records,
                                    rowsRead = existing.rowsRead,
                                    providerTotalCount = existing.providerTotalCount,
                                    nextOffset = existing.nextOffset,
                                    outcome = existing.outcome,
                                    issues = existing.issues + com.rdf112.sweep.domain.model.PageIssue(
                                        code = com.rdf112.sweep.domain.model.PageIssueCode.REVISION_UNVERIFIABLE,
                                        kind = existing.request.kind,
                                        volumeName = existing.request.volumeName,
                                    ),
                                    observedRowKeys = existing.observedRowKeys,
                                )
                            }
                        }
                        mutableState.value = current.copy(
                            status = ScanUiStatus.PARTIAL,
                            currentPage = page,
                            message = if (page == null) "revision_unverifiable" else null,
                        )
                    }
                }
            }
        }
    }

    private fun invalidateSession(message: String) {
        pageJob?.cancel()
        sequenceGuards.clear()
        val current = mutableState.value
        mutableState.value = current.copy(
            status = ScanUiStatus.SESSION_STALE,
            session = null,
            currentPage = null,
            pageHistory = emptyList(),
            selection = MediaSelectionLedger.empty(),
            message = message,
            isLoading = false,
        )
    }

    private fun loadPageAt(offset: Int, history: List<Int>) {
        pageJob?.cancel()
        val current = mutableState.value
        val session = current.session ?: return
        val volume = current.selectedVolume ?: return
        val request = runCatching {
            MediaPageRequest(
                sessionId = session.id,
                kind = current.selectedKind,
                volumeName = volume,
                offset = offset,
                pageSize = SWEEP_PAGE_SIZE,
            )
        }.getOrElse {
            mutableState.value = current.copy(status = ScanUiStatus.FAILED, message = null, isLoading = false)
            return
        }
        mutableState.value = current.copy(status = ScanUiStatus.LOADING_PAGE, isLoading = true, message = null)
        pageJob = viewModelScope.launch {
            try {
                val page = repository.loadPage(session, request)
                coroutineContext.ensureActive()
                val latest = mutableState.value
                if (latest.session?.id != session.id) return@launch
                if (page.outcome == MediaPageOutcome.MORE_AVAILABLE ||
                    page.outcome == MediaPageOutcome.END_OF_SOURCE
                ) {
                    val sourceKey = PageSourceKey(session.id, request.kind, request.volumeName)
                    val guard = sequenceGuards.getOrPut(sourceKey) { MediaPageSequenceGuard() }
                    when (guard.observe(page)) {
                        PageSequenceDecision.OVERLAPPING_ITEMS -> {
                            mutableState.value = latest.copy(
                                status = ScanUiStatus.PAGING_UNSUPPORTED,
                                isLoading = false,
                                message = null,
                            )
                            return@launch
                        }
                        PageSequenceDecision.SAME_OFFSET_CHANGED_CONTENT -> {
                            invalidateSession("page_content_changed")
                            return@launch
                        }
                        PageSequenceDecision.ACCEPTED,
                        PageSequenceDecision.SAME_OFFSET_SAME_CONTENT -> Unit
                    }
                }
                when (page.outcome) {
                    MediaPageOutcome.SESSION_STALE -> invalidateSession("session_source_changed")
                    MediaPageOutcome.PAGING_UNSUPPORTED -> mutableState.value = latest.copy(
                        status = ScanUiStatus.PAGING_UNSUPPORTED,
                        currentPage = page,
                        pageHistory = history,
                        isLoading = false,
                        message = null,
                    )
                    MediaPageOutcome.FAILED -> mutableState.value = latest.copy(
                        status = ScanUiStatus.FAILED,
                        currentPage = page,
                        pageHistory = history,
                        isLoading = false,
                        message = null,
                    )
                    MediaPageOutcome.CANCELLED -> mutableState.value = latest.copy(status = ScanUiStatus.CANCELLED, isLoading = false)
                    MediaPageOutcome.MORE_AVAILABLE,
                    MediaPageOutcome.END_OF_SOURCE -> mutableState.value = latest.copy(
                        status = if (page.issues.isEmpty()) ScanUiStatus.READY else ScanUiStatus.PARTIAL,
                        currentPage = page,
                        pageHistory = history,
                        isLoading = false,
                        message = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                // Do not let a superseded page mark the replacement page as cancelled.
                throw cancelled
            } catch (_: Exception) {
                coroutineContext.ensureActive()
                mutableState.value = mutableState.value.copy(
                    status = ScanUiStatus.FAILED,
                    message = null,
                    isLoading = false,
                )
            }
        }
    }

    class Factory(private val repository: MediaPageRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!modelClass.isAssignableFrom(MediaScanViewModel::class.java)) {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
            return MediaScanViewModel(repository, missionDispatcher = Dispatchers.Default) as T
        }
    }
}
