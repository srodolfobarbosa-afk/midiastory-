package com.rdf112.sweep

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
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
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.rdf112.sweep.data.billing.AndroidPlayBillingGateway
import com.rdf112.sweep.data.billing.PlayCatalogProductUi
import com.rdf112.sweep.data.media.AndroidMediaPageRepository
import com.rdf112.sweep.data.media.AndroidTrashOperationCoordinator
import com.rdf112.sweep.data.media.Api29DeleteAttempt
import com.rdf112.sweep.data.oaas.AndroidOutcomeTelemetryStore
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
import com.rdf112.sweep.presentation.scan.MediaScanUiState
import com.rdf112.sweep.presentation.scan.MediaScanViewModel
import com.rdf112.sweep.presentation.scan.ScanUiStatus
import com.rdf112.sweep.presentation.ui.StorageRingGaugeView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/**
 * Edição 2027/2028 (v2.0.0 / versionCode = 12) — Interface Moderna em 4 Abas com Medidor Circular,
 * Orbe Central de 1 Toque, Missões OaaS Automatizadas e Compatibilidade MediaStore Universal.
 */
class MainActivity : AppCompatActivity() {
    private enum class AppTab { HOME, FILES, OAAS, PRO }
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
        if (hasAnyMediaPermission()) {
            viewModel.startSession()
            switchTab(if (homeCleanupPending) AppTab.HOME else if (pendingOutcomeMission != null) AppTab.OAAS else AppTab.FILES)
        } else {
            pendingKindAfterPermission = null
            pendingOutcomeMission = null
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
                viewModel.clearSelection()
                viewModel.startSession()
                statusText.text = getString(R.string.trash_operation_result, confirmedCount, uris.size)
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
                outcomeTelemetry.recordConfirmedFreedBytes(confirmedBytes)
            }
            refreshStorageStats()
            refreshFlywheelSummary()
            updateSpaceMetricsCard(viewModel.state.value)
            val freedFmt = android.text.format.Formatter.formatFileSize(this@MainActivity, confirmedBytes)
            if (result.resultCode == Activity.RESULT_OK && confirmedDeleted > 0) {
                viewModel.clearSelection()
                viewModel.startSession()
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

    // Containers das 4 Abas
    private lateinit var homeSection: LinearLayout
    private lateinit var filesSection: LinearLayout
    private lateinit var oaasSection: LinearLayout
    private lateinit var proSection: LinearLayout

    // Botões da Barra de Navegação Inferior
    private lateinit var navHomeBtn: MaterialButton
    private lateinit var navFilesBtn: MaterialButton
    private lateinit var navOaasBtn: MaterialButton
    private lateinit var navProBtn: MaterialButton
    private lateinit var bottomNavContainer: LinearLayout

    // Widgets da Aba Início (2027 Dashboard)
    private lateinit var storageRingGauge: StorageRingGaugeView
    private lateinit var storageUsedHeadline: TextView
    private lateinit var storageFreeSubline: TextView
    private lateinit var heroOrbButton: MaterialButton
    private lateinit var heroOrbSubtitle: TextView
    private lateinit var homeCleanupResult: TextView
    private lateinit var homeDirectCleanRow: LinearLayout
    private var homeCleanupCandidateCount = 0

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
    private lateinit var filterOldButton: MaterialButton
    private lateinit var oaasMissionLargeButton: MaterialButton
    private lateinit var oaasMissionDuplicatesButton: MaterialButton
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
    private val thumbnailJobs = mutableListOf<Job>()
    private val thumbnailDispatcher by lazy { Dispatchers.IO.limitedParallelism(4) }
    private val checkBoxesByUri = mutableMapOf<String, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        billingGateway = AndroidPlayBillingGateway(applicationContext)
        trashCoordinator = AndroidTrashOperationCoordinator(applicationContext)
        outcomeTelemetry = AndroidOutcomeTelemetryStore(applicationContext)
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
        if (::storageText.isInitialized) refreshStorageStats()
        if (::viewModel.isInitialized) viewModel.checkSessionFreshness()
        if (::billingGateway.isInitialized) billingGateway.startAndReconcile()
        if (::restoreTrashButton.isInitialized) updateRestoreButtonState()
    }

    override fun onDestroy() {
        if (::billingGateway.isInitialized) billingGateway.endConnection()
        super.onDestroy()
    }

    private fun createModern2027Layout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.sweep_background))
        }
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
            background = roundedBackground(color(R.color.sweep_surface), dp(14))
            setPadding(dp(8), dp(6), dp(14), dp(6))
        }
        val mark = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = getString(R.string.app_name)
        }
        brandLockup.addView(mark, LinearLayout.LayoutParams(dp(40), dp(40)))

        val brandCopy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val wordmark = label(getString(R.string.app_name), 22f, R.color.sweep_text, true).apply {
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = -0.015f
            val brandName = getString(R.string.app_name)
            val accentStart = brandName.indexOf("store", ignoreCase = true)
            if (accentStart >= 0) {
                text = SpannableString(brandName).apply {
                    setSpan(
                        ForegroundColorSpan(color(R.color.sweep_primary)),
                        accentStart,
                        brandName.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }
        }
        brandCopy.addView(wordmark)
        brandLockup.addView(brandCopy, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(9) })
        topBar.addView(brandLockup, LinearLayout.LayoutParams(0, -2, 1f))
        val premiumAreaButton = button(getString(R.string.premium_area_button)) {
            switchTab(AppTab.PRO)
        }.apply {
            minHeight = dp(38)
            setPadding(dp(12), 0, dp(12), 0)
            textSize = 12f
        }
        topBar.addView(premiumAreaButton, LinearLayout.LayoutParams(-2, dp(38)).apply { marginStart = dp(6) })
        root.addView(topBar, LinearLayout.LayoutParams(-1, -2))

        // Barra de progresso fina e status global
        progress = ProgressBar(this).apply {
            isIndeterminate = true
            visibility = View.GONE
            contentDescription = getString(R.string.loading_page)
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(28)))

        // 2. ÁREA CENTRAL ROLÁVEL (Exibe apenas a Aba ativa!)
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
        }
        val bodyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(24))
        }
        scroll.addView(bodyContainer, ViewGroup.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        statusText = label(getString(R.string.ready_status), 12f, R.color.sweep_primary_glow, false).apply {
            setPadding(dp(4), 0, dp(4), dp(10))
        }
        bodyContainer.addView(statusText)

        // Construção das 4 Seções Separadas
        homeSection = buildHomeDashboardSection()
        filesSection = buildFilesExplorerSection()
        oaasSection = buildOaasMissionsSection()
        proSection = buildProPaywallSection()

        bodyContainer.addView(homeSection)
        bodyContainer.addView(filesSection)
        bodyContainer.addView(oaasSection)
        bodyContainer.addView(proSection)

        // 3. BARRA DE NAVEGAÇÃO INFERIOR (4 Abas limpas estilo 2027)
        bottomNavContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(color(R.color.sweep_surface))
            setPadding(dp(10), dp(8), dp(10), dp(10))
        }
        navHomeBtn = navTabButton(getString(R.string.nav_home)) { switchTab(AppTab.HOME) }
        navFilesBtn = navTabButton(getString(R.string.nav_files)) { switchTab(AppTab.FILES) }
        navOaasBtn = navTabButton(getString(R.string.nav_missions)) { switchTab(AppTab.OAAS) }
        navProBtn = navTabButton(getString(R.string.nav_pro)) { switchTab(AppTab.PRO) }

        bottomNavContainer.addView(navHomeBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) })
        bottomNavContainer.addView(navFilesBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) })
        bottomNavContainer.addView(navOaasBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) })
        bottomNavContainer.addView(navProBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(4) })
        root.addView(bottomNavContainer, LinearLayout.LayoutParams(-1, -2))

        return root
    }

    private fun buildHomeDashboardSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        val heroHeader = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(12))
        }
        heroHeader.addView(label(getString(R.string.brand_tagline), 12f, R.color.sweep_primary_glow, true))
        heroHeader.addView(label(getString(R.string.home_welcome_title), 20f, R.color.sweep_text, true), top(4))
        addView(heroHeader, LinearLayout.LayoutParams(-1, -2))

        // Card de Medidor de Armazenamento Real (Anel + GB usados/livres)
        val storageCard = card()
        val storageRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        storageRingGauge = StorageRingGaugeView(this@MainActivity).apply {
            bottomLabel = getString(R.string.storage_ring_used)
        }
        storageRow.addView(storageRingGauge, LinearLayout.LayoutParams(dp(112), dp(112)).apply { marginEnd = dp(16) })
        val storageCopy = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        storageCopy.addView(label(getString(R.string.storage_title), 11f, R.color.sweep_primary_glow, true))
        storageUsedHeadline = label(getString(R.string.storage_unavailable), 19f, R.color.sweep_text, true)
        storageCopy.addView(storageUsedHeadline, top(4))
        storageFreeSubline = label("", 13f, R.color.sweep_muted, false)
        storageCopy.addView(storageFreeSubline, top(3))
        storageCopy.addView(label(getString(R.string.storage_note), 11f, R.color.sweep_muted, false), top(6))
        storageRow.addView(storageCopy, LinearLayout.LayoutParams(0, -2, 1f))
        storageCard.addView(storageRow)
        addView(wrap(storageCard), bottom(14))

        heroOrbButton = MaterialButton(this@MainActivity).apply {
            text = getString(R.string.home_cleanup_primary)
            textSize = 18f
            isAllCaps = false
            minHeight = dp(68)
            cornerRadius = dp(22)
            insetTop = 0
            insetBottom = 0
            tag = "primary_phone_cleaner_action"
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
            setOnClickListener { onHomeCleanupButtonClicked() }
        }
        addView(heroOrbButton, LinearLayout.LayoutParams(-1, dp(68)).apply { bottomMargin = dp(6) })

        heroOrbSubtitle = label(getString(R.string.home_cleanup_subtitle), 12f, R.color.sweep_muted, false)
        addView(heroOrbSubtitle, LinearLayout.LayoutParams(-1, -2).apply {
            leftMargin = dp(6)
            rightMargin = dp(6)
            bottomMargin = dp(12)
        })

        homeCleanupResult = label("", 13f, R.color.sweep_primary_glow, true).apply {
            visibility = View.GONE
            setPadding(dp(8), dp(2), dp(8), dp(8))
        }
        addView(homeCleanupResult)

        homeDirectCleanRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
        }
        val homeTrashBtn = button(getString(R.string.move_to_trash_button)) {
            requestMoveSelectedToTrash()
        }.apply {
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
        }
        val homeDeleteBtn = button(getString(R.string.permanent_delete_button)) {
            requestPermanentDeleteSelected()
        }.apply {
            setTextColor(color(R.color.sweep_text))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_danger))
        }
        homeDirectCleanRow.addView(homeTrashBtn, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(4) })
        homeDirectCleanRow.addView(homeDeleteBtn, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(4) })
        addView(homeDirectCleanRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        // Card de Ações Rápidas (Grade 2x2 estilo Files by Google + SD Maid SE)
        val quickCard = card()
        quickCard.addView(label(getString(R.string.quick_actions_title), 11f, R.color.sweep_primary_glow, true))
        quickCard.addView(label(getString(R.string.quick_actions_subtitle), 12f, R.color.sweep_muted, false), top(4))

        val quickRow1 = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val quickLargeBtn = button(getString(R.string.quick_find_large)) {
            startOneTapCleanup()
        }.apply { minHeight = dp(54) }
        val quickDupBtn = button(getString(R.string.quick_find_duplicates)) {
            runWithProAccess {
                switchTab(AppTab.OAAS)
                runOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
            }
        }.apply { minHeight = dp(54) }
        quickRow1.addView(quickLargeBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        quickRow1.addView(quickDupBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        quickCard.addView(quickRow1, top(10))

        val quickRow2 = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val quickOldBtn = button(getString(R.string.filter_older_30d)) {
            runWithProAccess {
                switchTab(AppTab.OAAS)
                runOutcomeMission(DesiredOutcomeMission.ARCHIVE_OLD_MEDIA_30D)
            }
        }.apply { minHeight = dp(54) }
        val quickUnusedAppsBtn = button(getString(R.string.pro_unused_apps_title)) {
            runWithProAccess { requestUnusedAppSuggestions() }
        }.apply { minHeight = dp(54) }
        quickRow2.addView(quickOldBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        quickRow2.addView(quickUnusedAppsBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        quickCard.addView(quickRow2, top(8))
        addView(wrap(quickCard), bottom(14))

        // Banner VIP Pro Moderno e Enxuto (Direciona para a Aba Pro, sem poluir a Home com botões repetidos)
        val homeProBanner = card().apply {
            setOnClickListener { switchTab(AppTab.PRO) }
        }
        val proBannerRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val proBadge = ImageView(this@MainActivity).apply {
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = getString(R.string.app_name)
        }
        proBannerRow.addView(proBadge, LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(10) })
        val proBannerCopy = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        proBannerCopy.addView(label(getString(R.string.home_plans_title), 15f, R.color.sweep_primary_glow, true))
        proBannerCopy.addView(label(getString(R.string.home_plans_subtitle), 12f, R.color.sweep_muted, false), top(2))
        proBannerRow.addView(proBannerCopy, LinearLayout.LayoutParams(0, -2, 1f))
        val proCtaBtn = button(getString(R.string.premium_area_button)) { switchTab(AppTab.PRO) }.apply {
            minHeight = dp(38)
            textSize = 12f
        }
        proBannerRow.addView(proCtaBtn, LinearLayout.LayoutParams(-2, dp(38)).apply { marginStart = dp(6) })
        homeProBanner.addView(proBannerRow)
        addView(wrap(homeProBanner), bottom(14))

        // Mantém as referências dos botões inicializadas para compatibilidade com renderCatalogPrices
        homePlanMonthlyButton = button("") { requestPlanPurchase(BillingStatePolicy.PRODUCT_MONTHLY) }.apply { visibility = View.GONE }
        homePlanAnnualButton = button("") { requestPlanPurchase(BillingStatePolicy.PRODUCT_ANNUAL) }.apply { visibility = View.GONE }
        homePlanLifetimeButton = button("") { requestPlanPurchase(BillingStatePolicy.PRODUCT_LIFETIME) }.apply { visibility = View.GONE }

        val shareAppButton = button(getString(R.string.share_app_button)) { shareApp() }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
            setTextColor(color(R.color.sweep_text))
            icon = ContextCompat.getDrawable(this@MainActivity, android.R.drawable.ic_menu_share)
            iconTint = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary_glow))
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        }
        addView(shareAppButton, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(8) })
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

    private fun buildFilesExplorerSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        val controls = card()
        controls.addView(label(getString(R.string.media_kind_title), 11f, R.color.sweep_primary_glow, true), bottom(8))
        val typeRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        photosButton = button(getString(R.string.photos_tab)) { viewModel.selectKind(MediaKind.IMAGE) }
        videosButton = button(getString(R.string.videos_tab)) { viewModel.selectKind(MediaKind.VIDEO) }
        audioButton = button(getString(R.string.audio_tab)) { viewModel.selectKind(MediaKind.AUDIO) }
        typeRow.addView(photosButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(4) })
        typeRow.addView(videosButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
        typeRow.addView(audioButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(4) })
        controls.addView(typeRow, bottom(12))

        controls.addView(label(getString(R.string.smart_filter_title), 11f, R.color.sweep_primary_glow, true), bottom(6))
        val filterRow1 = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        filterAllButton = button(getString(R.string.filter_all)) { viewModel.selectSmartFilter(SmartFilterMode.ALL_ITEMS) }.apply { minHeight = dp(52) }
        filterLargeButton = button(getString(R.string.filter_large_10mb)) { viewModel.selectSmartFilter(SmartFilterMode.LARGE_FILES_10MB) }.apply { minHeight = dp(52) }
        filterRow1.addView(filterAllButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        filterRow1.addView(filterLargeButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        controls.addView(filterRow1, bottom(6))

        val filterRow2 = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        filterDuplicatesButton = button(getString(R.string.filter_same_size)) {
            runWithProAccess { viewModel.selectSmartFilter(SmartFilterMode.SAME_SIZE_CANDIDATES) }
        }.apply { minHeight = dp(52) }
        filterOldButton = button(getString(R.string.filter_older_30d)) {
            runWithProAccess { viewModel.selectSmartFilter(SmartFilterMode.OLDER_THAN_30_DAYS) }
        }.apply { minHeight = dp(52) }
        filterRow2.addView(filterDuplicatesButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        filterRow2.addView(filterOldButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        controls.addView(filterRow2, bottom(12))

        controls.addView(label(getString(R.string.volume_title), 11f, R.color.sweep_muted, true), bottom(4))
        volumeSpinner = Spinner(this@MainActivity).apply {
            contentDescription = getString(R.string.volume_title)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!updatingVolumeSpinner && position in renderedVolumes.indices) {
                        viewModel.selectVolume(renderedVolumes[position])
                    }
                }
            }
        }
        controls.addView(volumeSpinner, LinearLayout.LayoutParams(-1, dp(48)))
        filesPermissionButton = button(getString(R.string.permission_continue)) {
            requestMediaPermissions()
        }.apply {
            visibility = View.GONE
        }
        controls.addView(filesPermissionButton, top(8))
        addView(wrap(controls), bottom(14))

        val reviewCard = card()
        reviewCard.addView(label(getString(R.string.review_title), 11f, R.color.sweep_primary_glow, true))
        reviewCard.addView(label(getString(R.string.space_accounting_title), 11f, R.color.sweep_primary_glow, true), top(8))
        spaceMetricsText = label(
            getString(R.string.space_metrics_summary, "0 B", "0 B", "0 B", "0 B"),
            11f,
            R.color.sweep_muted,
            false,
        )
        reviewCard.addView(spaceMetricsText, top(4))
        selectionSummary = label(getString(R.string.nothing_selected), 15f, R.color.sweep_text, true)
        reviewCard.addView(selectionSummary, top(8))
        reviewCard.addView(label(getString(R.string.review_readonly_note), 12f, R.color.sweep_muted, false), top(6))
        selectedContainer = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        reviewCard.addView(selectedContainer, top(8))

        moveToTrashButton = button(getString(R.string.move_to_trash_button)) { requestMoveSelectedToTrash() }.apply {
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
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

        val resultsCard = card()
        resultsCard.addView(label(getString(R.string.results_title), 11f, R.color.sweep_primary_glow, true))
        pageLabel = label(getString(R.string.not_scanned), 16f, R.color.sweep_text, true)
        resultsCard.addView(pageLabel, top(8))
        resultSummary = label(getString(R.string.page_results_hint), 12f, R.color.sweep_muted, false)
        resultsCard.addView(resultSummary, top(6))
        recordsContainer = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        resultsCard.addView(recordsContainer)

        val navigation = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        previousButton = button(getString(R.string.previous_page)) { viewModel.loadPreviousPage() }
        nextButton = button(getString(R.string.next_page)) { viewModel.loadNextPage() }.apply {
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
        }
        navigation.addView(previousButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(6) })
        navigation.addView(nextButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(6) })
        resultsCard.addView(navigation, top(12))
        addView(wrap(resultsCard), bottom(14))
    }

    private fun buildOaasMissionsSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        val oaasCard = card()
        oaasCard.addView(label(getString(R.string.oaas_card_title), 11f, R.color.sweep_primary_glow, true))
        oaasCard.addView(label(getString(R.string.oaas_card_headline), 18f, R.color.sweep_text, true), top(6))
        oaasCard.addView(label(getString(R.string.oaas_card_subtitle), 12f, R.color.sweep_muted, false), top(4))
        scanButton = button(getString(R.string.scan_button)) {
            switchTab(AppTab.FILES)
            startScanOrRequestAccess()
        }
        oaasCard.addView(scanButton, top(10))
        permissionButton = button(getString(R.string.permission_continue)) {
            requestMediaPermissions()
        }.apply { visibility = View.GONE }
        oaasCard.addView(permissionButton, top(6))
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
            runOutcomeMission(DesiredOutcomeMission.FREE_MAX_SPACE_NOW)
        }.apply {
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
        }
        oaasCard.addView(oaasMissionLargeButton, top(12))

        oaasMissionDuplicatesButton = button(getString(R.string.oaas_mission_duplicates)) {
            runWithProAccess { runOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES) }
        }
        oaasCard.addView(oaasMissionDuplicatesButton, top(8))

        oaasMissionOldButton = button(getString(R.string.oaas_mission_old)) {
            runWithProAccess { runOutcomeMission(DesiredOutcomeMission.ARCHIVE_OLD_MEDIA_30D) }
        }
        oaasCard.addView(oaasMissionOldButton, top(8))

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
            switchTab(AppTab.FILES)
            filesReviewCard.post {
                filesReviewCard.requestRectangleOnScreen(
                    android.graphics.Rect(0, 0, filesReviewCard.width, filesReviewCard.height),
                    true,
                )
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

        val proCard = card()
        val proHeaderRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val proHeaderLogo = ImageView(this@MainActivity).apply {
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = getString(R.string.app_name)
        }
        proHeaderRow.addView(proHeaderLogo, LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginEnd = dp(12) })
        val proHeaderTextCol = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        proHeaderTextCol.addView(label(getString(R.string.pro_card_title), 11f, R.color.sweep_accent_amber, true))
        proStatusBadge = label(getString(R.string.pro_status_free), 17f, R.color.sweep_text, true)
        proHeaderTextCol.addView(proStatusBadge, top(2))
        proHeaderRow.addView(proHeaderTextCol, LinearLayout.LayoutParams(0, -2, 1f))
        proCard.addView(proHeaderRow)
        proCard.addView(label(getString(R.string.pro_card_subtitle), 13f, R.color.sweep_muted, false), top(6))
        proCard.addView(label(getString(R.string.pro_free_included), 12f, R.color.sweep_text, false), top(12))
        proCard.addView(label(getString(R.string.pro_paid_included), 12f, R.color.sweep_accent_amber, false), top(10))

        val unusedAppsButton = button(getString(R.string.pro_unused_apps_button)) {
            runWithProAccess { requestUnusedAppSuggestions() }
        }
        proCard.addView(unusedAppsButton, top(12))

        val advancedCleanupButton = button(getString(R.string.pro_advanced_cleanup_button)) {
            runWithProAccess { switchTab(AppTab.OAAS) }
        }
        proCard.addView(advancedCleanupButton, top(8))

        val browseFilesButton = button(getString(R.string.pro_browse_files_button)) {
            runWithProAccess { switchTab(AppTab.FILES) }
        }
        proCard.addView(browseFilesButton, top(8))

        planAnnualButton = button(getString(R.string.plan_annual_default)) {
            requestPlanPurchase(BillingStatePolicy.PRODUCT_ANNUAL)
        }.apply {
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
        }
        stylePlanPurchaseButton(planAnnualButton, isProductAvailable = false)
        proCard.addView(planAnnualButton, top(14))

        planMonthlyButton = button(getString(R.string.plan_monthly_default)) {
            requestPlanPurchase(BillingStatePolicy.PRODUCT_MONTHLY)
        }
        stylePlanPurchaseButton(planMonthlyButton, isProductAvailable = false)
        proCard.addView(planMonthlyButton, top(8))

        planLifetimeButton = button(getString(R.string.plan_lifetime_default)) {
            requestPlanPurchase(BillingStatePolicy.PRODUCT_LIFETIME)
        }
        stylePlanPurchaseButton(planLifetimeButton, isProductAvailable = false)
        proCard.addView(planLifetimeButton, top(8))
        if (!billingGateway.securePurchaseVerificationAvailable()) {
            proCard.addView(label(getString(R.string.pro_purchase_unavailable_notice), 11f, R.color.sweep_muted, false), top(12))
        }

        if (isDevelopmentBuild) {
            val debugPreviewButton = button(getString(R.string.pro_debug_preview_off)) {
                debugProPreviewEnabled = !debugProPreviewEnabled
                debugProPreviewButton?.text = getString(
                    if (debugProPreviewEnabled) R.string.pro_debug_preview_on else R.string.pro_debug_preview_off,
                )
                renderBillingState(billingGateway.accessState.value)
                statusText.text = getString(
                    if (debugProPreviewEnabled) R.string.pro_debug_preview_enabled else R.string.pro_debug_preview_disabled,
                )
                updateReview(viewModel.state.value)
            }
            debugProPreviewButton = debugPreviewButton
            proCard.addView(debugPreviewButton, top(10))
            proCard.addView(label(getString(R.string.pro_debug_preview_note), 11f, R.color.sweep_muted, false), top(4))
        }

        restorePurchasesButton = button(getString(R.string.restore_purchases_button)) {
            billingGateway.startAndReconcile()
            statusText.text = getString(R.string.restoring_purchases_status)
            Toast.makeText(this@MainActivity, R.string.restoring_purchases_status, Toast.LENGTH_SHORT).show()
        }
        proCard.addView(restorePurchasesButton, top(10))

        manageSubscriptionButton = button(getString(R.string.manage_subscriptions_button)) {
            openPlayStoreSubscriptions()
        }
        proCard.addView(manageSubscriptionButton, top(6))
        addView(wrap(proCard), bottom(14))

        val privacy = card()
        privacy.addView(label(getString(R.string.privacy_title), 11f, R.color.sweep_muted, true))
        privacy.addView(label(getString(R.string.privacy_body), 12f, R.color.sweep_muted, false), top(8))
        addView(wrap(privacy))
    }

    private fun switchTab(tab: AppTab) {
        activeTab = tab
        statusText.visibility = if (tab == AppTab.FILES) View.VISIBLE else View.GONE
        homeSection.visibility = if (tab == AppTab.HOME) View.VISIBLE else View.GONE
        filesSection.visibility = if (tab == AppTab.FILES) View.VISIBLE else View.GONE
        oaasSection.visibility = if (tab == AppTab.OAAS) View.VISIBLE else View.GONE
        proSection.visibility = if (tab == AppTab.PRO) View.VISIBLE else View.GONE

        styleNavButton(navHomeBtn, tab == AppTab.HOME)
        styleNavButton(navFilesBtn, tab == AppTab.FILES)
        styleNavButton(navOaasBtn, tab == AppTab.OAAS)
        styleNavButton(navProBtn, tab == AppTab.PRO)
    }

    private fun styleNavButton(btn: MaterialButton, selected: Boolean) {
        if (selected) {
            btn.setTextColor(color(R.color.sweep_background))
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
        } else {
            btn.setTextColor(color(R.color.sweep_muted))
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
        }
    }

    private fun renderBillingState(state: ProAccessSummaryState) {
        if (!::proStatusBadge.isInitialized) return
        if (::bottomNavContainer.isInitialized) {
            navFilesBtn.visibility = View.VISIBLE
            navOaasBtn.visibility = View.VISIBLE
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
    }

    private fun stylePlanPurchaseButton(button: MaterialButton, isProductAvailable: Boolean) {
        val purchasable = billingGateway.securePurchaseVerificationAvailable() && isProductAvailable
        // Keep unavailable offers tappable so they can explain why checkout is not ready.
        button.isEnabled = true
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(
            color(if (purchasable) R.color.sweep_primary else R.color.sweep_surface_alt),
        )
        button.setTextColor(color(if (purchasable) R.color.sweep_background else R.color.sweep_muted))
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

    private fun startOneTapCleanup() {
        homeCleanupCandidateCount = 0
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
        if (!heroOrbButton.isEnabled) return
        if (homeCleanupCandidateCount > 0) {
            val selected = viewModel.state.value.selection.records
            val totalBytes = viewModel.state.value.selection.totalBytesOrNull() ?: 0L
            val formattedBytes = android.text.format.Formatter.formatFileSize(this, totalBytes)
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.home_cleanup_review_button, if (selected.isNotEmpty()) selected.size else homeCleanupCandidateCount, formattedBytes))
            builder.setMessage(getString(R.string.home_cleanup_confirm_prompt))
            builder.setPositiveButton(R.string.permanent_delete_button) { dialog, _ ->
                dialog.dismiss()
                requestPermanentDeleteSelected()
            }
            builder.setNegativeButton(R.string.nav_files) { dialog, _ ->
                dialog.dismiss()
                switchTab(AppTab.FILES)
                filesReviewCard.post {
                    filesReviewCard.requestRectangleOnScreen(
                        android.graphics.Rect(0, 0, filesReviewCard.width, filesReviewCard.height), true,
                    )
                }
            }
            if (trashCoordinator.supportsSystemTrash()) {
                builder.setNeutralButton(R.string.move_to_trash_button) { dialog, _ ->
                    dialog.dismiss()
                    requestMoveSelectedToTrash()
                }
            }
            builder.show()
        } else {
            startOneTapCleanup()
        }
    }

    private fun runWithProAccess(action: () -> Unit) {
        val hasVerifiedAccess = billingGateway.accessState.value == ProAccessSummaryState.PRO_GRANTED
        if (hasVerifiedAccess || (isDevelopmentBuild && debugProPreviewEnabled)) {
            action()
        } else {
            switchTab(AppTab.PRO)
            Toast.makeText(this, R.string.pro_feature_requires_pro, Toast.LENGTH_LONG).show()
        }
    }

    private fun requestUnusedAppSuggestions() {
        if (!hasUsageStatsAccess()) {
            Toast.makeText(this, R.string.pro_unused_apps_permission, Toast.LENGTH_LONG).show()
            runCatching {
                usageAccessLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }.onFailure {
                Toast.makeText(this, R.string.pro_unused_apps_permission_denied, Toast.LENGTH_LONG).show()
            }
            return
        }
        loadUnusedAppSuggestions()
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
        oaasMissionOldButton.isEnabled = !state.isLoading
        oaasCancelMissionButton.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        oaasCancelMissionButton.isEnabled = state.isLoading
        filterDuplicatesButton.isEnabled = !state.isLoading
        filterOldButton.isEnabled = !state.isLoading
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

        if (state.currentPage != null && state.status in setOf(ScanUiStatus.READY, ScanUiStatus.PARTIAL)) {
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
            setOnCheckedChangeListener { _, checked -> viewModel.setSelected(record, checked) }
        }
        checkBoxesByUri[record.uri] = check
        row.addView(check, LinearLayout.LayoutParams(dp(46), dp(46)))

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
            if (target.tag == record.uri && bitmap != null) target.setImageBitmap(bitmap)
            else if (target.tag == record.uri) target.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        job.invokeOnCompletion { cause ->
            if (cause is CancellationException) signal.cancel()
        }
        thumbnailJobs += job
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
            line.addView(remove, LinearLayout.LayoutParams(-2, dp(38)))
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
            android.util.Log.e(
                "MidiastoreMediaScan",
                "Global mission returned no plan: mission=${progress.mission.name}, reason=${progress.message ?: "none"}",
            )
            if (isFreeHomeCleanup) {
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
        missionReviewButton.text = getString(R.string.oaas_cleanup_continue, formattedBytes)
        missionReviewButton.visibility = if (plan.autoSelectedRecords.isNotEmpty()) View.VISIBLE else View.GONE
        if (isFreeHomeCleanup) {
            homeCleanupCandidateCount = plan.autoSelectedRecords.size
            heroOrbButton.isEnabled = true
            heroOrbSubtitle.text = getString(R.string.home_cleanup_subtitle)
            if (homeCleanupCandidateCount > 0) {
                heroOrbButton.text = getString(
                    R.string.home_cleanup_review_button,
                    homeCleanupCandidateCount,
                    formattedBytes,
                )
                if (::homeDirectCleanRow.isInitialized) {
                    homeDirectCleanRow.visibility = View.VISIBLE
                }
                val messageId = if (progress.coverageComplete) {
                    R.string.home_cleanup_candidates_found
                } else {
                    R.string.home_cleanup_candidates_partial
                }
                showHomeCleanupMessage(getString(messageId, homeCleanupCandidateCount, formattedBytes))
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

    private fun requestMoveSelectedToTrash() {
        val selected = viewModel.state.value.selection.records
        if (selected.isEmpty() || !trashCoordinator.supportsSystemTrash()) return
        lifecycleScope.launch {
            val plan = trashCoordinator.prepareAndRevalidatePlan(selected, MediaOperationMode.MOVE_TO_TRASH)
            val firstBatch = plan.uriBatches.firstOrNull()
            if (firstBatch.isNullOrEmpty()) {
                statusText.text = getString(R.string.revalidation_none_eligible, plan.rejectedItems.size)
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
                statusText.text = getString(R.string.page_failed)
            }
        }
    }

    private fun requestPermanentDeleteSelected() {
        val selected = viewModel.state.value.selection.records
        if (selected.isEmpty() || !trashCoordinator.supportsPermanentDelete()) return
        lifecycleScope.launch {
            val plan = trashCoordinator.prepareAndRevalidatePlan(selected, MediaOperationMode.PERMANENT_DELETE)
            val firstBatch = plan.uriBatches.firstOrNull()
            if (firstBatch.isNullOrEmpty()) {
                statusText.text = getString(R.string.revalidation_none_eligible, plan.rejectedItems.size)
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
                statusText.text = getString(R.string.page_failed)
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
        if (confirmedBytes > 0L) outcomeTelemetry.recordConfirmedFreedBytes(confirmedBytes)
        refreshStorageStats()
        refreshFlywheelSummary()
        updateSpaceMetricsCard(viewModel.state.value)
        val freedFmt = android.text.format.Formatter.formatFileSize(this, confirmedBytes)
        if (verifiedUris.isNotEmpty()) {
            viewModel.clearSelection()
            viewModel.startSession()
        }
        statusText.text = getString(
            R.string.delete_operation_result,
            verifiedUris.size,
            attempted,
            freedFmt,
        )
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
        if (isDevelopmentBuild && debugProPreviewEnabled) {
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
            }
        }.onFailure {
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
        textSize = 12f
        cornerRadius = dp(16)
        setPadding(dp(10), dp(8), dp(10), dp(8))
        backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_surface_alt))
        setTextColor(color(R.color.sweep_text))
        setOnClickListener { onClick() }
    }

    private fun navTabButton(text: String, onClick: () -> Unit): MaterialButton = MaterialButton(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 11f
        cornerRadius = dp(14)
        setPadding(dp(4), 0, dp(4), 0)
        setOnClickListener { onClick() }
    }

    private fun fullButton(height: Int, bottom: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, dp(height)).apply { bottomMargin = dp(bottom) }

    private fun bottom(value: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(value) }

    private fun top(value: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(value) }

    private fun color(id: Int): Int = ContextCompat.getColor(this, id)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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
