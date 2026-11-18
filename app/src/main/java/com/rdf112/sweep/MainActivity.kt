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
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.provider.OpenableColumns
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
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
import com.rdf112.sweep.data.media.AndroidMediaBreakdownReader
import com.rdf112.sweep.data.media.AndroidMediaPageRepository
import com.rdf112.sweep.data.media.AndroidTrashOperationCoordinator
import com.rdf112.sweep.data.media.Api29DeleteAttempt
import com.rdf112.sweep.data.oaas.AndroidOutcomeTelemetryStore
import com.rdf112.sweep.util.InAppReviewCoordinator
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
import com.rdf112.sweep.presentation.ui.HomeUiState
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
import android.content.ClipData
import android.content.ClipboardManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import com.rdf112.sweep.presentation.studio.CountryHubInfo
import com.rdf112.sweep.presentation.studio.FallingBlocksGameView
import com.rdf112.sweep.presentation.studio.GlobalHubTools
import com.rdf112.sweep.presentation.studio.MagicDiceEngine
import com.rdf112.sweep.presentation.studio.MagicRiddlesTools
import com.rdf112.sweep.presentation.studio.MediaStudioTools
import com.rdf112.sweep.presentation.studio.SnakeGameView
import com.rdf112.sweep.presentation.studio.TwentyOneCardTrickEngine
import com.rdf112.sweep.presentation.studio.SevenDifferencesGameView
import com.rdf112.sweep.presentation.studio.WorldNewsGazetteTools
import com.rdf112.sweep.presentation.studio.WorldNewsItem

/**
 * Midiastore (`com.rdf112.midiastore`, namespace interno `com.rdf112.sweep`).
 * Interface modular em abas espaçosas com Medidor Circular, Limpeza MediaStore Universal,
 * Hub Mundial Offline (Cobrinha, Relógio, Moedas, Turismo) e Salão Secreto de Mágica & Charadas.
 */
class MainActivity : AppCompatActivity() {
    private enum class AppTab { HOME, NEWS, FILES, GAMES, STUDIO, OAAS, WORLD, PRO }
    private data class UnusedAppInfo(
        val packageName: String,
        val displayName: String,
        val lastUsedMillis: Long,
    )

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
        val homeCleanupPending = pendingOutcomeMission == DesiredOutcomeMission.FREE_MAX_SPACE_NOW
        val whatsAppPending = pendingWhatsAppCleanerAfterPermission
        val blurPending = pendingBlurScanAfterPermission
        pendingWhatsAppCleanerAfterPermission = false
        pendingBlurScanAfterPermission = false
        if (hasAnyMediaPermission()) {
            viewModel.startSession()
            refreshStorageStats()
            when {
                whatsAppPending -> openWhatsAppCleanerFlow()
                blurPending -> openBlurCleanerFlow()
                else -> switchTab(if (homeCleanupPending) AppTab.HOME else if (pendingOutcomeMission != null) AppTab.OAAS else AppTab.FILES)
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
                homeCleanupCandidateCount = 0
                heroOrbButton.isEnabled = true
                heroOrbButton.text = getString(R.string.home_cleanup_primary)
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
            if (confirmedBytes > 0L) {
                try {
                    window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                } catch (_: Exception) {}
                outcomeTelemetry.recordConfirmedFreedBytes(confirmedBytes)
                inAppReviewCoordinator.requestReviewIfAppropriate(confirmedBytes)
            }
            refreshStorageStats()
            refreshFlywheelSummary()
            updateSpaceMetricsCard(viewModel.state.value)
            val freedFmt = android.text.format.Formatter.formatFileSize(this@MainActivity, confirmedBytes)
            if (result.resultCode == Activity.RESULT_OK && confirmedDeleted > 0) {
                homeInteractiveRecords = emptyList()
                homeInteractiveFallback = false
                homeCleanupCandidateCount = 0
                heroOrbButton.isEnabled = true
                heroOrbButton.text = getString(R.string.home_cleanup_primary)
                if (::homeDirectCleanRow.isInitialized) {
                    homeDirectCleanRow.visibility = View.GONE
                }
                showHomeCleanupMessage(getString(R.string.delete_operation_result, confirmedDeleted, uris.size, freedFmt))
                viewModel.clearSelection()
                viewModel.startSession()
                Toast.makeText(this@MainActivity, getString(R.string.delete_operation_result, confirmedDeleted, uris.size, freedFmt), Toast.LENGTH_LONG).show()
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

    private val photoFilterPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { openInteractivePhotoFilterEditor(it) }
    }

    private val videoAudioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { openVideoAudioStudio(it) }
    }

    private val exifPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { openInteractiveExifCleaner(it) }
    }

    private val photosToPdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            processPhotosToPdf(uris)
        } else {
            Toast.makeText(this, R.string.no_photos_selected_toast, Toast.LENGTH_SHORT).show()
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
    private var pendingOutcomeMission: DesiredOutcomeMission? = null
    private var lastRecordedMissionProgress: com.rdf112.sweep.presentation.scan.GlobalMissionProgress? = null

    // Containers das Abas Dedicadas (Fim do app embolado: cada recurso em sua página)
    private lateinit var homeSection: LinearLayout
    private lateinit var homeComposeView: ComposeView
    private val homeComposeState = mutableStateOf(HomeUiState())
    private lateinit var newsSection: LinearLayout
    private lateinit var filesSection: LinearLayout
    private lateinit var studioSection: LinearLayout
    private lateinit var oaasSection: LinearLayout
    private lateinit var gamesSection: LinearLayout
    private lateinit var worldSection: LinearLayout
    private lateinit var proSection: LinearLayout

    // Botões da Barra de Navegação Inferior (5 Abas Limpas)
    private lateinit var navHomeBtn: MaterialButton
    private lateinit var navNewsBtn: MaterialButton
    private lateinit var navFilesBtn: MaterialButton
    private lateinit var navGamesBtn: MaterialButton
    private lateinit var navProBtn: MaterialButton
    private lateinit var bottomNavContainer: LinearLayout
    private var logoEasterEggTapCount = 0
    private var logoLastTapMs = 0L

    // Widgets da Aba Início (2027 Dashboard)
    private lateinit var appRootView: LinearLayout
    private lateinit var brandLockupCard: LinearLayout
    private lateinit var brandWordmarkView: TextView
    private lateinit var brandTaglineLabel: TextView
    private lateinit var storageCardContainer: MaterialCardView
    private lateinit var mainScrollView: ScrollView
    private lateinit var topBarProButton: MaterialButton
    private lateinit var easyModeButton: MaterialButton
    private lateinit var homeGlobalHubTitleView: TextView
    private lateinit var homeGlobalHubSubtitleView: TextView
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
    private var latestMonthlyOfferText: String? = null
    private var latestLifetimeOfferText: String? = null
    private var latestHeroPriceBadge: String? = null
    private var homeCleanupCandidateCount = 0
    private var homeInteractiveRecords: List<MediaRecord> = emptyList()
    private var homeInteractiveFallback: Boolean = false
    private var openInteractiveDialogOnMissionFinish: Boolean = false
    private var lastInteractivePromptProgress: GlobalMissionProgress? = null
    private var testerVipButton: MaterialButton? = null
    private lateinit var selectAllPageButton: MaterialButton
    private var lastFilteredPageRecords: List<MediaRecord> = emptyList()
    private var ttsEngine: TextToSpeech? = null

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
    private var pendingBlurScanAfterPermission = false
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

        runCatching {
            ttsEngine = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val isPt = Locale.getDefault().language.startsWith("pt", ignoreCase = true)
                    ttsEngine?.language = if (isPt) Locale("pt", "BR") else Locale.US
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::renderState) }
                launch { billingGateway.accessState.collect(::renderBillingState) }
                launch { billingGateway.catalog.collect(::renderCatalogPrices) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::storageText.isInitialized || ::storageUsedHeadline.isInitialized) refreshStorageStats()
        if (::viewModel.isInitialized) viewModel.checkSessionFreshness()
        if (::billingGateway.isInitialized) billingGateway.startAndReconcile()
        if (::restoreTrashButton.isInitialized) updateRestoreButtonState()
    }

    override fun onDestroy() {
        runCatching {
            ttsEngine?.stop()
            ttsEngine?.shutdown()
            ttsEngine = null
        }
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
                val isProNow = billingAccessState == ProAccessSummaryState.PRO_GRANTED ||
                    isTesterVipProEnabled() ||
                    (isDevelopmentBuild && debugProPreviewEnabled)
                MidiastoreTheme {
                    CleanerHomeScreen(
                        state = homeComposeState.value.copy(
                            isScanning = scanState.missionProgress?.isRunning == true,
                        ),
                        isProActive = isProNow,
                        onAnalyze = { onHomeCleanupButtonClicked() },
                        onMissionClick = ::onCleanMissionClick,
                        onOpenPro = { switchTab(AppTab.PRO) },
                    )
                }
            }
        }
        newsSection = buildNewsGazetteSection()
        filesSection = buildFilesExplorerSection()
        studioSection = buildStudioToolsSection()
        oaasSection = buildOaasMissionsSection()
        gamesSection = buildGamesOfflineSection()
        worldSection = buildWorldGlobalSection()
        proSection = buildProPaywallSection()

        bodyContainer.addView(homeSection)
        bodyContainer.addView(homeComposeView)
        bodyContainer.addView(newsSection)
        bodyContainer.addView(filesSection)
        bodyContainer.addView(studioSection)
        bodyContainer.addView(oaasSection)
        bodyContainer.addView(gamesSection)
        bodyContainer.addView(worldSection)
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
        navNewsBtn = navTabButton("") {}.apply { visibility = View.GONE }
        navGamesBtn = navTabButton("") {}.apply { visibility = View.GONE }

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

    private fun shareApp() {
        val playStoreLink = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_message, playStoreLink))
        }
        runCatching {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app_chooser_title)))
        }.onFailure {
            Toast.makeText(this, R.string.share_app_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun buildNewsGazetteSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        val isPt = Locale.getDefault().language.startsWith("pt", ignoreCase = true)
        var activeScope = WorldNewsGazetteTools.NewsScope.COUNTRY

        val headerCard = card()
        val dateLabel = TextView(this@MainActivity).apply {
            text = WorldNewsGazetteTools.getTodayDateFormatted(isPt)
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.google_blue_primary))
            letterSpacing = 0.05f
        }
        headerCard.addView(dateLabel)
        headerCard.addView(label(getString(R.string.gazette_header_title), 19f, R.color.sweep_text, true), top(4))
        val subtitleLabel = label(getString(R.string.gazette_country_subtitle), 12.5f, R.color.sweep_muted, false)
        headerCard.addView(subtitleLabel, top(4))
        addView(wrap(headerCard), bottom(10))

        // Seletor de Escopo: [ 🇧🇷 Meu País ] vs [ 🌐 Mundo & Descobertas ]
        val scopeRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(2), 0, dp(2), dp(10))
        }

        lateinit var scopeCountryBtn: MaterialButton
        lateinit var scopeWorldBtn: MaterialButton
        lateinit var renderCards: (String) -> Unit
        lateinit var rebuildChips: () -> Unit

        fun updateScopeStyles() {
            val isCountry = activeScope == WorldNewsGazetteTools.NewsScope.COUNTRY
            scopeCountryBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isCountry) color(R.color.google_blue_primary) else color(R.color.sweep_surface_alt)
            )
            scopeCountryBtn.setTextColor(if (isCountry) Color.WHITE else color(R.color.sweep_muted))

            scopeWorldBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (!isCountry) color(R.color.google_blue_primary) else color(R.color.sweep_surface_alt)
            )
            scopeWorldBtn.setTextColor(if (!isCountry) Color.WHITE else color(R.color.sweep_muted))

            subtitleLabel.text = getString(
                if (isCountry) R.string.gazette_country_subtitle else R.string.gazette_world_subtitle
            )
        }

        scopeCountryBtn = MaterialButton(this@MainActivity).apply {
            text = getString(R.string.gazette_scope_country)
            textSize = 12f
            minHeight = dp(44)
            insetTop = 0
            insetBottom = 0
            cornerRadius = dp(14)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { v ->
                try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                if (activeScope != WorldNewsGazetteTools.NewsScope.COUNTRY) {
                    activeScope = WorldNewsGazetteTools.NewsScope.COUNTRY
                    updateScopeStyles()
                    rebuildChips()
                }
            }
        }

        scopeWorldBtn = MaterialButton(this@MainActivity).apply {
            text = getString(R.string.gazette_scope_world)
            textSize = 12f
            minHeight = dp(44)
            insetTop = 0
            insetBottom = 0
            cornerRadius = dp(14)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { v ->
                try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                if (activeScope != WorldNewsGazetteTools.NewsScope.WORLD) {
                    activeScope = WorldNewsGazetteTools.NewsScope.WORLD
                    updateScopeStyles()
                    rebuildChips()
                }
            }
        }

        scopeRow.addView(scopeCountryBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        scopeRow.addView(scopeWorldBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        addView(scopeRow)

        // Chips horizontais de categoria
        val chipScroll = HorizontalScrollView(this@MainActivity).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
        }
        val chipRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(2), 0, dp(2), dp(10))
        }
        chipScroll.addView(chipRow)
        addView(chipScroll, bottom(6))

        val newsCardsContainer = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
        }
        addView(newsCardsContainer)

        renderCards = { filterCategory ->
            newsCardsContainer.removeAllViews()
            val allNews = WorldNewsGazetteTools.getNewsForScope(activeScope)
            val filtered = if (filterCategory == "TODAS" || filterCategory == "ALL") {
                allNews
            } else {
                allNews.filter { it.category.contains(filterCategory, ignoreCase = true) }
            }

            filtered.forEach { news ->
                val newsCard = card().apply {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { v ->
                        try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                        openNewsStoryDialog(news, isPt)
                    }
                }

                val topRow = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val catBadge = TextView(this@MainActivity).apply {
                    text = news.category
                    textSize = 10.5f
                    typeface = Typeface.DEFAULT_BOLD
                    val badgeColor = Color.parseColor(news.categoryBadgeHex)
                    setTextColor(badgeColor)
                    background = roundedBackground(Color.argb(35, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)), dp(8))
                    setPadding(dp(8), dp(3), dp(8), dp(3))
                }
                topRow.addView(catBadge)
                topRow.addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 1, 1f))
                val timeView = TextView(this@MainActivity).apply {
                    text = news.timeLabel
                    textSize = 11f
                    setTextColor(color(R.color.sweep_muted))
                }
                topRow.addView(timeView)
                newsCard.addView(topRow)

                val titleText = if (isPt) news.titlePt else news.titleEn
                val titleView = label(titleText, 15f, R.color.sweep_text, true)
                newsCard.addView(titleView, top(8))

                val summaryText = if (isPt) news.summaryPt else news.summaryEn
                val summaryView = label(summaryText, 12f, R.color.sweep_muted, false)
                newsCard.addView(summaryView, top(4))

                val footerRow = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val sourceView = TextView(this@MainActivity).apply {
                    text = "Fonte: ${news.source}"
                    textSize = 10.5f
                    setTextColor(color(R.color.sweep_muted))
                }
                footerRow.addView(sourceView, LinearLayout.LayoutParams(0, -2, 1f))
                val readMore = TextView(this@MainActivity).apply {
                    text = getString(R.string.gazette_read_more)
                    textSize = 11.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(color(R.color.google_blue_primary))
                }
                footerRow.addView(readMore)
                newsCard.addView(footerRow, top(8))

                newsCardsContainer.addView(wrap(newsCard), bottom(10))
            }
        }

        rebuildChips = {
            chipRow.removeAllViews()
            val categories = WorldNewsGazetteTools.getFilterCategories(activeScope, isPt)
            var selectedCat = categories.first()
            val chipButtons = mutableListOf<MaterialButton>()

            categories.forEach { cat ->
                val chip = MaterialButton(this@MainActivity).apply {
                    text = cat
                    textSize = 11f
                    minHeight = dp(36)
                    insetTop = 0
                    insetBottom = 0
                    cornerRadius = dp(14)
                    setPadding(dp(12), dp(4), dp(12), dp(4))
                    typeface = Typeface.DEFAULT_BOLD
                    val isSelected = cat == selectedCat
                    backgroundTintList = android.content.res.ColorStateList.valueOf(
                        if (isSelected) color(R.color.google_blue_primary) else color(R.color.sweep_surface_alt)
                    )
                    setTextColor(if (isSelected) Color.WHITE else color(R.color.sweep_muted))
                    setOnClickListener { v ->
                        try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                        selectedCat = cat
                        chipButtons.forEach { b ->
                            val active = b.text == selectedCat
                            b.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                if (active) color(R.color.google_blue_primary) else color(R.color.sweep_surface_alt)
                            )
                            b.setTextColor(if (active) Color.WHITE else color(R.color.sweep_muted))
                        }
                        renderCards(selectedCat)
                    }
                }
                chipButtons.add(chip)
                chipRow.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
            }
            renderCards(selectedCat)
        }

        updateScopeStyles()
        rebuildChips()
    }

    private fun openNewsStoryDialog(news: WorldNewsItem, isPt: Boolean) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        val topBadge = TextView(this).apply {
            text = "${news.category} • ${news.timeLabel}"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(news.categoryBadgeHex))
        }
        container.addView(topBadge)
        val titleText = if (isPt) news.titlePt else news.titleEn
        container.addView(label(titleText, 17f, R.color.sweep_text, true), top(6))

        val bodyText = if (isPt) news.fullBodyPt else news.fullBodyEn
        container.addView(label(bodyText, 13.5f, R.color.sweep_muted, false), top(10))

        val sourceText = TextView(this).apply {
            text = "Publicado por: ${news.source}"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.sweep_muted))
        }
        container.addView(sourceText, top(14))

        val listenButton = button("🔊 " + getString(R.string.gazette_listen_audio)) {
            val textToSpeak = "${titleText}. ${bodyText}"
            ttsEngine?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "news_speech")
            Toast.makeText(this@MainActivity, R.string.gazette_listening_toast, Toast.LENGTH_SHORT).show()
        }.apply {
            minHeight = dp(46)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
            setTextColor(color(R.color.google_blue_primary))
        }
        container.addView(listenButton, top(12))

        val scroll = ScrollView(this).apply { addView(container) }
        AlertDialog.Builder(this)
            .setView(scroll)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ttsEngine?.stop()
            }
            .setOnDismissListener {
                ttsEngine?.stop()
            }
            .show()
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

        moveToTrashButton = button(getString(R.string.move_to_trash_button)) { requestMoveSelectedToTrash() }.apply {
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        reviewCard.addView(moveToTrashButton, top(10))

        permanentDeleteButton = button(getString(R.string.permanent_delete_button)) { requestPermanentDeleteSelected() }
        reviewCard.addView(permanentDeleteButton, top(8))

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
        val oaasWhatsAppBtn = button("💬 " + getString(R.string.quick_clean_whatsapp)) {
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

    private fun buildProPaywallSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2), dp(4), dp(2), dp(16))

        addView(label(getString(R.string.pro_card_title), 24f, R.color.sweep_text, true))
        proStatusBadge = label(getString(R.string.pro_status_free), 12f, R.color.google_blue_primary, true)
        addView(proStatusBadge, top(4))
        addView(label(getString(R.string.pro_card_subtitle), 13.5f, R.color.sweep_muted, false), top(6))

        // Cartão 1: Plano Grátis Completo (Muitos benefícios liberados para todos)
        val freeIncludedCard = card()
        freeIncludedCard.addView(label(getString(R.string.pro_free_included), 13f, R.color.sweep_text, false))
        addView(wrap(freeIncludedCard), top(12))

        // Cartão 2: Benefícios Extras dos 3 Planos Pro
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
        addView(wrap(benefitsCard), top(12))

        planAnnualButton = button(getString(R.string.plan_annual_default)) {
            requestPlanPurchase(BillingStatePolicy.PRODUCT_ANNUAL)
        }.apply {
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.google_blue_primary))
        }
        stylePlanPurchaseButton(planAnnualButton, isProductAvailable = false)
        addView(planAnnualButton, top(18))

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

        val unusedAppsButton = button(getString(R.string.pro_unused_apps_button)) {
            runWithProAccess { requestUnusedAppSuggestions() }
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
            setTextColor(color(R.color.sweep_text))
        }
        addView(unusedAppsButton, top(16))

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

        addView(label(getString(R.string.pro_billing_terms_disclosure), 10.5f, R.color.sweep_muted, false), top(8))
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

    private fun buildStudioToolsSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        setPadding(dp(2), dp(2), dp(2), dp(16))

        // Hero Header do Estúdio de Mídia
        val header = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(12))
        }
        val heroBadge = TextView(this@MainActivity).apply {
            text = getString(R.string.studio_header_badge)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.studio_accent))
            includeFontPadding = false
        }
        header.addView(heroBadge)
        header.addView(label(getString(R.string.studio_header_title), 20f, R.color.sweep_text, true), top(4))
        header.addView(
            label(
                getString(R.string.studio_header_subtitle),
                12f,
                R.color.sweep_muted,
                false
            ),
            top(4)
        )
        addView(header)

        // AMBIENTE WHATSAPP: Limpador de Mídias do WhatsApp (WhatsApp Green #25D366)
        val waCard = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val waBadge = TextView(this@MainActivity).apply {
            text = getString(R.string.wa_card_badge)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#25D366"))
        }
        waCard.addView(waBadge)
        waCard.addView(label(getString(R.string.wa_card_title), 16f, R.color.sweep_text, true), top(4))
        waCard.addView(
            label(getString(R.string.wa_card_desc), 12f, R.color.sweep_muted, false),
            top(4)
        )
        val openWaBtn = button(getString(R.string.wa_card_btn)) {
            openWhatsAppCleanerFlow()
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#25D366"))
            setTextColor(Color.parseColor("#070B14"))
            minHeight = dp(48)
        }
        waCard.addView(openWaBtn, top(12))
        addView(themedCard(waCard, Color.parseColor("#0A1D14"), Color.parseColor("#25D366")), bottom(14))

        // AMBIENTE 0: Baixador de Vídeos da Web & Redes (Rose Neon #F50057)
        val dlCard = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val dlBadge = TextView(this@MainActivity).apply {
            text = getString(R.string.dl_badge)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F50057"))
        }
        dlCard.addView(dlBadge)
        dlCard.addView(label(getString(R.string.studio_dl_title), 16f, R.color.sweep_text, true), top(4))
        dlCard.addView(
            label(getString(R.string.studio_dl_desc), 12f, R.color.sweep_muted, false),
            top(4)
        )
        val openDlBtn = button(getString(R.string.studio_dl_btn)) {
            openVideoDownloaderStudio()
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F50057"))
            setTextColor(Color.WHITE)
            minHeight = dp(48)
        }
        dlCard.addView(openDlBtn, top(12))
        addView(themedCard(dlCard, Color.parseColor("#1B0C14"), Color.parseColor("#F50057")), bottom(14))

        // AMBIENTE 1: Filtros de Fotos (Neon Sunset #FF4081)
        val photoCard = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val photoBadge = TextView(this@MainActivity).apply {
            text = getString(R.string.filter_badge)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FF4081"))
        }
        photoCard.addView(photoBadge)
        photoCard.addView(label(getString(R.string.studio_filter_title), 16f, R.color.sweep_text, true), top(4))
        photoCard.addView(
            label(getString(R.string.studio_filter_desc), 12f, R.color.sweep_muted, false),
            top(4)
        )
        val openFiltersBtn = button(getString(R.string.studio_filter_btn)) {
            photoFilterPickerLauncher.launch("image/*")
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
            setTextColor(color(R.color.sweep_background))
            minHeight = dp(48)
        }
        photoCard.addView(openFiltersBtn, top(12))
        addView(themedCard(photoCard, color(R.color.sweep_surface), color(R.color.sweep_surface_alt)), bottom(14))

        // AMBIENTE 2: Áudio & Vídeo (Harmony Slate + Mint Accent)
        val videoCard = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val videoBadge = TextView(this@MainActivity).apply {
            text = getString(R.string.audio_badge)
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
        }
        videoCard.addView(videoBadge)
        videoCard.addView(label(getString(R.string.studio_audio_title), 16f, R.color.sweep_text, true), top(4))
        videoCard.addView(
            label(getString(R.string.studio_audio_desc), 12f, R.color.sweep_muted, false),
            top(4)
        )
        val openVideoBtn = button(getString(R.string.studio_audio_btn)) {
            videoAudioPickerLauncher.launch("video/*")
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#38BDF8"))
            setTextColor(color(R.color.sweep_background))
            minHeight = dp(48)
        }
        videoCard.addView(openVideoBtn, top(12))
        addView(themedCard(videoCard, color(R.color.sweep_surface), color(R.color.sweep_surface_alt)), bottom(14))

        // AMBIENTE 3: Limpador de Privacidade EXIF (Harmony Slate + Emerald)
        val exifCard = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val exifBadge = TextView(this@MainActivity).apply {
            text = getString(R.string.exif_badge)
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.sweep_primary_glow))
        }
        exifCard.addView(exifBadge)
        exifCard.addView(label(getString(R.string.studio_exif_title), 16f, R.color.sweep_text, true), top(4))
        exifCard.addView(
            label(getString(R.string.studio_exif_desc), 12f, R.color.sweep_muted, false),
            top(4)
        )
        val openExifBtn = button(getString(R.string.studio_exif_btn)) {
            exifPickerLauncher.launch("image/*")
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
            setTextColor(color(R.color.sweep_background))
            minHeight = dp(48)
        }
        exifCard.addView(openExifBtn, top(12))
        addView(themedCard(exifCard, color(R.color.sweep_surface), color(R.color.sweep_surface_alt)), bottom(14))

        // AMBIENTE 4: Gerador de QR Code (Harmony Slate + Amber)
        val qrCard = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val qrBadge = TextView(this@MainActivity).apply {
            text = getString(R.string.qr_badge)
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FFB300"))
        }
        qrCard.addView(qrBadge)
        qrCard.addView(label(getString(R.string.studio_qr_title), 16f, R.color.sweep_text, true), top(4))
        qrCard.addView(
            label(getString(R.string.studio_qr_desc), 12f, R.color.sweep_muted, false),
            top(4)
        )
        val openQrBtn = button(getString(R.string.studio_qr_btn)) {
            openQrCodeGeneratorStudio()
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFB300"))
            setTextColor(color(R.color.sweep_background))
            minHeight = dp(48)
        }
        qrCard.addView(openQrBtn, top(12))
        addView(themedCard(qrCard, color(R.color.sweep_surface), color(R.color.sweep_surface_alt)), bottom(14))

        // AMBIENTE 5: Fotos para PDF (Harmony Slate + Slate Alt)
        val pdfCard = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val pdfBadge = TextView(this@MainActivity).apply {
            text = getString(R.string.pdf_badge)
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
        }
        pdfCard.addView(pdfBadge)
        pdfCard.addView(label(getString(R.string.studio_pdf_title), 16f, R.color.sweep_text, true), top(4))
        pdfCard.addView(
            label(getString(R.string.studio_pdf_desc), 12f, R.color.sweep_muted, false),
            top(4)
        )
        val openPdfBtn = button(getString(R.string.studio_pdf_btn)) {
            photosToPdfPickerLauncher.launch("image/*")
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
            setTextColor(color(R.color.sweep_text))
            minHeight = dp(48)
        }
        pdfCard.addView(openPdfBtn, top(12))
        addView(themedCard(pdfCard, color(R.color.sweep_surface), color(R.color.sweep_surface_alt)), bottom(14))
    }

    private fun switchTab(tab: AppTab) {
        activeTab = tab
        statusText.visibility = if (statusText.text.isNotBlank() && (tab == AppTab.FILES || tab == AppTab.OAAS)) View.VISIBLE else View.GONE
        homeSection.visibility = View.GONE
        homeComposeView.visibility = if (tab == AppTab.HOME) View.VISIBLE else View.GONE
        newsSection.visibility = if (tab == AppTab.NEWS) View.VISIBLE else View.GONE
        filesSection.visibility = if (tab == AppTab.FILES || tab == AppTab.OAAS) View.VISIBLE else View.GONE
        gamesSection.visibility = if (tab == AppTab.GAMES) View.VISIBLE else View.GONE
        studioSection.visibility = if (tab == AppTab.STUDIO) View.VISIBLE else View.GONE
        oaasSection.visibility = View.GONE
        worldSection.visibility = View.GONE
        proSection.visibility = if (tab == AppTab.PRO) View.VISIBLE else View.GONE
        if (::mainScrollView.isInitialized) {
            mainScrollView.post { mainScrollView.scrollTo(0, 0) }
        }

        val tabAccent = color(R.color.google_blue_primary)
        styleNavButton(navHomeBtn, tab == AppTab.HOME, customSelectedColor = tabAccent)
        styleNavButton(navNewsBtn, tab == AppTab.NEWS, customSelectedColor = tabAccent)
        styleNavButton(navFilesBtn, tab == AppTab.FILES || tab == AppTab.OAAS, customSelectedColor = tabAccent)
        styleNavButton(navGamesBtn, tab == AppTab.GAMES, customSelectedColor = tabAccent)
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
            mission = CleanMission.LARGE_VIDEOS,
            title = getString(R.string.cleaner_home_fallback_large_videos_title),
            subtitle = getString(R.string.cleaner_home_fallback_large_videos_sub),
            sizeLabel = "> 10 MB",
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
            mission = CleanMission.OLD_SCREENSHOTS,
            title = getString(R.string.cleaner_home_fallback_screenshots_title),
            subtitle = getString(R.string.cleaner_home_fallback_screenshots_sub),
            sizeLabel = "> 30d",
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
            navNewsBtn.visibility = View.GONE
            navFilesBtn.visibility = View.VISIBLE
            navGamesBtn.visibility = View.GONE
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

    private fun copyTextToClipboard(text: String, toastResId: Int) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Midiastore", text))
        Toast.makeText(this, toastResId, Toast.LENGTH_SHORT).show()
    }

    private fun openProNotesVaultDialog() {
        val prefs = getSharedPreferences("midiastore_pro_suite", Context.MODE_PRIVATE)
        val notesKey = "pro_notes_json_v1"

        fun loadNotes(): MutableList<Triple<String, String, Long>> {
            val raw = prefs.getString(notesKey, "[]") ?: "[]"
            val list = mutableListOf<Triple<String, String, Long>>()
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        Triple(
                            obj.optString("title", ""),
                            obj.optString("body", ""),
                            obj.optLong("ts", 0L),
                        ),
                    )
                }
            } catch (_: Exception) {
            }
            return list
        }

        fun saveNotes(notes: List<Triple<String, String, Long>>) {
            val arr = JSONArray()
            notes.take(100).forEach { (title, body, ts) ->
                arr.put(
                    JSONObject().apply {
                        put("title", title)
                        put("body", body)
                        put("ts", ts)
                    },
                )
            }
            prefs.edit().putString(notesKey, arr.toString()).apply()
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val titleInput = EditText(this).apply {
            hint = getString(R.string.pro_notes_title_hint)
            setTextColor(color(R.color.sweep_text))
            setHintTextColor(color(R.color.sweep_muted))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedBackground(Color.parseColor("#181308"), dp(10))
        }
        val bodyInput = EditText(this).apply {
            hint = getString(R.string.pro_notes_body_hint)
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            setTextColor(color(R.color.sweep_text))
            setHintTextColor(color(R.color.sweep_muted))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedBackground(Color.parseColor("#181308"), dp(10))
        }
        container.addView(titleInput)
        container.addView(bodyInput, top(8))

        val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        fun renderNotes() {
            listContainer.removeAllViews()
            val notes = loadNotes()
            if (notes.isEmpty()) {
                listContainer.addView(label(getString(R.string.pro_notes_empty), 12f, R.color.sweep_muted, false), top(8))
                return
            }
            val dateFmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            notes.forEachIndexed { idx, (noteTitle, noteBody, ts) ->
                val cardContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                }
                val headerText = if (noteTitle.isNotBlank()) noteTitle else dateFmt.format(Date(ts))
                cardContent.addView(
                    TextView(this).apply {
                        text = headerText
                        textSize = 13f
                        setTypeface(typeface, Typeface.BOLD)
                        setTextColor(Color.parseColor("#FFD54F"))
                    },
                )
                if (noteTitle.isNotBlank() && ts > 0L) {
                    cardContent.addView(label(dateFmt.format(Date(ts)), 10f, R.color.sweep_muted, false), top(2))
                }
                cardContent.addView(label(noteBody, 12f, R.color.sweep_text, false), top(6))

                val actionsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                val fullText = if (noteTitle.isNotBlank()) "$noteTitle\n$noteBody" else noteBody
                val copyBtn = button("📋") {
                    copyTextToClipboard(fullText, R.string.pro_notes_copied_toast)
                }.apply { minHeight = dp(40) }
                val deleteBtn = button("🗑️") {
                    val updated = loadNotes().toMutableList()
                    if (idx in updated.indices) {
                        updated.removeAt(idx)
                        saveNotes(updated)
                        Toast.makeText(this@MainActivity, R.string.pro_notes_deleted_toast, Toast.LENGTH_SHORT).show()
                        renderNotes()
                    }
                }.apply { minHeight = dp(40) }
                actionsRow.addView(copyBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
                actionsRow.addView(deleteBtn, LinearLayout.LayoutParams(0, -2, 1f))
                cardContent.addView(actionsRow, top(8))

                listContainer.addView(
                    themedCard(cardContent, Color.parseColor("#1D1605"), Color.parseColor("#FFB300")),
                    top(10),
                )
            }
        }

        val saveBtn = button(getString(R.string.pro_notes_save_btn)) {
            val title = titleInput.text?.toString()?.trim().orEmpty()
            val body = bodyInput.text?.toString()?.trim().orEmpty()
            if (body.isEmpty() && title.isEmpty()) return@button
            val updated = loadNotes()
            updated.add(0, Triple(title, body, System.currentTimeMillis()))
            saveNotes(updated)
            titleInput.setText("")
            bodyInput.setText("")
            Toast.makeText(this, R.string.pro_notes_saved_toast, Toast.LENGTH_SHORT).show()
            renderNotes()
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFB300"))
            setTextColor(Color.parseColor("#120D03"))
        }
        container.addView(saveBtn, top(10))
        container.addView(listContainer, top(10))
        renderNotes()

        val scroll = ScrollView(this).apply { addView(container, ViewGroup.LayoutParams(-1, -2)) }
        AlertDialog.Builder(this)
            .setTitle(R.string.pro_notes_title)
            .setView(scroll)
            .setNegativeButton(R.string.close_btn, null)
            .show()
    }

    private fun openProSmartCalculatorDialog() {
        var expression = ""
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val exprView = TextView(this).apply {
            text = "0"
            textSize = 24f
            gravity = Gravity.END
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#FFD54F"))
            setPadding(dp(12), dp(10), dp(12), dp(4))
            background = roundedBackground(Color.parseColor("#181308"), dp(10))
        }
        val resultView = TextView(this).apply {
            text = "= 0"
            textSize = 15f
            gravity = Gravity.END
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#FFE082"))
            setPadding(dp(12), dp(2), dp(12), dp(8))
        }
        val capacityHintView = TextView(this).apply {
            textSize = 11f
            setTextColor(color(R.color.sweep_primary_glow))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = roundedBackground(Color.parseColor("#10221C"), dp(10))
        }

        fun formatCalcNumber(value: Double): String {
            if (!value.isFinite()) return "0"
            return if (value % 1.0 == 0.0 && kotlin.math.abs(value) < 1_000_000_000_000.0) {
                value.toLong().toString()
            } else {
                String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
            }
        }

        fun updateCalcUi() {
            exprView.text = if (expression.isEmpty()) "0" else expression
            val evaluated = if (expression.isBlank()) 0.0 else evaluateMathExpression(expression)
            val displayNum = evaluated ?: 0.0
            resultView.text = if (evaluated != null) "= ${formatCalcNumber(evaluated)}" else "…"
            val safeGb = displayNum.coerceIn(0.0, 100_000.0)
            val approxPhotos = ((safeGb * 1024.0) / 3.5).toInt().coerceAtLeast(0)
            val approxMins = ((safeGb * 1024.0) / 130.0).toInt().coerceAtLeast(0)
            capacityHintView.text = getString(R.string.pro_calc_capacity_hint, safeGb, approxPhotos, approxMins)
        }

        container.addView(exprView)
        container.addView(resultView)
        container.addView(capacityHintView, top(6))

        val rows = listOf(
            listOf("C", "⌫", "(", ")", "÷"),
            listOf("7", "8", "9", "×", "%"),
            listOf("4", "5", "6", "-", "√"),
            listOf("1", "2", "3", "+", "="),
            listOf("0", ".", "00", "COPY"),
        )
        rows.forEach { rowKeys ->
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowKeys.forEachIndexed { colIdx, key ->
                val labelText = if (key == "COPY") getString(R.string.pro_calc_copy_btn) else key
                val keyBtn = button(labelText) {
                    when (key) {
                        "C" -> expression = ""
                        "⌫" -> if (expression.isNotEmpty()) expression = expression.dropLast(1)
                        "=" -> {
                            evaluateMathExpression(expression)?.let { v ->
                                expression = formatCalcNumber(v)
                            }
                        }
                        "√" -> {
                            val current = evaluateMathExpression(expression)
                            if (current != null && current >= 0.0) {
                                expression = formatCalcNumber(kotlin.math.sqrt(current))
                            }
                        }
                        "÷" -> expression += "/"
                        "×" -> expression += "*"
                        "COPY" -> {
                            val toCopy = evaluateMathExpression(expression)?.let(::formatCalcNumber) ?: expression.ifBlank { "0" }
                            copyTextToClipboard(toCopy, R.string.pro_calc_copied_toast)
                        }
                        else -> expression += key
                    }
                    updateCalcUi()
                }.apply {
                    minHeight = dp(46)
                    textSize = if (key == "COPY") 11f else 15f
                    typeface = Typeface.DEFAULT_BOLD
                    when (key) {
                        "=" -> {
                            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFB300"))
                            setTextColor(Color.parseColor("#120D03"))
                        }
                        "÷", "×", "-", "+", "%", "√" -> {
                            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2A2109"))
                            setTextColor(Color.parseColor("#FFD54F"))
                        }
                        "C", "⌫" -> {
                            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2A1215"))
                            setTextColor(Color.parseColor("#FF8A80"))
                        }
                    }
                }
                val weight = if (key == "COPY") 2f else 1f
                rowLayout.addView(
                    keyBtn,
                    LinearLayout.LayoutParams(0, -2, weight).apply {
                        if (colIdx < rowKeys.lastIndex) marginEnd = dp(4)
                    },
                )
            }
            container.addView(rowLayout, top(6))
        }
        updateCalcUi()

        val scroll = ScrollView(this).apply { addView(container, ViewGroup.LayoutParams(-1, -2)) }
        AlertDialog.Builder(this)
            .setTitle(R.string.pro_calc_title)
            .setView(scroll)
            .setNegativeButton(R.string.close_btn, null)
            .show()
    }

    /** Evaluates arithmetic expressions (+, -, *, /, %, parentheses) safely without external dependencies. */
    private fun evaluateMathExpression(raw: String): Double? {
        val clean = raw.replace(" ", "").replace("×", "*").replace("÷", "/")
        if (clean.isEmpty()) return null
        return MathExprParser(clean).parse()
    }

    private class MathExprParser(private val clean: String) {
        private var pos = 0

        private fun peek(): Char? = if (pos < clean.length) clean[pos] else null

        fun parse(): Double? {
            val value = parseExpression() ?: return null
            return if (pos == clean.length && value.isFinite()) value else null
        }

        private fun parseExpression(): Double? {
            var left = parseTerm() ?: return null
            while (true) {
                when (peek()) {
                    '+' -> {
                        pos++
                        left += parseTerm() ?: return null
                    }
                    '-' -> {
                        pos++
                        left -= parseTerm() ?: return null
                    }
                    else -> return left
                }
            }
        }

        private fun parseTerm(): Double? {
            var left = parseFactor() ?: return null
            while (true) {
                when (peek()) {
                    '*' -> {
                        pos++
                        left *= parseFactor() ?: return null
                    }
                    '/' -> {
                        pos++
                        val right = parseFactor() ?: return null
                        if (right == 0.0) return null
                        left /= right
                    }
                    '%' -> {
                        pos++
                        val next = peek()
                        if (next != null && (next.isDigit() || next == '(')) {
                            val right = parseFactor() ?: return null
                            if (right == 0.0) return null
                            left %= right
                        } else {
                            left /= 100.0
                        }
                    }
                    else -> return left
                }
            }
        }

        private fun parseFactor(): Double? {
            return when (peek()) {
                '+' -> {
                    pos++
                    parseFactor()
                }
                '-' -> {
                    pos++
                    parseFactor()?.unaryMinus()
                }
                '(' -> {
                    pos++
                    val inside = parseExpression() ?: return null
                    if (peek() == ')') pos++
                    inside
                }
                else -> {
                    val start = pos
                    while (peek()?.let { it.isDigit() || it == '.' } == true) pos++
                    if (pos == start) null else clean.substring(start, pos).toDoubleOrNull()
                }
            }
        }
    }

    private fun openProWordSearchDialog() {
        val isPt = Locale.getDefault().language.startsWith("pt", ignoreCase = true)
        val easyMode = isEasySeniorModeEnabled()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        container.addView(label(getString(R.string.pro_ws_subtitle), if (easyMode) 14.5f else 13f, R.color.sweep_muted, false))

        var puzzle = com.rdf112.sweep.presentation.studio.ProWordSearchTools.generateDailyPuzzle(isPortuguese = isPt)
        val foundWords = mutableSetOf<String>()
        var selectedStart: Pair<Int, Int>? = null
        var hintCell: Pair<Int, Int>? = null

        val statusTv = label("", if (easyMode) 15f else 13.5f, R.color.sweep_accent_amber, true)
        container.addView(statusTv, top(10))

        val wordsListTv = TextView(this).apply {
            textSize = if (easyMode) 15f else 13f
            setTextColor(color(R.color.sweep_text))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = roundedBackground(Color.parseColor("#161105"), dp(10))
            typeface = Typeface.MONOSPACE
        }
        container.addView(wordsListTv, top(8))

        val gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = roundedBackground(Color.parseColor("#100D04"), dp(12))
        }
        container.addView(gridContainer, top(10))

        val selectionHintTv = label("", 12f, R.color.sweep_primary_glow, false)
        container.addView(selectionHintTv, top(6))

        lateinit var renderBoard: () -> Unit
        renderBoard = {
            val modeTag = if (puzzle.isDailyChallenge) {
                getString(R.string.pro_ws_daily_tag, puzzle.dateKey)
            } else {
                getString(R.string.pro_ws_random_tag)
            }
            statusTv.text = getString(
                R.string.pro_ws_found_count,
                foundWords.size,
                puzzle.placements.size,
                puzzle.themeTitle,
                modeTag,
            )
            wordsListTv.text = puzzle.placements.joinToString("   ") { p ->
                if (p.word in foundWords) "✅ ${p.word}" else "• ${p.word}"
            }
            selectionHintTv.text = if (selectedStart != null) {
                getString(R.string.pro_ws_select_start)
            } else {
                ""
            }
            selectionHintTv.visibility = if (selectedStart != null) View.VISIBLE else View.GONE

            val foundCells = buildSet {
                puzzle.placements
                    .filter { it.word in foundWords }
                    .forEach { addAll(com.rdf112.sweep.presentation.studio.ProWordSearchTools.cellsForPlacement(it)) }
            }

            gridContainer.removeAllViews()
            val cellHeight = dp(if (easyMode) 40 else 35)
            for (r in 0 until puzzle.size) {
                val rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                for (c in 0 until puzzle.size) {
                    val coord = r to c
                    val isStart = selectedStart == coord
                    val isFound = coord in foundCells
                    val isHint = hintCell == coord

                    val bgHex = when {
                        isStart -> Color.parseColor("#00E5B0")
                        isFound -> Color.parseColor("#FFB300")
                        isHint -> Color.parseColor("#E65100")
                        else -> Color.parseColor("#1E180A")
                    }
                    val txtColor = when {
                        isStart -> Color.parseColor("#041019")
                        isFound -> Color.parseColor("#100D04")
                        else -> Color.parseColor("#FFF8DC")
                    }

                    val cellView = TextView(this).apply {
                        text = puzzle.grid[r][c].toString()
                        gravity = Gravity.CENTER
                        textSize = if (easyMode) 17f else 15f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(txtColor)
                        background = roundedBackground(bgHex, dp(6))
                        setOnClickListener {
                            try { performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                            val curStart = selectedStart
                            if (curStart == null) {
                                selectedStart = coord
                                hintCell = null
                                renderBoard()
                            } else if (curStart == coord) {
                                selectedStart = null
                                renderBoard()
                            } else {
                                val matched = com.rdf112.sweep.presentation.studio.ProWordSearchTools.findMatchingWord(
                                    puzzle = puzzle,
                                    r1 = curStart.first,
                                    c1 = curStart.second,
                                    r2 = r,
                                    c2 = c,
                                    alreadyFoundWords = foundWords,
                                )
                                if (matched != null) {
                                    foundWords += matched.word
                                    selectedStart = null
                                    hintCell = null
                                    renderBoard()
                                    if (foundWords.size == puzzle.placements.size) {
                                        Toast.makeText(this@MainActivity, R.string.pro_ws_completed_toast, Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    selectedStart = null
                                    renderBoard()
                                }
                            }
                        }
                    }
                    rowLayout.addView(
                        cellView,
                        LinearLayout.LayoutParams(0, cellHeight, 1f).apply {
                            setMargins(dp(2), dp(2), dp(2), dp(2))
                        },
                    )
                }
                gridContainer.addView(rowLayout, ViewGroup.LayoutParams(-1, -2))
            }
        }

        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val dailyBtn = button(getString(R.string.pro_ws_daily_btn)) {
            puzzle = com.rdf112.sweep.presentation.studio.ProWordSearchTools.generateDailyPuzzle(isPortuguese = isPt)
            foundWords.clear()
            selectedStart = null
            hintCell = null
            renderBoard()
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2A2109"))
            setTextColor(Color.parseColor("#FFE082"))
        }
        val newBtn = button(getString(R.string.pro_ws_new_puzzle_btn)) {
            puzzle = com.rdf112.sweep.presentation.studio.ProWordSearchTools.generateRandomPuzzle(isPortuguese = isPt)
            foundWords.clear()
            selectedStart = null
            hintCell = null
            renderBoard()
        }
        val hintBtn = button(getString(R.string.pro_ws_hint_btn)) {
            val next = puzzle.placements.firstOrNull { it.word !in foundWords }
            if (next != null) {
                hintCell = next.startRow to next.startCol
                renderBoard()
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.pro_ws_hint_toast, next.word),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        listOf(dailyBtn, newBtn, hintBtn).forEachIndexed { idx, btn ->
            controlsRow.addView(
                btn,
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (idx < 2) marginEnd = dp(6)
                },
            )
        }
        container.addView(controlsRow, top(12))

        renderBoard()

        val scroll = ScrollView(this).apply { addView(container, ViewGroup.LayoutParams(-1, -2)) }
        AlertDialog.Builder(this)
            .setTitle(R.string.pro_ws_title)
            .setView(scroll)
            .setNegativeButton(R.string.close_btn, null)
            .show()
    }

    private fun openProPasswordGeneratorDialog() {
        val rng = SecureRandom()
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lower = "abcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val symbols = "!@#$%&*+-=?"
        val allChars = upper + lower + digits + symbols

        var selectedLength = 16
        var currentPassword = ""

        fun generateStrongPassword(length: Int): String {
            val chars = mutableListOf(
                upper[rng.nextInt(upper.length)],
                lower[rng.nextInt(lower.length)],
                digits[rng.nextInt(digits.length)],
                symbols[rng.nextInt(symbols.length)],
            )
            while (chars.size < length) {
                chars.add(allChars[rng.nextInt(allChars.length)])
            }
            for (i in chars.lastIndex downTo 1) {
                val j = rng.nextInt(i + 1)
                val tmp = chars[i]
                chars[i] = chars[j]
                chars[j] = tmp
            }
            return chars.joinToString("")
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val lengthLabel = TextView(this).apply {
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD54F"))
        }
        val passwordBox = TextView(this).apply {
            textSize = 17f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setTextColor(color(R.color.sweep_text))
            setPadding(dp(14), dp(16), dp(14), dp(16))
            background = roundedBackground(Color.parseColor("#181308"), dp(12))
            setTextIsSelectable(true)
        }

        fun refreshPassword() {
            lengthLabel.text = getString(R.string.pro_pass_length_label, selectedLength)
            currentPassword = generateStrongPassword(selectedLength)
            passwordBox.text = currentPassword
        }

        container.addView(lengthLabel)
        val lengthRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(12, 16, 20, 24).forEachIndexed { idx, len ->
            val chip = button("$len") {
                selectedLength = len
                refreshPassword()
            }.apply {
                minHeight = dp(44)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2A2109"))
                setTextColor(Color.parseColor("#FFE082"))
            }
            lengthRow.addView(
                chip,
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (idx < 3) marginEnd = dp(6)
                },
            )
        }
        container.addView(lengthRow, top(8))
        container.addView(passwordBox, top(12))

        container.addView(
            button(getString(R.string.pro_pass_generate_btn)) { refreshPassword() }.apply {
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFB300"))
                setTextColor(Color.parseColor("#120D03"))
            },
            top(10),
        )
        container.addView(
            button(getString(R.string.pro_pass_copy_btn)) {
                copyTextToClipboard(currentPassword, R.string.pro_pass_copied_toast)
            },
            top(8),
        )

        val ssidInput = EditText(this).apply {
            hint = getString(R.string.pro_pass_wifi_ssid_hint)
            setText("Midiastore_VIP_5G")
            setTextColor(color(R.color.sweep_text))
            setHintTextColor(color(R.color.sweep_muted))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedBackground(Color.parseColor("#141C2E"), dp(10))
        }
        container.addView(ssidInput, top(14))

        var dialogRef: AlertDialog? = null
        container.addView(
            button(getString(R.string.pro_pass_wifi_qr_btn)) {
                val escapeWifi = { s: String ->
                    s.replace("\\", "\\\\").replace(";", "\\;").replace(":", "\\:").replace(",", "\\,")
                }
                val ssid = ssidInput.text?.toString()?.trim().orEmpty().ifBlank { "Midiastore_VIP_5G" }
                val payload = "WIFI:S:${escapeWifi(ssid)};T:WPA;P:${escapeWifi(currentPassword)};;"
                dialogRef?.dismiss()
                openQrCodeGeneratorStudio(payload)
            }.apply {
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2A2109"))
                setTextColor(Color.parseColor("#FFD54F"))
            },
            top(8),
        )
        refreshPassword()

        val scroll = ScrollView(this).apply { addView(container, ViewGroup.LayoutParams(-1, -2)) }
        dialogRef = AlertDialog.Builder(this)
            .setTitle(R.string.pro_pass_title)
            .setView(scroll)
            .setNegativeButton(R.string.close_btn, null)
            .create()
        dialogRef.show()
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
        if (::homeGlobalHubTitleView.isInitialized) homeGlobalHubTitleView.textSize = if (enabled) 19f else 15.5f
        if (::homeGlobalHubSubtitleView.isInitialized) homeGlobalHubSubtitleView.textSize = if (enabled) 15f else 12.5f
    }

    private data class WorldButtonSpec(
        val labelRes: Int,
        val bgHex: String,
        val textHex: String,
        val action: () -> Unit,
    )

    private fun buildActionGroupCard(
        badgeText: String,
        badgeHex: String,
        titleText: String,
        subtitleText: String?,
        cardBgHex: String,
        strokeHex: String,
        buttons: List<WorldButtonSpec>,
        saveTitleRefs: Boolean = false,
    ): MaterialCardView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }
        content.addView(
            TextView(this).apply {
                text = badgeText
                textSize = 11.5f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor(badgeHex))
                letterSpacing = 0.06f
            },
        )
        val titleView = label(titleText, 17f, R.color.sweep_text, true)
        content.addView(titleView, top(6))
        if (subtitleText != null) {
            val subtitleView = label(subtitleText, 13f, R.color.sweep_muted, false)
            content.addView(subtitleView, top(4))
            if (saveTitleRefs) {
                homeGlobalHubTitleView = titleView
                homeGlobalHubSubtitleView = subtitleView
            }
        }
        buttons.forEachIndexed { idx, spec ->
            val btn = button(getString(spec.labelRes)) { spec.action() }.apply {
                minHeight = dp(58)
                textSize = 14.5f
                typeface = Typeface.DEFAULT_BOLD
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(spec.bgHex))
                setTextColor(Color.parseColor(spec.textHex))
            }
            content.addView(btn, top(if (idx == 0) 14 else 10))
        }
        return themedCard(content, Color.parseColor(cardBgHex), Color.parseColor(strokeHex))
    }

    private fun buildGamesOfflineSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE

        // Card 1: NOVO • O Jogo dos 7 Erros (Spot the Differences)
        addView(
            buildActionGroupCard(
                badgeText = getString(R.string.seven_diff_badge),
                badgeHex = "#0A84FF",
                titleText = getString(R.string.seven_diff_title),
                subtitleText = getString(R.string.seven_diff_subtitle),
                cardBgHex = "#1C1C1E",
                strokeHex = "#2C2C2E",
                buttons = listOf(
                    WorldButtonSpec(R.string.seven_diff_btn, "#0A84FF", "#FFFFFF") {
                        openSevenDifferencesDialog()
                    },
                ),
            ),
            bottom(14),
        )

        // Card 2: Super Caça-Palavras Pro (12 Temas • 240+ Palavras sem repetição)
        addView(
            buildActionGroupCard(
                badgeText = getString(R.string.pro_ws_daily_btn).uppercase(java.util.Locale.getDefault()),
                badgeHex = "#0A84FF",
                titleText = getString(R.string.games_wordsearch_card_title),
                subtitleText = getString(R.string.pro_ws_subtitle),
                cardBgHex = "#1C1C1E",
                strokeHex = "#2C2C2E",
                buttons = listOf(
                    WorldButtonSpec(R.string.pro_suite_wordsearch_btn, "#0A84FF", "#FFFFFF") {
                        openProWordSearchDialog()
                    },
                ),
            ),
            bottom(14),
        )

        // Card 3: Fliperama 100% Offline (Cobrinha Retrô + Blocos em Queda)
        addView(
            buildActionGroupCard(
                badgeText = getString(R.string.games_arcade_badge),
                badgeHex = "#0A84FF",
                titleText = getString(R.string.snake_dialog_title),
                subtitleText = getString(R.string.snake_hint_offline),
                cardBgHex = "#1C1C1E",
                strokeHex = "#2C2C2E",
                buttons = listOf(
                    WorldButtonSpec(R.string.home_btn_snake, "#0A84FF", "#FFFFFF") { openOfflineSnakeGameDialog() },
                    WorldButtonSpec(R.string.home_btn_blocks, "#2C2C2E", "#FFFFFF") { openFallingBlocksGameDialog() },
                ),
            ),
            bottom(14),
        )

        // Card 4: Curiosidade & Segredo Oculto (Mágica de Cartas, Dados Mágicos & Charadas)
        addView(
            buildActionGroupCard(
                badgeText = getString(R.string.games_magic_badge),
                badgeHex = "#0A84FF",
                titleText = getString(R.string.magic_lab_title),
                subtitleText = getString(R.string.magic_lab_subtitle),
                cardBgHex = "#1C1C1E",
                strokeHex = "#2C2C2E",
                buttons = listOf(
                    WorldButtonSpec(R.string.home_btn_magic, "#2C2C2E", "#FFFFFF") { openMagicSecretLabDialog() },
                    WorldButtonSpec(R.string.dice_title, "#2C2C2E", "#FFFFFF") { openMagicDiceDialog() },
                ),
            ),
            bottom(18),
        )
    }

    private fun openSevenDifferencesDialog() {
        val isPt = Locale.getDefault().language.startsWith("pt", ignoreCase = true)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val statusLabel = TextView(this).apply {
            text = getString(R.string.seven_diff_found_status, 0, 7)
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.google_blue_primary))
        }
        statusRow.addView(statusLabel, LinearLayout.LayoutParams(0, -2, 1f))

        val hintButton = MaterialButton(this).apply {
            text = getString(R.string.seven_diff_hint_btn)
            textSize = 11.5f
            isAllCaps = false
            cornerRadius = dp(12)
            minHeight = dp(40)
            setTextColor(Color.parseColor("#070B14"))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD60A"))
        }
        statusRow.addView(hintButton)
        container.addView(statusRow, bottom(8))

        val gameView = SevenDifferencesGameView(this).apply {
            setLanguage(isPt)
        }
        val displayH = resources.displayMetrics.heightPixels
        val gameViewH = (displayH * 0.52f).toInt().coerceIn(dp(320), dp(440))
        container.addView(gameView, LinearLayout.LayoutParams(-1, gameViewH))

        hintButton.setOnClickListener {
            if (!gameView.provideHint()) {
                Toast.makeText(this, if (isPt) "Você já encontrou todos os erros desta cena!" else "All differences found in this scene!", Toast.LENGTH_SHORT).show()
            }
        }

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val nextSceneBtn = button(getString(R.string.seven_diff_next_btn)) {
            gameView.nextScene()
            statusLabel.text = getString(R.string.seven_diff_found_status, 0, 7)
        }.apply {
            minHeight = dp(48)
            textSize = 13f
        }
        actionRow.addView(nextSceneBtn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        container.addView(actionRow)

        gameView.onProgressChanged = { found, total, lastFoundName ->
            statusLabel.text = getString(R.string.seven_diff_found_status, found, total)
            if (lastFoundName.isNotBlank()) {
                val toastMsg = if (isPt) "✨ Encontrado: $lastFoundName!" else "✨ Found: $lastFoundName!"
                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
            }
        }
        gameView.onAllFound = {
            Toast.makeText(this, R.string.seven_diff_win_toast, Toast.LENGTH_LONG).show()
        }

        val scroll = ScrollView(this).apply { addView(container) }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.seven_diff_title)
            .setView(scroll)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun buildWorldGlobalSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE

        // Card Único e Focado: Hub Global Offline (Relógio Mundial + Conversor de Moedas + Clima & Turismo)
        addView(
            buildActionGroupCard(
                badgeText = getString(R.string.home_global_badge),
                badgeHex = "#64D2FF",
                titleText = getString(R.string.home_global_title),
                subtitleText = getString(R.string.home_global_subtitle),
                cardBgHex = "#1C1C1E",
                strokeHex = "#2C2C2E",
                buttons = listOf(
                    WorldButtonSpec(R.string.home_btn_clock, "#0A84FF", "#FFFFFF") { openWorldClockDialog() },
                    WorldButtonSpec(R.string.home_btn_currency, "#2C2C2E", "#FFFFFF") { openCurrencyConverterDialog() },
                    WorldButtonSpec(R.string.home_btn_travel, "#2C2C2E", "#64D2FF") { openCountryTravelAndPlacesDialog() },
                ),
                saveTitleRefs = true,
            ),
            bottom(18),
        )
    }

    private fun openFallingBlocksGameDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val hudLabel = label(getString(R.string.blocks_score_line, 0, 0, 1, 0), 14f, R.color.sweep_primary_glow, true).apply {
            gravity = Gravity.CENTER
        }
        container.addView(hudLabel)
        container.addView(
            label(getString(R.string.blocks_hint_offline), 11.5f, R.color.sweep_muted, false).apply {
                gravity = Gravity.CENTER
            },
            top(4),
        )

        val blocksView = FallingBlocksGameView(this).apply {
            isGoldVipTheme = hasEffectiveProAccess()
        }
        val boardW = (resources.displayMetrics.widthPixels * 0.56f).toInt().coerceIn(dp(190), dp(260))
        val boardH = (boardW * 1.8f).toInt()
        container.addView(blocksView, LinearLayout.LayoutParams(boardW, boardH).apply { topMargin = dp(10) })

        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val pauseBtn = button(getString(R.string.snake_pause_btn)) { blocksView.togglePause() }.apply { minHeight = dp(48) }
        val restartBtn = button(getString(R.string.snake_restart_btn)) { blocksView.startNewGame() }.apply { minHeight = dp(48) }
        topRow.addView(pauseBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        topRow.addView(restartBtn, LinearLayout.LayoutParams(0, -2, 1f))
        container.addView(topRow, top(10))

        // Botões grandes e acessíveis para controlar os blocos
        val padRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val leftBtn = button("⬅️") { blocksView.moveLeft() }.apply { textSize = 18f; minHeight = dp(56) }
        val rotateBtn = button("🔄") { blocksView.rotateClockwise() }.apply {
            textSize = 18f
            minHeight = dp(56)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1D3557"))
        }
        val dropBtn = button("⏬") { blocksView.hardDrop() }.apply { textSize = 18f; minHeight = dp(56) }
        val rightBtn = button("➡️") { blocksView.moveRight() }.apply { textSize = 18f; minHeight = dp(56) }
        listOf(leftBtn, rotateBtn, dropBtn, rightBtn).forEachIndexed { idx, b ->
            padRow.addView(
                b,
                LinearLayout.LayoutParams(0, dp(56), 1f).apply {
                    if (idx < 3) marginEnd = dp(6)
                },
            )
        }
        container.addView(padRow, top(8))

        blocksView.onStateChanged = { score, lines, lvl, best, _, _ ->
            hudLabel.text = getString(R.string.blocks_score_line, score, lines, lvl, best)
        }
        blocksView.startNewGame()

        val scroll = ScrollView(this).apply { addView(container, ViewGroup.LayoutParams(-1, -2)) }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.blocks_dialog_title)
            .setView(scroll)
            .setNegativeButton(R.string.close_btn, null)
            .create()
        dialog.setOnDismissListener { blocksView.stopLoop() }
        dialog.show()
    }

    private fun openMagicSecretLabDialog() {
        val isPt = Locale.getDefault().language.startsWith("pt", ignoreCase = true)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        container.addView(label(getString(R.string.magic_lab_subtitle), 13f, R.color.sweep_muted, false))

        val riddlesHolder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        container.addView(riddlesHolder, top(10))

        val cardsBtn = button(getString(R.string.magic_tab_cards_btn)) {
            if (MagicRiddlesTools.isRiddleUnlocked(this, 0)) {
                open21CardMindReaderDialog()
            } else {
                Toast.makeText(this, R.string.magic_locked_toast, Toast.LENGTH_SHORT).show()
            }
        }.apply {
            minHeight = dp(56)
            textSize = 14f
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2B154D"))
            setTextColor(Color.parseColor("#E9D5FF"))
        }
        val diceBtn = button(getString(R.string.magic_tab_dice_btn)) {
            openMagicDiceDialog()
        }.apply {
            minHeight = dp(56)
            textSize = 14f
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2A1D08"))
            setTextColor(Color.parseColor("#FFD54F"))
        }

        val masterBadgeView = TextView(this).apply {
            text = getString(R.string.magic_master_badge_on)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFD54F"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedBackground(Color.parseColor("#261A05"), dp(12))
        }

        fun renderLab() {
            riddlesHolder.removeAllViews()
            masterBadgeView.visibility = if (MagicRiddlesTools.isRiddleUnlocked(this, 2)) View.VISIBLE else View.GONE

            MagicRiddlesTools.RIDDLES.forEach { riddle ->
                val solved = MagicRiddlesTools.isRiddleUnlocked(this, riddle.id)
                val cardContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                }
                cardContent.addView(label(if (isPt) riddle.questionPt else riddle.questionEn, 13.5f, R.color.sweep_text, true))
                cardContent.addView(
                    label(if (isPt) riddle.rewardPt else riddle.rewardEn, 11.5f, R.color.sweep_primary_glow, true),
                    top(4),
                )
                if (solved) {
                    cardContent.addView(label(getString(R.string.magic_riddle_unlocked_msg), 12.5f, R.color.sweep_success, true), top(8))
                } else {
                    val hintLabel = label(if (isPt) riddle.hintPt else riddle.hintEn, 11.5f, R.color.sweep_accent_amber, false).apply {
                        visibility = View.GONE
                    }
                    cardContent.addView(hintLabel, top(6))

                    val answerInput = EditText(this).apply {
                        hint = getString(R.string.magic_riddle_answer_hint)
                        setTextColor(color(R.color.sweep_text))
                        setHintTextColor(color(R.color.sweep_muted))
                        setPadding(dp(10), dp(10), dp(10), dp(10))
                        background = roundedBackground(Color.parseColor("#111A2B"), dp(8))
                    }
                    cardContent.addView(answerInput, top(8))

                    val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                    val hintBtn = button(getString(R.string.magic_riddle_hint_btn)) {
                        hintLabel.visibility = View.VISIBLE
                    }.apply { minHeight = dp(46) }
                    val unlockBtn = button(getString(R.string.magic_riddle_unlock_btn)) {
                        val raw = answerInput.text?.toString().orEmpty()
                        if (MagicRiddlesTools.tryUnlockRiddle(this, riddle.id, raw)) {
                            Toast.makeText(this, R.string.magic_riddle_unlocked_msg, Toast.LENGTH_SHORT).show()
                            renderLab()
                        } else {
                            Toast.makeText(this, R.string.magic_riddle_wrong_toast, Toast.LENGTH_SHORT).show()
                        }
                    }.apply {
                        minHeight = dp(46)
                        backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#A855F7"))
                        setTextColor(Color.WHITE)
                    }
                    btnRow.addView(hintBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
                    btnRow.addView(unlockBtn, LinearLayout.LayoutParams(0, -2, 1.3f))
                    cardContent.addView(btnRow, top(8))
                }
                riddlesHolder.addView(
                    themedCard(
                        cardContent,
                        Color.parseColor(if (solved) "#0B2419" else "#161128"),
                        Color.parseColor(if (solved) "#10B981" else "#7C3AED"),
                    ),
                    top(10),
                )
            }
        }
        renderLab()

        container.addView(masterBadgeView, top(12))
        container.addView(cardsBtn, top(14))
        container.addView(diceBtn, top(10))

        val scroll = ScrollView(this).apply { addView(container, ViewGroup.LayoutParams(-1, -2)) }
        AlertDialog.Builder(this)
            .setTitle(R.string.magic_lab_title)
            .setView(scroll)
            .setNegativeButton(R.string.close_btn, null)
            .show()
    }

    private fun open21CardMindReaderDialog() {
        val engine = TwentyOneCardTrickEngine()
        engine.reset(shuffleOrder = true)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val promptLabel = label("", 13.5f, R.color.sweep_primary_glow, true)
        val columnsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val buttonsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val revealBox = TextView(this).apply {
            textSize = 26f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD54F"))
            setPadding(dp(16), dp(22), dp(16), dp(22))
            background = roundedBackground(Color.parseColor("#241A06"), dp(16))
            visibility = View.GONE
        }
        val restartBtn = button(getString(R.string.cards_restart_btn)) {}.apply {
            visibility = View.GONE
            minHeight = dp(52)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFB300"))
            setTextColor(Color.parseColor("#120D03"))
        }

        fun renderTrickState(revealedCard: String? = null) {
            if (revealedCard != null) {
                columnsRow.visibility = View.GONE
                buttonsRow.visibility = View.GONE
                promptLabel.text = getString(R.string.cards_revealed_title)
                revealBox.text = "🪄 $revealedCard"
                revealBox.visibility = View.VISIBLE
                restartBtn.visibility = View.VISIBLE
                return
            }
            columnsRow.visibility = View.VISIBLE
            buttonsRow.visibility = View.VISIBLE
            revealBox.visibility = View.GONE
            restartBtn.visibility = View.GONE
            promptLabel.text = getString(R.string.cards_step_prompt, engine.round)

            columnsRow.removeAllViews()
            val cols = engine.dealColumns()
            cols.forEachIndexed { idx, colCards ->
                val colView = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(6), dp(8), dp(6), dp(8))
                    background = roundedBackground(Color.parseColor("#111A2E"), dp(12))
                }
                colView.addView(
                    label(getString(R.string.cards_col_btn, idx + 1), 11.5f, R.color.sweep_muted, true).apply {
                        gravity = Gravity.CENTER
                    },
                )
                colCards.forEach { cardText ->
                    val isRed = cardText.contains("♥") || cardText.contains("♦")
                    colView.addView(
                        TextView(this).apply {
                            text = cardText
                            textSize = 14f
                            setTypeface(typeface, Typeface.BOLD)
                            gravity = Gravity.CENTER
                            setTextColor(Color.parseColor(if (isRed) "#FF6B81" else "#F8FAFC"))
                            setPadding(dp(4), dp(8), dp(4), dp(8))
                            background = roundedBackground(Color.parseColor("#1B2742"), dp(8))
                        },
                        top(6),
                    )
                }
                columnsRow.addView(
                    colView,
                    LinearLayout.LayoutParams(0, -2, 1f).apply {
                        if (idx < 2) marginEnd = dp(6)
                    },
                )
            }
        }

        for (c in 0..2) {
            val colBtn = button(getString(R.string.cards_col_btn, c + 1)) {
                val revealed = engine.chooseColumn(c)
                renderTrickState(revealed)
            }.apply {
                minHeight = dp(52)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#7C3AED"))
                setTextColor(Color.WHITE)
            }
            buttonsRow.addView(
                colBtn,
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (c < 2) marginEnd = dp(6)
                },
            )
        }
        restartBtn.setOnClickListener {
            engine.reset(shuffleOrder = true)
            renderTrickState()
        }

        container.addView(promptLabel)
        container.addView(columnsRow, top(12))
        container.addView(buttonsRow, top(12))
        container.addView(revealBox, top(16))
        container.addView(restartBtn, top(12))
        renderTrickState()

        val scroll = ScrollView(this).apply { addView(container, ViewGroup.LayoutParams(-1, -2)) }
        AlertDialog.Builder(this)
            .setTitle(R.string.cards_title)
            .setView(scroll)
            .setNegativeButton(R.string.close_btn, null)
            .show()
    }

    private fun openMagicDiceDialog() {
        val illusionistUnlocked = MagicRiddlesTools.isRiddleUnlocked(this, 1)
        val forceCycle = listOf<Int?>(null, 7, 11, 12, 2, 6)
        var forceIdx = 0

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val diceDisplay = TextView(this).apply {
            text = "⚄  ⚂"
            textSize = 56f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFD54F"))
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBackground(Color.parseColor("#181308"), dp(16))
        }
        val resultLabel = label(getString(R.string.dice_result_line, "5 + 3", 8), 16f, R.color.sweep_text, true).apply {
            gravity = Gravity.CENTER
        }
        var rollJob: Job? = null
        val rollBtn = button(getString(R.string.dice_roll_btn)) {
            rollJob?.cancel()
            rollJob = lifecycleScope.launch {
                repeat(5) {
                    val (t1, t2) = MagicDiceEngine.rollTwoDice(null)
                    diceDisplay.text = "${MagicDiceEngine.faceGlyph(t1)}  ${MagicDiceEngine.faceGlyph(t2)}"
                    delay(55L)
                }
                val (d1, d2) = MagicDiceEngine.rollTwoDice(forceCycle[forceIdx])
                diceDisplay.text = "${MagicDiceEngine.faceGlyph(d1)}  ${MagicDiceEngine.faceGlyph(d2)}"
                resultLabel.text = getString(R.string.dice_result_line, "$d1 + $d2", d1 + d2)
            }
        }.apply {
            minHeight = dp(58)
            textSize = 15f
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFB300"))
            setTextColor(Color.parseColor("#120D03"))
        }

        container.addView(diceDisplay, LinearLayout.LayoutParams(-1, -2))
        container.addView(resultLabel, top(12))
        container.addView(rollBtn, top(14))

        if (illusionistUnlocked) {
            container.addView(label(getString(R.string.dice_secret_unlocked), 12f, R.color.sweep_primary_glow, false), top(16))
            lateinit var secretModeBtn: MaterialButton
            secretModeBtn = button(getString(R.string.dice_secret_force_off)) {
                forceIdx = (forceIdx + 1) % forceCycle.size
                val target = forceCycle[forceIdx]
                secretModeBtn.text = if (target == null) {
                    getString(R.string.dice_secret_force_off)
                } else {
                    getString(R.string.dice_secret_force_on, target)
                }
            }.apply {
                minHeight = dp(50)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2B154D"))
                setTextColor(Color.parseColor("#E9D5FF"))
            }
            container.addView(secretModeBtn, top(8))
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.dice_title)
            .setView(container)
            .setNegativeButton(R.string.close_btn, null)
            .create()
        dialog.setOnDismissListener { rollJob?.cancel() }
        dialog.show()
    }

    private fun openOfflineSnakeGameDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val scoreLabel = label(getString(R.string.snake_score_line, 0, 0), 15f, R.color.sweep_primary_glow, true).apply {
            gravity = Gravity.CENTER
        }
        container.addView(scoreLabel)
        container.addView(
            label(getString(R.string.snake_hint_offline), 11.5f, R.color.sweep_muted, false).apply {
                gravity = Gravity.CENTER
            },
            top(4),
        )

        val snakeView = SnakeGameView(this).apply {
            isGoldVipTheme = hasEffectiveProAccess()
        }
        val boardPx = (resources.displayMetrics.widthPixels * 0.72f).toInt().coerceIn(dp(220), dp(310))
        container.addView(snakeView, LinearLayout.LayoutParams(boardPx, boardPx).apply { topMargin = dp(10) })

        var isTurbo = false
        val speedBtn = button(getString(R.string.snake_speed_normal)) {}
        speedBtn.setOnClickListener {
            isTurbo = !isTurbo
            snakeView.setTurboSpeed(isTurbo)
            speedBtn.text = getString(if (isTurbo) R.string.snake_speed_turbo else R.string.snake_speed_normal)
        }
        val pauseBtn = button(getString(R.string.snake_pause_btn)) { snakeView.togglePause() }
        val restartBtn = button(getString(R.string.snake_restart_btn)) { snakeView.startNewGame() }

        val topControls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(pauseBtn, restartBtn, speedBtn).forEachIndexed { idx, b ->
            b.minHeight = dp(46)
            topControls.addView(
                b,
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (idx < 2) marginEnd = dp(6)
                },
            )
        }
        container.addView(topControls, top(10))

        // Big senior-friendly D-Pad arrows beneath board
        val dpadRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val upBtn = button("⬆️") { snakeView.steer(SnakeGameView.Direction.UP) }.apply {
            textSize = 20f
            minHeight = dp(54)
        }
        dpadRow1.addView(upBtn, LinearLayout.LayoutParams(dp(92), dp(54)))
        container.addView(dpadRow1, top(10))

        val dpadRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val leftBtn = button("⬅️") { snakeView.steer(SnakeGameView.Direction.LEFT) }.apply { textSize = 20f }
        val downBtn = button("⬇️") { snakeView.steer(SnakeGameView.Direction.DOWN) }.apply { textSize = 20f }
        val rightBtn = button("➡️") { snakeView.steer(SnakeGameView.Direction.RIGHT) }.apply { textSize = 20f }
        listOf(leftBtn, downBtn, rightBtn).forEachIndexed { idx, btn ->
            btn.minHeight = dp(54)
            dpadRow2.addView(
                btn,
                LinearLayout.LayoutParams(dp(92), dp(54)).apply {
                    if (idx < 2) marginEnd = dp(8)
                },
            )
        }
        container.addView(dpadRow2, top(6))

        snakeView.onStateChanged = { s, best, _, _ ->
            scoreLabel.text = getString(R.string.snake_score_line, s, best)
        }
        snakeView.startNewGame()

        val scroll = ScrollView(this).apply { addView(container, ViewGroup.LayoutParams(-1, -2)) }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.snake_dialog_title)
            .setView(scroll)
            .setNegativeButton(R.string.close_btn, null)
            .create()
        dialog.setOnDismissListener { snakeView.stopLoop() }
        dialog.show()
    }

    private fun openWorldClockDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        container.addView(label(getString(R.string.world_clock_subtitle), 12.5f, R.color.sweep_muted, false))
        val rowsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        container.addView(rowsContainer, top(10))

        val isPt = Locale.getDefault().language.startsWith("pt", ignoreCase = true)

        fun renderClocks() {
            rowsContainer.removeAllViews()
            val nowMs = System.currentTimeMillis()
            GlobalHubTools.COUNTRIES.forEach { country ->
                val snap = GlobalHubTools.formatCountryClock(country, nowMillis = nowMs)
                val dayTag = when (snap.dayDelta) {
                    1 -> getString(R.string.world_clock_day_next)
                    -1 -> getString(R.string.world_clock_day_prev)
                    else -> getString(R.string.world_clock_day_same)
                }
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    background = roundedBackground(Color.parseColor("#111D30"), dp(12))
                }
                val leftCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                val countryTitle = "${country.flag} ${if (isPt) country.namePt else country.nameEn}"
                leftCol.addView(label(countryTitle, 14f, R.color.sweep_text, true))
                leftCol.addView(
                    label("${country.cityName} · ${snap.utcOffsetLabel} (${GlobalHubTools.formatOffsetDelta(snap.deltaMinutes)})", 11.5f, R.color.sweep_muted, false),
                    top(2),
                )

                val rightCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.END
                }
                val timeLabel = TextView(this).apply {
                    text = snap.time24h
                    textSize = 18f
                    typeface = Typeface.MONOSPACE
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.parseColor("#5DF5D3"))
                }
                rightCol.addView(timeLabel)
                rightCol.addView(label("$dayTag · ${snap.dateLabel}", 10.5f, R.color.sweep_muted, false))

                row.addView(leftCol, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
                row.addView(rightCol, LinearLayout.LayoutParams(-2, -2))
                rowsContainer.addView(row, top(8))
            }
        }
        renderClocks()

        var tickJob: Job? = null
        val scroll = ScrollView(this).apply { addView(container, ViewGroup.LayoutParams(-1, -2)) }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.world_clock_title)
            .setView(scroll)
            .setNegativeButton(R.string.close_btn, null)
            .create()
        dialog.setOnShowListener {
            tickJob = lifecycleScope.launch {
                while (isActive) {
                    delay(1000L)
                    renderClocks()
                }
            }
        }
        dialog.setOnDismissListener { tickJob?.cancel() }
        dialog.show()
    }

    private fun openCurrencyConverterDialog() {
        val (initialRates, initialStamp) = GlobalHubTools.loadCachedRates(this)
        var currentRates = initialRates
        var currentStamp = initialStamp

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val statusLabel = label(
            if (currentStamp != null) getString(R.string.currency_status_live, currentStamp) else getString(R.string.currency_status_offline),
            12f,
            R.color.sweep_primary_glow,
            true,
        )
        container.addView(statusLabel)

        val amountInput = EditText(this).apply {
            hint = getString(R.string.currency_amount_hint)
            setText("100")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 18f
            setTextColor(color(R.color.sweep_text))
            setHintTextColor(color(R.color.sweep_muted))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedBackground(Color.parseColor("#141C2E"), dp(10))
        }
        container.addView(amountInput, top(10))

        val codes = GlobalHubTools.SUPPORTED_CURRENCIES
        val fromSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, codes)
            setSelection(codes.indexOf("USD").coerceAtLeast(0))
        }
        val toSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, codes)
            setSelection(codes.indexOf("BRL").coerceAtLeast(0))
        }

        val selectorsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val swapBtn = button(getString(R.string.currency_swap_btn)) {
            val fromPos = fromSpinner.selectedItemPosition
            fromSpinner.setSelection(toSpinner.selectedItemPosition)
            toSpinner.setSelection(fromPos)
        }.apply { minHeight = dp(46) }

        selectorsRow.addView(fromSpinner, LinearLayout.LayoutParams(0, -2, 1f))
        selectorsRow.addView(swapBtn, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6); marginEnd = dp(6) })
        selectorsRow.addView(toSpinner, LinearLayout.LayoutParams(0, -2, 1f))
        container.addView(selectorsRow, top(10))

        val resultBox = TextView(this).apply {
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFD54F"))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = roundedBackground(Color.parseColor("#1F1808"), dp(12))
        }
        val quickCompareText = label("", 12.5f, R.color.sweep_text, false).apply {
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedBackground(Color.parseColor("#111D30"), dp(10))
        }

        fun recalculate() {
            val amount = amountInput.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            val fromCode = codes.getOrElse(fromSpinner.selectedItemPosition) { "USD" }
            val toCode = codes.getOrElse(toSpinner.selectedItemPosition) { "BRL" }
            val converted = GlobalHubTools.convertCurrency(amount, fromCode, toCode, currentRates) ?: 0.0
            resultBox.text = String.format(Locale.US, "%.2f %s = %.2f %s", amount, fromCode, converted, toCode)

            val quickHeader = getString(R.string.currency_quick_examples, String.format(Locale.US, "%.2f", amount), fromCode)
            val lines = listOf("USD", "BRL", "EUR", "INR", "AED", "RUB", "JPY", "GBP")
                .filter { it != fromCode }
                .joinToString("\n") { target ->
                    val v = GlobalHubTools.convertCurrency(amount, fromCode, target, currentRates) ?: 0.0
                    "• $target: ${String.format(Locale.US, "%.2f", v)}"
                }
            quickCompareText.text = "$quickHeader\n$lines"
        }

        val itemWatcher = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = recalculate()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        fromSpinner.onItemSelectedListener = itemWatcher
        toSpinner.onItemSelectedListener = itemWatcher
        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = recalculate()
            override fun afterTextChanged(s: Editable?) {}
        })

        val refreshBtn = button(getString(R.string.currency_refresh_btn)) {
            lifecycleScope.launch {
                val (freshRates, freshStamp) = GlobalHubTools.fetchLiveUsdRatesIfOnline(this@MainActivity)
                currentRates = freshRates
                currentStamp = freshStamp
                statusLabel.text = if (freshStamp != null) {
                    getString(R.string.currency_status_live, freshStamp)
                } else {
                    getString(R.string.currency_status_offline)
                }
                recalculate()
            }
        }

        container.addView(resultBox, top(12))
        container.addView(refreshBtn, top(10))
        container.addView(quickCompareText, top(10))
        recalculate()

        val scroll = ScrollView(this).apply { addView(container, ViewGroup.LayoutParams(-1, -2)) }
        AlertDialog.Builder(this)
            .setTitle(R.string.currency_title)
            .setView(scroll)
            .setNegativeButton(R.string.close_btn, null)
            .show()
    }

    private fun openCountryTravelAndPlacesDialog() {
        val isPt = Locale.getDefault().language.startsWith("pt", ignoreCase = true)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        container.addView(label(getString(R.string.travel_country_label), 13f, R.color.sweep_primary_glow, true))

        val countryLabels = GlobalHubTools.COUNTRIES.map { c ->
            "${c.flag} ${if (isPt) c.namePt else c.nameEn} (${c.cityName})"
        }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, countryLabels)
        }
        container.addView(spinner, top(8))

        val weatherText = label(getString(R.string.travel_weather_loading), 14f, R.color.sweep_text, true).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedBackground(Color.parseColor("#112138"), dp(12))
        }
        val seasonHeader = label(getString(R.string.travel_season_label), 12.5f, R.color.sweep_primary_glow, true)
        val seasonBody = label("", 13f, R.color.sweep_text, false)
        val landmarksHeader = label(getString(R.string.travel_landmarks_label), 12.5f, R.color.sweep_primary_glow, true)
        val landmarksBody = label("", 13f, R.color.sweep_text, false)

        container.addView(weatherText, top(12))
        container.addView(seasonHeader, top(12))
        container.addView(seasonBody, top(4))
        container.addView(landmarksHeader, top(12))
        container.addView(landmarksBody, top(4))

        fun selectedCountry(): CountryHubInfo =
            GlobalHubTools.COUNTRIES.getOrElse(spinner.selectedItemPosition) { GlobalHubTools.COUNTRIES.first() }

        fun openMapsSafe(query: String) {
            try {
                startActivity(GlobalHubTools.buildMapsIntent(this, query))
            } catch (_: Exception) {
                Toast.makeText(this, R.string.travel_no_maps_toast, Toast.LENGTH_SHORT).show()
            }
        }

        val nearbyRestaurantsBtn = button(getString(R.string.travel_btn_restaurants)) {
            openMapsSafe(if (isPt) "melhores restaurantes avaliados perto de mim" else "best rated restaurants near me")
        }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2A1D08"))
            setTextColor(Color.parseColor("#FFD54F"))
        }
        val countryRestaurantsBtn = button(getString(R.string.travel_btn_country_food)) {
            val c = selectedCountry()
            openMapsSafe("top rated traditional restaurants ${c.cityName} ${c.nameEn}")
        }
        val attractionsBtn = button(getString(R.string.travel_btn_attractions)) {
            val c = selectedCountry()
            openMapsSafe("tourist attractions ${c.cityName} ${c.nameEn}")
        }
        container.addView(nearbyRestaurantsBtn, top(14))
        container.addView(countryRestaurantsBtn, top(8))
        container.addView(attractionsBtn, top(8))

        var weatherFetchJob: Job? = null
        fun updateCountryDetails(country: CountryHubInfo) {
            seasonBody.text = if (isPt) country.seasonSummaryPt else country.seasonSummaryEn
            val marks = if (isPt) country.landmarksPt else country.landmarksEn
            landmarksBody.text = marks.joinToString("\n") { "• $it" }

            val cached = GlobalHubTools.loadCachedWeather(this, country.id)
            weatherText.text = if (cached != null) {
                getString(
                    R.string.travel_weather_cached,
                    cached.observedLabel,
                    cached.tempC,
                    GlobalHubTools.describeWmoWeatherCode(cached.weatherCode, isPt),
                )
            } else {
                getString(R.string.travel_weather_loading)
            }

            weatherFetchJob?.cancel()
            weatherFetchJob = lifecycleScope.launch {
                val live = GlobalHubTools.fetchLiveCountryWeather(this@MainActivity, country)
                weatherText.text = when {
                    live != null && !live.isCachedOffline -> getString(
                        R.string.travel_weather_live,
                        live.observedLabel,
                        live.tempC,
                        GlobalHubTools.describeWmoWeatherCode(live.weatherCode, isPt),
                        live.windKmh,
                    )
                    live != null -> getString(
                        R.string.travel_weather_cached,
                        live.observedLabel,
                        live.tempC,
                        GlobalHubTools.describeWmoWeatherCode(live.weatherCode, isPt),
                    )
                    else -> getString(R.string.travel_weather_offline)
                }
            }
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCountryDetails(selectedCountry())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        updateCountryDetails(selectedCountry())

        val scroll = ScrollView(this).apply { addView(container, ViewGroup.LayoutParams(-1, -2)) }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.travel_title)
            .setView(scroll)
            .setNegativeButton(R.string.close_btn, null)
            .create()
        dialog.setOnDismissListener { weatherFetchJob?.cancel() }
        dialog.show()
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

        box.addView(label(getString(R.string.pro_billing_terms_disclosure), 11f, R.color.sweep_muted, false), top(12))

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
            stylePlanPurchaseButton(button, product?.isAvailable == true)
        }

        val annualLabel = if (annual != null && annual.formattedPrice.isNotBlank()) {
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
        } else if (annual != null && !annual.isAvailable) {
            getString(R.string.plan_annual_unavailable)
        } else {
            null
        }
        annualLabel?.let {
            planAnnualButton.text = it
            homePlanAnnualButton.text = it
        }
        val monthlyLabel = if (monthly != null && monthly.formattedPrice.isNotBlank()) {
            getString(R.string.plan_monthly_with_price, monthly.formattedPrice)
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
        if (hasAnyMediaPermission()) viewModel.startSession() else requestMediaPermissions()
    }

    private fun checkAndShowFirstRunOnboarding() {
        val prefs = getSharedPreferences("midiastore_prefs", MODE_PRIVATE)
        val shown = prefs.getBoolean("onboarding_shown_v1", false)
        if (!shown) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.onboarding_welcome_title))
                .setMessage(getString(R.string.onboarding_welcome_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.onboarding_start_button)) { dialog, _ ->
                    prefs.edit().putBoolean("onboarding_shown_v1", true).apply()
                    dialog.dismiss()
                    startOneTapCleanup()
                }
                .show()
        }
    }

    private fun startOneTapCleanup() {
        if (!hasAnyMediaPermission()) {
            pendingOutcomeMission = DesiredOutcomeMission.FREE_MAX_SPACE_NOW
            openInteractiveDialogOnMissionFinish = true
            heroOrbButton.isEnabled = true
            heroOrbButton.text = getString(R.string.home_cleanup_primary)
            requestMediaPermissions()
            return
        }
        homeCleanupCandidateCount = 0
        homeInteractiveRecords = emptyList()
        homeInteractiveFallback = false
        openInteractiveDialogOnMissionFinish = true
        heroOrbButton.isEnabled = false
        heroOrbButton.text = getString(R.string.home_cleanup_scanning_button)
        heroOrbSubtitle.text = getString(R.string.home_cleanup_scanning)
        showHomeCleanupMessage(getString(R.string.home_cleanup_scanning))
        runOutcomeMission(DesiredOutcomeMission.FREE_MAX_SPACE_NOW)
    }

    private fun showHomeCleanupMessage(message: String) {
        if (::homeCleanupResult.isInitialized) {
            homeCleanupResult.text = message
            homeCleanupResult.visibility = View.VISIBLE
        }
    }

    private fun onHomeCleanupButtonClicked() {
        if (!heroOrbButton.isEnabled || viewModel.state.value.missionProgress?.isRunning == true) {
            Toast.makeText(this, R.string.cleanup_scan_in_progress_toast, Toast.LENGTH_SHORT).show()
            return
        }
        val selectedNow = viewModel.state.value.selection.records
        val displayRecords = when {
            homeInteractiveRecords.isNotEmpty() -> homeInteractiveRecords
            selectedNow.isNotEmpty() -> selectedNow
            else -> emptyList()
        }
        if (displayRecords.isNotEmpty()) {
            openInteractiveCleanupReviewDialog(
                records = displayRecords,
                isFallbackList = selectedNow.isEmpty() && homeInteractiveFallback,
                mission = DesiredOutcomeMission.FREE_MAX_SPACE_NOW,
                coverageComplete = viewModel.state.value.missionProgress?.coverageComplete ?: true,
            )
        } else {
            startOneTapCleanup()
        }
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
        if (records.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.cleanup_dialog_empty_title)
                .setMessage(R.string.cleanup_dialog_empty_body)
                .setPositiveButton(R.string.quick_clean_whatsapp) { _, _ -> openWhatsAppCleanerFlow() }
                .setNeutralButton(R.string.nav_files) { _, _ -> switchTab(AppTab.FILES) }
                .setNegativeButton(R.string.permission_continue) { _, _ -> requestMediaPermissions() }
                .show()
            return
        }

        val cappedRecords = records.filter { it.isSelectable && !it.isProtected }.take(200)
        if (cappedRecords.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.cleanup_dialog_empty_title)
                .setMessage(R.string.cleanup_dialog_empty_body)
                .setPositiveButton(R.string.nav_files) { _, _ -> switchTab(AppTab.FILES) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            return
        }

        val currentlySelectedUris = viewModel.state.value.selection.records.map { it.uri }.toSet()
        val initialChecked = BooleanArray(cappedRecords.size) { index ->
            val rec = cappedRecords[index]
            when {
                rec.uri in currentlySelectedUris -> true
                !isFallbackList && mission == DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES && coverageComplete -> true
                !isFallbackList && currentlySelectedUris.isEmpty() -> true
                else -> false
            }
        }

        val headerSubtitle = customSubtitle ?: getString(
            if (isFallbackList) R.string.cleanup_dialog_subtitle_fallback
            else R.string.cleanup_dialog_subtitle_candidates,
        )
        val (dialog, root) = createAppleBottomSheet(
            title = getString(R.string.cleanup_sheet_title),
            subtitle = headerSubtitle,
        )

        val summaryLabel = label("", 13f, R.color.sweep_primary_glow, true)
        root.addView(summaryLabel, top(10))

        lateinit var toggleAllBtn: MaterialButton
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
        val deleteActionBtn = button(getString(R.string.permanent_delete_button)) {
            val chosen = selectedFromDialog()
            if (chosen.isEmpty()) {
                Toast.makeText(this@MainActivity, R.string.cleanup_dialog_select_none_toast, Toast.LENGTH_SHORT).show()
                return@button
            }
            dialog.dismiss()
            viewModel.prepareExternalReviewSelection(chosen)
            requestPermanentDeleteSelected(chosen)
        }.apply {
            minHeight = dp(50)
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_danger))
        }
        root.addView(deleteActionBtn, top(12))

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
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                color(if (supportsTrash) R.color.google_blue_primary else R.color.sweep_surface_alt),
            )
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

    private fun readClipboardTextSafely(): String? {
        return try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager ?: return null
            val clip = clipboard.primaryClip ?: return null
            if (clip.itemCount <= 0) return null
            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i) ?: continue
                val direct = item.text?.toString()?.trim()
                if (!direct.isNullOrBlank()) return direct
                val coerced = item.coerceToText(this)?.toString()?.trim()
                if (!coerced.isNullOrBlank()) return coerced
                val uriText = item.uri?.toString()?.trim()
                if (!uriText.isNullOrBlank()) return uriText
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun openWhatsAppCleanerFlow() {
        if (!hasAnyMediaPermission()) {
            pendingWhatsAppCleanerAfterPermission = true
            requestMediaPermissions()
            return
        }
        lifecycleScope.launch {
            val scanDialog = AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.whatsapp_cleaner_title)
                .setMessage(R.string.wa_scanning_message)
                .setCancelable(true)
                .create()
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
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.whatsapp_cleaner_title)
                    .setMessage(R.string.whatsapp_cleaner_empty)
                    .setPositiveButton(getString(R.string.quick_find_large)) { _, _ ->
                        startOneTapCleanup()
                    }
                    .setNeutralButton(getString(R.string.nav_files)) { _, _ ->
                        switchTab(AppTab.FILES)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                return@launch
            }

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

            fun refreshWaSummary() {
                val selectedRecords = orderedCategories.indices
                    .filter { checked[it] }
                    .flatMap { idx -> mediaRecordsByCategory[orderedCategories[idx].first].orEmpty() }
                val totalBytes = selectedRecords.sumOf { it.sizeBytes.coerceAtLeast(0L) }
                waSummary.text = getString(
                    R.string.cleanup_dialog_selected_summary,
                    selectedRecords.size,
                    mediaRecordsByCategory.values.sumOf { it.size },
                    Formatter.formatFileSize(this@MainActivity, totalBytes),
                )
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
            val waDeleteBtn = button(getString(R.string.permanent_delete_button)) {
                val chosenRecords = chosenWaRecords()
                if (chosenRecords.isEmpty()) {
                    Toast.makeText(this@MainActivity, R.string.wa_select_category_toast, Toast.LENGTH_SHORT).show()
                    return@button
                }
                waSheet.dismiss()
                viewModel.prepareExternalReviewSelection(chosenRecords)
                requestPermanentDeleteSelected(chosenRecords)
            }.apply {
                minHeight = dp(50)
                setTextColor(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_danger))
            }
            waRoot.addView(waDeleteBtn, top(12))

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
                setTextColor(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    color(if (supportsTrash) R.color.google_blue_primary else R.color.sweep_surface_alt),
                )
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
        if (!hasAnyMediaPermission()) {
            pendingBlurScanAfterPermission = true
            requestMediaPermissions()
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
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.blur_scan_empty, result.photosAnalyzed),
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            }
            openInteractiveCleanupReviewDialog(
                records = result.candidates.map { it.record },
                isFallbackList = false,
                mission = null,
                coverageComplete = !result.capReached,
                customSubtitle = getString(R.string.blur_sheet_subtitle),
            )
        }
    }

    private fun handleIncomingSendIntent(incomingIntent: Intent?) {
        val action = incomingIntent?.action ?: return
        if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            val sharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank() && (sharedText.startsWith("http://") || sharedText.startsWith("https://") || sharedText.contains("http"))) {
                val url = Regex("""https?://[^\s]+""").find(sharedText)?.value ?: sharedText
                val actions = arrayOf(
                    getString(R.string.incoming_link_download),
                    getString(R.string.incoming_link_qr)
                )
                AlertDialog.Builder(this)
                    .setTitle(R.string.incoming_link_title)
                    .setItems(actions) { _, which ->
                        if (which == 0) openVideoDownloaderStudio(url)
                        else openQrCodeGeneratorStudio(url)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                return
            }
            if (action == Intent.ACTION_SEND_MULTIPLE) {
                val multipleUris: List<Uri> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    incomingIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
                } else {
                    @Suppress("DEPRECATION")
                    incomingIntent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
                }
                if (multipleUris.isNotEmpty()) {
                    Toast.makeText(this, R.string.media_studio_incoming_media, Toast.LENGTH_SHORT).show()
                    val firstUri = multipleUris.first()
                    val options = arrayOf(
                        getString(R.string.incoming_convert_pdf, multipleUris.size),
                        getString(R.string.incoming_filter_first),
                        getString(R.string.incoming_exif_first)
                    )
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.incoming_medias_title, multipleUris.size))
                        .setItems(options) { _, which ->
                            when (which) {
                                0 -> processPhotosToPdf(multipleUris)
                                1 -> openInteractivePhotoFilterEditor(firstUri)
                                2 -> openInteractiveExifCleaner(firstUri)
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                    return
                }
            }
            val sharedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            Toast.makeText(this, R.string.media_studio_incoming_media, Toast.LENGTH_SHORT).show()
            showMediaStudioDialog(sharedUri, sharedText)
        }
    }

    private fun showMediaStudioDialog(initialUri: Uri? = null, initialText: String? = null) {
        if (initialUri != null) {
            val mimeType = contentResolver.getType(initialUri) ?: ""
            if (mimeType.startsWith("video/")) {
                openVideoAudioStudio(initialUri)
                return
            } else if (mimeType.startsWith("image/")) {
                val options = arrayOf(
                    getString(R.string.incoming_single_filter),
                    getString(R.string.incoming_single_exif),
                    getString(R.string.incoming_single_pdf)
                )
                AlertDialog.Builder(this)
                    .setTitle(R.string.incoming_single_media_title)
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> openInteractivePhotoFilterEditor(initialUri)
                            1 -> openInteractiveExifCleaner(initialUri)
                            2 -> processPhotosToPdf(listOf(initialUri))
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                return
            }
        }

        val options = arrayOf(
            getString(R.string.studio_menu_wa),
            getString(R.string.studio_menu_dl),
            getString(R.string.media_studio_photo_filters),
            getString(R.string.media_studio_video_tools),
            getString(R.string.media_studio_exif_cleaner),
            getString(R.string.media_studio_qr_generator),
            getString(R.string.media_studio_photos_to_pdf)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.media_studio_title))
            .setItems(options) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> openWhatsAppCleanerFlow()
                    1 -> openVideoDownloaderStudio(initialText)
                    2 -> photoFilterPickerLauncher.launch("image/*")
                    3 -> videoAudioPickerLauncher.launch("video/*")
                    4 -> exifPickerLauncher.launch("image/*")
                    5 -> openQrCodeGeneratorStudio(initialText)
                    6 -> photosToPdfPickerLauncher.launch("image/*")
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openVideoDownloaderStudio(initialUrl: String? = null) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val title = TextView(this).apply {
            text = getString(R.string.dl_dialog_title)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.sweep_text))
        }
        container.addView(title)

        val subtitle = TextView(this).apply {
            text = getString(R.string.dl_dialog_subtitle)
            textSize = 12f
            setTextColor(color(R.color.sweep_muted))
        }
        container.addView(subtitle, top(4))

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(this).apply {
            hint = getString(R.string.dl_url_hint)
            setText(initialUrl ?: "")
            setTextColor(color(R.color.sweep_text))
            setHintTextColor(color(R.color.sweep_muted))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(0xFF141C2E.toInt())
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), 0xFF2A3A5A.toInt())
            }
        }
        val pasteBtn = button(getString(R.string.dl_paste_btn)) {
            val clipText = readClipboardTextSafely()
            if (!clipText.isNullOrBlank()) {
                val urlMatch = Regex("""https?://[^\s"'<>]+""").find(clipText)?.value ?: clipText
                input.setText(urlMatch)
                input.setSelection(input.text.length)
                Toast.makeText(this@MainActivity, R.string.dl_pasted_toast, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, R.string.dl_clipboard_empty_toast, Toast.LENGTH_SHORT).show()
            }
        }.apply {
            textSize = 12f
            minHeight = dp(48)
        }
        inputRow.addView(input, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
        inputRow.addView(pasteBtn, LinearLayout.LayoutParams(-2, dp(48)))
        container.addView(inputRow, top(10))

        val presetsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val samples = listOf(
            getString(R.string.dl_sample_1) to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            getString(R.string.dl_sample_2) to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
        )
        for ((label, urlSample) in samples) {
            val btn = button(label) {
                input.setText(urlSample)
                input.setSelection(input.text.length)
            }.apply {
                textSize = 11f
                minHeight = dp(48)
            }
            presetsRow.addView(btn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        }
        container.addView(presetsRow, top(8))

        val progressText = TextView(this).apply {
            text = getString(R.string.dl_ready_status)
            textSize = 11f
            setTextColor(color(R.color.sweep_muted))
            visibility = View.GONE
        }
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            visibility = View.GONE
        }
        container.addView(progressText, top(12))
        container.addView(progressBar, top(4))

        var downloadJob: Job? = null

        val scrollWrapper = ScrollView(this).apply {
            addView(container, ViewGroup.LayoutParams(-1, -2))
        }

        val dialog = AlertDialog.Builder(this)
            .setView(scrollWrapper)
            .setPositiveButton(R.string.dl_download_btn, null)
            .setNegativeButton(R.string.close_btn, null)
            .create()

        dialog.setOnDismissListener {
            downloadJob?.cancel()
        }

        dialog.setOnShowListener {
            val downloadBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            downloadBtn.setOnClickListener {
                val urlToDownload = input.text.toString().trim()
                val host = runCatching { java.net.URL(urlToDownload).host.orEmpty() }.getOrDefault("")
                if (
                    urlToDownload.isBlank() ||
                    (!urlToDownload.startsWith("http://") && !urlToDownload.startsWith("https://")) ||
                    host.isBlank() || !host.contains('.')
                ) {
                    Toast.makeText(
                        this@MainActivity,
                        R.string.dl_invalid_url_toast,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                downloadBtn.isEnabled = false
                progressText.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
                progressText.text = getString(R.string.dl_connecting_status)

                downloadJob = lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        MediaStudioTools.resolveAndDownloadVideo(
                            context = this@MainActivity,
                            rawUrl = urlToDownload,
                            shouldCancel = { !isActive }
                        ) { percent, bytesRead, totalBytes ->
                            runOnUiThread {
                                if (percent >= 0) {
                                    progressBar.isIndeterminate = false
                                    progressBar.progress = percent
                                    val currentMb = String.format("%.2f", bytesRead / 1048576.0)
                                    val totalMb = String.format("%.2f", totalBytes / 1048576.0)
                                    progressText.text = getString(R.string.dl_progress_pct, percent, currentMb, totalMb)
                                } else {
                                    progressBar.isIndeterminate = true
                                    val currentMb = String.format("%.2f", bytesRead / 1048576.0)
                                    progressText.text = getString(R.string.dl_progress_mb, currentMb)
                                }
                            }
                        }
                    }

                    downloadBtn.isEnabled = true
                    if (result.success && result.savedUri != null) {
                        dialog.dismiss()
                        val sizeStr = Formatter.formatFileSize(this@MainActivity, result.bytesDownloaded)
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(R.string.dl_saved_title)
                            .setMessage(getString(R.string.dl_saved_message, result.fileName, sizeStr))
                            .setPositiveButton(R.string.dl_watch_btn) { _, _ ->
                                try {
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(result.savedUri, "video/mp4")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(viewIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(this@MainActivity, R.string.dl_no_player_toast, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNeutralButton(R.string.share_generic_btn) { _, _ ->
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, result.savedUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(Intent.createChooser(shareIntent, getString(R.string.dl_share_chooser)))
                                } catch (e: Exception) {
                                    Toast.makeText(this@MainActivity, R.string.dl_share_error_toast, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton(android.R.string.ok, null)
                            .show()
                    } else {
                        val errMsg = result.errorMessage ?: getString(R.string.dl_error_fallback)
                        progressText.text = getString(R.string.dl_error_prefix, errMsg)
                        Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun getFilterColorMatrix(filter: MediaStudioTools.PhotoFilter): ColorMatrix {
        return MediaStudioTools.buildFilterMatrix(filter)
    }

    private fun openInteractivePhotoFilterEditor(uri: Uri) {
        lifecycleScope.launch {
            val progress = AlertDialog.Builder(this@MainActivity)
                .setMessage(R.string.filter_loading_image)
                .setCancelable(false)
                .show()

            val sourceBitmap = withContext(Dispatchers.IO) {
                MediaStudioTools.loadSampledBitmap(this@MainActivity, uri, 1280)
            }
            progress.dismiss()

            if (sourceBitmap == null) {
                Toast.makeText(this@MainActivity, R.string.filter_load_error, Toast.LENGTH_SHORT).show()
                return@launch
            }

            var currentFilter = MediaStudioTools.PhotoFilter.NORMAL
            var currentBrightness = 0f
            var currentContrast = 1.0f
            var currentSaturation = 1.0f
            var currentRotation = 0

            val rootLayout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }

            val previewImage = ImageView(this@MainActivity).apply {
                setImageBitmap(sourceBitmap)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.parseColor("#121212"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(240)
                ).apply {
                    bottomMargin = dp(12)
                }
            }
            rootLayout.addView(previewImage)

            fun updatePreview() {
                previewImage.colorFilter = ColorMatrixColorFilter(
                    MediaStudioTools.buildCompositeColorMatrix(
                        currentFilter,
                        currentBrightness,
                        currentContrast,
                        currentSaturation
                    )
                )
                previewImage.rotation = currentRotation.toFloat()
            }

            val filtersTitle = TextView(this@MainActivity).apply {
                text = getString(R.string.filter_cinema_title)
                textSize = 12f
                setTextColor(color(R.color.sweep_muted))
            }
            rootLayout.addView(filtersTitle, top(4))

            val scroll = HorizontalScrollView(this@MainActivity).apply {
                isHorizontalScrollBarEnabled = false
            }
            val buttonsRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            for (filter in MediaStudioTools.PhotoFilter.values()) {
                val btn = button(filter.displayName) {
                    currentFilter = filter
                    updatePreview()
                }.apply {
                    textSize = 11f
                    minHeight = dp(48)
                }
                buttonsRow.addView(btn, LinearLayout.LayoutParams(-2, dp(48)).apply { marginEnd = dp(6) })
            }
            scroll.addView(buttonsRow)
            rootLayout.addView(scroll, top(4))

            val toolsTitle = TextView(this@MainActivity).apply {
                text = getString(R.string.filter_tools_title)
                textSize = 12f
                setTextColor(color(R.color.sweep_muted))
            }
            rootLayout.addView(toolsTitle, top(10))

            val toolsScroll = HorizontalScrollView(this@MainActivity).apply {
                isHorizontalScrollBarEnabled = false
            }
            val toolsRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val rotateBtn = button(getString(R.string.filter_rotate_btn)) {
                currentRotation = (currentRotation + 90) % 360
                updatePreview()
            }.apply {
                textSize = 11f
                minHeight = dp(48)
            }
            val brightBtn = button(getString(R.string.filter_brightness_btn)) {
                currentBrightness = if (currentBrightness < 40f) currentBrightness + 20f else -20f
                updatePreview()
                Toast.makeText(this@MainActivity, getString(R.string.filter_brightness_toast, currentBrightness.toInt()), Toast.LENGTH_SHORT).show()
            }.apply {
                textSize = 11f
                minHeight = dp(48)
            }
            val contrastBtn = button(getString(R.string.filter_contrast_btn)) {
                currentContrast = if (currentContrast < 1.4f) currentContrast + 0.2f else 0.8f
                updatePreview()
                Toast.makeText(this@MainActivity, getString(R.string.filter_contrast_toast, String.format("%.1f", currentContrast)), Toast.LENGTH_SHORT).show()
            }.apply {
                textSize = 11f
                minHeight = dp(48)
            }
            val saturationBtn = button(getString(R.string.filter_saturation_btn)) {
                currentSaturation = if (currentSaturation < 1.9f) ((currentSaturation + 0.5f).coerceAtMost(2.0f)) else 0.0f
                updatePreview()
                Toast.makeText(this@MainActivity, getString(R.string.filter_saturation_toast, String.format("%.1f", currentSaturation)), Toast.LENGTH_SHORT).show()
            }.apply {
                textSize = 11f
                minHeight = dp(48)
            }
            val resetBtn = button(getString(R.string.filter_reset_btn)) {
                currentFilter = MediaStudioTools.PhotoFilter.NORMAL
                currentBrightness = 0f
                currentContrast = 1.0f
                currentSaturation = 1.0f
                currentRotation = 0
                updatePreview()
            }.apply {
                textSize = 11f
                minHeight = dp(48)
            }

            toolsRow.addView(rotateBtn, LinearLayout.LayoutParams(-2, dp(48)).apply { marginEnd = dp(6) })
            toolsRow.addView(brightBtn, LinearLayout.LayoutParams(-2, dp(48)).apply { marginEnd = dp(6) })
            toolsRow.addView(contrastBtn, LinearLayout.LayoutParams(-2, dp(48)).apply { marginEnd = dp(6) })
            toolsRow.addView(saturationBtn, LinearLayout.LayoutParams(-2, dp(48)).apply { marginEnd = dp(6) })
            toolsRow.addView(resetBtn, LinearLayout.LayoutParams(-2, dp(48)))
            toolsScroll.addView(toolsRow)
            rootLayout.addView(toolsScroll, top(4))

            val dialogScroll = ScrollView(this@MainActivity).apply {
                addView(rootLayout, ViewGroup.LayoutParams(-1, -2))
            }

            AlertDialog.Builder(this@MainActivity)
                .setTitle(getString(R.string.media_studio_photo_filters))
                .setView(dialogScroll)
                .setPositiveButton(R.string.filter_save_btn) { _, _ ->
                    lifecycleScope.launch {
                        val savingDialog = AlertDialog.Builder(this@MainActivity)
                            .setMessage(R.string.filter_saving_msg)
                            .setCancelable(false)
                            .show()

                        val savedUri = withContext(Dispatchers.IO) {
                            val exportSource = MediaStudioTools.loadSampledBitmap(this@MainActivity, uri, 4096) ?: sourceBitmap
                            var filtered: Bitmap? = null
                            try {
                                filtered = MediaStudioTools.applyTransformations(
                                    exportSource,
                                    filter = currentFilter,
                                    brightness = currentBrightness,
                                    contrast = currentContrast,
                                    saturation = currentSaturation,
                                    rotationDeg = currentRotation
                                )
                                MediaStudioTools.saveBitmapToGallery(this@MainActivity, filtered, "Midiastore_Studio_${currentFilter.name}")
                            } finally {
                                if (filtered != null && filtered !== exportSource) {
                                    filtered.recycle()
                                }
                                if (exportSource !== sourceBitmap) {
                                    exportSource.recycle()
                                }
                            }
                        }
                        savingDialog.dismiss()

                        if (savedUri != null) {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.filter_saved_toast,
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(this@MainActivity, R.string.filter_save_error_toast, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNeutralButton(R.string.filter_share_btn) { _, _ ->
                    lifecycleScope.launch {
                        val shareFile = withContext(Dispatchers.IO) {
                            val exportSource = MediaStudioTools.loadSampledBitmap(this@MainActivity, uri, 4096) ?: sourceBitmap
                            var filtered: Bitmap? = null
                            try {
                                filtered = MediaStudioTools.applyTransformations(
                                    exportSource,
                                    filter = currentFilter,
                                    brightness = currentBrightness,
                                    contrast = currentContrast,
                                    saturation = currentSaturation,
                                    rotationDeg = currentRotation
                                )
                                val file = File(cacheDir, "filtered_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(file).use { out ->
                                    filtered.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                }
                                file
                            } finally {
                                if (filtered != null && filtered !== exportSource) {
                                    filtered.recycle()
                                }
                                if (exportSource !== sourceBitmap) {
                                    exportSource.recycle()
                                }
                            }
                        }
                        try {
                            val chooserTitle = getString(R.string.filter_share_chooser)
                            val shareIntent = MediaStudioTools.getShareIntent(
                                this@MainActivity,
                                shareFile,
                                "image/jpeg",
                                chooserTitle
                            )
                            startActivity(Intent.createChooser(shareIntent, chooserTitle))
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, R.string.filter_share_error_toast, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton(R.string.close_btn, null)
                .show()
        }
    }

    private fun openVideoAudioStudio(uri: Uri) {
        lifecycleScope.launch {
            var fileName = "video.mp4"
            var fileSize = 0L
            withContext(Dispatchers.IO) {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                    }
                }
            }
            val sizeStr = Formatter.formatFileSize(this@MainActivity, fileSize)

            val actions = arrayOf(
                getString(R.string.audio_extract_action),
                getString(R.string.audio_diag_action)
            )
            AlertDialog.Builder(this@MainActivity)
                .setTitle(getString(R.string.audio_video_title, fileName))
                .setMessage(getString(R.string.audio_video_size_msg, sizeStr))
                .setItems(actions) { _, which ->
                    if (which == 0) {
                        processVideoAudioExtraction(uri, fileName)
                    } else {
                        processVideoOptimization(uri, fileName, fileSize)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun processVideoAudioExtraction(uri: Uri, fileName: String) {
        lifecycleScope.launch {
            val progress = AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.audio_extracting_title)
                .setMessage(getString(R.string.audio_extracting_msg, fileName))
                .setCancelable(false)
                .show()

            val extractionPair = withContext(Dispatchers.IO) {
                val cacheFile = MediaStudioTools.extractAudioToCache(this@MainActivity, uri)
                val savedUri = cacheFile?.let { MediaStudioTools.saveAudioToMusic(this@MainActivity, it, it.name) }
                cacheFile to savedUri
            }
            val extractedFile = extractionPair.first
            val savedMusicUri = extractionPair.second
            progress.dismiss()

            if (extractedFile != null && extractedFile.exists()) {
                val audioSize = Formatter.formatFileSize(this@MainActivity, extractedFile.length())
                val locationNote = getString(
                    if (savedMusicUri != null) R.string.audio_saved_note_music else R.string.audio_saved_note_cache
                )
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.audio_extracted_title)
                    .setMessage(getString(R.string.audio_extracted_msg, extractedFile.name, audioSize, locationNote))
                    .setPositiveButton(R.string.audio_listen_btn) { _, _ ->
                        try {
                            val audioUri = savedMusicUri ?: androidx.core.content.FileProvider.getUriForFile(
                                this@MainActivity,
                                "${packageName}.fileprovider",
                                extractedFile
                            )
                            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(audioUri, "audio/mp4")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(playIntent)
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, R.string.audio_no_player_toast, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNeutralButton(R.string.share_generic_btn) { _, _ ->
                        try {
                            val chooserTitle = getString(R.string.audio_share_chooser)
                            val shareIntent = MediaStudioTools.getShareIntent(
                                this@MainActivity,
                                extractedFile,
                                "audio/mp4",
                                chooserTitle
                            )
                            startActivity(Intent.createChooser(shareIntent, chooserTitle))
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, R.string.audio_share_error_toast, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(android.R.string.ok, null)
                    .show()
            } else {
                Toast.makeText(this@MainActivity, R.string.audio_extract_error_toast, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun processVideoOptimization(uri: Uri, fileName: String, fileSize: Long) {
        val originalStr = Formatter.formatFileSize(this, fileSize)

        AlertDialog.Builder(this)
            .setTitle(R.string.audio_diag_title)
            .setMessage(getString(R.string.audio_diag_msg, fileName, originalStr))
            .setPositiveButton(R.string.audio_diag_btn) { _, _ ->
                processVideoAudioExtraction(uri, fileName)
            }
            .setNegativeButton(R.string.close_btn, null)
            .show()
    }

    private fun openInteractiveExifCleaner(uri: Uri) {
        lifecycleScope.launch {
            val progress = AlertDialog.Builder(this@MainActivity)
                .setMessage(R.string.exif_analyzing_msg)
                .setCancelable(false)
                .show()

            val summary = withContext(Dispatchers.IO) {
                MediaStudioTools.readExifSummary(this@MainActivity, uri)
            }
            progress.dismiss()

            val msg = buildString {
                if (summary.hasGps) {
                    append(getString(R.string.exif_gps_alert_header))
                    if (summary.latitude != null && summary.longitude != null) {
                        append(getString(R.string.exif_gps_lat, String.format("%.5f", summary.latitude)))
                        append(getString(R.string.exif_gps_lon, String.format("%.5f", summary.longitude)))
                    }
                    append(getString(R.string.exif_gps_risk))
                } else {
                    append(getString(R.string.exif_no_gps))
                }
                if (!summary.camera.isNullOrBlank()) {
                    append(getString(R.string.exif_camera_line, summary.camera))
                }
                if (!summary.dateTime.isNullOrBlank()) {
                    append(getString(R.string.exif_date_line, summary.dateTime))
                }
                append(getString(R.string.exif_clean_prompt))
            }

            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.exif_dialog_title)
                .setMessage(msg)
                .setPositiveButton(R.string.exif_clean_save_btn) { _, _ ->
                    lifecycleScope.launch {
                        val cleanProgress = AlertDialog.Builder(this@MainActivity)
                            .setMessage(R.string.exif_cleaning_msg)
                            .setCancelable(false)
                            .show()

                        val savedUri = withContext(Dispatchers.IO) {
                            MediaStudioTools.cleanExifAndSave(this@MainActivity, uri)
                        }
                        cleanProgress.dismiss()

                        if (savedUri != null) {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.exif_saved_toast,
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(this@MainActivity, R.string.exif_save_error_toast, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun openQrCodeGeneratorStudio(initialText: String? = null) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(this).apply {
            hint = getString(R.string.qr_input_hint)
            setText(initialText ?: "https://midiastory-df6a0.web.app")
            setTextColor(color(R.color.sweep_text))
            setHintTextColor(color(R.color.sweep_muted))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(0xFF141C2E.toInt())
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), 0xFF2A3A5A.toInt())
            }
        }
        val pasteBtn = button(getString(R.string.dl_paste_btn)) {
            val clipText = readClipboardTextSafely()
            if (!clipText.isNullOrBlank()) {
                input.setText(clipText)
                input.setSelection(input.text.length)
                Toast.makeText(this@MainActivity, R.string.qr_text_pasted_toast, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, R.string.dl_clipboard_empty_toast, Toast.LENGTH_SHORT).show()
            }
        }.apply {
            textSize = 12f
            minHeight = dp(48)
        }
        inputRow.addView(input, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
        inputRow.addView(pasteBtn, LinearLayout.LayoutParams(-2, dp(48)))
        container.addView(inputRow)

        val captionInput = EditText(this).apply {
            hint = getString(R.string.qr_caption_hint)
            setText(if (initialText?.startsWith("WIFI:") == true) "WI-FI VIP" else "")
            setTextColor(color(R.color.sweep_text))
            setHintTextColor(color(R.color.sweep_muted))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(0xFF141C2E.toInt())
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), 0xFF2A3A5A.toInt())
            }
        }
        container.addView(captionInput, top(8))

        val chipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val presets = listOf(
            "PIX" to "00020126580014BR.GOV.BCB.PIX0136123e4567-e89b-12d3-a456-4266141740005204000053039865802BR5913Midiastore6008BRASILIA62070503***6304",
            "Link" to "https://midiastory-df6a0.web.app",
            "WhatsApp" to "https://wa.me/5511999999999",
            "Wi-Fi" to "WIFI:S:Midiastore_WiFi;T:WPA;P:internet123;;"
        )
        for ((name, sample) in presets) {
            val btn = button(name) {
                input.setText(sample)
                input.setSelection(input.text.length)
            }.apply {
                textSize = 11f
                minHeight = dp(44)
            }
            chipsRow.addView(btn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        }
        container.addView(chipsRow, top(8))

        var selectedTheme = if (hasEffectiveProAccess()) {
            MediaStudioTools.QrVisualTheme.ROYAL_GOLD
        } else {
            MediaStudioTools.QrVisualTheme.CYBER_MINT
        }
        var currentBitmap = MediaStudioTools.generateDesignerQrBitmap(
            input.text.toString().ifBlank { "Midiastore" },
            selectedTheme,
            captionInput.text?.toString(),
            720,
        )

        val qrImageView = ImageView(this).apply {
            setImageBitmap(currentBitmap)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(dp(240), dp(240)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
        }

        val themeButtons = mutableListOf<Pair<MediaStudioTools.QrVisualTheme, MaterialButton>>()

        fun regenerateQrPreview() {
            val content = input.text?.toString()?.ifBlank { "Midiastore" } ?: "Midiastore"
            val caption = captionInput.text?.toString()
            currentBitmap = MediaStudioTools.generateDesignerQrBitmap(content, selectedTheme, caption, 720)
            qrImageView.setImageBitmap(currentBitmap)
            themeButtons.forEach { (theme, btn) ->
                val isSelected = theme == selectedTheme
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (isSelected) Color.parseColor("#FFB300") else color(R.color.sweep_surface_alt),
                )
                btn.setTextColor(
                    if (isSelected) Color.parseColor("#120D03") else color(R.color.sweep_text),
                )
            }
        }

        val themesScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val themesRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val themes = listOf(
            MediaStudioTools.QrVisualTheme.ROYAL_GOLD to R.string.qr_style_gold,
            MediaStudioTools.QrVisualTheme.CYBER_MINT to R.string.qr_style_mint,
            MediaStudioTools.QrVisualTheme.STUDIO_NEON to R.string.qr_style_neon,
            MediaStudioTools.QrVisualTheme.OCEAN_BLUE to R.string.qr_style_ocean,
            MediaStudioTools.QrVisualTheme.CLASSIC_DARK to R.string.qr_style_classic,
        )
        themes.forEach { (theme, labelRes) ->
            lateinit var themeBtn: MaterialButton
            themeBtn = button(getString(labelRes)) {
                selectedTheme = theme
                regenerateQrPreview()
            }.apply {
                textSize = 11f
                minHeight = dp(42)
            }
            themeButtons.add(theme to themeBtn)
            themesRow.addView(themeBtn, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(6) })
        }
        themesScroll.addView(themesRow)
        container.addView(themesScroll, top(8))
        container.addView(qrImageView)
        regenerateQrPreview()

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                regenerateQrPreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        input.addTextChangedListener(textWatcher)
        captionInput.addTextChangedListener(textWatcher)

        val scrollWrapper = ScrollView(this).apply {
            addView(container, ViewGroup.LayoutParams(-1, -2))
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.media_studio_qr_generator))
            .setView(scrollWrapper)
            .setPositiveButton(R.string.filter_save_btn) { _, _ ->
                lifecycleScope.launch {
                    val uri = withContext(Dispatchers.IO) {
                        MediaStudioTools.saveBitmapToGallery(this@MainActivity, currentBitmap, "Midiastore_QRCode")
                    }
                    if (uri != null) {
                        Toast.makeText(this@MainActivity, R.string.qr_saved_toast, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, R.string.qr_save_error_toast, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNeutralButton(R.string.filter_share_btn) { _, _ ->
                lifecycleScope.launch {
                    val tempFile = withContext(Dispatchers.IO) {
                        val file = File(cacheDir, "qrcode_${System.currentTimeMillis()}.png")
                        FileOutputStream(file).use { out ->
                            currentBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        file
                    }
                    try {
                        val chooserTitle = getString(R.string.qr_share_chooser)
                        val shareIntent = MediaStudioTools.getShareIntent(this@MainActivity, tempFile, "image/png", chooserTitle)
                        startActivity(Intent.createChooser(shareIntent, chooserTitle))
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, R.string.qr_share_error_toast, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.close_btn, null)
            .show()
    }

    private fun processPhotosToPdf(uris: List<Uri>) {
        lifecycleScope.launch {
            val progress = AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.pdf_creating_title)
                .setMessage(getString(R.string.pdf_creating_msg, uris.size))
                .setCancelable(false)
                .show()

            val pdfPair = withContext(Dispatchers.IO) {
                val fileName = "Documento_Midiastore_${System.currentTimeMillis()}.pdf"
                val cachePdf = File(cacheDir, fileName)
                val ok = MediaStudioTools.createPdfFromUris(this@MainActivity, uris, cachePdf)
                if (ok && cachePdf.exists() && cachePdf.length() > 0L) {
                    val downloadsUri = MediaStudioTools.savePdfToDownloads(this@MainActivity, cachePdf, fileName)
                    cachePdf to downloadsUri
                } else {
                    null to null
                }
            }
            val pdfFile = pdfPair.first
            val savedDownloadsUri = pdfPair.second
            progress.dismiss()

            if (pdfFile != null) {
                val sizeStr = Formatter.formatFileSize(this@MainActivity, pdfFile.length())
                val savedNote = getString(
                    if (savedDownloadsUri != null) R.string.pdf_saved_note_downloads else R.string.pdf_saved_note_cache
                )
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.pdf_created_title)
                    .setMessage(getString(R.string.pdf_created_msg, pdfFile.name, sizeStr, uris.size, savedNote))
                    .setPositiveButton(R.string.pdf_open_btn) { _, _ ->
                        try {
                            val pdfUri = savedDownloadsUri ?: androidx.core.content.FileProvider.getUriForFile(
                                this@MainActivity,
                                "${packageName}.fileprovider",
                                pdfFile
                            )
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(pdfUri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(viewIntent)
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, R.string.pdf_no_viewer_toast, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNeutralButton(R.string.pdf_share_btn) { _, _ ->
                        try {
                            val chooserTitle = getString(R.string.pdf_share_chooser)
                            val shareIntent = MediaStudioTools.getShareIntent(
                                this@MainActivity,
                                pdfFile,
                                "application/pdf",
                                chooserTitle
                            )
                            startActivity(Intent.createChooser(shareIntent, chooserTitle))
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, R.string.pdf_share_error_toast, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(android.R.string.ok, null)
                    .show()
            } else {
                Toast.makeText(this@MainActivity, R.string.pdf_create_error_toast, Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun requestMediaPermissions() {
        val requested = MediaPermissionPolicy.permissionsToRequest(
            apiLevel = Build.VERSION.SDK_INT,
            kinds = setOf(MediaKind.IMAGE, MediaKind.VIDEO, MediaKind.AUDIO),
        ).toTypedArray()
        permissionLauncher.launch(requested)
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
        renderMissionProgress(state.missionProgress)
        resumePendingOutcomeMission(state)
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
                ScanUiStatus.FAILED,
                ScanUiStatus.PAGING_UNSUPPORTED,
                ScanUiStatus.NO_ACCESS,
                ScanUiStatus.SESSION_STALE,
                ScanUiStatus.CANCELLED,
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
            openInteractiveDialogOnMissionFinish = false
            android.util.Log.e(
                "MidiastoreMediaScan",
                "Global mission returned no plan: mission=${progress.mission.name}, reason=${progress.message ?: "none"}",
            )
            if (isFreeHomeCleanup) {
                homeInteractiveRecords = emptyList()
                homeInteractiveFallback = false
                homeCleanupCandidateCount = 0
                heroOrbButton.isEnabled = true
                heroOrbButton.text = getString(R.string.home_cleanup_primary)
                heroOrbSubtitle.text = getString(R.string.home_cleanup_subtitle)
                showHomeCleanupMessage(getString(R.string.home_cleanup_scan_failed))
            }
            missionReviewButton.visibility = View.GONE
            missionRetryButton.visibility = if (progress.message in setOf("session_stale", "cancelled")) View.VISIBLE else View.GONE
            oaasPipelineStatus.text = when (progress.message) {
                "cancelled" -> getString(R.string.oaas_global_scan_cancelled)
                "safety_limit_reached" -> getString(R.string.oaas_global_scan_limit)
                "session_stale" -> getString(R.string.session_stale)
                else -> getString(R.string.oaas_global_scan_failed)
            }
            return
        }
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
            progress.mission != DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES &&
            progress.fallbackRecords.isNotEmpty()
        val interactiveRecords = when {
            primaryRecords.isNotEmpty() -> primaryRecords
            useFallback -> progress.fallbackRecords
            else -> emptyList()
        }
        homeInteractiveRecords = interactiveRecords
        homeInteractiveFallback = useFallback
        val interactiveFormattedBytes = android.text.format.Formatter.formatFileSize(
            this,
            interactiveRecords.sumOf { it.sizeBytes.coerceAtLeast(0L) },
        )
        missionReviewButton.text = getString(R.string.oaas_cleanup_continue, interactiveFormattedBytes)
        missionReviewButton.visibility = if (interactiveRecords.isNotEmpty()) View.VISIBLE else View.GONE
        if (isFreeHomeCleanup) {
            homeCleanupCandidateCount = interactiveRecords.size
            heroOrbButton.isEnabled = true
            heroOrbSubtitle.text = getString(R.string.home_cleanup_subtitle)
            if (homeCleanupCandidateCount > 0) {
                heroOrbButton.text = getString(
                    R.string.home_cleanup_review_button,
                    homeCleanupCandidateCount,
                    interactiveFormattedBytes,
                )
                val messageId = if (progress.coverageComplete) {
                    R.string.home_cleanup_candidates_found
                } else {
                    R.string.home_cleanup_candidates_partial
                }
                showHomeCleanupMessage(getString(messageId, homeCleanupCandidateCount, interactiveFormattedBytes))
            } else {
                heroOrbButton.text = getString(R.string.home_cleanup_primary)
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

    private fun runOutcomeMission(mission: DesiredOutcomeMission) {
        val currentState = viewModel.state.value
        if (currentState.session == null || currentState.isLoading) {
            pendingOutcomeMission = mission
            if (!currentState.isLoading) startScanOrRequestAccess()
            oaasPipelineStatus.text = getString(R.string.oaas_starting_scan_first)
            return
        }
        pendingOutcomeMission = null
        lastRecordedMissionProgress = null
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
        if (confirmedBytes > 0L) {
            try {
                window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            } catch (_: Exception) {}
            outcomeTelemetry.recordConfirmedFreedBytes(confirmedBytes)
            inAppReviewCoordinator.requestReviewIfAppropriate(confirmedBytes)
        }
        refreshStorageStats()
        refreshFlywheelSummary()
        updateSpaceMetricsCard(viewModel.state.value)
        val freedFmt = android.text.format.Formatter.formatFileSize(this, confirmedBytes)
        if (verifiedUris.isNotEmpty()) {
            homeInteractiveRecords = emptyList()
            homeInteractiveFallback = false
            homeCleanupCandidateCount = 0
            heroOrbButton.text = getString(R.string.home_cleanup_primary)
            if (::homeDirectCleanRow.isInitialized) {
                homeDirectCleanRow.visibility = View.GONE
            }
            viewModel.clearSelection()
            viewModel.startSession()
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
            homeComposeState.value = homeComposeState.value.copy(
                usedLabel = usedFmt,
                freeLabel = freeFmt,
                totalLabel = totalFmt,
                collections = buildDefaultHomeCollections(),
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
                                storageBreakdownLegend.text = getString(R.string.storage_legend_summary, pFmt, vFmt, aFmt)
                                storageBreakdownLegend.visibility = View.VISIBLE
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
