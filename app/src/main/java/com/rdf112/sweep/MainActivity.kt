package com.rdf112.sweep

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.format.Formatter
import android.text.style.ForegroundColorSpan
import android.util.LruCache
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.rdf112.sweep.data.WhatsAppCleanerDataSource
import com.rdf112.sweep.data.WhatsAppMediaCategory
import com.rdf112.sweep.data.WhatsAppMediaItem
import com.rdf112.sweep.data.billing.AndroidPlayBillingGateway
import com.rdf112.sweep.data.billing.PlayCatalogProductUi
import com.rdf112.sweep.data.media.AndroidBlurPhotoScanner
import com.rdf112.sweep.data.media.AndroidHardwareVideoCompressor
import com.rdf112.sweep.data.media.AndroidMediaBreakdownReader
import com.rdf112.sweep.data.media.AndroidMediaPageRepository
import com.rdf112.sweep.data.media.AndroidSimilarPhotoScanner
import com.rdf112.sweep.data.media.AndroidTrashOperationCoordinator
import com.rdf112.sweep.data.media.Api29DeleteAttempt
import com.rdf112.sweep.data.media.VideoCompressionOutcome
import com.rdf112.sweep.data.oaas.AndroidOutcomeTelemetryStore
import com.rdf112.sweep.domain.analysis.SimilarPhotoGroup
import com.rdf112.sweep.domain.analysis.VideoCompressionPlanner
import com.rdf112.sweep.util.InAppReviewCoordinator
import com.rdf112.sweep.util.ShareAchievementCardRenderer
import com.rdf112.sweep.work.WeeklyDigestScheduler
import com.rdf112.sweep.domain.billing.BillingStatePolicy
import com.rdf112.sweep.domain.billing.ProAccessSummaryState
import com.rdf112.sweep.domain.grouping.SmartFilterMode
import com.rdf112.sweep.domain.grouping.SmartMediaGroupingPolicy
import com.rdf112.sweep.domain.model.AccessLevel
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import com.rdf112.sweep.domain.oaas.DesiredOutcomeMission
import com.rdf112.sweep.domain.operations.MediaOperationMode
import com.rdf112.sweep.domain.operations.ObservedMediaPostState
import com.rdf112.sweep.domain.permissions.MediaPermissionPolicy
import com.rdf112.sweep.domain.storage.StorageUsageCalculator
import com.rdf112.sweep.presentation.scan.GlobalMissionProgress
import com.rdf112.sweep.presentation.scan.MediaScanUiState
import com.rdf112.sweep.presentation.scan.MediaScanViewModel
import com.rdf112.sweep.presentation.scan.ScanUiStatus
import com.rdf112.sweep.presentation.ui.CleanCollectionUiModel
import com.rdf112.sweep.presentation.ui.CleanMission
import com.rdf112.sweep.presentation.ui.CleanerHomeScreen
import com.rdf112.sweep.presentation.ui.FilesMariaHeaderSection
import com.rdf112.sweep.presentation.ui.HomeUiState
import com.rdf112.sweep.presentation.ui.MariaI18n
import com.rdf112.sweep.presentation.ui.MediaFullscreenPreviewDialog
import com.rdf112.sweep.presentation.ui.ProGeminiNanoBottomCard
import com.rdf112.sweep.presentation.ui.ProMariaHeaderSection
import com.rdf112.sweep.presentation.ui.StorageBarSegment
import com.rdf112.sweep.presentation.ui.StorageRingGaugeView
import com.rdf112.sweep.presentation.ui.StorageSegmentedBarView
import com.rdf112.sweep.presentation.ui.theme.MidiastoreTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Midiastore (`com.rdf112.midiastore`, namespace interno `com.rdf112.sweep`).
 * Limpador de armazenamento 100% on-device em 3 abas puras (Limpar, Arquivos, Pro) com Scoped Storage oficial.
 */
class MainActivity : AppCompatActivity() {
    private companion object {
        /** A permission callback faster than this means Android auto-denied without showing any dialog. */
        const val SYSTEM_PERMISSION_DIALOG_SKIPPED_MS = 500L
    }
    private enum class AppTab { HOME, FILES, OAAS, PRO }
    private data class UnusedAppInfo(
        val packageName: String,
        val displayName: String,
        val lastUsedMillis: Long,
    )

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            WeeklyDigestScheduler.recordAppOpenedAndSchedule(this)
        }
    }

    private val usageAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasUsageStatsAccess()) {
            loadUnusedAppSuggestions()
        } else {
            Toast.makeText(this, R.string.pro_unused_apps_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Android silently auto-denies (no dialog, ~instant callback) once the user has denied twice or
        // set USER_FIXED. Detect it and send the user to App settings instead of leaving a dead button.
        val elapsedMs = SystemClock.elapsedRealtime() - permissionRequestStartedAtMs
        val accessUnchanged = hasFullVisualAccess() == permissionRequestHadFullAccess &&
            hasAnyMediaPermission() == permissionRequestHadAnyAccess
        if (accessUnchanged && !hasFullVisualAccess() && elapsedMs in 0..SYSTEM_PERMISSION_DIALOG_SKIPPED_MS) {
            awaitingPermissionSettingsReturn = true
            Toast.makeText(this, R.string.limited_access_settings_hint, Toast.LENGTH_LONG).show()
            openAppDetailsSettings()
            return@registerForActivityResult
        }
        val homeCleanupPending = pendingOutcomeMission == DesiredOutcomeMission.FREE_MAX_SPACE_NOW
        val whatsAppPending = pendingWhatsAppCleanerAfterPermission
        val blurPending = pendingBlurScanAfterPermission
        val similarPending = pendingSimilarScanAfterPermission
        val videoCompressPending = pendingVideoCompressAfterPermission
        pendingWhatsAppCleanerAfterPermission = false
        pendingBlurScanAfterPermission = false
        pendingSimilarScanAfterPermission = false
        pendingVideoCompressAfterPermission = false
        homeComposeState.value = homeComposeState.value.copy(needsMediaPermissionPrompt = !hasFullVisualAccess())
        if (hasAnyMediaPermission()) {
            // The user saw the system sheet and decided; never nag again with our own sheet in this session.
            if (!hasFullVisualAccess()) limitedAccessAcknowledged = true
            lastHadFullVisualAccess = hasFullVisualAccess()
            viewModel.startSession()
            refreshStorageStats()
            // Stay on the tab the user is on: the review sheet opens on top of it when the mission ends.
            when {
                whatsAppPending -> openWhatsAppCleanerFlow()
                blurPending -> openBlurCleanerFlow()
                similarPending -> openSimilarPhotosCleanerFlow()
                videoCompressPending -> openVideoCompressorCandidatesSheet()
            }
        } else {
            pendingKindAfterPermission = null
            pendingOutcomeMission = null
            openInteractiveDialogOnMissionFinish = false
            statusText.text = getString(R.string.permission_denied)
            if (homeCleanupPending) {
                heroOrbButton.isEnabled = true
                heroOrbButton.text = getString(R.string.home_cleanup_primary)
                heroOrbSubtitle.text = getString(R.string.home_cleanup_subtitle)
                showHomeCleanupMessage(getString(R.string.home_cleanup_permission_denied))
            }
            if (::oaasPipelineStatus.isInitialized) {
                oaasPipelineStatus.text = getString(R.string.oaas_mission_scan_failed)
            }
        }
    }

    private val trashIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val uris = pendingOperationUris
        pendingOperationUris = emptyList()
        if (uris.isEmpty()) return@registerForActivityResult
        val bytesMap = pendingOperationBytesByUri
        pendingOperationBytesByUri = emptyMap()
        lifecycleScope.launch {
            val states = trashCoordinator.verifyPostTrashAndRecord(uris)
            val confirmedUris = states.filterValues { it == ObservedMediaPostState.IN_TRASH }.keys
            val confirmedCount = confirmedUris.size
            val authorizedBytes = if (result.resultCode == Activity.RESULT_OK) {
                uris.sumOf { bytesMap[it] ?: 0L }
            } else 0L
            if (authorizedBytes > 0L) sessionAuthorizedBytes += authorizedBytes
            refreshStorageStats()
            updateRestoreButtonState()
        updateSpaceMetricsCard(viewModel.state.value)
            refreshFlywheelSummary()
            updateSpaceMetricsCard(viewModel.state.value)
            if (result.resultCode == Activity.RESULT_OK && confirmedCount > 0) {
                homeInteractiveRecords = emptyList()
                homeInteractiveFallback = false
                homeInteractiveMission = null
                homeCleanupCandidateCount = 0
                heroOrbButton.isEnabled = true
                heroOrbButton.text = getString(R.string.home_cleanup_primary)
                homeComposeState.value = homeComposeState.value.copy(
                    primaryActionLabel = null,
                    collections = buildDefaultHomeCollections(),
                )
                if (::homeDirectCleanRow.isInitialized) {
                    homeDirectCleanRow.visibility = View.GONE
                }
                showHomeCleanupMessage(getString(R.string.trash_operation_result, confirmedCount, uris.size))
                viewModel.clearSelection()
                viewModel.startSession()
                statusText.text = getString(R.string.trash_operation_result, confirmedCount, uris.size)
                Toast.makeText(this@MainActivity, getString(R.string.trash_operation_result, confirmedCount, uris.size), Toast.LENGTH_LONG).show()
            } else {
                statusText.text = getString(R.string.trash_operation_cancelled_or_unconfirmed, confirmedCount, uris.size)
            }
        }
    }

    private val deleteIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val api29Uri = pendingApi29ConsentUri
        if (api29Uri != null) {
            pendingApi29ConsentUri = null
            lifecycleScope.launch {
                if (result.resultCode == Activity.RESULT_OK) {
                    val record = pendingApi29RecordsByUri[api29Uri]
                    val retry = record?.let { trashCoordinator.deleteApi29Item(it) }
                        ?: Api29DeleteAttempt.NotDeleted("record_missing")
                    when (retry) {
                        Api29DeleteAttempt.Deleted -> pendingApi29DeletedUris += api29Uri
                        else -> pendingApi29RejectedCount++
                    }
                    pendingApi29DeleteQueue.remove(api29Uri)
                } else {
                    pendingApi29RejectedCount++
                    pendingApi29DeleteQueue.clear()
                }
                continueApi29DeleteBatch()
            }
            return@registerForActivityResult
        }
        val uris = pendingOperationUris
        pendingOperationUris = emptyList()
        if (uris.isEmpty()) return@registerForActivityResult
        val bytesMap = pendingOperationBytesByUri
        pendingOperationBytesByUri = emptyMap()
        lifecycleScope.launch {
            val verifiedDeletedUris = if (result.resultCode == Activity.RESULT_OK) {
                trashCoordinator.verifyPostDeleteUris(uris)
            } else {
                emptySet()
            }
            val confirmedDeleted = verifiedDeletedUris.size
            val totalBatchBytes = uris.sumOf { bytesMap[it] ?: 0L }
            val confirmedBytes = verifiedDeletedUris.fold(0L) { total, uri ->
                val itemBytes = (bytesMap[uri] ?: 0L).coerceAtLeast(0L)
                if (Long.MAX_VALUE - total < itemBytes) Long.MAX_VALUE else total + itemBytes
            }
            if (result.resultCode == Activity.RESULT_OK) {
                sessionAuthorizedBytes += totalBatchBytes
            }
            var reviewPrompted = false
            if (confirmedBytes > 0L) {
                try {
                    window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                } catch (_: Exception) {}
                outcomeTelemetry.recordConfirmedFreedBytes(confirmedBytes)
                val lifetimeBytesNow = outcomeTelemetry.loadSnapshot().totalConfirmedFreedBytes
                reviewPrompted = inAppReviewCoordinator.requestReviewIfAppropriate(confirmedBytes, lifetimeBytesNow)
            }
            refreshStorageStats()
            refreshFlywheelSummary()
            updateSpaceMetricsCard(viewModel.state.value)
            val freedFmt = android.text.format.Formatter.formatFileSize(this@MainActivity, confirmedBytes)
            if (result.resultCode == Activity.RESULT_OK && confirmedDeleted > 0) {
                homeInteractiveRecords = emptyList()
                homeInteractiveFallback = false
                homeInteractiveMission = null
                homeCleanupCandidateCount = 0
                heroOrbButton.isEnabled = true
                heroOrbButton.text = getString(R.string.home_cleanup_primary)
                homeComposeState.value = homeComposeState.value.copy(
                    recoverableBytes = 0L,
                    recoverableLabel = "0 MB",
                    primaryActionLabel = null,
                    collections = buildDefaultHomeCollections(),
                )
                if (::homeDirectCleanRow.isInitialized) {
                    homeDirectCleanRow.visibility = View.GONE
                }
                showHomeCleanupMessage(getString(R.string.delete_operation_result, confirmedDeleted, uris.size, freedFmt))
                viewModel.clearSelection()
                viewModel.startSession()
                Toast.makeText(this@MainActivity, getString(R.string.delete_operation_result, confirmedDeleted, uris.size, freedFmt), Toast.LENGTH_LONG).show()
                if (!reviewPrompted) {
                    if (hasEffectiveProAccess()) {
                        maybePromptWeeklyDigestOptIn()
                    } else {
                        showPostCleanupReliefOffer(confirmedBytes, confirmedDeleted)
                    }
                }
            }
            statusText.text = getString(R.string.delete_operation_result, confirmedDeleted, uris.size, freedFmt)
        }
    }

    private val restoreIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        lifecycleScope.launch {
            val reconciliation = trashCoordinator.verifyPostRestoreAndReconcile()
            refreshStorageStats()
            updateRestoreButtonState()
        updateSpaceMetricsCard(viewModel.state.value)
            val restored = reconciliation.confirmedRestoredUris.size
            val remaining = reconciliation.unresolvedUris.size
            if (restored > 0) {
                viewModel.startSession()
            }
            statusText.text = getString(R.string.restore_operation_result, restored, remaining)
        }
    }

    private lateinit var viewModel: MediaScanViewModel
    private lateinit var billingGateway: AndroidPlayBillingGateway
    private lateinit var trashCoordinator: AndroidTrashOperationCoordinator
    private lateinit var outcomeTelemetry: AndroidOutcomeTelemetryStore
    private lateinit var inAppReviewCoordinator: InAppReviewCoordinator
    private lateinit var whatsAppCleaner: WhatsAppCleanerDataSource
    private var pendingWhatsAppCleanerAfterPermission: Boolean = false
    private var pendingOperationUris: List<String> = emptyList()
    private var pendingOperationBytesByUri: Map<String, Long> = emptyMap()
    private val pendingApi29DeleteQueue = mutableListOf<String>()
    private val pendingApi29DeletedUris = mutableListOf<String>()
    private var pendingApi29ConsentUri: String? = null
    private var pendingApi29BytesByUri: Map<String, Long> = emptyMap()
    private var pendingApi29RecordsByUri: Map<String, MediaRecord> = emptyMap()
    private var pendingApi29TotalCount: Int = 0
    private var pendingApi29RejectedCount: Int = 0
    private var sessionAuthorizedBytes: Long = 0L
    private lateinit var spaceMetricsText: TextView
    private lateinit var radarConsentCheckBox: CheckBox
    private var activeTab: AppTab = AppTab.HOME
    private val activeTabComposeState = mutableStateOf(AppTab.HOME)
    private var pendingOutcomeMission: DesiredOutcomeMission? = null
    private var lastRecordedMissionProgress: com.rdf112.sweep.presentation.scan.GlobalMissionProgress? = null

    // Containers das Abas Dedicadas (Fim do app embolado: cada recurso em sua página)
    private lateinit var homeSection: LinearLayout
    private lateinit var homeComposeView: ComposeView
    private val homeComposeState = mutableStateOf(HomeUiState())
    private val livePhotosSizeState = mutableStateOf<String?>(null)
    private val liveVideosSizeState = mutableStateOf<String?>(null)
    private val liveChatsSizeState = mutableStateOf<String?>(null)
    private lateinit var filesSection: LinearLayout
    private lateinit var oaasSection: LinearLayout
    private lateinit var proSection: LinearLayout

    // Botões da Barra de Navegação Inferior (3 Abas Limpas: Limpar, Arquivos, Pro)
    private lateinit var navHomeBtn: MaterialButton
    private lateinit var navFilesBtn: MaterialButton
    private lateinit var navProBtn: MaterialButton
    private lateinit var bottomNavContainer: LinearLayout

    // Widgets da Aba Início (2027 Dashboard)
    private lateinit var appRootView: LinearLayout
    private lateinit var brandLockupCard: LinearLayout
    private lateinit var brandWordmarkView: TextView
    private lateinit var brandTaglineLabel: TextView
    private lateinit var storageCardContainer: MaterialCardView
    private lateinit var mainScrollView: ScrollView
    private lateinit var topBarProButton: MaterialButton
    private lateinit var easyModeButton: MaterialButton
    private lateinit var storageRingGauge: StorageRingGaugeView
    private lateinit var storageSegmentedBar: StorageSegmentedBarView
    private lateinit var storageBreakdownLegend: TextView
    private lateinit var storageProgressBar: ProgressBar
    private lateinit var storageUsedHeadline: TextView
    private lateinit var storageFreeSubline: TextView
    private lateinit var heroOrbButton: MaterialButton
    private lateinit var heroOrbSubtitle: TextView
    private lateinit var homeCleanupResult: TextView
    private lateinit var homeDirectCleanRow: LinearLayout
    private lateinit var homeProCtaCard: MaterialCardView
    private lateinit var homeProCtaBadge: TextView
    private lateinit var homeProCtaTitle: TextView
    private lateinit var homeProCtaSubtitle: TextView
    private lateinit var homeProSubscribeButton: MaterialButton
    private var latestAnnualOfferText: String? = null
    private var latestAnnualHasFreeTrial: Boolean = false
    private var latestAnnualFormattedPrice: String? = null
    private var latestMonthlyOfferText: String? = null
    private var latestLifetimeOfferText: String? = null
    private var latestHeroPriceBadge: String? = null
    private var proAnnualSavingsBadgeView: TextView? = null
    private var proTermsDisclosureView: TextView? = null
    private var homeCleanupCandidateCount = 0
    private var homeInteractiveRecords: List<MediaRecord> = emptyList()
    private var homeInteractiveFallback: Boolean = false
    /** Mission that produced [homeInteractiveRecords]; the Home CTA must not reopen another mission's list. */
    private var homeInteractiveMission: DesiredOutcomeMission? = null
    private var homeInteractiveCoverageComplete: Boolean = true
    private var openInteractiveDialogOnMissionFinish: Boolean = false
    /**
     * True once the user explicitly chose to keep Android 14+ "Select photos" (or otherwise limited)
     * media access. Cleanup then runs on the authorized subset and coverage is reported as partial.
     */
    private var limitedAccessAcknowledged: Boolean = false
    private var permissionRequestStartedAtMs: Long = 0L
    private var permissionRequestHadFullAccess: Boolean = false
    private var permissionRequestHadAnyAccess: Boolean = false
    private var awaitingPermissionSettingsReturn: Boolean = false
    private var lastInteractivePromptProgress: GlobalMissionProgress? = null
    private var testerVipButton: MaterialButton? = null
    private lateinit var selectAllPageButton: MaterialButton
    private var lastFilteredPageRecords: List<MediaRecord> = emptyList()

    // Widgets compartilhados e das demais abas
    private lateinit var storageText: TextView
    private lateinit var statusText: TextView
    private lateinit var pageLabel: TextView
    private lateinit var selectionSummary: TextView
    private lateinit var resultSummary: TextView
    private lateinit var proStatusBadge: TextView
    private lateinit var oaasPipelineStatus: TextView
    private lateinit var oaasCancelMissionButton: MaterialButton
    private lateinit var missionPermissionButton: MaterialButton
    private lateinit var missionReviewButton: MaterialButton
    private lateinit var missionRetryButton: MaterialButton
    private var debugProPreviewEnabled = false
    private var debugProPreviewButton: MaterialButton? = null
    private lateinit var oaasFlywheelMetrics: TextView
    private lateinit var recordsContainer: LinearLayout
    private lateinit var selectedContainer: LinearLayout
    private lateinit var volumeSpinner: Spinner
    private lateinit var scanButton: MaterialButton
    private lateinit var permissionButton: MaterialButton
    private lateinit var filesPermissionButton: MaterialButton
    private lateinit var cancelButton: MaterialButton
    private lateinit var previousButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var photosButton: MaterialButton
    private lateinit var videosButton: MaterialButton
    private lateinit var audioButton: MaterialButton
    private lateinit var filterAllButton: MaterialButton
    private lateinit var filterLargeButton: MaterialButton
    private lateinit var filterDuplicatesButton: MaterialButton
    private lateinit var filterScreenshotsButton: MaterialButton
    private lateinit var filterOldButton: MaterialButton
    private lateinit var oaasMissionLargeButton: MaterialButton
    private lateinit var oaasMissionDuplicatesButton: MaterialButton
    private lateinit var oaasMissionScreenshotsButton: MaterialButton
    private lateinit var oaasMissionOldButton: MaterialButton
    private lateinit var customOutcomeInput: EditText
    private lateinit var registerCustomOutcomeButton: MaterialButton
    private lateinit var moveToTrashButton: MaterialButton
    private lateinit var permanentDeleteButton: MaterialButton
    private lateinit var restoreTrashButton: MaterialButton
    private lateinit var exportSelectionButton: MaterialButton
    private lateinit var clearSelectionButton: MaterialButton
    private lateinit var filesReviewCard: MaterialCardView
    private lateinit var planMonthlyButton: MaterialButton
    private lateinit var planAnnualButton: MaterialButton
    private lateinit var planLifetimeButton: MaterialButton
    private lateinit var homePlanMonthlyButton: MaterialButton
    private lateinit var homePlanAnnualButton: MaterialButton
    private lateinit var homePlanLifetimeButton: MaterialButton
    private lateinit var restorePurchasesButton: MaterialButton
    private lateinit var manageSubscriptionButton: MaterialButton
    private lateinit var progress: ProgressBar

    private var updatingVolumeSpinner = false
    private var pendingKindAfterPermission: MediaKind? = null
    private var renderedVolumes: List<String> = emptyList()
    private var renderedPageKey: String? = null
    private var storageBreakdownJob: Job? = null
    private var blurScanJob: Job? = null
    private var similarScanJob: Job? = null
    private var videoCompressJob: Job? = null
    private var pendingBlurScanAfterPermission = false
    private var pendingSimilarScanAfterPermission = false
    private var pendingVideoCompressAfterPermission = false
    private val thumbnailJobs = mutableListOf<Job>()
    private val thumbnailDispatcher by lazy { Dispatchers.IO.limitedParallelism(4) }
    private val thumbnailCache: LruCache<String, Bitmap> by lazy {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt().coerceIn(4 * 1024, 16 * 1024)
        object : LruCache<String, Bitmap>(maxKb) {
            override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
        }
    }
    private val checkBoxesByUri = mutableMapOf<String, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        billingGateway = AndroidPlayBillingGateway(applicationContext)
        trashCoordinator = AndroidTrashOperationCoordinator(applicationContext)
        outcomeTelemetry = AndroidOutcomeTelemetryStore(applicationContext)
        inAppReviewCoordinator = InAppReviewCoordinator(this)
        whatsAppCleaner = WhatsAppCleanerDataSource(applicationContext)
        viewModel = ViewModelProvider(
            this,
            MediaScanViewModel.Factory(AndroidMediaPageRepository(applicationContext)),
        )[MediaScanViewModel::class.java]
        setContentView(createModern2027Layout())
        refreshStorageStats()
        updateRestoreButtonState()
        updateSpaceMetricsCard(viewModel.state.value)
        refreshFlywheelSummary()
        switchTab(AppTab.HOME)

        // Se o usuário já concedeu permissão antes, inicia a leitura automaticamente sem tela vazia!
        if (hasAnyMediaPermission()) {
            viewModel.startSession()
        }

        checkAndShowFirstRunOnboarding()
        handleIncomingSendIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::renderState) }
                launch { billingGateway.accessState.collect(::renderBillingState) }
                launch { billingGateway.catalog.collect(::renderCatalogPrices) }
            }
        }
    }

    private var lastHadFullVisualAccess: Boolean = false
    private var autoRetriedStaleMission: DesiredOutcomeMission? = null
    private var autoInitialCleanupScanLaunched: Boolean = false

    private fun hasFullVisualAccess(): Boolean = when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 ->
            isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        else -> isGranted(Manifest.permission.READ_MEDIA_IMAGES) &&
            isGranted(Manifest.permission.READ_MEDIA_VIDEO)
    }

    override fun onResume() {
        super.onResume()
        WeeklyDigestScheduler.recordAppOpenedAndSchedule(this)
        val nowFullVisual = hasFullVisualAccess()
        val gainedVisualAccess = !lastHadFullVisualAccess && nowFullVisual
        val returnedFromSettingsWithAccess = awaitingPermissionSettingsReturn && hasAnyMediaPermission()
        awaitingPermissionSettingsReturn = false
        lastHadFullVisualAccess = nowFullVisual
        if (nowFullVisual) limitedAccessAcknowledged = false
        homeComposeState.value = homeComposeState.value.copy(
            needsMediaPermissionPrompt = !nowFullVisual,
        )
        if (::storageText.isInitialized || ::storageUsedHeadline.isInitialized) refreshStorageStats()
        if (::viewModel.isInitialized) {
            if (gainedVisualAccess || returnedFromSettingsWithAccess) {
                viewModel.startSession()
            } else {
                viewModel.checkSessionFreshness()
            }
        }
        if (::billingGateway.isInitialized) billingGateway.startAndReconcile()
        if (::restoreTrashButton.isInitialized) updateRestoreButtonState()
    }

    override fun onDestroy() {
        similarScanJob?.cancel()
        blurScanJob?.cancel()
        videoCompressJob?.cancel()
        if (::billingGateway.isInitialized) billingGateway.endConnection()
        super.onDestroy()
    }

    private fun createModern2027Layout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.sweep_background))
        }
        appRootView = root
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        // Brand bar with the Midiastore cleanup mark and wordmark.
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(10))
        }
        val brandLockup = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = null
            setPadding(0, dp(4), dp(8), dp(4))
        }
        brandLockupCard = brandLockup

        val brandCopy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val wordmark = label(getString(R.string.app_name), 22f, R.color.sweep_text, true).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = -0.02f
            maxLines = 1
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        brandWordmarkView = wordmark
        renderBrandWordmark(isPro = false)
        brandCopy.addView(wordmark)
        brandLockup.addView(brandCopy, LinearLayout.LayoutParams(-2, -2))
        topBar.addView(brandLockup, LinearLayout.LayoutParams(0, -2, 1f))
        easyModeButton = button("") {}.apply { visibility = View.GONE }
        brandTaglineLabel = label("", 11f, R.color.sweep_muted, false).apply { visibility = View.GONE }
        topBarProButton = button(getString(R.string.premium_area_button)) {
            switchTab(AppTab.PRO)
        }.apply {
            minHeight = dp(34)
            insetTop = 0
            insetBottom = 0
            cornerRadius = dp(17)
            setPadding(dp(14), dp(4), dp(14), dp(4))
            textSize = 12f
            maxLines = 1
            isSingleLine = true
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.sweep_muted))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
        }
        topBar.addView(topBarProButton, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(10) })
        // O nome Midiastore agora fica integrado dentro do quadro da Maria no topo das 3 abas!
        topBar.visibility = View.GONE
        root.addView(topBar, LinearLayout.LayoutParams(-1, -2))

        // Barra de progresso fina e moderna
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
            indeterminateTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
            contentDescription = getString(R.string.loading_page)
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(4)))

        // 2. ÁREA CENTRAL ROLÁVEL (Exibe apenas a Aba ativa!)
        mainScrollView = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
        }
        val bodyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(24))
        }
        mainScrollView.addView(bodyContainer, ViewGroup.LayoutParams(-1, -2))
        root.addView(mainScrollView, LinearLayout.LayoutParams(-1, 0, 1f))

        statusText = label(getString(R.string.ready_status), 12f, R.color.sweep_primary_glow, false).apply {
            setPadding(dp(4), 0, dp(4), dp(10))
        }
        bodyContainer.addView(statusText)

        // Construção das Abas Puras e Separadas por Finalidade (Padrão Jornal & Fim do app embolado)
        homeSection = buildHomeDashboardSection().apply { visibility = View.GONE }
        homeComposeView = ComposeView(this@MainActivity).apply {
            id = View.generateViewId()
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val scanState by viewModel.state.collectAsStateWithLifecycle()
                val billingAccessState by billingGateway.accessState.collectAsStateWithLifecycle()
                val catalogMap by billingGateway.catalog.collectAsStateWithLifecycle()
                val annualItem = catalogMap[BillingStatePolicy.PRODUCT_ANNUAL]
                val hasAnnualTrialNow = (annualItem?.hasFreeTrialOffer == true) ||
                    (isDevelopmentBuild && (annualItem == null || !annualItem.isAvailable))
                val isProNow = billingAccessState == ProAccessSummaryState.PRO_GRANTED ||
                    isTesterVipProEnabled() ||
                    (isDevelopmentBuild && debugProPreviewEnabled)
                MidiastoreTheme {
                    CleanerHomeScreen(
                        state = homeComposeState.value.copy(
                            isScanning = scanState.isLoading || scanState.missionProgress?.isRunning == true,
                        ),
                        isProActive = isProNow,
                        onAnalyze = { onHomeCleanupButtonClicked() },
                        onMissionClick = ::onCleanMissionClick,
                        onOpenPro = { onHomeProSubscribeClicked() },
                        onManagePermissions = { requestMediaPermissions(allowSettingsFallback = true) },
                        onShareResultOrInvite = { bytes -> shareApp(bytes) },
                        hasAnnualFreeTrial = hasAnnualTrialNow,
                        isTabActive = activeTabComposeState.value == AppTab.HOME,
                    )
                }
            }
        }
        filesSection = buildFilesExplorerSection()
        oaasSection = buildOaasMissionsSection()
        proSection = buildProPaywallSection()

        bodyContainer.addView(homeSection)
        bodyContainer.addView(homeComposeView)
        bodyContainer.addView(filesSection)
        bodyContainer.addView(oaasSection)
        bodyContainer.addView(proSection)

        // 3. BARRA DE NAVEGAÇÃO FIXA & MINIMALISTA (3 Abas Limpas Apple HIG: Limpar, Arquivos, Pro)
        val navDivider = View(this).apply {
            setBackgroundColor(color(R.color.sweep_surface_alt))
        }
        root.addView(navDivider, LinearLayout.LayoutParams(-1, dp(1)))

        bottomNavContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#0A0A0C"))
            setPadding(dp(8), dp(4), dp(8), dp(6))
        }
        navHomeBtn = navTabButton(getString(R.string.nav_home)) { switchTab(AppTab.HOME) }.apply {
            icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_apple_clean)
            iconGravity = MaterialButton.ICON_GRAVITY_TOP
            iconSize = dp(22)
            iconPadding = dp(3)
        }
        navFilesBtn = navTabButton(getString(R.string.nav_files)) { switchTab(AppTab.FILES) }.apply {
            icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_apple_files)
            iconGravity = MaterialButton.ICON_GRAVITY_TOP
            iconSize = dp(22)
            iconPadding = dp(3)
        }
        navProBtn = navTabButton(getString(R.string.nav_pro)) { switchTab(AppTab.PRO) }.apply {
            icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_apple_pro)
            iconGravity = MaterialButton.ICON_GRAVITY_TOP
            iconSize = dp(22)
            iconPadding = dp(3)
        }
        listOf(navHomeBtn, navFilesBtn, navProBtn).forEach { tabBtn ->
            tabBtn.textSize = 11f
            tabBtn.minHeight = dp(52)
            tabBtn.insetLeft = 0
            tabBtn.insetRight = 0
            tabBtn.setPadding(dp(4), dp(6), dp(4), dp(6))
            tabBtn.typeface = Typeface.DEFAULT_BOLD
            bottomNavContainer.addView(
                tabBtn,
                LinearLayout.LayoutParams(0, -2, 1f),
            )
        }
        root.addView(bottomNavContainer, LinearLayout.LayoutParams(-1, -2))

        return root
    }

    private fun buildHomeDashboardSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        // Card de Medidor de Armazenamento Real (Padrão Apple Clean)
        val storageCard = card()
        val storageRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        storageRingGauge = StorageRingGaugeView(this@MainActivity).apply {
            bottomLabel = ""
        }
        storageRow.addView(storageRingGauge, LinearLayout.LayoutParams(dp(92), dp(92)).apply { marginEnd = dp(16) })
        val storageCopy = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        storageUsedHeadline = label(getString(R.string.storage_unavailable), 19f, R.color.sweep_text, true)
        storageCopy.addView(storageUsedHeadline)
        storageFreeSubline = label("", 13f, R.color.sweep_muted, false)
        storageCopy.addView(storageFreeSubline, top(4))
        storageProgressBar = ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
            visibility = View.GONE
            isIndeterminate = false
            max = 100
            progress = 0
        }
        storageCopy.addView(storageProgressBar)
        storageRow.addView(storageCopy, LinearLayout.LayoutParams(0, -2, 1f))
        storageCard.addView(storageRow)
        storageSegmentedBar = StorageSegmentedBarView(this@MainActivity)
        storageCard.addView(storageSegmentedBar, LinearLayout.LayoutParams(-1, dp(10)).apply { topMargin = dp(14) })
        storageBreakdownLegend = label("", 11.5f, R.color.sweep_muted, false).apply {
            visibility = View.GONE
            maxLines = 2
        }
        storageCard.addView(storageBreakdownLegend, top(8))
        storageCardContainer = wrap(storageCard)
        addView(storageCardContainer, bottom(16))

        heroOrbButton = MaterialButton(this@MainActivity).apply {
            text = getString(R.string.home_cleanup_primary)
            textSize = 15.5f
            isAllCaps = false
            minHeight = dp(56)
            cornerRadius = dp(16)
            insetTop = 0
            insetBottom = 0
            typeface = Typeface.DEFAULT_BOLD
            tag = "primary_phone_cleaner_action"
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
            setOnClickListener {
                try { it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                onHomeCleanupButtonClicked()
            }
        }
        addView(heroOrbButton, LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(16) })

        heroOrbSubtitle = label(getString(R.string.home_cleanup_subtitle), 12f, R.color.sweep_muted, false).apply {
            visibility = View.GONE
        }
        addView(heroOrbSubtitle)

        homeCleanupResult = label("", 13f, R.color.google_blue_primary, true).apply {
            visibility = View.GONE
            setPadding(dp(8), dp(2), dp(8), dp(10))
        }
        addView(homeCleanupResult)

        homeDirectCleanRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
        }
        val homeTrashBtn = button(getString(R.string.move_to_trash_button)) {
            requestMoveSelectedToTrash()
        }.apply {
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
            visibility = if (trashCoordinator.supportsSystemTrash()) View.VISIBLE else View.GONE
            minHeight = dp(52)
        }
        val homeDeleteBtn = button(getString(R.string.permanent_delete_button)) {
            requestPermanentDeleteSelected()
        }.apply {
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_danger))
            minHeight = dp(52)
        }
        homeDirectCleanRow.addView(homeTrashBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        homeDirectCleanRow.addView(homeDeleteBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        addView(homeDirectCleanRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        // Grade 2x2 Limpeza Rápida (Padrão Apple Clean)
        addView(buildQuickClean2x2Grid(), bottom(18))

        // Linha discreta de confiança (Sem card pesado)
        addView(
            label(getString(R.string.family_shield_title), 12f, R.color.sweep_muted, false).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
            bottom(12),
        )

        // Referências internas mantidas ocultas para compatibilidade com Billing
        val homeProCardContent = card()
        homeProCtaBadge = label("", 11f, R.color.google_blue_primary, true)
        homeProCtaTitle = label("", 15f, R.color.sweep_text, true)
        homeProCtaSubtitle = label("", 12f, R.color.sweep_muted, false)
        homeProSubscribeButton = button("") { onHomeProSubscribeClicked() }
        homeProCtaCard = themedCard(homeProCardContent, color(R.color.sweep_surface), color(R.color.sweep_surface_alt)).apply {
            visibility = View.GONE
        }
        addView(homeProCtaCard)

        homePlanMonthlyButton = button("") { requestPlanPurchase(BillingStatePolicy.PRODUCT_MONTHLY) }.apply { visibility = View.GONE }
        homePlanAnnualButton = button("") { requestPlanPurchase(BillingStatePolicy.PRODUCT_ANNUAL) }.apply { visibility = View.GONE }
        homePlanLifetimeButton = button("") { requestPlanPurchase(BillingStatePolicy.PRODUCT_LIFETIME) }.apply { visibility = View.GONE }
    }

    private fun shareApp(freedBytes: Long = 0L) {
        lifecycleScope.launch {
            ShareAchievementCardRenderer.shareFreedSpaceOrInvite(
                activity = this@MainActivity,
                freedBytes = freedBytes,
            )
        }
    }

    private fun maybePromptWeeklyDigestOptIn() {
        if (isFinishing || isDestroyed) return
        if (!WeeklyDigestScheduler.shouldPromptNotificationOptInAfterCleanup(this)) return
        WeeklyDigestScheduler.markNotificationOptInPromptShown(this)
        val (sheet, root) = createAppleBottomSheet(
            title = getString(R.string.weekly_digest_optin_title),
            subtitle = getString(R.string.weekly_digest_optin_body),
        )
        val allowBtn = button(getString(R.string.weekly_digest_optin_allow)) {
            sheet.dismiss()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.apply {
            minHeight = dp(52)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        root.addView(allowBtn, top(14))

        val skipBtn = button(getString(R.string.post_cleanup_relief_later)) {
            sheet.dismiss()
        }.apply {
            minHeight = dp(46)
            setTextColor(color(R.color.sweep_muted))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        }
        root.addView(skipBtn, top(4))
        sheet.show()
    }

    private fun buildQuickClean2x2Grid(): View {
        val gridRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        gridRoot.addView(
            label(getString(R.string.quick_clean_title).uppercase(), 11f, R.color.sweep_muted, true).apply {
                letterSpacing = 0.06f
                setPadding(dp(2), 0, dp(2), dp(10))
            }
        )
        fun buildGridCell(
            iconRes: Int,
            accentColor: Int,
            title: String,
            description: String,
            onClick: () -> Unit
        ): View {
            val cellContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(14), dp(18), dp(14), dp(18))
                background = borderedPill(
                    color(R.color.sweep_surface),
                    color(R.color.sweep_surface_alt),
                    dp(18),
                    dp(1),
                )
                isClickable = true
                isFocusable = true
                setOnClickListener { v ->
                    try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                    onClick()
                }
            }
            val iconView = ImageView(this).apply {
                setImageResource(iconRes)
                setColorFilter(accentColor)
            }
            cellContent.addView(iconView, LinearLayout.LayoutParams(dp(28), dp(28)))

            val titleView = label(title, 14f, R.color.sweep_text, true).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                maxLines = 1
                isSingleLine = true
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    this,
                    11,
                    14,
                    1,
                    TypedValue.COMPLEX_UNIT_SP,
                )
            }
            cellContent.addView(titleView, top(10))
            val descView = label(description, 12f, R.color.sweep_muted, false).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            cellContent.addView(descView, top(2))

            return cellContent
        }

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val waCell = buildGridCell(
            iconRes = R.drawable.ic_apple_whatsapp,
            accentColor = color(R.color.sweep_success),
            title = getString(R.string.quick_clean_whatsapp_title),
            description = getString(R.string.quick_clean_whatsapp_desc),
            onClick = { openWhatsAppCleanerFlow() }
        )
        val dupCell = buildGridCell(
            iconRes = R.drawable.ic_apple_duplicates,
            accentColor = color(R.color.google_blue_primary),
            title = getString(R.string.quick_clean_duplicates_title),
            description = getString(R.string.quick_clean_duplicates_desc),
            onClick = {
                openInteractiveDialogOnMissionFinish = true
                runOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
            }
        )
        row1.addView(waCell, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        row1.addView(dupCell, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        gridRoot.addView(row1)

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val largeCell = buildGridCell(
            iconRes = R.drawable.ic_apple_large_video,
            accentColor = color(R.color.sweep_accent_amber),
            title = getString(R.string.quick_clean_large_title),
            description = getString(R.string.quick_clean_large_desc),
            onClick = {
                openInteractiveDialogOnMissionFinish = true
                runOutcomeMission(DesiredOutcomeMission.FREE_MAX_SPACE_NOW)
            }
        )
        val screenshotsCell = buildGridCell(
            iconRes = R.drawable.ic_apple_screenshots,
            accentColor = Color.parseColor("#BF5AF2"),
            title = getString(R.string.quick_clean_screenshots_title),
            description = getString(R.string.quick_clean_screenshots_desc),
            onClick = {
                openInteractiveDialogOnMissionFinish = true
                runOutcomeMission(DesiredOutcomeMission.CLEAN_OLD_SCREENSHOTS_30D)
            }
        )
        row2.addView(largeCell, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        row2.addView(screenshotsCell, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        gridRoot.addView(row2, top(12))

        val blurCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = borderedPill(
                color(R.color.sweep_surface),
                color(R.color.sweep_surface_alt),
                dp(18),
                dp(1),
            )
            isClickable = true
            isFocusable = true
            setOnClickListener { v ->
                try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                openBlurCleanerFlow()
            }
        }
        val blurIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_apple_blur)
            setColorFilter(Color.parseColor("#FF375F"))
        }
        blurCard.addView(blurIcon, LinearLayout.LayoutParams(dp(26), dp(26)).apply { marginEnd = dp(12) })
        val blurTextCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        blurTextCol.addView(label(getString(R.string.quick_clean_blur_title), 14f, R.color.sweep_text, true).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        blurTextCol.addView(label(getString(R.string.quick_clean_blur_desc), 12f, R.color.sweep_muted, false).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, top(2))
        blurCard.addView(blurTextCol, LinearLayout.LayoutParams(0, -2, 1f))
        blurCard.addView(label("›", 20f, R.color.sweep_muted, false).apply {
            setPadding(dp(8), 0, dp(2), 0)
        })
        gridRoot.addView(blurCard, top(12))

        return gridRoot
    }

    private fun buildFilesExplorerSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        // 0. Hero Banner da Maria + Grid 2x2 de Quadradinhos na 2ª Aba (Arquivos)
        val filesMariaView = ComposeView(this@MainActivity).apply {
            id = View.generateViewId()
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MidiastoreTheme {
                    FilesMariaHeaderSection(
                        isActive = activeTabComposeState.value == AppTab.FILES || activeTabComposeState.value == AppTab.OAAS,
                        onSelectPhotos = {
                            viewModel.selectKind(MediaKind.IMAGE)
                            viewModel.selectSmartFilter(SmartFilterMode.ALL_ITEMS)
                        },
                        onSelectLargeVideos = {
                            viewModel.selectKind(MediaKind.VIDEO)
                            viewModel.selectSmartFilter(SmartFilterMode.LARGE_FILES_10MB)
                        },
                        onSelectWhatsApp = {
                            openWhatsAppCleanerFlow()
                        },
                        onSelectScreenshots = {
                            viewModel.selectKind(MediaKind.IMAGE)
                            viewModel.selectSmartFilter(SmartFilterMode.SCREENSHOTS_30D)
                        },
                        photosSizeLabel = livePhotosSizeState.value,
                        videosSizeLabel = liveVideosSizeState.value,
                        chatsSizeLabel = liveChatsSizeState.value,
                    )
                }
            }
        }
        addView(filesMariaView)

        // 1. Controle Segmentado Limpo (Fotos | Vídeos | Áudios)
        val segmentedBar = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = borderedPill(color(R.color.sweep_surface), color(R.color.sweep_surface_alt), dp(14), dp(1))
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        photosButton = button(getString(R.string.photos_tab)) { viewModel.selectKind(MediaKind.IMAGE) }.apply {
            minHeight = dp(38)
            cornerRadius = dp(10)
            textSize = 12.5f
        }
        videosButton = button(getString(R.string.videos_tab)) { viewModel.selectKind(MediaKind.VIDEO) }.apply {
            minHeight = dp(38)
            cornerRadius = dp(10)
            textSize = 12.5f
        }
        audioButton = button(getString(R.string.audio_tab)) { viewModel.selectKind(MediaKind.AUDIO) }.apply {
            minHeight = dp(38)
            cornerRadius = dp(10)
            textSize = 12.5f
        }
        segmentedBar.addView(photosButton, LinearLayout.LayoutParams(0, dp(38), 1f))
        segmentedBar.addView(videosButton, LinearLayout.LayoutParams(0, dp(38), 1f))
        segmentedBar.addView(audioButton, LinearLayout.LayoutParams(0, dp(38), 1f))
        addView(segmentedBar, bottom(10))

        // 2. Pílulas Horizontais de Filtro (Todos | Grandes | Duplicatas | Antigos)
        val filterScroll = HorizontalScrollView(this@MainActivity).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
        }
        val filterRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        filterAllButton = button(getString(R.string.filter_all)) { viewModel.selectSmartFilter(SmartFilterMode.ALL_ITEMS) }.apply {
            minHeight = dp(36)
            cornerRadius = dp(18)
            textSize = 12f
            setPadding(dp(14), dp(4), dp(14), dp(4))
        }
        filterLargeButton = button(getString(R.string.filter_large_10mb)) { viewModel.selectSmartFilter(SmartFilterMode.LARGE_FILES_10MB) }.apply {
            minHeight = dp(36)
            cornerRadius = dp(18)
            textSize = 12f
            setPadding(dp(14), dp(4), dp(14), dp(4))
        }
        filterDuplicatesButton = button(getString(R.string.filter_same_size)) { viewModel.selectSmartFilter(SmartFilterMode.SAME_SIZE_CANDIDATES) }.apply {
            minHeight = dp(36)
            cornerRadius = dp(18)
            textSize = 12f
            setPadding(dp(14), dp(4), dp(14), dp(4))
        }
        filterScreenshotsButton = button(getString(R.string.filter_screenshots_30d)) { viewModel.selectSmartFilter(SmartFilterMode.SCREENSHOTS_30D) }.apply {
            minHeight = dp(36)
            cornerRadius = dp(18)
            textSize = 12f
            setPadding(dp(14), dp(4), dp(14), dp(4))
        }
        filterOldButton = button(getString(R.string.filter_older_30d)) { viewModel.selectSmartFilter(SmartFilterMode.OLDER_THAN_30_DAYS) }.apply {
            minHeight = dp(36)
            cornerRadius = dp(18)
            textSize = 12f
            setPadding(dp(14), dp(4), dp(14), dp(4))
        }
        filterRow.addView(filterAllButton, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
        filterRow.addView(filterLargeButton, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
        filterRow.addView(filterDuplicatesButton, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
        filterRow.addView(filterScreenshotsButton, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
        filterRow.addView(filterOldButton, LinearLayout.LayoutParams(-2, -2))
        filterScroll.addView(filterRow)
        addView(filterScroll, bottom(12))

        volumeSpinner = Spinner(this@MainActivity).apply {
            contentDescription = getString(R.string.volume_title)
            visibility = View.GONE
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!updatingVolumeSpinner && position in renderedVolumes.indices) {
                        viewModel.selectVolume(renderedVolumes[position])
                    }
                }
            }
        }
        addView(volumeSpinner)

        filesPermissionButton = button(getString(R.string.permission_continue)) {
            requestMediaPermissions()
        }.apply {
            visibility = View.GONE
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
            setTextColor(Color.WHITE)
        }
        addView(filesPermissionButton, bottom(10))

        // 3. Lista de Arquivos (Logo no topo para revisão rápida)
        val resultsCard = card()
        pageLabel = label(getString(R.string.not_scanned), 15f, R.color.sweep_text, true)
        resultsCard.addView(pageLabel)
        resultSummary = label(getString(R.string.page_results_hint), 12f, R.color.sweep_muted, false)
        resultsCard.addView(resultSummary, top(4))
        selectAllPageButton = button(getString(R.string.select_all_page_button, 0)) {
            val eligible = lastFilteredPageRecords.filter { it.isSelectable && !it.isProtected }
            if (eligible.isNotEmpty()) {
                viewModel.prepareExternalReviewSelection(eligible, viewModel.state.value.smartFilter)
                openInteractiveCleanupReviewDialog(
                    records = eligible,
                    isFallbackList = false,
                    mission = null,
                    coverageComplete = true,
                )
            } else {
                Toast.makeText(this@MainActivity, R.string.cleanup_dialog_empty_title, Toast.LENGTH_SHORT).show()
            }
        }.apply {
            visibility = View.GONE
        }
        resultsCard.addView(selectAllPageButton, top(8))
        recordsContainer = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        resultsCard.addView(recordsContainer)

        val navigation = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        previousButton = button(getString(R.string.previous_page)) { viewModel.loadPreviousPage() }.apply {
            minHeight = dp(48)
            maxLines = 2
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        nextButton = button(getString(R.string.next_page)) { viewModel.loadNextPage() }.apply {
            minHeight = dp(48)
            maxLines = 2
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        navigation.addView(previousButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        navigation.addView(nextButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        resultsCard.addView(navigation, top(12))
        addView(wrap(resultsCard), bottom(14))

        // 4. Painel de Ação Segura (Lixeira & Exclusão)
        val reviewCard = card()
        selectionSummary = label(getString(R.string.nothing_selected), 15f, R.color.sweep_text, true)
        reviewCard.addView(selectionSummary)
        spaceMetricsText = label(
            getString(R.string.space_metrics_summary, "0 B", "0 B", "0 B", "0 B"),
            11f,
            R.color.sweep_muted,
            false,
        )
        reviewCard.addView(spaceMetricsText, top(4))
        selectedContainer = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        reviewCard.addView(selectedContainer, top(6))

        permanentDeleteButton = button(getString(R.string.permanent_delete_button)) { requestPermanentDeleteSelected() }.apply {
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_danger))
        }
        reviewCard.addView(permanentDeleteButton, top(10))

        moveToTrashButton = button(getString(R.string.move_to_trash_button)) { requestMoveSelectedToTrash() }
        reviewCard.addView(moveToTrashButton, top(8))

        restoreTrashButton = button(getString(R.string.restore_trash_button, 0)) { requestRestoreFromTrash() }
        reviewCard.addView(restoreTrashButton, top(8))

        exportSelectionButton = button(getString(R.string.export_review_summary)) { exportOrUnlockProSummary() }
        reviewCard.addView(exportSelectionButton, top(8))

        clearSelectionButton = button(getString(R.string.clear_review)) { viewModel.clearSelection() }
        reviewCard.addView(clearSelectionButton, top(8))
        filesReviewCard = wrap(reviewCard)
        addView(filesReviewCard, bottom(14))
    }

    private fun buildOaasMissionsSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        val oaasCard = card()
        oaasCard.addView(label(getString(R.string.oaas_card_title), 11f, R.color.sweep_primary_glow, true))
        oaasCard.addView(label(getString(R.string.oaas_card_headline), 18f, R.color.sweep_text, true), top(6))
        oaasCard.addView(label(getString(R.string.oaas_card_subtitle), 12.5f, R.color.sweep_muted, false), top(4))

        oaasPipelineStatus = label(getString(R.string.oaas_pipeline_idle), 12f, R.color.sweep_primary_glow, true)
        oaasCard.addView(oaasPipelineStatus, top(10))
        oaasCancelMissionButton = button(getString(R.string.cancel_scan)) {
            viewModel.cancelLoading()
        }.apply {
            visibility = View.GONE
        }
        cancelButton = oaasCancelMissionButton
        oaasCard.addView(oaasCancelMissionButton, top(8))

        oaasMissionLargeButton = button(getString(R.string.oaas_mission_large)) {
            openInteractiveDialogOnMissionFinish = true
            runOutcomeMission(DesiredOutcomeMission.FREE_MAX_SPACE_NOW)
        }.apply {
            minHeight = dp(54)
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
        }
        oaasCard.addView(oaasMissionLargeButton, top(12))

        oaasMissionDuplicatesButton = button(getString(R.string.oaas_mission_duplicates)) {
            openInteractiveDialogOnMissionFinish = true
            runOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
        }.apply { minHeight = dp(52) }
        oaasCard.addView(oaasMissionDuplicatesButton, top(8))

        oaasMissionOldButton = button(getString(R.string.oaas_mission_old)) {
            openInteractiveDialogOnMissionFinish = true
            runOutcomeMission(DesiredOutcomeMission.ARCHIVE_OLD_MEDIA_30D)
        }.apply { minHeight = dp(52) }
        oaasCard.addView(oaasMissionOldButton, top(8))

        oaasMissionScreenshotsButton = button(getString(R.string.oaas_mission_screenshots)) {
            openInteractiveDialogOnMissionFinish = true
            runOutcomeMission(DesiredOutcomeMission.CLEAN_OLD_SCREENSHOTS_30D)
        }.apply { minHeight = dp(52) }
        oaasCard.addView(oaasMissionScreenshotsButton, top(8))

        val cleanExtrasRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val oaasWhatsAppBtn = button(getString(R.string.quick_clean_whatsapp)) {
            openWhatsAppCleanerFlow()
        }.apply {
            minHeight = dp(52)
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_success))
        }
        val oaasUnusedAppsBtn = button(getString(R.string.pro_unused_apps_title)) {
            runWithProAccess { requestUnusedAppSuggestions() }
        }.apply { minHeight = dp(52) }
        cleanExtrasRow.addView(oaasWhatsAppBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        cleanExtrasRow.addView(oaasUnusedAppsBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        oaasCard.addView(cleanExtrasRow, top(8))

        scanButton = button(getString(R.string.scan_button)) {
            switchTab(AppTab.FILES)
            startScanOrRequestAccess()
        }.apply { minHeight = dp(48) }
        oaasCard.addView(scanButton, top(10))
        permissionButton = button(getString(R.string.permission_continue)) {
            requestMediaPermissions()
        }.apply { visibility = View.GONE }
        oaasCard.addView(permissionButton, top(6))

        missionPermissionButton = button(getString(R.string.permission_continue)) {
            requestMediaPermissions()
        }.apply { visibility = View.GONE }
        oaasCard.addView(missionPermissionButton, top(10))

        missionRetryButton = button(getString(R.string.oaas_retry_scan_button)) {
            val mission = viewModel.state.value.missionProgress?.mission
                ?: DesiredOutcomeMission.FREE_MAX_SPACE_NOW
            pendingOutcomeMission = mission
            oaasPipelineStatus.text = getString(R.string.oaas_starting_scan_first)
            if (hasAnyMediaPermission()) viewModel.startSession() else requestMediaPermissions()
        }.apply { visibility = View.GONE }
        oaasCard.addView(missionRetryButton, top(8))

        missionReviewButton = button(getString(R.string.oaas_review_results_button)) {
            val selectedNow = viewModel.state.value.selection.records
            val displayNow = if (selectedNow.isNotEmpty()) selectedNow else homeInteractiveRecords
            if (displayNow.isNotEmpty()) {
                openInteractiveCleanupReviewDialog(
                    records = displayNow,
                    isFallbackList = selectedNow.isEmpty() && homeInteractiveFallback,
                    mission = viewModel.state.value.missionProgress?.mission,
                    coverageComplete = viewModel.state.value.missionProgress?.coverageComplete ?: true,
                )
            } else {
                switchTab(AppTab.FILES)
                filesReviewCard.post {
                    filesReviewCard.requestRectangleOnScreen(
                        android.graphics.Rect(0, 0, filesReviewCard.width, filesReviewCard.height),
                        true,
                    )
                }
            }
        }.apply {
            visibility = View.GONE
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
        }
        oaasCard.addView(missionReviewButton, top(8))

        oaasFlywheelMetrics = label(
            getString(R.string.oaas_flywheel_summary, 0, 0, "0 B"),
            11f,
            R.color.sweep_muted,
            false,
        )
        oaasCard.addView(oaasFlywheelMetrics, top(12))

        customOutcomeInput = EditText(this@MainActivity).apply {
            hint = getString(R.string.oaas_custom_goal_hint)
            setTextColor(color(R.color.sweep_text))
            setHintTextColor(color(R.color.sweep_muted))
            textSize = 13f
        }
        oaasCard.addView(customOutcomeInput, top(8))

        radarConsentCheckBox = CheckBox(this@MainActivity).apply {
            text = getString(R.string.oaas_consent_checkbox)
            setTextColor(color(R.color.sweep_muted))
            textSize = 11f
            isChecked = outcomeTelemetry.isRadarConsentGranted()
            setOnCheckedChangeListener { _, checked ->
                outcomeTelemetry.setRadarConsentGranted(checked)
            }
        }
        oaasCard.addView(radarConsentCheckBox, top(8))

        registerCustomOutcomeButton = button(getString(R.string.oaas_custom_goal_submit)) {
            val goal = customOutcomeInput.text?.toString().orEmpty()
            if (!outcomeTelemetry.isRadarConsentGranted()) {
                oaasPipelineStatus.text = getString(R.string.oaas_consent_required)
            } else if (goal.isNotBlank() && outcomeTelemetry.recordCustomOutcomeRequest(goal)) {
                customOutcomeInput.setText("")
                refreshFlywheelSummary()
                oaasPipelineStatus.text = getString(R.string.oaas_custom_goal_saved)
            }
        }
        oaasCard.addView(registerCustomOutcomeButton, top(6))

        val clearRadarBtn = button(getString(R.string.oaas_clear_telemetry_button)) {
            outcomeTelemetry.clearAllTelemetry()
            radarConsentCheckBox.isChecked = false
            refreshFlywheelSummary()
            updateSpaceMetricsCard(viewModel.state.value)
        }
        oaasCard.addView(clearRadarBtn, top(6))
        addView(wrap(oaasCard), bottom(14))
    }

    private var postCleanupReliefShownThisSession: Boolean = false

    private fun showPostCleanupReliefOffer(confirmedBytes: Long, confirmedCount: Int) {
        if (confirmedBytes <= 0L || confirmedCount <= 0) return
        if (hasEffectiveProAccess() || postCleanupReliefShownThisSession) return
        if (isFinishing || isDestroyed) return
        postCleanupReliefShownThisSession = true
        val freedFmt = android.text.format.Formatter.formatFileSize(this, confirmedBytes)
        val (sheet, root) = createAppleBottomSheet(
            title = getString(R.string.post_cleanup_relief_title),
            subtitle = getString(R.string.post_cleanup_relief_body, freedFmt, confirmedCount),
        )
        val badge = TextView(this).apply {
            text = getString(
                if (latestAnnualHasFreeTrial) R.string.pro_annual_trial_badge else R.string.pro_annual_savings_badge,
            )
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#30D158"))
            setPadding(dp(12), dp(5), dp(12), dp(5))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#163322"))
                cornerRadius = dp(999).toFloat()
                setStroke(dp(1), Color.parseColor("#30D158"))
            }
        }
        root.addView(badge, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })

        val shareResultBtn = button(getString(R.string.share_freed_result_button)) {
            sheet.dismiss()
            shareApp(confirmedBytes)
        }.apply {
            minHeight = dp(52)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#30D158"))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#122B1D"))
            strokeWidth = dp(1)
            strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#30D158"))
        }
        root.addView(shareResultBtn, top(10))

        val annualBtn = button(latestAnnualOfferText ?: getString(R.string.post_cleanup_relief_cta)) {
            sheet.dismiss()
            requestPlanPurchase(BillingStatePolicy.PRODUCT_ANNUAL)
        }.apply {
            minHeight = dp(54)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        root.addView(annualBtn, top(8))

        val lifetimeBtn = button(latestLifetimeOfferText ?: getString(R.string.plan_lifetime_default)) {
            sheet.dismiss()
            requestPlanPurchase(BillingStatePolicy.PRODUCT_LIFETIME)
        }.apply {
            minHeight = dp(50)
            textSize = 13f
            setTextColor(color(R.color.sweep_text))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
        }
        root.addView(lifetimeBtn, top(8))

        val termsText = if (latestAnnualHasFreeTrial && !latestAnnualFormattedPrice.isNullOrBlank()) {
            getString(R.string.pro_annual_trial_terms_disclosure, latestAnnualFormattedPrice)
        } else {
            getString(R.string.pro_billing_terms_disclosure)
        }
        root.addView(
            label(termsText, 10.5f, R.color.sweep_muted, false),
            top(10),
        )

        val laterBtn = button(getString(R.string.post_cleanup_relief_later)) {
            sheet.dismiss()
            maybePromptWeeklyDigestOptIn()
        }.apply {
            minHeight = dp(46)
            setTextColor(color(R.color.sweep_muted))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        }
        root.addView(laterBtn, top(4))
        sheet.show()
    }

    private fun buildProPaywallSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2), dp(4), dp(2), dp(16))

        // 0. Hero Banner da Maria + Card Gemini Nano Nativo do Android (0 MB) + Grid 2x2 Pro na 3ª Aba
        val proMariaView = ComposeView(this@MainActivity).apply {
            id = View.generateViewId()
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val billingAccessState by billingGateway.accessState.collectAsStateWithLifecycle()
                val isProNow = billingAccessState == ProAccessSummaryState.PRO_GRANTED ||
                    isTesterVipProEnabled() ||
                    (isDevelopmentBuild && debugProPreviewEnabled)
                MidiastoreTheme {
                    ProMariaHeaderSection(
                        isActive = activeTabComposeState.value == AppTab.PRO,
                        isProActive = isProNow,
                        onRunSmartDiagnosis = {
                            openInteractiveDialogOnMissionFinish = true
                            runOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
                        },
                        onRunSimilarPhotos = { openSimilarPhotosCleanerFlow() },
                        onRunVideoCompressor = { runWithProAccess { openVideoCompressorCandidatesSheet() } },
                        onRunBlurScan = { openBlurCleanerFlow() },
                        onRunWhatsAppClean = { openWhatsAppCleanerFlow() },
                        onRunUnusedApps = { runWithProAccess { requestUnusedAppSuggestions() } },
                    )
                }
            }
        }
        addView(proMariaView)

        addView(label(getString(R.string.pro_card_title), 20f, R.color.sweep_text, true), top(2))
        proStatusBadge = label(getString(R.string.pro_status_free), 12f, R.color.google_blue_primary, true)
        addView(proStatusBadge, top(2))
        addView(label(getString(R.string.pro_card_subtitle), 12.5f, R.color.sweep_muted, false), top(4))

        // Cartões antigos ocultos para que os 3 Planos Google Play fiquem visíveis logo acima e o Modelo IA fique no final
        val freeIncludedCard = card()
        freeIncludedCard.addView(label(getString(R.string.pro_free_included), 13f, R.color.sweep_text, false))
        addView(wrap(freeIncludedCard).apply { visibility = View.GONE }, top(8))

        val benefitsCard = card()
        val benefits = listOf(
            getString(R.string.pro_unused_apps_button),
            getString(R.string.export_review_summary),
            getString(R.string.pro_paid_included),
        )
        benefits.forEachIndexed { idx, lineText ->
            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val checkIcon = ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_apple_check)
                setColorFilter(color(R.color.google_blue_primary))
            }
            row.addView(checkIcon, LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(10) })
            row.addView(label(lineText, 13.5f, R.color.sweep_text, false), LinearLayout.LayoutParams(0, -2, 1f))
            benefitsCard.addView(row, top(if (idx == 0) 0 else 10))
        }
        addView(wrap(benefitsCard).apply { visibility = View.GONE }, top(8))

        val annualBadge = TextView(this@MainActivity).apply {
            text = getString(R.string.pro_annual_savings_badge)
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#30D158"))
            setPadding(dp(12), dp(5), dp(12), dp(5))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#163322"))
                cornerRadius = dp(999).toFloat()
                setStroke(dp(1), Color.parseColor("#30D158"))
            }
        }
        proAnnualSavingsBadgeView = annualBadge
        addView(
            annualBadge,
            LinearLayout.LayoutParams(-2, -2).apply {
                topMargin = dp(16)
                bottomMargin = dp(6)
            },
        )

        planAnnualButton = button(getString(R.string.plan_annual_default)) {
            requestPlanPurchase(BillingStatePolicy.PRODUCT_ANNUAL)
        }.apply {
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        stylePlanPurchaseButton(planAnnualButton, isProductAvailable = false)
        addView(planAnnualButton, top(0))

        planMonthlyButton = button(getString(R.string.plan_monthly_default)) {
            requestPlanPurchase(BillingStatePolicy.PRODUCT_MONTHLY)
        }
        stylePlanPurchaseButton(planMonthlyButton, isProductAvailable = false)
        addView(planMonthlyButton, top(10))

        planLifetimeButton = button(getString(R.string.plan_lifetime_default)) {
            requestPlanPurchase(BillingStatePolicy.PRODUCT_LIFETIME)
        }
        stylePlanPurchaseButton(planLifetimeButton, isProductAvailable = false)
        addView(planLifetimeButton, top(10))

        val videoCompressProButton = button(getString(R.string.video_compress_pro_button)) {
            runWithProAccess { openVideoCompressorCandidatesSheet() }
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
            setTextColor(color(R.color.sweep_text))
        }
        addView(videoCompressProButton, top(12))

        val unusedAppsButton = button(getString(R.string.pro_unused_apps_button)) {
            runWithProAccess { requestUnusedAppSuggestions() }
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
            setTextColor(color(R.color.sweep_text))
        }
        addView(unusedAppsButton, top(8))

        val shareInviteButton = button(getString(R.string.share_invite_friend_button)) {
            val freed = outcomeTelemetry.loadSnapshot().totalConfirmedFreedBytes
            shareApp(freed)
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#122B1D"))
            setTextColor(Color.parseColor("#30D158"))
            strokeWidth = dp(1)
            strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#30D158"))
        }
        addView(shareInviteButton, top(8))

        // Console Interativo do Modelo IA Nativa (Gemini Nano Android — LIBERADO PARA USO) posicionado lá no final da aba Pro
        val proGeminiNanoBottomView = ComposeView(this@MainActivity).apply {
            id = View.generateViewId()
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MidiastoreTheme {
                    ProGeminiNanoBottomCard(
                        onRunSmartDiagnosis = {
                            openInteractiveDialogOnMissionFinish = true
                            runOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
                        },
                        onRunBlurScan = { openBlurCleanerFlow() },
                        onRunWhatsAppClean = { openWhatsAppCleanerFlow() },
                        onRunUnusedApps = { runWithProAccess { requestUnusedAppSuggestions() } },
                    )
                }
            }
        }
        addView(proGeminiNanoBottomView, top(8))

        val linksRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        restorePurchasesButton = button(getString(R.string.restore_purchases_button)) {
            billingGateway.startAndReconcile()
            statusText.text = getString(R.string.restoring_purchases_status)
            Toast.makeText(this@MainActivity, R.string.restoring_purchases_status, Toast.LENGTH_SHORT).show()
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            setTextColor(color(R.color.sweep_muted))
            textSize = 12f
        }
        manageSubscriptionButton = button(getString(R.string.manage_subscriptions_button)) {
            openPlayStoreSubscriptions()
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            setTextColor(color(R.color.sweep_muted))
            textSize = 12f
        }
        linksRow.addView(restorePurchasesButton, LinearLayout.LayoutParams(0, -2, 1f))
        linksRow.addView(manageSubscriptionButton, LinearLayout.LayoutParams(0, -2, 1f))
        addView(linksRow, top(12))

        val privacyPolicyButton = button(getString(R.string.privacy_policy_button)) {
            val isPt = Locale.getDefault().language.startsWith("pt", ignoreCase = true)
            val privacyUrl = if (isPt) {
                "https://midiastory-df6a0.web.app/privacidade.html"
            } else {
                "https://midiastory-df6a0.web.app/privacy.html"
            }
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
            }
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            setTextColor(color(R.color.sweep_muted))
            textSize = 11.5f
            minHeight = dp(38)
        }
        addView(privacyPolicyButton, top(2))

        val termsDisclosure = label(getString(R.string.pro_billing_terms_disclosure), 10.5f, R.color.sweep_muted, false)
        proTermsDisclosureView = termsDisclosure
        addView(termsDisclosure, top(8))
        if (!billingGateway.securePurchaseVerificationAvailable()) {
            addView(label(getString(R.string.pro_purchase_unavailable_notice), 10.5f, R.color.sweep_muted, false), top(6))
        }

        if (isDevelopmentBuild) {
            val debugPreviewButton = button(getString(R.string.pro_debug_preview_off)) {
                debugProPreviewEnabled = !debugProPreviewEnabled
                debugProPreviewButton?.text = getString(
                    if (debugProPreviewEnabled) R.string.pro_debug_preview_on else R.string.pro_debug_preview_off,
                )
                renderBillingState(billingGateway.accessState.value)
                updateReview(viewModel.state.value)
            }.apply {
                backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
                setTextColor(color(R.color.sweep_muted))
                textSize = 11f
            }
            debugProPreviewButton = debugPreviewButton
            addView(debugPreviewButton, top(12))
        }
    }

    private fun switchTab(tab: AppTab) {
        activeTab = tab
        activeTabComposeState.value = tab
        if (::topBarProButton.isInitialized) {
            topBarProButton.visibility = if (tab == AppTab.PRO) View.GONE else View.VISIBLE
        }
        statusText.visibility = if (statusText.text.isNotBlank() && tab == AppTab.OAAS) View.VISIBLE else View.GONE
        homeSection.visibility = View.GONE
        homeComposeView.visibility = if (tab == AppTab.HOME) View.VISIBLE else View.GONE
        filesSection.visibility = if (tab == AppTab.FILES || tab == AppTab.OAAS) View.VISIBLE else View.GONE
        oaasSection.visibility = View.GONE
        proSection.visibility = if (tab == AppTab.PRO) View.VISIBLE else View.GONE
        if ((tab == AppTab.FILES || tab == AppTab.OAAS) && ::viewModel.isInitialized) {
            val st = viewModel.state.value
            if (st.currentPage == null && !st.isLoading && hasAnyMediaPermission()) {
                viewModel.startSession()
            } else if (st.selectedKind == MediaKind.IMAGE && st.smartFilter == SmartFilterMode.LARGE_FILES_10MB) {
                viewModel.selectSmartFilter(SmartFilterMode.ALL_ITEMS)
            }
        }
        if (::mainScrollView.isInitialized) {
            mainScrollView.post { mainScrollView.scrollTo(0, 0) }
        }

        val tabAccent = color(R.color.google_blue_primary)
        styleNavButton(navHomeBtn, tab == AppTab.HOME, customSelectedColor = tabAccent)
        styleNavButton(navFilesBtn, tab == AppTab.FILES || tab == AppTab.OAAS, customSelectedColor = tabAccent)
        styleNavButton(navProBtn, tab == AppTab.PRO, customSelectedColor = tabAccent)
    }

    private fun styleNavButton(btn: MaterialButton, selected: Boolean, customSelectedColor: Int? = null) {
        val tintColor = if (selected) (customSelectedColor ?: color(R.color.google_blue_primary)) else color(R.color.sweep_muted)
        btn.setTextColor(tintColor)
        btn.iconTint = android.content.res.ColorStateList.valueOf(tintColor)
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
    }

    private fun onCleanMissionClick(mission: CleanMission) {
        when (mission) {
            CleanMission.WHATSAPP -> openWhatsAppCleanerFlow()
            CleanMission.SIMILAR_PHOTOS -> openSimilarPhotosCleanerFlow()
            CleanMission.DUPLICATES -> {
                openInteractiveDialogOnMissionFinish = true
                runOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
            }
            CleanMission.LARGE_VIDEOS -> {
                openInteractiveDialogOnMissionFinish = true
                runOutcomeMission(DesiredOutcomeMission.FREE_MAX_SPACE_NOW)
            }
            CleanMission.OLD_SCREENSHOTS -> {
                openInteractiveDialogOnMissionFinish = true
                runOutcomeMission(DesiredOutcomeMission.CLEAN_OLD_SCREENSHOTS_30D)
            }
            CleanMission.BLURRY_PHOTOS -> openBlurCleanerFlow()
        }
    }

    private fun buildDefaultHomeCollections(): List<CleanCollectionUiModel> = listOf(
        CleanCollectionUiModel(
            mission = CleanMission.DUPLICATES,
            title = getString(R.string.cleaner_home_fallback_duplicates_title),
            subtitle = getString(R.string.cleaner_home_fallback_duplicates_sub),
            sizeLabel = "SHA-256",
            itemCount = 0,
        ),
        CleanCollectionUiModel(
            mission = CleanMission.SIMILAR_PHOTOS,
            title = getString(R.string.cleaner_home_fallback_similar_title),
            subtitle = getString(R.string.cleaner_home_fallback_similar_sub),
            sizeLabel = "dHash",
            itemCount = 0,
        ),
        CleanCollectionUiModel(
            mission = CleanMission.LARGE_VIDEOS,
            title = getString(R.string.cleaner_home_fallback_large_videos_title),
            subtitle = getString(R.string.cleaner_home_fallback_large_videos_sub),
            sizeLabel = "> 10 MB",
            itemCount = 0,
        ),
        CleanCollectionUiModel(
            mission = CleanMission.OLD_SCREENSHOTS,
            title = getString(R.string.cleaner_home_fallback_screenshots_title),
            subtitle = getString(R.string.cleaner_home_fallback_screenshots_sub),
            sizeLabel = "> 30d",
            itemCount = 0,
        ),
        CleanCollectionUiModel(
            mission = CleanMission.WHATSAPP,
            title = getString(R.string.cleaner_home_fallback_whatsapp_title),
            subtitle = getString(R.string.cleaner_home_fallback_whatsapp_sub),
            sizeLabel = "MediaStore",
            itemCount = 0,
        ),
        CleanCollectionUiModel(
            mission = CleanMission.BLURRY_PHOTOS,
            title = getString(R.string.cleaner_home_fallback_blur_title),
            subtitle = getString(R.string.cleaner_home_fallback_blur_sub),
            sizeLabel = "Local",
            itemCount = 0,
        ),
    )

    private fun isTesterVipProEnabled(): Boolean =
        isDevelopmentBuild &&
            getSharedPreferences("midiastore_prefs", MODE_PRIVATE).getBoolean("tester_vip_pro_v25", false)

    private fun setTesterVipProEnabled(enabled: Boolean) {
        getSharedPreferences("midiastore_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("tester_vip_pro_v25", enabled)
            .apply()
    }

    private fun hasEffectiveProAccess(): Boolean =
        billingGateway.accessState.value == ProAccessSummaryState.PRO_GRANTED ||
            isTesterVipProEnabled() ||
            (isDevelopmentBuild && debugProPreviewEnabled)

    private fun styleTesterVipButton(button: MaterialButton, active: Boolean) {
        button.text = getString(if (active) R.string.tester_vip_active_button else R.string.tester_vip_activate_button)
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (active) color(R.color.sweep_surface) else color(R.color.google_blue_primary),
        )
        button.setTextColor(if (active) color(R.color.google_blue_primary) else Color.WHITE)
    }

    private fun renderBillingState(state: ProAccessSummaryState) {
        if (!::proStatusBadge.isInitialized) return
        if (::bottomNavContainer.isInitialized) {
            navFilesBtn.visibility = View.VISIBLE
            navProBtn.visibility = View.VISIBLE
        }
        testerVipButton?.let { styleTesterVipButton(it, isTesterVipProEnabled()) }
        refreshHomeProConversionCta(state)
        if (state == ProAccessSummaryState.PRO_GRANTED) {
            proStatusBadge.text = getString(R.string.pro_status_active)
            return
        }
        if (isTesterVipProEnabled()) {
            proStatusBadge.text = getString(R.string.tester_vip_status_badge)
            return
        }
        if (isDevelopmentBuild && debugProPreviewEnabled) {
            proStatusBadge.text = getString(R.string.pro_debug_preview_badge)
            return
        }
        proStatusBadge.text = when (state) {
            ProAccessSummaryState.PRO_GRANTED -> getString(R.string.pro_status_active)
            ProAccessSummaryState.PENDING_PAYMENT -> getString(R.string.pro_status_pending)
            ProAccessSummaryState.RECONCILIATION_REQUIRED -> getString(R.string.pro_status_reconciliation)
            ProAccessSummaryState.FREE_NO_ACTIVE_PURCHASE -> getString(R.string.pro_status_free)
        }
    }

    private fun refreshHomeProConversionCta(state: ProAccessSummaryState) {
        val isPro = state == ProAccessSummaryState.PRO_GRANTED ||
            isTesterVipProEnabled() ||
            (isDevelopmentBuild && debugProPreviewEnabled)
        if (::topBarProButton.isInitialized) {
            topBarProButton.text = getString(if (isPro) R.string.top_pro_active_pill else R.string.premium_area_button)
            topBarProButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                color(R.color.sweep_surface_alt),
            )
            topBarProButton.setTextColor(
                if (isPro) color(R.color.google_blue_primary) else color(R.color.sweep_text),
            )
        }
        applyDynamicProTheme(isPro)
        applyEasySeniorMode(isEasySeniorModeEnabled())
        if (!::homeProSubscribeButton.isInitialized) return
        homeProCtaCard.setCardBackgroundColor(color(R.color.sweep_surface))
        homeProCtaCard.strokeColor = color(R.color.sweep_surface_alt)
        homeProCtaBadge.setTextColor(color(R.color.google_blue_primary))
        homeProSubscribeButton.icon = ContextCompat.getDrawable(this, android.R.drawable.btn_star_big_on)
        if (isPro) {
            homeProCtaTitle.text = getString(R.string.home_pro_cta_active_title)
            homeProSubscribeButton.text = getString(R.string.home_pro_active_button)
            homeProSubscribeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
            homeProSubscribeButton.setTextColor(color(R.color.google_blue_primary))
            homeProSubscribeButton.iconTint = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        } else {
            homeProCtaTitle.text = getString(R.string.home_pro_cta_title)
            val priceHint = latestHeroPriceBadge
            homeProSubscribeButton.text = if (!priceHint.isNullOrBlank()) {
                getString(R.string.home_pro_subscribe_button_with_price, priceHint)
            } else {
                getString(R.string.home_pro_subscribe_button)
            }
            homeProSubscribeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
            homeProSubscribeButton.setTextColor(Color.WHITE)
            homeProSubscribeButton.iconTint = android.content.res.ColorStateList.valueOf(Color.WHITE)
        }
    }

    /** Mantém a paleta unificada Apple Dark OLED (#000000 / #1C1C1E / #0A84FF) sem poluição visual. */
    private fun applyDynamicProTheme(isPro: Boolean) {
        if (::appRootView.isInitialized) {
            appRootView.setBackgroundColor(color(R.color.sweep_background))
        }
        if (::brandLockupCard.isInitialized) {
            brandLockupCard.background = null
        }
        if (::brandWordmarkView.isInitialized) {
            renderBrandWordmark(isPro)
        }
        if (::brandTaglineLabel.isInitialized) {
            brandTaglineLabel.text = getString(
                if (isPro) R.string.brand_tagline_pro else R.string.brand_tagline,
            )
            brandTaglineLabel.setTextColor(color(R.color.sweep_muted))
        }
        if (::storageCardContainer.isInitialized) {
            storageCardContainer.setCardBackgroundColor(color(R.color.sweep_surface))
            storageCardContainer.strokeColor = color(R.color.sweep_surface_alt)
        }
        if (::storageRingGauge.isInitialized) {
            storageRingGauge.isProVipTheme = false
        }
        if (::heroOrbButton.isInitialized) {
            heroOrbButton.backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
            heroOrbButton.setTextColor(Color.WHITE)
        }
        if (::bottomNavContainer.isInitialized) {
            bottomNavContainer.setBackgroundColor(Color.parseColor("#0A0A0C"))
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            run {
                val barColor = color(R.color.sweep_background)
                window.statusBarColor = barColor
                window.navigationBarColor = barColor
            }
        }
    }

    private fun isEasySeniorModeEnabled(): Boolean =
        getSharedPreferences("midiastore_global_hub", Context.MODE_PRIVATE)
            .getBoolean("easy_senior_mode_v1", false)

    private fun setEasySeniorModeEnabled(enabled: Boolean) {
        getSharedPreferences("midiastore_global_hub", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("easy_senior_mode_v1", enabled)
            .apply()
    }

    private fun applyEasySeniorMode(enabled: Boolean) {
        if (::easyModeButton.isInitialized) {
            easyModeButton.text = getString(if (enabled) R.string.easy_mode_btn_on else R.string.easy_mode_btn_off)
            easyModeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (enabled) color(R.color.sweep_primary) else color(R.color.sweep_surface_alt),
            )
            easyModeButton.setTextColor(
                if (enabled) color(R.color.sweep_background) else color(R.color.sweep_text),
            )
        }
        if (::brandTaglineLabel.isInitialized) brandTaglineLabel.textSize = if (enabled) 14f else 12f
        if (::storageUsedHeadline.isInitialized) storageUsedHeadline.textSize = if (enabled) 23f else 19f
        if (::storageFreeSubline.isInitialized) storageFreeSubline.textSize = if (enabled) 16f else 13f
        if (::heroOrbButton.isInitialized) {
            heroOrbButton.textSize = if (enabled) 21f else 18f
            heroOrbButton.minHeight = dp(if (enabled) 80 else 68)
        }
        if (::heroOrbSubtitle.isInitialized) heroOrbSubtitle.textSize = if (enabled) 15f else 13f
        if (::homeProCtaTitle.isInitialized) homeProCtaTitle.textSize = if (enabled) 19f else 15f
        if (::homeProCtaSubtitle.isInitialized) homeProCtaSubtitle.textSize = if (enabled) 15f else 12.5f
        if (::homeProSubscribeButton.isInitialized) {
            homeProSubscribeButton.textSize = if (enabled) 16.5f else 14f
            homeProSubscribeButton.minHeight = dp(if (enabled) 68 else 60)
        }
    }

    private fun onHomeProSubscribeClicked() {
        if (hasEffectiveProAccess()) {
            switchTab(AppTab.PRO)
            return
        }
        openQuickProCheckoutDialog()
    }

    private fun openQuickProCheckoutDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(10))
        }
        box.addView(label(getString(R.string.quick_pro_dialog_subtitle), 13f, R.color.sweep_muted, false))

        var dialogRef: AlertDialog? = null

        val annualBtn = button(latestAnnualOfferText ?: getString(R.string.plan_annual_default)) {
            dialogRef?.dismiss()
            requestPlanPurchase(BillingStatePolicy.PRODUCT_ANNUAL)
        }.apply {
            minHeight = dp(58)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        box.addView(annualBtn, top(14))

        val monthlyBtn = button(latestMonthlyOfferText ?: getString(R.string.plan_monthly_default)) {
            dialogRef?.dismiss()
            requestPlanPurchase(BillingStatePolicy.PRODUCT_MONTHLY)
        }.apply {
            minHeight = dp(52)
            textSize = 13f
            setTextColor(color(R.color.sweep_text))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
        }
        box.addView(monthlyBtn, top(10))

        val lifetimeBtn = button(latestLifetimeOfferText ?: getString(R.string.plan_lifetime_default)) {
            dialogRef?.dismiss()
            requestPlanPurchase(BillingStatePolicy.PRODUCT_LIFETIME)
        }.apply {
            minHeight = dp(52)
            textSize = 13f
            setTextColor(color(R.color.sweep_text))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
        }
        box.addView(lifetimeBtn, top(10))

        val dialogTerms = if (latestAnnualHasFreeTrial && !latestAnnualFormattedPrice.isNullOrBlank()) {
            getString(R.string.pro_annual_trial_terms_disclosure, latestAnnualFormattedPrice)
        } else {
            getString(R.string.pro_billing_terms_disclosure)
        }
        box.addView(label(dialogTerms, 11f, R.color.sweep_muted, false), top(12))

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.quick_pro_dialog_title)
            .setView(box)
            .setPositiveButton(R.string.quick_pro_open_tab_btn) { d, _ ->
                d.dismiss()
                switchTab(AppTab.PRO)
            }
            .setNeutralButton(R.string.quick_pro_restore_btn) { _, _ ->
                billingGateway.startAndReconcile()
                Toast.makeText(this, R.string.restoring_purchases_status, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialogRef = dialog
        dialog.show()
    }

    private fun renderCatalogPrices(catalog: Map<String, PlayCatalogProductUi>) {
        if (!::planAnnualButton.isInitialized || !::homePlanAnnualButton.isInitialized) return
        val annual = catalog[BillingStatePolicy.PRODUCT_ANNUAL]
        val monthly = catalog[BillingStatePolicy.PRODUCT_MONTHLY]
        val lifetime = catalog[BillingStatePolicy.PRODUCT_LIFETIME]
        listOf(
            planAnnualButton to annual,
            homePlanAnnualButton to annual,
            planMonthlyButton to monthly,
            homePlanMonthlyButton to monthly,
            planLifetimeButton to lifetime,
            homePlanLifetimeButton to lifetime,
        ).forEach { (button, product) ->
            stylePlanPurchaseButton(button, (product?.isAvailable == true) || isDevelopmentBuild)
        }

        val isDevFallbackPreview = isDevelopmentBuild && (annual == null || !annual.isAvailable)
        val hasTrialOffer = (annual?.hasFreeTrialOffer == true) || isDevFallbackPreview
        val displayAnnualPrice = annual?.formattedPrice?.takeIf { it.isNotBlank() }
            ?: if (isDevFallbackPreview) "R$ 79,99" else null
        latestAnnualHasFreeTrial = hasTrialOffer && !displayAnnualPrice.isNullOrBlank()
        latestAnnualFormattedPrice = displayAnnualPrice

        val annualLabel = if (annual != null && annual.formattedPrice.isNotBlank()) {
            if (annual.hasFreeTrialOffer) {
                getString(R.string.plan_annual_free_trial_cta, annual.formattedPrice)
            } else {
                val realDiscount = BillingStatePolicy.calculateRealAnnualDiscountPercent(
                    monthlyPriceMicros = monthly?.priceAmountMicros,
                    annualPriceMicros = annual.priceAmountMicros,
                    monthlyCurrencyCode = monthly?.priceCurrencyCode,
                    annualCurrencyCode = annual.priceCurrencyCode,
                )
                if (realDiscount != null) {
                    getString(R.string.plan_annual_with_price_and_discount, annual.formattedPrice, realDiscount)
                } else {
                    getString(R.string.plan_annual_with_price, annual.formattedPrice)
                }
            }
        } else if (isDevelopmentBuild) {
            getString(R.string.plan_annual_free_trial_cta, "R$ 79,99")
        } else if (annual != null && !annual.isAvailable) {
            getString(R.string.plan_annual_unavailable)
        } else {
            null
        }
        proAnnualSavingsBadgeView?.text = getString(
            if (latestAnnualHasFreeTrial) R.string.pro_annual_trial_badge else R.string.pro_annual_savings_badge,
        )
        proTermsDisclosureView?.text = if (latestAnnualHasFreeTrial && !displayAnnualPrice.isNullOrBlank()) {
            getString(R.string.pro_annual_trial_terms_disclosure, displayAnnualPrice)
        } else {
            getString(R.string.pro_billing_terms_disclosure)
        }
        annualLabel?.let {
            planAnnualButton.text = it
            homePlanAnnualButton.text = it
        }
        val monthlyLabel = if (monthly != null && monthly.formattedPrice.isNotBlank()) {
            getString(R.string.plan_monthly_with_price, monthly.formattedPrice)
        } else if (isDevelopmentBuild) {
            getString(R.string.plan_monthly_with_price, "R$ 14,99")
        } else if (monthly != null && !monthly.isAvailable) {
            getString(R.string.plan_monthly_unavailable)
        } else {
            null
        }
        monthlyLabel?.let {
            planMonthlyButton.text = it
            homePlanMonthlyButton.text = it
        }
        val lifetimeLabel = if (lifetime != null && lifetime.formattedPrice.isNotBlank()) {
            getString(R.string.plan_lifetime_with_price, lifetime.formattedPrice)
        } else if (isDevelopmentBuild) {
            getString(R.string.plan_lifetime_with_price, "R$ 149,99")
        } else if (lifetime != null && !lifetime.isAvailable) {
            getString(R.string.plan_lifetime_unavailable)
        } else {
            null
        }
        lifetimeLabel?.let {
            planLifetimeButton.text = it
            homePlanLifetimeButton.text = it
        }
        latestAnnualOfferText = annualLabel
        latestMonthlyOfferText = monthlyLabel
        latestLifetimeOfferText = lifetimeLabel
        latestHeroPriceBadge = annual?.formattedPrice?.takeIf { it.isNotBlank() }
            ?: monthly?.formattedPrice?.takeIf { it.isNotBlank() }
            ?: lifetime?.formattedPrice?.takeIf { it.isNotBlank() }
        refreshHomeProConversionCta(billingGateway.accessState.value)
    }

    private fun stylePlanPurchaseButton(button: MaterialButton, isProductAvailable: Boolean) {
        val purchasable = billingGateway.securePurchaseVerificationAvailable() && isProductAvailable
        val isAnnual = ::planAnnualButton.isInitialized && button === planAnnualButton
        button.isEnabled = true
        button.cornerRadius = dp(16)
        button.minHeight = dp(58)
        button.strokeWidth = dp(1)
        button.strokeColor = android.content.res.ColorStateList.valueOf(
            color(if (isAnnual) R.color.google_blue_primary else R.color.sweep_surface_alt),
        )
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(
            color(if (purchasable && isAnnual) R.color.google_blue_primary else R.color.sweep_surface),
        )
        button.setTextColor(
            if (purchasable && isAnnual) Color.WHITE else color(if (purchasable) R.color.sweep_text else R.color.sweep_muted),
        )
    }

    private fun requestPlanPurchase(productId: String) {
        val launched = billingGateway.launchPurchaseFlow(this, productId)
        val message = getString(
            when {
                launched -> R.string.billing_flow_started
                !billingGateway.securePurchaseVerificationAvailable() -> R.string.billing_secure_verification_unavailable
                else -> R.string.billing_launch_not_ready
            },
        )
        statusText.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun openPlayStoreSubscriptions() {
        val opened = runCatching {
            val uri = Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.isSuccess
        if (!opened) Toast.makeText(this, R.string.billing_subscriptions_open_failed, Toast.LENGTH_LONG).show()
    }

    private fun startScanOrRequestAccess() {
        when {
            hasFullVisualAccess() || (limitedAccessAcknowledged && hasAnyMediaPermission()) -> viewModel.startSession()
            hasAnyMediaPermission() -> showLimitedAccessPromptSheet { viewModel.startSession() }
            else -> requestMediaPermissions(allowSettingsFallback = true)
        }
    }

    private fun checkAndShowFirstRunOnboarding() {
        // Abertura imediata padrão Apple/CleanMyPhone: sem modal cinza bloqueador travando a Home.
        val prefs = getSharedPreferences("midiastore_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_shown_v1", false)) {
            prefs.edit().putBoolean("onboarding_shown_v1", true).apply()
        }
    }

    private fun startOneTapCleanup() {
        homeCleanupCandidateCount = 0
        homeInteractiveRecords = emptyList()
        homeInteractiveFallback = false
        homeInteractiveMission = null
        openInteractiveDialogOnMissionFinish = true
        homeComposeState.value = homeComposeState.value.copy(primaryActionLabel = null)
        runOutcomeMission(DesiredOutcomeMission.FREE_MAX_SPACE_NOW)
    }

    private fun showHomeCleanupMessage(message: String) {
        homeComposeState.value = homeComposeState.value.copy(statusMessage = message)
        if (::homeCleanupResult.isInitialized) {
            homeCleanupResult.text = message
            homeCleanupResult.visibility = View.VISIBLE
        }
    }

    private fun onHomeCleanupButtonClicked() {
        val currentState = viewModel.state.value
        if (currentState.missionProgress?.isRunning == true || currentState.isLoading) {
            // Se a varredura já está rodando, agenda a abertura automática da folha de revisão assim que terminar!
            openInteractiveDialogOnMissionFinish = true
            Toast.makeText(this, R.string.cleanup_scan_in_progress_toast, Toast.LENGTH_SHORT).show()
            return
        }
        heroOrbButton.isEnabled = true
        val selectedNow = currentState.selection.records
        val freeSpaceRecords = if (homeInteractiveMission == DesiredOutcomeMission.FREE_MAX_SPACE_NOW) {
            homeInteractiveRecords
        } else {
            emptyList()
        }
        val displayRecords = when {
            freeSpaceRecords.isNotEmpty() -> freeSpaceRecords
            selectedNow.isNotEmpty() -> selectedNow
            else -> emptyList()
        }
        if (displayRecords.isNotEmpty()) {
            openInteractiveCleanupReviewDialog(
                records = displayRecords,
                isFallbackList = selectedNow.isEmpty() && homeInteractiveFallback,
                mission = DesiredOutcomeMission.FREE_MAX_SPACE_NOW,
                coverageComplete = homeInteractiveCoverageComplete,
            )
        } else {
            startOneTapCleanup()
        }
    }

    /**
     * Shown when the user granted Android 14+ partial access ("Select photos") or audio-only access.
     * Lets them either upgrade to full access or clean the subset they already authorized.
     */
    private fun showLimitedAccessPromptSheet(
        onContinueWithSelected: () -> Unit,
    ) {
        val (sheet, root) = createAppleBottomSheet(
            title = getString(R.string.limited_access_sheet_title),
            subtitle = getString(R.string.limited_access_sheet_body),
        )
        val allowAllBtn = button(getString(R.string.limited_access_allow_all)) {
            sheet.dismiss()
            requestMediaPermissions(allowSettingsFallback = true)
        }.apply {
            minHeight = dp(50)
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        root.addView(allowAllBtn, top(14))

        val continueBtn = button(getString(R.string.limited_access_continue_selected)) {
            sheet.dismiss()
            limitedAccessAcknowledged = true
            onContinueWithSelected()
        }.apply {
            minHeight = dp(48)
            setTextColor(color(R.color.sweep_text))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
        }
        root.addView(continueBtn, top(8))

        val cancelBtn = button(getString(android.R.string.cancel)) {
            pendingOutcomeMission = null
            pendingWhatsAppCleanerAfterPermission = false
            pendingBlurScanAfterPermission = false
            pendingSimilarScanAfterPermission = false
            pendingVideoCompressAfterPermission = false
            openInteractiveDialogOnMissionFinish = false
            sheet.dismiss()
        }.apply {
            minHeight = dp(46)
            setTextColor(color(R.color.sweep_muted))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        }
        root.addView(cancelBtn, top(4))
        sheet.show()
    }

    private fun showEmptyCleanupBottomSheet() {
        val (sheet, root) = createAppleBottomSheet(
            title = getString(R.string.cleanup_dialog_empty_title),
            subtitle = getString(R.string.cleanup_dialog_empty_body),
        )
        val waBtn = button(getString(R.string.quick_clean_whatsapp)) {
            sheet.dismiss()
            openWhatsAppCleanerFlow()
        }.apply {
            minHeight = dp(50)
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        root.addView(waBtn, top(14))

        val filesBtn = button(getString(R.string.nav_files)) {
            sheet.dismiss()
            switchTab(AppTab.FILES)
        }.apply {
            minHeight = dp(48)
            setTextColor(color(R.color.sweep_text))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
        }
        root.addView(filesBtn, top(8))

        val closeBtn = button(getString(android.R.string.cancel)) {
            sheet.dismiss()
        }.apply {
            minHeight = dp(46)
            setTextColor(color(R.color.sweep_muted))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        }
        root.addView(closeBtn, top(4))
        sheet.show()
    }

    private fun createAppleBottomSheet(
        title: String,
        subtitle: String? = null,
    ): Pair<BottomSheetDialog, LinearLayout> {
        val dialog = BottomSheetDialog(this)
        val sheetBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color(R.color.sweep_surface))
            val r = dp(24).toFloat()
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = sheetBg
            setPadding(dp(20), dp(12), dp(20), dp(22))
        }
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(dp(20), dp(12), dp(20), dp(22) + nav.bottom)
            insets
        }
        val handle = View(this).apply {
            background = roundedBackground(color(R.color.sweep_surface_highest), dp(3))
        }
        container.addView(handle, LinearLayout.LayoutParams(dp(38), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(14)
        })
        container.addView(label(title, 18.5f, R.color.sweep_text, true))
        if (!subtitle.isNullOrBlank()) {
            container.addView(label(subtitle, 12.5f, R.color.sweep_muted, false), top(4))
        }
        dialog.setContentView(container)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }
        return dialog to container
    }

    private fun openInteractiveCleanupReviewDialog(
        records: List<MediaRecord>,
        isFallbackList: Boolean,
        mission: DesiredOutcomeMission?,
        coverageComplete: Boolean,
        customSubtitle: String? = null,
    ) {
        val currentPageSelectable = viewModel.state.value.currentPage?.records
            ?.filter { it.isSelectable && !it.isProtected }
            ?.sortedByDescending { it.sizeBytes }
            .orEmpty()
        val effectiveRecords = when {
            records.isNotEmpty() -> records
            homeInteractiveRecords.isNotEmpty() -> homeInteractiveRecords
            currentPageSelectable.isNotEmpty() -> currentPageSelectable
            else -> emptyList()
        }
        val effectiveIsFallback = isFallbackList || (records.isEmpty() && effectiveRecords.isNotEmpty())
        if (effectiveRecords.isEmpty()) {
            if (!hasFullVisualAccess()) {
                pendingOutcomeMission = mission
                openInteractiveDialogOnMissionFinish = mission != null
                showLimitedAccessPromptSheet {
                    if (mission != null) {
                        openInteractiveDialogOnMissionFinish = true
                        runOutcomeMission(mission)
                    } else {
                        switchTab(AppTab.FILES)
                    }
                }
                return
            }
            showEmptyCleanupBottomSheet()
            return
        }

        val cappedRecords = effectiveRecords.filter { it.isSelectable && !it.isProtected }.take(200)
        if (cappedRecords.isEmpty()) {
            showEmptyCleanupBottomSheet()
            return
        }

        val currentlySelectedUris = viewModel.state.value.selection.records.map { it.uri }.toSet()
        val hasOverlapWithSelection = cappedRecords.any { it.uri in currentlySelectedUris }
        val allowAutoCheck = mission != DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES || coverageComplete
        val initialChecked = BooleanArray(cappedRecords.size) { index ->
            val rec = cappedRecords[index]
            when {
                hasOverlapWithSelection -> rec.uri in currentlySelectedUris
                allowAutoCheck -> true
                else -> false
            }
        }

        val headerSubtitle = customSubtitle ?: getString(
            if (effectiveIsFallback) R.string.cleanup_dialog_subtitle_fallback
            else R.string.cleanup_dialog_subtitle_candidates,
        )
        val (dialog, root) = createAppleBottomSheet(
            title = getString(R.string.cleanup_sheet_title),
            subtitle = headerSubtitle,
        )

        if (!hasFullVisualAccess()) {
            val upgradeAccessBtn = button(getString(R.string.limited_access_allow_all)) {
                dialog.dismiss()
                if (mission != null) {
                    pendingOutcomeMission = mission
                    openInteractiveDialogOnMissionFinish = true
                }
                requestMediaPermissions(allowSettingsFallback = true)
            }.apply {
                minHeight = dp(42)
                textSize = 12f
                setTextColor(color(R.color.google_blue_primary))
                backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
            }
            root.addView(upgradeAccessBtn, top(8))
        }

        val summaryLabel = label("", 13f, R.color.sweep_primary_glow, true)
        root.addView(summaryLabel, top(10))

        lateinit var toggleAllBtn: MaterialButton
        var deleteActionBtn: MaterialButton? = null
        val itemBoxes = ArrayList<CheckBox>(cappedRecords.size)

        fun refreshSummary() {
            var count = 0
            var totalBytes = 0L
            for (i in cappedRecords.indices) {
                if (initialChecked[i]) {
                    count++
                    totalBytes += cappedRecords[i].sizeBytes.coerceAtLeast(0L)
                }
            }
            val sizeFmt = Formatter.formatFileSize(this@MainActivity, totalBytes)
            summaryLabel.text = getString(R.string.cleanup_dialog_selected_summary, count, cappedRecords.size, sizeFmt)
            toggleAllBtn.text = getString(
                if (count == cappedRecords.size) R.string.cleanup_dialog_deselect_all
                else R.string.cleanup_dialog_select_all,
                cappedRecords.size,
            )
            deleteActionBtn?.text = if (count > 0 && totalBytes > 0L) {
                "${getString(R.string.permanent_delete_button)} ($sizeFmt)"
            } else {
                getString(R.string.permanent_delete_button)
            }
        }

        toggleAllBtn = button(getString(R.string.cleanup_dialog_select_all, cappedRecords.size)) {
            val allSelected = initialChecked.all { it }
            val nextState = !allSelected
            for (i in initialChecked.indices) {
                initialChecked[i] = nextState
                itemBoxes[i].setOnCheckedChangeListener(null)
                itemBoxes[i].isChecked = nextState
                val idx = i
                itemBoxes[i].setOnCheckedChangeListener { _, isChecked ->
                    initialChecked[idx] = isChecked
                    refreshSummary()
                }
            }
            refreshSummary()
        }.apply {
            minHeight = dp(48)
        }
        root.addView(toggleAllBtn, top(8))

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        cappedRecords.forEachIndexed { idx, record ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(6), dp(8), dp(6))
                background = roundedBackground(color(R.color.sweep_surface_alt), dp(12))
            }
            val box = CheckBox(this).apply {
                isChecked = initialChecked[idx]
                minWidth = dp(48)
                minHeight = dp(48)
                setOnCheckedChangeListener { _, isChecked ->
                    initialChecked[idx] = isChecked
                    refreshSummary()
                }
            }
            itemBoxes += box
            row.addView(box, LinearLayout.LayoutParams(-2, -2))

            val thumb = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(color(R.color.sweep_background))
                tag = record.uri
                isClickable = true
                isFocusable = true
                setOnClickListener { openMediaPreview(record) }
            }
            if (record.kind == MediaKind.AUDIO) {
                thumb.setImageResource(android.R.drawable.ic_media_play)
            } else {
                loadThumbnail(thumb, record)
            }
            row.addView(thumb, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(4); marginEnd = dp(8) })

            val meta = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val displayName = record.displayName?.takeIf(String::isNotBlank) ?: getString(R.string.unnamed_file)
            meta.addView(
                label(displayName, 12f, R.color.sweep_text, true).apply {
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                },
            )
            val kindStr = getString(
                when (record.kind) {
                    MediaKind.IMAGE -> R.string.photo_label
                    MediaKind.VIDEO -> R.string.video_label
                    MediaKind.AUDIO -> R.string.audio_label
                },
            )
            val dupTag = if (record.duplicateReferenceSnapshot != null) getString(R.string.exact_copy_badge) else ""
            meta.addView(
                label(
                    "${Formatter.formatFileSize(this@MainActivity, record.sizeBytes)} · $kindStr$dupTag",
                    11f,
                    R.color.sweep_primary_glow,
                    false,
                ),
                top(2),
            )
            row.setOnClickListener { box.isChecked = !box.isChecked }
            row.addView(meta, LinearLayout.LayoutParams(0, -2, 1f))
            val quickDeleteBtn = button("×") {
                dialog.dismiss()
                requestPermanentDeleteSelected(listOf(record))
            }.apply {
                minWidth = dp(44)
                minHeight = dp(44)
                textSize = 18f
                setTextColor(color(R.color.sweep_danger))
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#22FF453A"))
                setPadding(dp(4), dp(4), dp(4), dp(4))
            }
            row.addView(quickDeleteBtn, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(4) })
            listContainer.addView(row, bottom(6))
        }

        val maxListHeight = (resources.displayMetrics.heightPixels * 0.42f).toInt().coerceIn(dp(200), dp(360))
        val listScroll = ScrollView(this).apply {
            addView(listContainer, ViewGroup.LayoutParams(-1, -2))
        }
        root.addView(listScroll, LinearLayout.LayoutParams(-1, maxListHeight).apply { topMargin = dp(8) })
        refreshSummary()

        fun selectedFromDialog(): List<MediaRecord> =
            cappedRecords.indices.filter { initialChecked[it] }.map { cappedRecords[it] }

        val supportsTrash = trashCoordinator.supportsSystemTrash()
        deleteActionBtn = button(getString(R.string.permanent_delete_button)) {
            val chosen = selectedFromDialog()
            if (chosen.isEmpty()) {
                Toast.makeText(this@MainActivity, R.string.cleanup_dialog_select_none_toast, Toast.LENGTH_SHORT).show()
                return@button
            }
            dialog.dismiss()
            viewModel.prepareExternalReviewSelection(chosen)
            requestPermanentDeleteSelected(chosen)
        }.apply {
            minHeight = dp(52)
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_danger))
        }
        root.addView(deleteActionBtn, top(12))
        refreshSummary()

        val secondaryBtn = button(
            if (supportsTrash) getString(R.string.move_to_trash_button)
            else getString(R.string.cleanup_dialog_open_files_tab),
        ) {
            val chosen = selectedFromDialog()
            if (chosen.isEmpty()) {
                Toast.makeText(this@MainActivity, R.string.cleanup_dialog_select_none_toast, Toast.LENGTH_SHORT).show()
                return@button
            }
            dialog.dismiss()
            viewModel.prepareExternalReviewSelection(chosen)
            if (supportsTrash) {
                requestMoveSelectedToTrash(chosen)
            } else {
                switchTab(AppTab.FILES)
            }
        }.apply {
            minHeight = dp(48)
            setTextColor(color(R.color.sweep_text))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
        }
        root.addView(secondaryBtn, top(8))

        val cancelSheetBtn = button(getString(android.R.string.cancel)) {
            dialog.dismiss()
        }.apply {
            minHeight = dp(48)
            setTextColor(color(R.color.sweep_muted))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        }
        root.addView(cancelSheetBtn, top(4))

        dialog.show()
    }

    private fun runWithProAccess(action: () -> Unit) {
        if (hasEffectiveProAccess()) {
            action()
        } else {
            switchTab(AppTab.PRO)
            Toast.makeText(this, R.string.pro_feature_requires_pro, Toast.LENGTH_LONG).show()
        }
    }

    private fun requestUnusedAppSuggestions() {
        if (!hasUsageStatsAccess()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.pro_unused_apps_disclosure_title)
                .setMessage(R.string.pro_unused_apps_disclosure_message)
                .setPositiveButton(R.string.pro_unused_apps_disclosure_continue) { _, _ ->
                    runCatching {
                        usageAccessLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }.onFailure {
                        Toast.makeText(this, R.string.pro_unused_apps_permission_denied, Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton(R.string.pro_unused_apps_disclosure_not_now, null)
                .show()
            return
        }
        loadUnusedAppSuggestions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingSendIntent(intent)
    }

    private fun openWhatsAppCleanerFlow() {
        if (!hasFullVisualAccess() && !limitedAccessAcknowledged) {
            pendingWhatsAppCleanerAfterPermission = true
            if (hasAnyMediaPermission()) {
                showLimitedAccessPromptSheet {
                    pendingWhatsAppCleanerAfterPermission = false
                    openWhatsAppCleanerFlow()
                }
            } else {
                requestMediaPermissions(allowSettingsFallback = true)
            }
            return
        }
        lifecycleScope.launch {
            val (scanDialog, scanRoot) = createAppleBottomSheet(
                title = getString(R.string.whatsapp_cleaner_title),
                subtitle = getString(R.string.wa_scanning_message),
            )
            val scanBar = ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                isIndeterminate = true
                indeterminateTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
            }
            scanRoot.addView(scanBar, top(12))
            var scanCancelled = false
            scanDialog.setOnCancelListener { scanCancelled = true }
            scanDialog.show()

            val items: List<WhatsAppMediaItem> = try {
                withContext(Dispatchers.IO) {
                    whatsAppCleaner.scanViaMediaStore(this@MainActivity)
                }
            } catch (_: Exception) {
                emptyList()
            } finally {
                if (scanDialog.isShowing) scanDialog.dismiss()
            }
            if (scanCancelled) return@launch

            val mediaRecordsByCategory = items
                .mapNotNull { item -> item.toMediaRecord()?.let { record -> item.category to record } }
                .groupBy({ it.first }, { it.second })

            if (mediaRecordsByCategory.isEmpty()) {
                if (!hasFullVisualAccess()) {
                    pendingWhatsAppCleanerAfterPermission = true
                    showLimitedAccessPromptSheet {
                        pendingWhatsAppCleanerAfterPermission = false
                        startOneTapCleanup()
                    }
                    return@launch
                }
                val (emptySheet, emptyRoot) = createAppleBottomSheet(
                    title = getString(R.string.whatsapp_cleaner_title),
                    subtitle = getString(R.string.whatsapp_cleaner_empty),
                )
                val findLargeBtn = button(getString(R.string.quick_find_large)) {
                    emptySheet.dismiss()
                    startOneTapCleanup()
                }.apply {
                    minHeight = dp(50)
                    setTextColor(Color.WHITE)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
                }
                emptyRoot.addView(findLargeBtn, top(14))
                val openFilesBtn = button(getString(R.string.nav_files)) {
                    emptySheet.dismiss()
                    switchTab(AppTab.FILES)
                }.apply {
                    minHeight = dp(48)
                    setTextColor(color(R.color.sweep_text))
                    backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
                }
                emptyRoot.addView(openFilesBtn, top(8))
                val closeBtn = button(getString(android.R.string.cancel)) {
                    emptySheet.dismiss()
                }.apply {
                    minHeight = dp(46)
                    setTextColor(color(R.color.sweep_muted))
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                }
                emptyRoot.addView(closeBtn, top(4))
                emptySheet.show()
                return@launch
            }

            val totalWaRecords = mediaRecordsByCategory.values.flatten()
            val totalWaBytes = totalWaRecords.sumOf { it.sizeBytes.coerceAtLeast(0L) }
            updateHomeMissionCollection(
                mission = CleanMission.WHATSAPP,
                itemCount = totalWaRecords.size,
                sizeLabel = Formatter.formatFileSize(this@MainActivity, totalWaBytes),
            )

            val orderedCategories = listOf(
                WhatsAppMediaCategory.VIDEO to getString(R.string.whatsapp_cat_videos),
                WhatsAppMediaCategory.VOICE_NOTE to getString(R.string.whatsapp_cat_voice),
                WhatsAppMediaCategory.AUDIO to getString(R.string.wa_cat_audio),
                WhatsAppMediaCategory.IMAGE to getString(R.string.whatsapp_cat_images),
                WhatsAppMediaCategory.GIF to getString(R.string.wa_cat_gifs),
                WhatsAppMediaCategory.DOCUMENT to getString(R.string.wa_cat_docs),
                WhatsAppMediaCategory.OTHER to getString(R.string.wa_cat_other),
            ).filter { (cat, _) -> !mediaRecordsByCategory[cat].isNullOrEmpty() }

            val checked = BooleanArray(orderedCategories.size) { true }
            val (waSheet, waRoot) = createAppleBottomSheet(
                title = getString(R.string.wa_found_title),
                subtitle = getString(R.string.wa_sheet_subtitle),
            )
            val waSummary = label("", 13f, R.color.sweep_primary_glow, true)
            waRoot.addView(waSummary, top(8))
            var waDeleteBtn: MaterialButton? = null

            fun refreshWaSummary() {
                val selectedRecords = orderedCategories.indices
                    .filter { checked[it] }
                    .flatMap { idx -> mediaRecordsByCategory[orderedCategories[idx].first].orEmpty() }
                val totalBytes = selectedRecords.sumOf { it.sizeBytes.coerceAtLeast(0L) }
                val sizeFmt = Formatter.formatFileSize(this@MainActivity, totalBytes)
                waSummary.text = getString(
                    R.string.cleanup_dialog_selected_summary,
                    selectedRecords.size,
                    mediaRecordsByCategory.values.sumOf { it.size },
                    sizeFmt,
                )
                waDeleteBtn?.text = if (selectedRecords.isNotEmpty() && totalBytes > 0L) {
                    "${getString(R.string.permanent_delete_button)} ($sizeFmt)"
                } else {
                    getString(R.string.permanent_delete_button)
                }
            }

            val waList = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            orderedCategories.forEachIndexed { idx, (cat, title) ->
                val list = mediaRecordsByCategory[cat].orEmpty()
                val bytes = list.sumOf { it.sizeBytes }
                val sizeFmt = Formatter.formatFileSize(this@MainActivity, bytes)
                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    background = roundedBackground(color(R.color.sweep_surface_alt), dp(12))
                }
                val box = CheckBox(this@MainActivity).apply {
                    isChecked = checked[idx]
                    minWidth = dp(48)
                    minHeight = dp(48)
                    setOnCheckedChangeListener { _, isChecked ->
                        checked[idx] = isChecked
                        refreshWaSummary()
                    }
                }
                row.addView(box, LinearLayout.LayoutParams(-2, -2))
                val meta = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
                meta.addView(label(title, 13.5f, R.color.sweep_text, true))
                meta.addView(
                    label(getString(R.string.wa_cat_item_count, title, list.size, sizeFmt), 11.5f, R.color.sweep_primary_glow, false),
                    top(2),
                )
                row.addView(meta, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
                row.setOnClickListener { box.isChecked = !box.isChecked }
                waList.addView(row, bottom(6))
            }
            val waMaxHeight = (resources.displayMetrics.heightPixels * 0.38f).toInt().coerceIn(dp(180), dp(340))
            waRoot.addView(
                ScrollView(this@MainActivity).apply { addView(waList, ViewGroup.LayoutParams(-1, -2)) },
                LinearLayout.LayoutParams(-1, waMaxHeight).apply { topMargin = dp(8) },
            )
            refreshWaSummary()

            fun chosenWaRecords(): List<MediaRecord> = orderedCategories.indices
                .filter { checked[it] }
                .flatMap { idx -> mediaRecordsByCategory[orderedCategories[idx].first].orEmpty() }

            val supportsTrash = trashCoordinator.supportsSystemTrash()
            waDeleteBtn = button(getString(R.string.permanent_delete_button)) {
                val chosenRecords = chosenWaRecords()
                if (chosenRecords.isEmpty()) {
                    Toast.makeText(this@MainActivity, R.string.wa_select_category_toast, Toast.LENGTH_SHORT).show()
                    return@button
                }
                waSheet.dismiss()
                viewModel.prepareExternalReviewSelection(chosenRecords)
                requestPermanentDeleteSelected(chosenRecords)
            }.apply {
                minHeight = dp(52)
                setTextColor(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_danger))
            }
            waRoot.addView(waDeleteBtn, top(12))
            refreshWaSummary()

            val waTrashBtn = button(
                if (supportsTrash) getString(R.string.move_to_trash_button)
                else getString(R.string.cleanup_dialog_open_files_tab),
            ) {
                val chosenRecords = chosenWaRecords()
                if (chosenRecords.isEmpty()) {
                    Toast.makeText(this@MainActivity, R.string.wa_select_category_toast, Toast.LENGTH_SHORT).show()
                    return@button
                }
                waSheet.dismiss()
                viewModel.prepareExternalReviewSelection(chosenRecords)
                if (supportsTrash) {
                    requestMoveSelectedToTrash(chosenRecords)
                } else {
                    switchTab(AppTab.FILES)
                }
            }.apply {
                minHeight = dp(48)
                setTextColor(color(R.color.sweep_text))
                backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
            }
            waRoot.addView(waTrashBtn, top(8))

            val waCancelBtn = button(getString(android.R.string.cancel)) {
                waSheet.dismiss()
            }.apply {
                minHeight = dp(48)
                setTextColor(color(R.color.sweep_muted))
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            }
            waRoot.addView(waCancelBtn, top(4))
            waSheet.show()
        }
    }

    private fun openBlurCleanerFlow() {
        if (!hasFullVisualAccess() && !limitedAccessAcknowledged) {
            pendingBlurScanAfterPermission = true
            if (hasAnyMediaPermission()) {
                showLimitedAccessPromptSheet {
                    pendingBlurScanAfterPermission = false
                    openBlurCleanerFlow()
                }
            } else {
                requestMediaPermissions(allowSettingsFallback = true)
            }
            return
        }
        blurScanJob?.cancel()
        val (progressSheet, progressRoot) = createAppleBottomSheet(
            title = getString(R.string.blur_scan_title),
            subtitle = getString(R.string.quick_clean_blur_desc),
        )
        val statusLabel = label(getString(R.string.blur_scan_progress, 0, 0), 13f, R.color.sweep_primary_glow, true)
        progressRoot.addView(statusLabel, top(12))
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            indeterminateTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        progressRoot.addView(bar, top(12))
        val cancelBtn = button(getString(android.R.string.cancel)) {
            blurScanJob?.cancel()
            progressSheet.dismiss()
        }.apply {
            minHeight = dp(48)
            setTextColor(color(R.color.sweep_muted))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        }
        progressRoot.addView(cancelBtn, top(12))
        progressSheet.setOnCancelListener { blurScanJob?.cancel() }
        progressSheet.show()

        blurScanJob = lifecycleScope.launch {
            val result = try {
                AndroidBlurPhotoScanner.scanRecentPhotos(
                    context = this@MainActivity,
                    onProgress = { analyzed, found ->
                        if (progressSheet.isShowing) {
                            statusLabel.text = getString(R.string.blur_scan_progress, analyzed, found)
                        }
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } finally {
                if (progressSheet.isShowing) progressSheet.dismiss()
            }
            if (result == null) return@launch
            if (result.candidates.isEmpty()) {
                if (!hasFullVisualAccess()) {
                    pendingBlurScanAfterPermission = true
                    showLimitedAccessPromptSheet {
                        pendingBlurScanAfterPermission = false
                        openBlurCleanerFlow()
                    }
                    return@launch
                }
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.blur_scan_empty, result.photosAnalyzed),
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            }
            updateInteractiveCleanupReviewAfterBlur(result.candidates.map { it.record }, !result.capReached)
        }
    }

    private fun updateInteractiveCleanupReviewAfterBlur(blurRecords: List<MediaRecord>, coverageComplete: Boolean) {
        val blurBytes = blurRecords.sumOf { it.sizeBytes.coerceAtLeast(0L) }
        updateHomeMissionCollection(
            mission = CleanMission.BLURRY_PHOTOS,
            itemCount = blurRecords.size,
            sizeLabel = Formatter.formatFileSize(this@MainActivity, blurBytes),
        )
        openInteractiveCleanupReviewDialog(
            records = blurRecords,
            isFallbackList = false,
            mission = null,
            coverageComplete = coverageComplete,
            customSubtitle = getString(R.string.blur_sheet_subtitle),
        )
    }

    private fun openMediaPreview(record: MediaRecord) {
        MediaFullscreenPreviewDialog.show(
            activity = this,
            record = record,
            onCompressVideoRequested = { videoRecord ->
                runWithProAccess { startHardwareVideoCompressionFlow(videoRecord) }
            },
        )
    }

    private fun openSimilarPhotosCleanerFlow() {
        if (!hasFullVisualAccess() && !limitedAccessAcknowledged) {
            pendingSimilarScanAfterPermission = true
            if (hasAnyMediaPermission()) {
                showLimitedAccessPromptSheet {
                    pendingSimilarScanAfterPermission = false
                    openSimilarPhotosCleanerFlow()
                }
            } else {
                requestMediaPermissions(allowSettingsFallback = true)
            }
            return
        }
        similarScanJob?.cancel()
        val (progressSheet, progressRoot) = createAppleBottomSheet(
            title = getString(R.string.similar_scan_title),
            subtitle = getString(R.string.similar_scan_subtitle),
        )
        val statusLabel = label(getString(R.string.similar_scan_progress, 0, 0), 13f, R.color.sweep_primary_glow, true)
        progressRoot.addView(statusLabel, top(12))
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            indeterminateTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        progressRoot.addView(bar, top(12))
        val cancelBtn = button(getString(android.R.string.cancel)) {
            similarScanJob?.cancel()
            progressSheet.dismiss()
        }.apply {
            minHeight = dp(48)
            setTextColor(color(R.color.sweep_muted))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        }
        progressRoot.addView(cancelBtn, top(12))
        progressSheet.setOnCancelListener { similarScanJob?.cancel() }
        progressSheet.show()

        similarScanJob = lifecycleScope.launch {
            val result = try {
                AndroidSimilarPhotoScanner.scanSimilarPhotos(
                    context = this@MainActivity,
                    onProgress = { inspected, cacheHits ->
                        if (progressSheet.isShowing) {
                            statusLabel.text = getString(R.string.similar_scan_progress, inspected, cacheHits)
                        }
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } finally {
                if (progressSheet.isShowing) progressSheet.dismiss()
            }
            if (result == null) return@launch
            if (result.groups.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.similar_scan_empty, result.photosInspected),
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            }
            val candidateCount = result.groups.sumOf { it.candidatesToReview.size }
            updateHomeMissionCollection(
                mission = CleanMission.SIMILAR_PHOTOS,
                itemCount = candidateCount,
                sizeLabel = Formatter.formatFileSize(this@MainActivity, result.totalRecoverableBytes),
            )
            openSimilarPhotosGroupReviewSheet(
                groups = result.groups,
                coverageComplete = !result.capReached,
            )
        }
    }

    private fun openSimilarPhotosGroupReviewSheet(
        groups: List<SimilarPhotoGroup>,
        coverageComplete: Boolean,
    ) {
        val cappedGroups = groups.take(60)
        val allCandidates = cappedGroups.flatMap { g -> g.candidatesToReview.map { it.record } }.take(200)
        if (allCandidates.isEmpty()) {
            showEmptyCleanupBottomSheet()
            return
        }
        val indexByUri = allCandidates.withIndex().associate { it.value.uri to it.index }
        val checked = BooleanArray(allCandidates.size) { coverageComplete }
        val boxByIndex = arrayOfNulls<CheckBox>(allCandidates.size)

        val (dialog, root) = createAppleBottomSheet(
            title = getString(R.string.similar_scan_title),
            subtitle = getString(R.string.similar_sheet_subtitle),
        )
        val summaryLabel = label("", 13f, R.color.sweep_primary_glow, true)
        root.addView(summaryLabel, top(10))
        var deleteActionBtn: MaterialButton? = null

        fun refreshSimilarSummary() {
            var count = 0
            var bytes = 0L
            for (i in allCandidates.indices) {
                if (checked[i]) {
                    count++
                    bytes += allCandidates[i].sizeBytes.coerceAtLeast(0L)
                }
            }
            val sizeFmt = Formatter.formatFileSize(this, bytes)
            summaryLabel.text = getString(R.string.cleanup_dialog_selected_summary, count, allCandidates.size, sizeFmt)
            deleteActionBtn?.text = if (count > 0 && bytes > 0L) {
                "${getString(R.string.permanent_delete_button)} ($sizeFmt)"
            } else {
                getString(R.string.permanent_delete_button)
            }
        }

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }

        cappedGroups.forEachIndexed { groupIdx, group ->
            val groupBytesFmt = Formatter.formatFileSize(this, group.recoverableBytes)
            listContainer.addView(
                label(
                    getString(R.string.similar_group_header, groupIdx + 1, group.totalPhotos, groupBytesFmt),
                    12f,
                    R.color.google_blue_primary,
                    true,
                ),
                top(if (groupIdx == 0) 2 else 10),
            )

            // 1. Best shot row: locked, protected, unchecked, green badge
            val bestRec = group.bestKeep.record
            val bestRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(8), dp(10), dp(8))
                background = borderedPill(Color.parseColor("#10291A"), Color.parseColor("#30D158"), dp(12), dp(1))
            }
            val lockedBox = CheckBox(this).apply {
                isChecked = false
                isEnabled = false
                minWidth = dp(48)
                minHeight = dp(48)
            }
            bestRow.addView(lockedBox, LinearLayout.LayoutParams(-2, -2))
            val bestThumb = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(color(R.color.sweep_background))
                tag = bestRec.uri
                isClickable = true
                isFocusable = true
                setOnClickListener { openMediaPreview(bestRec) }
            }
            loadThumbnail(bestThumb, bestRec)
            bestRow.addView(bestThumb, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(4); marginEnd = dp(8) })

            val bestMeta = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val bestBadge = TextView(this).apply {
                text = getString(R.string.similar_best_photo_badge)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#30D158"))
            }
            bestMeta.addView(bestBadge)
            bestMeta.addView(
                label(
                    bestRec.displayName?.takeIf(String::isNotBlank) ?: getString(R.string.unnamed_file),
                    12f,
                    R.color.sweep_text,
                    true,
                ).apply {
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                },
                top(2),
            )
            bestMeta.addView(
                label(Formatter.formatFileSize(this, bestRec.sizeBytes), 11f, R.color.sweep_muted, false),
                top(1),
            )
            bestRow.addView(bestMeta, LinearLayout.LayoutParams(0, -2, 1f))
            bestRow.setOnClickListener { openMediaPreview(bestRec) }
            listContainer.addView(bestRow, top(4))

            // 2. Reviewable candidate shots in this burst group
            for (candFp in group.candidatesToReview) {
                val rec = candFp.record
                val flatIdx = indexByUri[rec.uri] ?: continue
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(8), dp(6), dp(10), dp(6))
                    background = roundedBackground(color(R.color.sweep_surface_alt), dp(12))
                }
                val box = CheckBox(this).apply {
                    isChecked = checked[flatIdx]
                    minWidth = dp(48)
                    minHeight = dp(48)
                    setOnCheckedChangeListener { _, isChecked ->
                        checked[flatIdx] = isChecked
                        refreshSimilarSummary()
                    }
                }
                boxByIndex[flatIdx] = box
                row.addView(box, LinearLayout.LayoutParams(-2, -2))

                val thumb = ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(color(R.color.sweep_background))
                    tag = rec.uri
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { openMediaPreview(rec) }
                }
                loadThumbnail(thumb, rec)
                row.addView(thumb, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(4); marginEnd = dp(8) })

                val meta = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                meta.addView(
                    label(
                        rec.displayName?.takeIf(String::isNotBlank) ?: getString(R.string.unnamed_file),
                        12f,
                        R.color.sweep_text,
                        true,
                    ).apply {
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                    },
                )
                meta.addView(
                    label(Formatter.formatFileSize(this, rec.sizeBytes), 11f, R.color.sweep_primary_glow, false),
                    top(2),
                )
                row.addView(meta, LinearLayout.LayoutParams(0, -2, 1f))
                row.setOnClickListener { box.isChecked = !box.isChecked }
                listContainer.addView(row, top(4))
            }
        }

        val maxListHeight = (resources.displayMetrics.heightPixels * 0.44f).toInt().coerceIn(dp(220), dp(380))
        root.addView(
            ScrollView(this).apply { addView(listContainer, ViewGroup.LayoutParams(-1, -2)) },
            LinearLayout.LayoutParams(-1, maxListHeight).apply { topMargin = dp(8) },
        )

        fun chosenSimilarRecords(): List<MediaRecord> =
            allCandidates.indices.filter { checked[it] }.map { allCandidates[it] }

        val supportsTrash = trashCoordinator.supportsSystemTrash()
        deleteActionBtn = button(getString(R.string.permanent_delete_button)) {
            val chosen = chosenSimilarRecords()
            if (chosen.isEmpty()) {
                Toast.makeText(this, R.string.cleanup_dialog_select_none_toast, Toast.LENGTH_SHORT).show()
                return@button
            }
            dialog.dismiss()
            viewModel.prepareExternalReviewSelection(chosen)
            requestPermanentDeleteSelected(chosen)
        }.apply {
            minHeight = dp(52)
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_danger))
        }
        root.addView(deleteActionBtn, top(12))
        refreshSimilarSummary()

        if (supportsTrash) {
            val trashBtn = button(getString(R.string.move_to_trash_button)) {
                val chosen = chosenSimilarRecords()
                if (chosen.isEmpty()) {
                    Toast.makeText(this, R.string.cleanup_dialog_select_none_toast, Toast.LENGTH_SHORT).show()
                    return@button
                }
                dialog.dismiss()
                viewModel.prepareExternalReviewSelection(chosen)
                requestMoveSelectedToTrash(chosen)
            }.apply {
                minHeight = dp(48)
                setTextColor(color(R.color.sweep_text))
                backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
            }
            root.addView(trashBtn, top(8))
        }

        val closeBtn = button(getString(android.R.string.cancel)) {
            dialog.dismiss()
        }.apply {
            minHeight = dp(46)
            setTextColor(color(R.color.sweep_muted))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        }
        root.addView(closeBtn, top(4))
        dialog.show()
    }

    private fun openVideoCompressorCandidatesSheet() {
        if (!hasFullVisualAccess() && !limitedAccessAcknowledged) {
            pendingVideoCompressAfterPermission = true
            if (hasAnyMediaPermission()) {
                showLimitedAccessPromptSheet {
                    pendingVideoCompressAfterPermission = false
                    openVideoCompressorCandidatesSheet()
                }
            } else {
                requestMediaPermissions(allowSettingsFallback = true)
            }
            return
        }
        lifecycleScope.launch {
            val heavyVideos = withContext(Dispatchers.IO) { queryHeavyVideosForCompression() }
            if (heavyVideos.isEmpty()) {
                Toast.makeText(this@MainActivity, R.string.video_compress_empty, Toast.LENGTH_LONG).show()
                return@launch
            }
            val (sheet, root) = createAppleBottomSheet(
                title = getString(R.string.video_compress_sheet_title),
                subtitle = getString(R.string.video_compress_sheet_subtitle),
            )
            val listContainer = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            for (video in heavyVideos) {
                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    background = roundedBackground(color(R.color.sweep_surface_alt), dp(12))
                }
                val thumb = ImageView(this@MainActivity).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(color(R.color.sweep_background))
                    tag = video.uri
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { openMediaPreview(video) }
                }
                loadThumbnail(thumb, video)
                row.addView(thumb, LinearLayout.LayoutParams(dp(50), dp(50)).apply { marginEnd = dp(10) })

                val meta = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
                meta.addView(
                    label(
                        video.displayName?.takeIf(String::isNotBlank) ?: getString(R.string.unnamed_file),
                        12.5f,
                        R.color.sweep_text,
                        true,
                    ).apply {
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                    },
                )
                meta.addView(
                    label(Formatter.formatFileSize(this@MainActivity, video.sizeBytes), 11f, R.color.sweep_primary_glow, true),
                    top(2),
                )
                row.addView(meta, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })

                val compressBtn = button(getString(R.string.video_compress_action_btn)) {
                    sheet.dismiss()
                    startHardwareVideoCompressionFlow(video)
                }.apply {
                    minHeight = dp(42)
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setTextColor(Color.WHITE)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
                }
                row.addView(compressBtn, LinearLayout.LayoutParams(-2, -2))
                listContainer.addView(row, bottom(6))
            }

            val maxHeight = (resources.displayMetrics.heightPixels * 0.45f).toInt().coerceIn(dp(220), dp(380))
            root.addView(
                ScrollView(this@MainActivity).apply { addView(listContainer, ViewGroup.LayoutParams(-1, -2)) },
                LinearLayout.LayoutParams(-1, maxHeight).apply { topMargin = dp(8) },
            )
            val closeBtn = button(getString(android.R.string.cancel)) {
                sheet.dismiss()
            }.apply {
                minHeight = dp(46)
                setTextColor(color(R.color.sweep_muted))
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            }
            root.addView(closeBtn, top(6))
            sheet.show()
        }
    }

    private fun queryHeavyVideosForCompression(limit: Int = 50): List<MediaRecord> {
        val volumes = runCatching { android.provider.MediaStore.getExternalVolumeNames(this) }
            .getOrElse { setOf(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY) }
        val projection = arrayOf(
            android.provider.BaseColumns._ID,
            android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
            android.provider.MediaStore.MediaColumns.SIZE,
            android.provider.MediaStore.MediaColumns.MIME_TYPE,
            android.provider.MediaStore.MediaColumns.DATE_MODIFIED,
            android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
            android.provider.MediaStore.MediaColumns.IS_PENDING,
        ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(android.provider.MediaStore.MediaColumns.IS_TRASHED)
        } else {
            emptyArray()
        }

        val found = ArrayList<MediaRecord>(limit)
        for (vol in volumes) {
            val collection = android.provider.MediaStore.Video.Media.getContentUri(vol)
            val selection = "${android.provider.MediaStore.MediaColumns.SIZE} >= ?"
            val selectionArgs = arrayOf(VideoCompressionPlanner.MIN_COMPRESSIBLE_VIDEO_BYTES.toString())
            val sortOrder = "${android.provider.MediaStore.MediaColumns.SIZE} DESC"
            val cursor = runCatching {
                contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            }.getOrNull() ?: continue

            cursor.use { c ->
                val idCol = c.getColumnIndex(android.provider.BaseColumns._ID)
                val nameCol = c.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndex(android.provider.MediaStore.MediaColumns.SIZE)
                val mimeCol = c.getColumnIndex(android.provider.MediaStore.MediaColumns.MIME_TYPE)
                val modCol = c.getColumnIndex(android.provider.MediaStore.MediaColumns.DATE_MODIFIED)
                val relCol = c.getColumnIndex(android.provider.MediaStore.MediaColumns.RELATIVE_PATH)
                val pendCol = c.getColumnIndex(android.provider.MediaStore.MediaColumns.IS_PENDING)
                val trashCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    c.getColumnIndex(android.provider.MediaStore.MediaColumns.IS_TRASHED)
                } else -1
                if (idCol < 0 || sizeCol < 0 || mimeCol < 0) return@use

                while (c.moveToNext() && found.size < limit) {
                    val pending = pendCol >= 0 && !c.isNull(pendCol) && c.getInt(pendCol) != 0
                    val trashed = trashCol >= 0 && !c.isNull(trashCol) && c.getInt(trashCol) != 0
                    val name = if (nameCol >= 0 && !c.isNull(nameCol)) c.getString(nameCol) else null
                    if (pending || trashed || name?.endsWith("_compact.mp4", ignoreCase = true) == true) continue
                    val id = c.getLong(idCol)
                    val size = c.getLong(sizeCol)
                    val mime = c.getString(mimeCol) ?: continue
                    if (id <= 0L || size < VideoCompressionPlanner.MIN_COMPRESSIBLE_VIDEO_BYTES) continue
                    val uri = android.content.ContentUris.withAppendedId(collection, id).toString()
                    found += MediaRecord(
                        uri = uri,
                        volumeName = vol,
                        kind = MediaKind.VIDEO,
                        displayName = name,
                        mimeType = mime,
                        sizeBytes = size,
                        dateModifiedEpochSeconds = if (modCol >= 0 && !c.isNull(modCol)) c.getLong(modCol) else null,
                        relativePath = if (relCol >= 0 && !c.isNull(relCol)) c.getString(relCol) else null,
                    )
                }
            }
        }
        return found.sortedByDescending { it.sizeBytes }.take(limit)
    }

    private fun startHardwareVideoCompressionFlow(video: MediaRecord) {
        videoCompressJob?.cancel()
        val displayName = video.displayName?.takeIf(String::isNotBlank) ?: getString(R.string.unnamed_file)
        val (progressSheet, progressRoot) = createAppleBottomSheet(
            title = getString(R.string.video_compress_sheet_title),
            subtitle = displayName,
        )
        val pctLabel = label(getString(R.string.video_compress_running_status, 0), 13.5f, R.color.sweep_primary_glow, true)
        progressRoot.addView(pctLabel, top(12))
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        progressRoot.addView(bar, top(10))
        val cancelBtn = button(getString(android.R.string.cancel)) {
            videoCompressJob?.cancel()
            progressSheet.dismiss()
        }.apply {
            minHeight = dp(48)
            setTextColor(color(R.color.sweep_muted))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        }
        progressRoot.addView(cancelBtn, top(12))
        progressSheet.setOnCancelListener { videoCompressJob?.cancel() }
        progressSheet.show()

        videoCompressJob = lifecycleScope.launch {
            val outcome = try {
                AndroidHardwareVideoCompressor.compressToMediaStore(
                    context = this@MainActivity,
                    source = video,
                    onProgressPercent = { pct ->
                        if (progressSheet.isShowing) {
                            bar.progress = pct
                            pctLabel.text = getString(R.string.video_compress_running_status, pct)
                        }
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                VideoCompressionOutcome.Failed("unexpected_error")
            } finally {
                if (progressSheet.isShowing) progressSheet.dismiss()
            }

            when (outcome) {
                is VideoCompressionOutcome.Success -> {
                    refreshStorageStats()
                    viewModel.startSession()
                    val origFmt = Formatter.formatFileSize(this@MainActivity, outcome.originalBytes)
                    val compFmt = Formatter.formatFileSize(this@MainActivity, outcome.compressedBytes)
                    val savedFmt = Formatter.formatFileSize(this@MainActivity, outcome.savedBytes)
                    val (doneSheet, doneRoot) = createAppleBottomSheet(
                        title = getString(R.string.video_compress_done_title),
                        subtitle = getString(R.string.video_compress_done_body, origFmt, compFmt, savedFmt),
                    )
                    val supportsTrash = trashCoordinator.supportsSystemTrash()
                    val moveOrigBtn = button(
                        if (supportsTrash) getString(R.string.move_to_trash_button)
                        else getString(R.string.permanent_delete_button),
                    ) {
                        doneSheet.dismiss()
                        viewModel.prepareExternalReviewSelection(listOf(video))
                        if (supportsTrash) {
                            requestMoveSelectedToTrash(listOf(video))
                        } else {
                            requestPermanentDeleteSelected(listOf(video))
                        }
                    }.apply {
                        minHeight = dp(52)
                        setTextColor(Color.WHITE)
                        backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
                    }
                    doneRoot.addView(moveOrigBtn, top(14))

                    val keepBothBtn = button(getString(R.string.post_cleanup_relief_later)) {
                        doneSheet.dismiss()
                    }.apply {
                        minHeight = dp(46)
                        setTextColor(color(R.color.sweep_muted))
                        backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                    }
                    doneRoot.addView(keepBothBtn, top(4))
                    doneSheet.show()
                }

                is VideoCompressionOutcome.IneligibleOrAlreadyCompact -> {
                    Toast.makeText(this@MainActivity, R.string.video_compress_already_compact, Toast.LENGTH_LONG).show()
                }

                is VideoCompressionOutcome.Failed -> {
                    Toast.makeText(this@MainActivity, R.string.video_compress_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleIncomingSendIntent(@Suppress("UNUSED_PARAMETER") incomingIntent: Intent?) {
        // ACTION_SEND / ACTION_SEND_MULTIPLE removidos do manifesto e neutralizados aqui
        // para jamais expor downloader externo ou estúdio desativado.
        return
    }

    @Suppress("DEPRECATION")
    private fun hasUsageStatsAccess(): Boolean = runCatching {
        getSystemService(AppOpsManager::class.java).checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    private fun loadUnusedAppSuggestions() {
        Toast.makeText(this, R.string.pro_unused_apps_searching, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val suggestions = withContext(Dispatchers.IO) { queryUnusedApps() }
            if (suggestions.isEmpty()) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.pro_unused_apps_title)
                    .setMessage(R.string.pro_unused_apps_empty)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return@launch
            }
            val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
            val labels = suggestions.map { app ->
                val usage = if (app.lastUsedMillis > 0L) {
                    getString(R.string.pro_unused_apps_last_used, dateFormat.format(Date(app.lastUsedMillis)))
                } else {
                    getString(R.string.pro_unused_apps_never_used)
                }
                "${app.displayName} · $usage"
            }.toTypedArray()
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.pro_unused_apps_title)
                .setItems(labels) { _, index -> requestUninstallUnusedApp(suggestions[index]) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    @Suppress("DEPRECATION")
    private fun queryUnusedApps(): List<UnusedAppInfo> {
        val endTime = System.currentTimeMillis()
        val ninetyDaysMillis = 90L * 24L * 60L * 60L * 1000L
        val cutoff = (endTime - ninetyDaysMillis).coerceAtLeast(0L)
        val usage = getSystemService(UsageStatsManager::class.java)
            .queryAndAggregateUsageStats(cutoff, endTime)
        val launchableApps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0,
        )
        val suggestionsByPackage = linkedMapOf<String, UnusedAppInfo>()
        for (resolveInfo in launchableApps) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val appInfo = activityInfo.applicationInfo ?: continue
            val appPackage = activityInfo.packageName
            if (appPackage == packageName || appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) continue
            val lastUsed = usage[appPackage]?.lastTimeUsed ?: 0L
            if (lastUsed > 0L && lastUsed >= cutoff) continue
            val firstInstalled = runCatching {
                packageManager.getPackageInfo(appPackage, 0).firstInstallTime
            }.getOrDefault(endTime)
            if (firstInstalled >= cutoff) continue
            val displayName = runCatching {
                resolveInfo.loadLabel(packageManager).toString()
            }.getOrDefault(appPackage)
            suggestionsByPackage.putIfAbsent(
                appPackage,
                UnusedAppInfo(appPackage, displayName, lastUsed),
            )
        }
        return suggestionsByPackage.values
            .sortedWith(compareBy<UnusedAppInfo> { if (it.lastUsedMillis == 0L) 0L else it.lastUsedMillis }
                .thenBy { it.displayName.lowercase() })
            .take(50)
    }

    private fun requestUninstallUnusedApp(app: UnusedAppInfo) {
        val uninstallIntent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
        runCatching { startActivity(uninstallIntent) }
            .onFailure { Toast.makeText(this, R.string.pro_unused_apps_permission_denied, Toast.LENGTH_LONG).show() }
    }

    private fun openAppDetailsSettings() {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        runCatching { startActivity(intent) }
    }

    private fun requestMediaPermissions(allowSettingsFallback: Boolean = true) {
        val requested = MediaPermissionPolicy.permissionsToRequest(
            apiLevel = Build.VERSION.SDK_INT,
            kinds = setOf(MediaKind.IMAGE, MediaKind.VIDEO, MediaKind.AUDIO),
        )
        val missing = requested.filter { !isGranted(it) }
        val prefs = getSharedPreferences("midiastore_prefs", MODE_PRIVATE)
        val alreadyRequestedBefore = prefs.getBoolean("media_perm_requested_once", false)
        // On Android 14+ with "Select photos" active, re-requesting READ_MEDIA_IMAGES + READ_MEDIA_VIDEO +
        // READ_MEDIA_VISUAL_USER_SELECTED opens the system's 3-option upgrade sheet ("Allow all" / "Select more")
        // unless the user/OS has marked the permission USER_FIXED — handled by the <500ms detector in permissionLauncher.
        val hasPartialVisualGrant = Build.VERSION.SDK_INT >= 34 &&
            isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
            !hasFullVisualAccess()
        val permanentlyDenied = missing.isNotEmpty() &&
            !hasPartialVisualGrant &&
            alreadyRequestedBefore &&
            missing.none { shouldShowRequestPermissionRationale(it) }

        if (allowSettingsFallback && permanentlyDenied) {
            awaitingPermissionSettingsReturn = true
            Toast.makeText(this, R.string.limited_access_settings_hint, Toast.LENGTH_LONG).show()
            openAppDetailsSettings()
            return
        }
        prefs.edit().putBoolean("media_perm_requested_once", true).apply()
        permissionRequestStartedAtMs = SystemClock.elapsedRealtime()
        permissionRequestHadFullAccess = hasFullVisualAccess()
        permissionRequestHadAnyAccess = hasAnyMediaPermission()
        permissionLauncher.launch(requested.toTypedArray())
    }

    private fun hasAnyMediaPermission(): Boolean = when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 ->
            isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        else -> isGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
            isGranted(Manifest.permission.READ_MEDIA_VIDEO) ||
            isGranted(Manifest.permission.READ_MEDIA_AUDIO) ||
            (Build.VERSION.SDK_INT >= 34 && isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasDeniedMediaKind(): Boolean {
        val granted = buildSet {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                MediaPermissionPolicy.READ_MEDIA_IMAGES,
                MediaPermissionPolicy.READ_MEDIA_VIDEO,
                MediaPermissionPolicy.READ_MEDIA_AUDIO,
                MediaPermissionPolicy.READ_MEDIA_VISUAL_USER_SELECTED,
            ).filterTo(this) { isGranted(it) }
        }
        return MediaKind.entries.any { kind ->
            MediaPermissionPolicy.accessLevel(Build.VERSION.SDK_INT, kind, granted) == AccessLevel.DENIED
        }
    }

    private fun renderState(state: MediaScanUiState) {
        progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        cancelButton.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        scanButton.isEnabled = !state.isLoading
        permissionButton.isEnabled = !state.isLoading
        val canChangePartialVisualAccess = Build.VERSION.SDK_INT >= 34 &&
            isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
            (!isGranted(MediaPermissionPolicy.READ_MEDIA_IMAGES) ||
                !isGranted(MediaPermissionPolicy.READ_MEDIA_VIDEO))
        val shouldReviewPermissions = state.shouldShowReviewAccessAction &&
            (hasDeniedMediaKind() || canChangePartialVisualAccess)
        permissionButton.visibility = if (shouldReviewPermissions) View.VISIBLE else View.GONE
        filesPermissionButton.visibility = if (shouldReviewPermissions) View.VISIBLE else View.GONE
        filesPermissionButton.isEnabled = !state.isLoading
        photosButton.isEnabled = !state.isLoading && isKindAccessible(state, MediaKind.IMAGE)
        videosButton.isEnabled = !state.isLoading && isKindAccessible(state, MediaKind.VIDEO)
        audioButton.isEnabled = !state.isLoading && isKindAccessible(state, MediaKind.AUDIO)
        filterAllButton.isEnabled = !state.isLoading
        filterLargeButton.isEnabled = !state.isLoading
        oaasMissionLargeButton.isEnabled = !state.isLoading
        oaasMissionDuplicatesButton.isEnabled = !state.isLoading
        oaasMissionScreenshotsButton.isEnabled = !state.isLoading
        oaasMissionOldButton.isEnabled = !state.isLoading
        oaasCancelMissionButton.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        oaasCancelMissionButton.isEnabled = state.isLoading
        filterDuplicatesButton.isEnabled = !state.isLoading
        filterScreenshotsButton.isEnabled = !state.isLoading
        filterOldButton.isEnabled = !state.isLoading
        updateFilesControlsVisuals(state)
        previousButton.isEnabled = state.canLoadPrevious
        nextButton.isEnabled = state.canLoadNext

        val defaultStatus = when (state.status) {
            ScanUiStatus.IDLE -> getString(R.string.ready_status)
            ScanUiStatus.STARTING -> getString(R.string.starting_scan)
            ScanUiStatus.LOADING_PAGE -> getString(R.string.loading_page)
            ScanUiStatus.READY -> getString(R.string.page_ready)
            ScanUiStatus.PARTIAL -> getString(R.string.partial_results)
            ScanUiStatus.FAILED -> getString(R.string.page_failed)
            ScanUiStatus.PAGING_UNSUPPORTED -> getString(R.string.pagination_unsupported)
            ScanUiStatus.NO_ACCESS -> getString(R.string.no_access)
            ScanUiStatus.SESSION_STALE -> getString(R.string.session_stale)
            ScanUiStatus.CANCELLED -> getString(R.string.scan_cancelled_readonly)
        }
        statusText.text = when (state.message) {
            "selection_limit_reached" -> getString(R.string.selection_limit_reached)
            "item_not_selectable" -> getString(R.string.item_not_selectable)
            "session_source_changed" -> getString(R.string.session_stale)
            "page_content_changed" -> getString(R.string.page_content_changed)
            "partial_access_requires_refresh" -> getString(R.string.partial_access_requires_refresh)
            "revision_unverifiable" -> getString(R.string.warn_revision_unverifiable)
            "paging_args_unverified" -> getString(R.string.warn_paging_args_unverified)
            else -> if (state.status == ScanUiStatus.PARTIAL) {
                val details = state.currentPage?.issues.orEmpty()
                    .groupBy { (it.kind ?: state.selectedKind) to it.code }
                    .mapNotNull { (key, grouped) ->
                        val (kind, code) = key
                        val base = when (code) {
                            com.rdf112.sweep.domain.model.PageIssueCode.REVISION_UNVERIFIABLE -> getString(R.string.warn_revision_unverifiable)
                            com.rdf112.sweep.domain.model.PageIssueCode.PAGING_ARGS_UNVERIFIED -> getString(R.string.warn_paging_args_unverified)
                            com.rdf112.sweep.domain.model.PageIssueCode.PAGING_UNSUPPORTED -> getString(R.string.pagination_unsupported)
                            com.rdf112.sweep.domain.model.PageIssueCode.PARTIAL_ACCESS -> getString(R.string.warn_partial_access)
                            com.rdf112.sweep.domain.model.PageIssueCode.INVALID_ROW -> getString(R.string.warn_invalid_rows)
                            else -> null
                        } ?: return@mapNotNull null
                        val kindLabel = getString(when (kind) {
                            MediaKind.IMAGE -> R.string.photos_tab
                            MediaKind.VIDEO -> R.string.videos_tab
                            MediaKind.AUDIO -> R.string.audio_tab
                        })
                        val volumeCount = grouped.mapNotNull { it.volumeName }.distinct().size.coerceAtLeast(1)
                        getString(R.string.issue_summary_line, kindLabel, base, grouped.size, volumeCount)
                    }
                (listOf(defaultStatus) + details).joinToString("\n")
            } else state.message ?: defaultStatus
        }

        val pendingKind = pendingKindAfterPermission
        if (pendingKind != null && !state.isLoading && state.session != null) {
            pendingKindAfterPermission = null
            if (isKindAccessible(state, pendingKind)) viewModel.selectKind(pendingKind)
        }

        updateVolumeSpinner(state)
        updatePage(state)
        updateReview(state)
        updateVisibleCheckboxes(state)
        resumePendingOutcomeMission(state)
        renderMissionProgress(state.missionProgress)
        if (
            !autoInitialCleanupScanLaunched &&
            hasFullVisualAccess() &&
            !state.isLoading &&
            state.session != null &&
            state.missionProgress == null &&
            pendingOutcomeMission == null
        ) {
            autoInitialCleanupScanLaunched = true
            openInteractiveDialogOnMissionFinish = false
            viewModel.executeGlobalOutcomeMission(DesiredOutcomeMission.FREE_MAX_SPACE_NOW)
        }
    }

    private fun resumePendingOutcomeMission(state: MediaScanUiState) {
        val mission = pendingOutcomeMission ?: return
        if (state.isLoading) return

        if (state.session != null && state.status in setOf(ScanUiStatus.READY, ScanUiStatus.PARTIAL)) {
            pendingOutcomeMission = null
            runOutcomeMission(mission)
            return
        }

        if (state.status in setOf(
                ScanUiStatus.SESSION_STALE,
                ScanUiStatus.CANCELLED,
                ScanUiStatus.IDLE,
            ) && hasAnyMediaPermission()
        ) {
            pendingOutcomeMission = null
            runOutcomeMission(mission)
            return
        }

        if (state.status in setOf(
                ScanUiStatus.FAILED,
                ScanUiStatus.PAGING_UNSUPPORTED,
                ScanUiStatus.NO_ACCESS,
            )
        ) {
            pendingOutcomeMission = null
            oaasPipelineStatus.text = getString(R.string.oaas_mission_scan_failed)
            if (mission == DesiredOutcomeMission.FREE_MAX_SPACE_NOW) {
                heroOrbButton.isEnabled = true
                heroOrbButton.text = getString(R.string.home_cleanup_primary)
                heroOrbSubtitle.text = getString(R.string.home_cleanup_subtitle)
                showHomeCleanupMessage(getString(R.string.home_cleanup_scan_failed))
            }
        }
    }

    private fun isKindAccessible(state: MediaScanUiState, kind: MediaKind): Boolean {
        val level = state.session?.accessByKind?.get(kind)
        return level == AccessLevel.FULL || level == AccessLevel.PARTIAL
    }

    private fun updateVolumeSpinner(state: MediaScanUiState) {
        val volumes = state.availableVolumes
        if (volumes != renderedVolumes) {
            updatingVolumeSpinner = true
            renderedVolumes = volumes
            val labels = volumes.map(::displayVolumeName)
            volumeSpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                labels,
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            val index = volumes.indexOf(state.selectedVolume).coerceAtLeast(0)
            if (volumes.isNotEmpty()) volumeSpinner.setSelection(index, false)
            updatingVolumeSpinner = false
        } else if (volumes.isNotEmpty()) {
            val index = volumes.indexOf(state.selectedVolume)
            if (index >= 0 && volumeSpinner.selectedItemPosition != index) {
                updatingVolumeSpinner = true
                volumeSpinner.setSelection(index, false)
                updatingVolumeSpinner = false
            }
        }
        volumeSpinner.isEnabled = !state.isLoading && volumes.size > 1
    }

    private fun displayVolumeName(name: String): String =
        if (name == "external_primary") getString(R.string.internal_volume) else getString(R.string.external_volume, name)

    private fun updatePage(state: MediaScanUiState) {
        val page = state.currentPage
        if (page == null) {
            lastFilteredPageRecords = emptyList()
            if (::selectAllPageButton.isInitialized) {
                selectAllPageButton.visibility = View.GONE
            }
            pageLabel.text = getString(R.string.not_scanned)
            resultSummary.text = getString(R.string.page_results_hint)
            if (renderedPageKey != null) {
                thumbnailJobs.forEach { it.cancel() }
                thumbnailJobs.clear()
                recordsContainer.removeAllViews()
                checkBoxesByUri.clear()
                renderedPageKey = null
            }
            return
        }

        val filteredRecords = SmartMediaGroupingPolicy.applyFilter(
            records = page.records,
            mode = state.smartFilter,
            nowEpochSeconds = System.currentTimeMillis() / 1000L,
        )
        lastFilteredPageRecords = filteredRecords
        if (::selectAllPageButton.isInitialized) {
            val selectablePageRecords = filteredRecords.filter { it.isSelectable && !it.isProtected }
            selectAllPageButton.visibility = if (selectablePageRecords.isNotEmpty()) View.VISIBLE else View.GONE
            if (selectablePageRecords.isNotEmpty()) {
                selectAllPageButton.text = getString(R.string.select_all_page_button, selectablePageRecords.size)
            }
        }

        val start = if (page.rowsRead == 0) 0 else page.request.offset + 1
        val end = page.request.offset + page.rowsRead
        val total = page.providerTotalCount?.let { getString(R.string.of_total, it) }.orEmpty()
        pageLabel.text = getString(R.string.page_range, start, end, total)
        resultSummary.text = when {
            page.outcome == com.rdf112.sweep.domain.model.MediaPageOutcome.PAGING_UNSUPPORTED ->
                getString(R.string.pagination_unsupported)
            page.outcome == com.rdf112.sweep.domain.model.MediaPageOutcome.FAILED ->
                getString(R.string.page_failed)
            page.outcome == com.rdf112.sweep.domain.model.MediaPageOutcome.SESSION_STALE ->
                getString(R.string.session_stale)
            page.outcome == com.rdf112.sweep.domain.model.MediaPageOutcome.CANCELLED ->
                getString(R.string.scan_cancelled_readonly)
            filteredRecords.isEmpty() && page.records.isNotEmpty() ->
                getString(R.string.smart_filter_empty_in_page)
            page.records.isEmpty() && page.issues.isNotEmpty() ->
                getString(R.string.page_with_warnings, page.issues.size)
            page.records.isEmpty() && page.rowsRead == 0 -> getString(R.string.empty_page_not_device)
            page.records.isEmpty() -> getString(R.string.page_no_visible_rows)
            page.issues.isNotEmpty() -> getString(R.string.page_items_count, filteredRecords.size)
            else -> getString(R.string.page_items_count, filteredRecords.size)
        }

        val currentKey = "${page.request.sessionId}:${page.request.kind}:${page.request.volumeName}:${page.request.offset}:${state.smartFilter}"
        if (renderedPageKey != currentKey) {
            thumbnailJobs.forEach { it.cancel() }
            thumbnailJobs.clear()
            recordsContainer.removeAllViews()
            checkBoxesByUri.clear()
            renderedPageKey = currentKey
            filteredRecords.forEachIndexed { index, record ->
                recordsContainer.addView(createRecordRow(record, index))
            }
        }
    }

    private fun createRecordRow(record: MediaRecord, index: Int): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = roundedBackground(color(R.color.sweep_surface_alt), dp(14))
        }
        val thumbnail = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(color(R.color.sweep_background))
            contentDescription = getString(R.string.thumbnail_description, index + 1)
            tag = record.uri
            isClickable = true
            isFocusable = true
            setOnClickListener { openMediaPreview(record) }
        }
        row.addView(thumbnail, LinearLayout.LayoutParams(dp(60), dp(60)).apply { marginEnd = dp(12) })
        if (record.kind == MediaKind.AUDIO) {
            thumbnail.setImageResource(android.R.drawable.ic_media_play)
        } else {
            loadThumbnail(thumbnail, record)
        }

        val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val baseName = record.displayName?.takeIf(String::isNotBlank) ?: getString(R.string.unnamed_file)
        val name = if (record.isProtected) "$baseName (${getString(R.string.protected_reference_badge)})" else baseName
        details.addView(label(name, 13f, R.color.sweep_text, true))
        details.addView(label(
            "${android.text.format.Formatter.formatFileSize(this@MainActivity, record.sizeBytes)} - ${getString(when (record.kind) {
                MediaKind.IMAGE -> R.string.photo_label
                MediaKind.VIDEO -> R.string.video_label
                MediaKind.AUDIO -> R.string.audio_label
            })}",
            11f,
            R.color.sweep_primary_glow,
            true,
        ), top(3))
        val date = record.dateModifiedEpochSeconds?.let {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it * 1000L))
        } ?: getString(R.string.date_unavailable)
        val shaBadge = if (record.isProtected || record.duplicateReferenceSnapshot != null) {
            getString(R.string.exact_copy_badge)
        } else {
            ""
        }
        details.addView(label(getString(R.string.media_metadata_line, displayVolumeName(record.volumeName), date, shaBadge), 10f, R.color.sweep_muted, false), top(2))
        row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))

        val check = CheckBox(this).apply {
            isChecked = viewModel.state.value.selection.isSelected(record.uri)
            contentDescription = getString(R.string.select_for_review, name)
            isEnabled = record.isSelectable && !record.isProtected
            minWidth = dp(48)
            minHeight = dp(48)
            setOnCheckedChangeListener { view, checked ->
                try { view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                viewModel.setSelected(record, checked)
            }
        }
        checkBoxesByUri[record.uri] = check
        row.addView(check, LinearLayout.LayoutParams(dp(48), dp(48)))

        val wrapper = FrameLayout(this).apply {
            setPadding(0, 0, 0, dp(8))
            addView(row, ViewGroup.LayoutParams(-1, -2))
        }
        return wrapper
    }

    private fun updateVisibleCheckboxes(state: MediaScanUiState) {
        val visibleRecords = state.currentPage?.records?.associateBy { it.uri }.orEmpty()
        checkBoxesByUri.forEach { (uri, checkBox) ->
            val currentRecord = visibleRecords[uri]
            checkBox.isEnabled = !state.isLoading && currentRecord != null &&
                currentRecord.isSelectable && !currentRecord.isProtected
            val shouldBeChecked = state.selection.isSelected(uri)
            if (checkBox.isChecked != shouldBeChecked) {
                checkBox.setOnCheckedChangeListener(null)
                checkBox.isChecked = shouldBeChecked
                val record = state.currentPage?.records?.firstOrNull { it.uri == uri }
                checkBox.setOnCheckedChangeListener { _, checked ->
                    if (record != null) viewModel.setSelected(record, checked)
                }
            }
        }
    }

    private fun loadThumbnail(target: ImageView, record: MediaRecord) {
        val cached = thumbnailCache.get(record.uri)
        if (cached != null && !cached.isRecycled) {
            target.setImageBitmap(cached)
            return
        }
        val signal = CancellationSignal()
        val job = lifecycleScope.launch {
            val bitmap: Bitmap? = withContext(thumbnailDispatcher) {
                try {
                    contentResolver.loadThumbnail(
                        Uri.parse(record.uri),
                        Size(dp(96), dp(96)),
                        signal,
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                thumbnailCache.put(record.uri, bitmap)
            }
            if (target.tag == record.uri && bitmap != null) target.setImageBitmap(bitmap)
            else if (target.tag == record.uri) target.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        job.invokeOnCompletion { cause ->
            if (cause is CancellationException) signal.cancel()
        }
        thumbnailJobs += job
    }

    private fun updateFilesControlsVisuals(state: MediaScanUiState) {
        val selectedColor = color(R.color.sweep_surface_alt)
        val activeText = color(R.color.sweep_text)
        val mutedText = color(R.color.sweep_muted)
        val blueFill = color(R.color.google_blue_primary)

        fun styleSegment(button: MaterialButton, selected: Boolean) {
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (selected) selectedColor else Color.TRANSPARENT,
            )
            button.setTextColor(if (selected) activeText else mutedText)
            button.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        fun styleFilterPill(button: MaterialButton, selected: Boolean) {
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (selected) blueFill else selectedColor,
            )
            button.setTextColor(if (selected) Color.WHITE else mutedText)
            button.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        if (::photosButton.isInitialized) styleSegment(photosButton, state.selectedKind == MediaKind.IMAGE)
        if (::videosButton.isInitialized) styleSegment(videosButton, state.selectedKind == MediaKind.VIDEO)
        if (::audioButton.isInitialized) styleSegment(audioButton, state.selectedKind == MediaKind.AUDIO)

        if (::filterAllButton.isInitialized) styleFilterPill(filterAllButton, state.smartFilter == SmartFilterMode.ALL_ITEMS)
        if (::filterLargeButton.isInitialized) styleFilterPill(filterLargeButton, state.smartFilter == SmartFilterMode.LARGE_FILES_10MB)
        if (::filterDuplicatesButton.isInitialized) styleFilterPill(filterDuplicatesButton, state.smartFilter == SmartFilterMode.SAME_SIZE_CANDIDATES)
        if (::filterScreenshotsButton.isInitialized) styleFilterPill(filterScreenshotsButton, state.smartFilter == SmartFilterMode.SCREENSHOTS_30D)
        if (::filterOldButton.isInitialized) styleFilterPill(filterOldButton, state.smartFilter == SmartFilterMode.OLDER_THAN_30_DAYS)
    }

    private fun updateReview(state: MediaScanUiState) {
        val selected = state.selection.records
        val totalBytes = state.selection.totalBytesOrNull()
        selectionSummary.text = if (selected.isEmpty()) {
            getString(R.string.nothing_selected)
        } else {
            getString(
                R.string.review_count_and_size,
                selected.size,
                totalBytes?.let { android.text.format.Formatter.formatFileSize(this, it) } ?: getString(R.string.size_unavailable),
            )
        }
        val hasSelection = selected.isNotEmpty() && !state.isLoading
        if (::homeDirectCleanRow.isInitialized) {
            homeDirectCleanRow.visibility = if (hasSelection) View.VISIBLE else View.GONE
        }
        moveToTrashButton.isEnabled = hasSelection && trashCoordinator.supportsSystemTrash()
        permanentDeleteButton.isEnabled = hasSelection && trashCoordinator.supportsPermanentDelete()
        permanentDeleteButton.text = if (hasSelection && totalBytes != null && totalBytes > 0L) {
            "${getString(R.string.permanent_delete_button)} (${android.text.format.Formatter.formatFileSize(this, totalBytes)})"
        } else {
            getString(R.string.permanent_delete_button)
        }
        exportSelectionButton.isEnabled = hasSelection
        clearSelectionButton.isEnabled = hasSelection
        updateRestoreButtonState()
        updateSpaceMetricsCard(viewModel.state.value)

        selectedContainer.removeAllViews()
        selected.take(20).forEach { record ->
            val name = record.displayName?.takeIf(String::isNotBlank) ?: getString(R.string.unnamed_file)
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            line.addView(label(
                "$name - ${android.text.format.Formatter.formatFileSize(this, record.sizeBytes)}",
                11f,
                R.color.sweep_muted,
                false,
            ), LinearLayout.LayoutParams(0, -2, 1f))
            val remove = button(getString(R.string.remove_review_item)) {
                viewModel.setSelected(record, false)
            }
            line.addView(remove, LinearLayout.LayoutParams(-2, dp(48)))
            selectedContainer.addView(line, bottom(4))
        }
        if (selected.size > 20) {
            selectedContainer.addView(label(getString(R.string.more_review_items, selected.size - 20), 11f, R.color.sweep_muted, false))
        }
    }

    private fun renderMissionProgress(progress: com.rdf112.sweep.presentation.scan.GlobalMissionProgress?) {
        progress ?: return
        val isFreeHomeCleanup = progress.mission == DesiredOutcomeMission.FREE_MAX_SPACE_NOW
        missionPermissionButton.visibility = if (hasDeniedMediaKind()) View.VISIBLE else View.GONE
        if (progress.isRunning) {
            if (isFreeHomeCleanup) {
                heroOrbButton.isEnabled = false
                heroOrbButton.text = getString(R.string.home_cleanup_scanning_button)
                heroOrbSubtitle.text = getString(R.string.home_cleanup_scanning)
                showHomeCleanupMessage(
                    getString(
                        R.string.home_cleanup_progress,
                        progress.pagesProcessed,
                        progress.recordsExamined,
                    ),
                )
            }
            missionReviewButton.visibility = View.GONE
            missionRetryButton.visibility = View.GONE
            oaasPipelineStatus.text = getString(
                R.string.oaas_global_scan_progress,
                progress.pagesProcessed,
                progress.recordsExamined,
                progress.sourcesProcessed,
                progress.sourceCount,
            )
            return
        }

        val plan = progress.plan
        if (plan == null) {
            if (progress.message == "session_stale" && autoRetriedStaleMission != progress.mission) {
                autoRetriedStaleMission = progress.mission
                pendingOutcomeMission = progress.mission
                openInteractiveDialogOnMissionFinish = true
                viewModel.startSession()
                return
            }
            autoRetriedStaleMission = null
            openInteractiveDialogOnMissionFinish = false
            android.util.Log.e(
                "MidiastoreMediaScan",
                "Global mission returned no plan: mission=${progress.mission.name}, reason=${progress.message ?: "none"}",
            )
            if (isFreeHomeCleanup) {
                homeInteractiveRecords = emptyList()
                homeInteractiveFallback = false
                homeInteractiveMission = null
                homeCleanupCandidateCount = 0
                heroOrbButton.isEnabled = true
                heroOrbButton.text = getString(R.string.home_cleanup_primary)
                heroOrbSubtitle.text = getString(R.string.home_cleanup_subtitle)
                homeComposeState.value = homeComposeState.value.copy(primaryActionLabel = null)
                showHomeCleanupMessage(getString(R.string.home_cleanup_scan_failed))
            }
            missionReviewButton.visibility = View.GONE
            missionRetryButton.visibility = if (progress.message in setOf("session_stale", "cancelled")) View.VISIBLE else View.GONE
            val errMsg = when (progress.message) {
                "cancelled" -> getString(R.string.oaas_global_scan_cancelled)
                "safety_limit_reached" -> getString(R.string.oaas_global_scan_limit)
                "session_stale" -> getString(R.string.session_stale)
                else -> getString(R.string.oaas_global_scan_failed)
            }
            oaasPipelineStatus.text = errMsg
            Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show()
            return
        }
        autoRetriedStaleMission = null
        missionRetryButton.visibility = View.GONE

        if (progress !== lastRecordedMissionProgress) {
            outcomeTelemetry.recordMissionRequested(progress.mission, plan.autoSelectedRecords.size)
            lastRecordedMissionProgress = progress
            refreshFlywheelSummary()
        }

        val formattedBytes = android.text.format.Formatter.formatFileSize(this, plan.estimatedDeliverableBytes)
        homeComposeState.value = homeComposeState.value.copy(
            recoverableBytes = plan.estimatedDeliverableBytes,
            recoverableLabel = formattedBytes,
        )
        val resultMessage = when {
            plan.autoSelectedRecords.isEmpty() && progress.coverageComplete -> getString(
                R.string.oaas_no_candidates_complete,
                progress.recordsExamined,
            )
            plan.autoSelectedRecords.isEmpty() -> getString(
                R.string.oaas_no_candidates_partial,
                progress.recordsExamined,
                progress.warningCount,
            )
            plan.isReadyToDeliver && progress.coverageComplete -> getString(
                R.string.oaas_global_scan_result_complete,
                progress.recordsExamined,
                progress.pagesProcessed,
                plan.autoSelectedRecords.size,
                formattedBytes,
                plan.protectedReferencesKept,
            )
            plan.isReadyToDeliver || !progress.coverageComplete -> getString(
                R.string.oaas_global_scan_result_partial,
                progress.recordsExamined,
                progress.pagesProcessed,
                plan.autoSelectedRecords.size,
                formattedBytes,
                progress.warningCount,
            )
            else -> getString(
                R.string.oaas_global_scan_result_empty,
                progress.recordsExamined,
                progress.warningCount,
            )
        }
        val primaryRecords = plan.autoSelectedRecords
        val useFallback = primaryRecords.isEmpty() &&
            progress.fallbackRecords.isNotEmpty()
        val interactiveRecords = when {
            primaryRecords.isNotEmpty() -> primaryRecords
            useFallback -> progress.fallbackRecords
            else -> emptyList()
        }
        homeInteractiveRecords = interactiveRecords
        homeInteractiveFallback = useFallback
        homeInteractiveMission = progress.mission
        homeInteractiveCoverageComplete = progress.coverageComplete
        val interactiveBytes = interactiveRecords.sumOf { it.sizeBytes.coerceAtLeast(0L) }
        val interactiveFormattedBytes = android.text.format.Formatter.formatFileSize(
            this,
            interactiveBytes,
        )
        val homeCleanMission = when (progress.mission) {
            DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES -> CleanMission.DUPLICATES
            DesiredOutcomeMission.FREE_MAX_SPACE_NOW -> CleanMission.LARGE_VIDEOS
            DesiredOutcomeMission.CLEAN_OLD_SCREENSHOTS_30D -> CleanMission.OLD_SCREENSHOTS
            else -> null
        }
        if (homeCleanMission != null) {
            updateHomeMissionCollection(
                mission = homeCleanMission,
                itemCount = interactiveRecords.size,
                sizeLabel = interactiveFormattedBytes,
            )
        }
        missionReviewButton.text = getString(R.string.oaas_cleanup_continue, interactiveFormattedBytes)
        missionReviewButton.visibility = if (interactiveRecords.isNotEmpty()) View.VISIBLE else View.GONE
        if (isFreeHomeCleanup) {
            homeCleanupCandidateCount = interactiveRecords.size
            heroOrbButton.isEnabled = true
            heroOrbSubtitle.text = getString(R.string.home_cleanup_subtitle)
            if (homeCleanupCandidateCount > 0) {
                val reviewLabel = getString(
                    R.string.home_cleanup_review_button,
                    homeCleanupCandidateCount,
                    interactiveFormattedBytes,
                )
                heroOrbButton.text = reviewLabel
                homeComposeState.value = homeComposeState.value.copy(
                    recoverableBytes = interactiveBytes,
                    recoverableLabel = interactiveFormattedBytes,
                    primaryActionLabel = reviewLabel,
                )
                val messageId = if (progress.coverageComplete) {
                    R.string.home_cleanup_candidates_found
                } else {
                    R.string.home_cleanup_candidates_partial
                }
                showHomeCleanupMessage(getString(messageId, homeCleanupCandidateCount, interactiveFormattedBytes))
            } else {
                heroOrbButton.text = getString(R.string.home_cleanup_primary)
                homeComposeState.value = homeComposeState.value.copy(primaryActionLabel = null)
                if (::homeDirectCleanRow.isInitialized) {
                    homeDirectCleanRow.visibility = View.GONE
                }
                showHomeCleanupMessage(
                    if (progress.coverageComplete) getString(R.string.home_cleanup_no_candidates)
                    else getString(R.string.home_cleanup_no_candidates_partial),
                )
            }
        }
        missionPermissionButton.visibility = if (!progress.coverageComplete && hasDeniedMediaKind()) View.VISIBLE else View.GONE
        val coverageNote = when (progress.message) {
            "paging_args_unverified" -> getString(R.string.warn_paging_args_unverified)
            "safety_limit_reached" -> getString(R.string.oaas_global_scan_limit)
            else -> null
        }
        oaasPipelineStatus.text = listOfNotNull(resultMessage, coverageNote).joinToString("\n")
        if (!progress.isRunning && openInteractiveDialogOnMissionFinish && progress !== lastInteractivePromptProgress) {
            openInteractiveDialogOnMissionFinish = false
            lastInteractivePromptProgress = progress
            openInteractiveCleanupReviewDialog(
                records = interactiveRecords,
                isFallbackList = useFallback,
                mission = progress.mission,
                coverageComplete = progress.coverageComplete,
            )
        }
    }

    private fun updateHomeMissionCollection(
        mission: CleanMission,
        itemCount: Int,
        sizeLabel: String,
    ) {
        val current = homeComposeState.value.collections.ifEmpty { buildDefaultHomeCollections() }
        val updated = current.map { col ->
            if (col.mission == mission) {
                col.copy(itemCount = itemCount, sizeLabel = if (itemCount > 0) sizeLabel else col.sizeLabel)
            } else {
                col
            }
        }
        homeComposeState.value = homeComposeState.value.copy(collections = updated)
    }

    private fun runOutcomeMission(mission: DesiredOutcomeMission) {
        if (!hasFullVisualAccess() && !limitedAccessAcknowledged) {
            pendingOutcomeMission = mission
            if (hasAnyMediaPermission()) {
                showLimitedAccessPromptSheet { runOutcomeMission(mission) }
            } else {
                requestMediaPermissions(allowSettingsFallback = true)
            }
            return
        }
        if (mission == DesiredOutcomeMission.FREE_MAX_SPACE_NOW) {
            heroOrbButton.isEnabled = false
            heroOrbButton.text = getString(R.string.home_cleanup_scanning_button)
            heroOrbSubtitle.text = getString(R.string.home_cleanup_scanning)
            showHomeCleanupMessage(getString(R.string.home_cleanup_scanning))
        }
        pendingOutcomeMission = null
        lastRecordedMissionProgress = null
        // Executa a missão diretamente (executeGlobalOutcomeMission já cria/revalida a sessão e cancela varreduras anteriores sem travar em estado stale)
        viewModel.executeGlobalOutcomeMission(mission)
    }

    private fun refreshFlywheelSummary() {
        if (!::oaasFlywheelMetrics.isInitialized) return
        val snap = outcomeTelemetry.loadSnapshot()
        val freedFmt = android.text.format.Formatter.formatFileSize(this, snap.totalConfirmedFreedBytes)
        oaasFlywheelMetrics.text = getString(
            R.string.oaas_flywheel_summary,
            snap.missionsExecutedCount,
            snap.totalVerifiedItemsDelivered,
            freedFmt,
        )
    }

    private fun updateSpaceMetricsCard(state: MediaScanUiState) {
        if (!::spaceMetricsText.isInitialized) return
        val pageRecords = state.currentPage?.records.orEmpty()
        val nowSec = System.currentTimeMillis() / 1000L
        val potentialBytes = com.rdf112.sweep.domain.oaas.OutcomeServiceEngine.calculatePotentiallyRecoverableBytes(pageRecords, nowSec)
        val preparedBytes = state.selection.totalBytesOrNull() ?: 0L
        val confirmedBytes = outcomeTelemetry.loadSnapshot().totalConfirmedFreedBytes

        val potentialFmt = android.text.format.Formatter.formatFileSize(this, potentialBytes)
        val preparedFmt = android.text.format.Formatter.formatFileSize(this, preparedBytes)
        val authFmt = android.text.format.Formatter.formatFileSize(this, sessionAuthorizedBytes)
        val confirmedFmt = android.text.format.Formatter.formatFileSize(this, confirmedBytes)
        spaceMetricsText.text = getString(
            R.string.space_metrics_summary,
            potentialFmt,
            preparedFmt,
            authFmt,
            confirmedFmt,
        )
    }

    private fun updateRestoreButtonState() {
        val ledger = trashCoordinator.loadPersistedTrashLedger()
        restoreTrashButton.text = getString(R.string.restore_trash_button, ledger.size)
        restoreTrashButton.isEnabled = !ledger.isEmpty && trashCoordinator.supportsSystemTrash()
    }

    private fun requestMoveSelectedToTrash(targetRecords: Collection<MediaRecord>? = null) {
        val selected = targetRecords ?: viewModel.state.value.selection.records
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.cleanup_dialog_select_none_toast, Toast.LENGTH_SHORT).show()
            return
        }
        if (!trashCoordinator.supportsSystemTrash()) {
            Toast.makeText(this, R.string.trash_unsupported_on_device, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val plan = trashCoordinator.prepareAndRevalidatePlan(selected, MediaOperationMode.MOVE_TO_TRASH)
            val firstBatch = plan.uriBatches.firstOrNull()
            if (firstBatch.isNullOrEmpty()) {
                val count = plan.rejectedItems.size
                val msg = getString(R.string.revalidation_none_eligible, count)
                statusText.text = msg
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                return@launch
            }
            val sender = trashCoordinator.createTrashIntentSender(firstBatch, moveToTrash = true)
            if (sender != null) {
                val sizeMap = selected.associate { it.uri to it.sizeBytes }
                pendingOperationUris = firstBatch
                pendingOperationBytesByUri = sizeMap
                updateSpaceMetricsCard(viewModel.state.value)
                trashIntentLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } else {
                val err = getString(R.string.page_failed)
                statusText.text = err
                Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermanentDeleteSelected(targetRecords: Collection<MediaRecord>? = null) {
        val selected = targetRecords ?: viewModel.state.value.selection.records
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.cleanup_dialog_select_none_toast, Toast.LENGTH_SHORT).show()
            return
        }
        if (!trashCoordinator.supportsPermanentDelete()) return
        lifecycleScope.launch {
            val plan = trashCoordinator.prepareAndRevalidatePlan(selected, MediaOperationMode.PERMANENT_DELETE)
            val firstBatch = plan.uriBatches.firstOrNull()
            if (firstBatch.isNullOrEmpty()) {
                val count = plan.rejectedItems.size
                val msg = getString(R.string.revalidation_none_eligible, count)
                statusText.text = msg
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                return@launch
            }
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                pendingApi29DeleteQueue.clear()
                pendingApi29DeleteQueue.addAll(firstBatch)
                pendingApi29DeletedUris.clear()
                pendingApi29BytesByUri = selected.associate { it.uri to it.sizeBytes }
                pendingApi29RecordsByUri = selected.associateBy { it.uri }
                pendingApi29TotalCount = firstBatch.size
                pendingApi29RejectedCount = 0
                statusText.text = getString(R.string.api29_delete_notice)
                continueApi29DeleteBatch()
                return@launch
            }
            val sender = trashCoordinator.createPermanentDeleteIntentSender(firstBatch)
            if (sender != null) {
                val sizeMap = selected.associate { it.uri to it.sizeBytes }
                pendingOperationUris = firstBatch
                pendingOperationBytesByUri = sizeMap
                updateSpaceMetricsCard(viewModel.state.value)
                deleteIntentLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } else {
                val err = getString(R.string.page_failed)
                statusText.text = err
                Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Android 10 has no batch delete request; each protected item uses the official recoverable consent flow. */
    private fun continueApi29DeleteBatch() {
        lifecycleScope.launch {
            while (pendingApi29DeleteQueue.isNotEmpty()) {
                val uri = pendingApi29DeleteQueue.first()
                val record = pendingApi29RecordsByUri[uri]
                val attempt = record?.let { trashCoordinator.deleteApi29Item(it) }
                    ?: Api29DeleteAttempt.NotDeleted("record_missing")
                when (attempt) {
                    Api29DeleteAttempt.Deleted -> {
                        pendingApi29DeletedUris += uri
                        pendingApi29DeleteQueue.removeAt(0)
                    }
                    is Api29DeleteAttempt.ConsentRequired -> {
                        pendingApi29ConsentUri = uri
                        deleteIntentLauncher.launch(IntentSenderRequest.Builder(attempt.intentSender).build())
                        return@launch
                    }
                    is Api29DeleteAttempt.NotDeleted -> {
                        pendingApi29RejectedCount++
                        pendingApi29DeleteQueue.removeAt(0)
                    }
                }
            }
            finishApi29DeleteBatch()
        }
    }

    private suspend fun finishApi29DeleteBatch() {
        val attempted = pendingApi29TotalCount
        val verifiedUris = trashCoordinator.verifyPostDeleteUris(pendingApi29DeletedUris)
        val confirmedBytes = verifiedUris.sumOf { pendingApi29BytesByUri[it] ?: 0L }
        var reviewPrompted = false
        if (confirmedBytes > 0L) {
            try {
                window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            } catch (_: Exception) {}
            outcomeTelemetry.recordConfirmedFreedBytes(confirmedBytes)
            val lifetimeBytesNow = outcomeTelemetry.loadSnapshot().totalConfirmedFreedBytes
            reviewPrompted = inAppReviewCoordinator.requestReviewIfAppropriate(confirmedBytes, lifetimeBytesNow)
        }
        refreshStorageStats()
        refreshFlywheelSummary()
        updateSpaceMetricsCard(viewModel.state.value)
        val freedFmt = android.text.format.Formatter.formatFileSize(this, confirmedBytes)
        if (verifiedUris.isNotEmpty()) {
            homeInteractiveRecords = emptyList()
            homeInteractiveFallback = false
            homeInteractiveMission = null
            homeCleanupCandidateCount = 0
            heroOrbButton.text = getString(R.string.home_cleanup_primary)
            homeComposeState.value = homeComposeState.value.copy(
                recoverableBytes = 0L,
                recoverableLabel = "0 MB",
                primaryActionLabel = null,
                collections = buildDefaultHomeCollections(),
            )
            if (::homeDirectCleanRow.isInitialized) {
                homeDirectCleanRow.visibility = View.GONE
            }
            showHomeCleanupMessage(
                getString(
                    R.string.delete_operation_result,
                    verifiedUris.size,
                    attempted,
                    freedFmt,
                ),
            )
            viewModel.clearSelection()
            viewModel.startSession()
            if (!reviewPrompted) {
                if (hasEffectiveProAccess()) {
                    maybePromptWeeklyDigestOptIn()
                } else {
                    showPostCleanupReliefOffer(confirmedBytes, verifiedUris.size)
                }
            }
        }
        val resultMsg = getString(
            R.string.delete_operation_result,
            verifiedUris.size,
            attempted,
            freedFmt,
        )
        statusText.text = resultMsg
        android.widget.Toast.makeText(this, resultMsg, android.widget.Toast.LENGTH_LONG).show()
        pendingApi29DeleteQueue.clear()
        pendingApi29DeletedUris.clear()
        pendingApi29BytesByUri = emptyMap()
        pendingApi29RecordsByUri = emptyMap()
        pendingApi29TotalCount = 0
        pendingApi29RejectedCount = 0
    }

    private fun requestRestoreFromTrash() {
        val ledger = trashCoordinator.loadPersistedTrashLedger()
        if (ledger.isEmpty || !trashCoordinator.supportsSystemTrash()) return
        val batch = ledger.trackedUris.take(500)
        val sender = trashCoordinator.createTrashIntentSender(batch, moveToTrash = false)
        if (sender != null) {
            restoreIntentLauncher.launch(IntentSenderRequest.Builder(sender).build())
        } else {
            statusText.text = getString(R.string.page_failed)
        }
    }

    private fun exportOrUnlockProSummary() {
        val summaryText = viewModel.buildReviewSummaryExportText() ?: return
        if (hasEffectiveProAccess()) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_review_subject))
                putExtra(Intent.EXTRA_TEXT, summaryText)
            }
            startActivity(Intent.createChooser(sendIntent, getString(R.string.export_review_summary)))
            return
        }
        when (billingGateway.accessState.value) {
            ProAccessSummaryState.PRO_GRANTED -> {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_review_subject))
                    putExtra(Intent.EXTRA_TEXT, summaryText)
                }
                startActivity(Intent.createChooser(sendIntent, getString(R.string.export_review_summary)))
            }
            ProAccessSummaryState.PENDING_PAYMENT -> {
                statusText.text = getString(R.string.billing_pending_message)
            }
            ProAccessSummaryState.FREE_NO_ACTIVE_PURCHASE,
            ProAccessSummaryState.RECONCILIATION_REQUIRED -> {
                switchTab(AppTab.PRO)
                val launched = billingGateway.launchPurchaseFlow(this, BillingStatePolicy.PRODUCT_ANNUAL)
                if (!launched) {
                    statusText.text = getString(
                        if (!billingGateway.securePurchaseVerificationAvailable()) {
                            R.string.billing_secure_verification_unavailable
                        } else {
                            R.string.billing_pro_required_message
                        }
                    )
                }
            }
        }
    }

    private val isDevelopmentBuild: Boolean
        get() = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0

    @Suppress("DEPRECATION")
    private fun refreshStorageStats() {
        // The simplified Home screen no longer renders the storage gauge/text views.
        if (!::storageText.isInitialized && !::storageUsedHeadline.isInitialized) return
        runCatching {
            // Measure primary shared storage, where user media is normally stored, rather than app-private /data.
            val primarySharedStorage = Environment.getExternalStorageDirectory()
            val stat = StatFs(primarySharedStorage.absolutePath)
            val usage = StorageUsageCalculator.from(
                totalBytes = stat.totalBytes,
                availableBytes = stat.availableBytes,
            ) ?: error("Primary shared-storage statistics are unavailable.")

            val usedFmt = android.text.format.Formatter.formatFileSize(this, usage.usedBytes)
            val freeFmt = android.text.format.Formatter.formatFileSize(this, usage.availableBytes)
            val totalFmt = android.text.format.Formatter.formatFileSize(this, usage.totalBytes)
            val lifetimeFreed = if (::outcomeTelemetry.isInitialized) {
                outcomeTelemetry.loadSnapshot().totalConfirmedFreedBytes
            } else 0L
            val lifetimeFreedFmt = if (lifetimeFreed > 0L) {
                android.text.format.Formatter.formatFileSize(this, lifetimeFreed)
            } else ""
            homeComposeState.value = homeComposeState.value.copy(
                usedLabel = usedFmt,
                freeLabel = freeFmt,
                totalLabel = totalFmt,
                usedPercent = usage.usedPercent,
                usedFraction = usage.usedFraction.toFloat().coerceIn(0f, 1f),
                lifetimeFreedBytes = lifetimeFreed,
                lifetimeFreedLabel = lifetimeFreedFmt,
                collections = homeComposeState.value.collections.ifEmpty { buildDefaultHomeCollections() },
            )
            if (::storageText.isInitialized) {
                storageText.text = getString(R.string.storage_measurement, usedFmt, freeFmt, totalFmt)
            }
            if (::storageUsedHeadline.isInitialized) {
                storageUsedHeadline.text = getString(R.string.storage_used_headline, usedFmt)
                storageFreeSubline.text = getString(R.string.storage_free_subline, freeFmt)
                if (::storageRingGauge.isInitialized) {
                    storageRingGauge.usedFraction = usage.usedFraction.toFloat()
                    storageRingGauge.centerLabel = "${usage.usedPercent}%"
                }
                if (::storageProgressBar.isInitialized) {
                    storageProgressBar.progress = usage.usedPercent
                }
                if (::storageSegmentedBar.isInitialized) {
                    val neutralUsedColor = Color.parseColor("#636366")
                    storageSegmentedBar.segments = listOf(
                        StorageBarSegment(usage.usedFraction.toFloat(), neutralUsedColor),
                    )
                    storageBreakdownJob?.cancel()
                    if (hasAnyMediaPermission()) {
                        storageBreakdownJob = lifecycleScope.launch {
                            val breakdown = runCatching {
                                AndroidMediaBreakdownReader.read(this@MainActivity)
                            }.getOrNull() ?: return@launch
                            if (!breakdown.hasAnyBreakdown) return@launch
                            val totalBytes = usage.totalBytes.coerceAtLeast(1L).toDouble()
                            val pFrac = (breakdown.photosBytes / totalBytes).toFloat().coerceIn(0f, 1f)
                            val vFrac = (breakdown.videosBytes / totalBytes).toFloat().coerceIn(0f, 1f)
                            val aFrac = (breakdown.audioBytes / totalBytes).toFloat().coerceIn(0f, 1f)
                            val mediaSum = breakdown.totalAccessibleMediaBytes
                            val otherBytes = (usage.usedBytes - mediaSum).coerceAtLeast(0L)
                            val oFrac = (otherBytes / totalBytes).toFloat().coerceIn(0f, 1f)
                            homeComposeState.value = homeComposeState.value.copy(
                                categoryFractions = listOf(pFrac, vFrac, aFrac, oFrac),
                            )
                            storageSegmentedBar.segments = listOf(
                                StorageBarSegment(pFrac, color(R.color.google_blue_primary)),
                                StorageBarSegment(vFrac, color(R.color.sweep_success)),
                                StorageBarSegment(aFrac, Color.parseColor("#BF5AF2")),
                                StorageBarSegment(oFrac, neutralUsedColor),
                            )
                            if (::storageBreakdownLegend.isInitialized) {
                                val pFmt = if (breakdown.photosAccessible) Formatter.formatShortFileSize(this@MainActivity, breakdown.photosBytes) else "—"
                                val vFmt = if (breakdown.videosAccessible) Formatter.formatShortFileSize(this@MainActivity, breakdown.videosBytes) else "—"
                                val aFmt = if (breakdown.audioAccessible) Formatter.formatShortFileSize(this@MainActivity, breakdown.audioBytes) else "—"
                                livePhotosSizeState.value = pFmt
                                liveVideosSizeState.value = vFmt
                                storageBreakdownLegend.text = getString(R.string.storage_legend_summary, pFmt, vFmt, aFmt)
                                storageBreakdownLegend.visibility = View.VISIBLE
                            }
                            // Calcula em segundo plano o tamanho real das mídias de WhatsApp & Chats para exibir nos Quadradinhos
                            val waItems = runCatching {
                                whatsAppCleaner.scanViaMediaStore(this@MainActivity)
                            }.getOrDefault(emptyList())
                            if (waItems.isNotEmpty()) {
                                val waBytes = waItems.sumOf { it.sizeBytes.coerceAtLeast(0L) }
                                val waFmt = Formatter.formatShortFileSize(this@MainActivity, waBytes)
                                liveChatsSizeState.value = waFmt
                                updateHomeMissionCollection(
                                    mission = CleanMission.WHATSAPP,
                                    itemCount = waItems.size,
                                    sizeLabel = waFmt,
                                )
                            }
                        }
                    } else if (::storageBreakdownLegend.isInitialized) {
                        storageBreakdownLegend.visibility = View.GONE
                    }
                }
            }
        }.onFailure {
            homeComposeState.value = HomeUiState()
            if (::storageText.isInitialized) storageText.text = getString(R.string.storage_unavailable)
            if (::storageUsedHeadline.isInitialized) {
                storageUsedHeadline.text = getString(R.string.storage_unavailable)
                storageFreeSubline.text = ""
                if (::storageRingGauge.isInitialized) {
                    storageRingGauge.usedFraction = 0f
                    storageRingGauge.centerLabel = "—"
                }
            }
        }
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
    }

    private fun wrap(content: LinearLayout): MaterialCardView = MaterialCardView(this).apply {
        radius = dp(22).toFloat()
        cardElevation = 0f
        setCardBackgroundColor(color(R.color.sweep_surface))
        strokeWidth = dp(1)
        strokeColor = color(R.color.sweep_surface_alt)
        addView(content, ViewGroup.LayoutParams(-1, -2))
    }

    private fun themedCard(
        content: LinearLayout,
        bgColor: Int,
        strokeCol: Int
    ): MaterialCardView = MaterialCardView(this).apply {
        radius = dp(22).toFloat()
        cardElevation = 0f
        setCardBackgroundColor(bgColor)
        strokeWidth = dp(2)
        strokeColor = strokeCol
        addView(content, ViewGroup.LayoutParams(-1, -2))
    }

    private fun label(text: String, size: Float, colorId: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color(colorId))
        if (bold) typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = false
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun button(text: String, onClick: () -> Unit): MaterialButton = MaterialButton(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 13.5f
        cornerRadius = dp(16)
        minHeight = dp(52)
        insetTop = 0
        insetBottom = 0
        setPadding(dp(16), dp(12), dp(16), dp(12))
        backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
        setTextColor(color(R.color.sweep_text))
        setOnClickListener {
            try { performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
            onClick()
        }
    }

    private fun navTabButton(text: String, onClick: () -> Unit): MaterialButton = MaterialButton(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 11.5f
        cornerRadius = dp(14)
        minHeight = dp(52)
        insetTop = 0
        insetBottom = 0
        setPadding(dp(6), dp(8), dp(6), dp(8))
        setOnClickListener {
            try { performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
            onClick()
        }
    }

    private fun fullButton(height: Int, bottom: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) }

    private fun bottom(value: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(value) }

    private fun top(value: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(value) }

    private fun color(id: Int): Int = ContextCompat.getColor(this, id)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun renderBrandWordmark(isPro: Boolean) {
        if (!::brandWordmarkView.isInitialized) return
        val baseColor = color(R.color.sweep_text)
        val accentColor = color(R.color.google_blue_primary)
        val brandName = getString(R.string.app_name)
        brandWordmarkView.setTextColor(baseColor)
        val accentStart = brandName.indexOf("store", ignoreCase = true)
        brandWordmarkView.text = if (accentStart >= 0) {
            SpannableString(brandName).apply {
                setSpan(
                    ForegroundColorSpan(accentColor),
                    accentStart,
                    brandName.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        } else {
            brandName
        }
    }

    private fun borderedPill(fill: Int, strokeColor: Int, radius: Int, strokeWidthPx: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = radius.toFloat()
            setStroke(strokeWidthPx, strokeColor)
        }

    private fun roundedBackground(fill: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = radius.toFloat()
        }

    private fun gradientPill(startColor: Int, endColor: Int, radius: Int): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(startColor, endColor),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius.toFloat()
        }

    private fun circleGlowBackground(fillColor: Int, strokeColor: Int): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(fillColor, color(R.color.sweep_primary_glow)),
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(dp(6), strokeColor)
        }
}
