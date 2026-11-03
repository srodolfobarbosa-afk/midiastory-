package com.rdf112.sweep

import android.Manifest
import android.app.Activity
import android.content.Intent
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
import android.os.StatFs
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
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasAnyMediaPermission()) {
            viewModel.startSession()
            switchTab(AppTab.FILES)
        } else {
            statusText.text = getString(R.string.permission_denied)
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
            if (result.resultCode == Activity.RESULT_OK) {
                sessionAuthorizedBytes += uris.sumOf { bytesMap[it] ?: 0L }
            }
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
        val uris = pendingOperationUris
        pendingOperationUris = emptyList()
        if (uris.isEmpty()) return@registerForActivityResult
        val bytesMap = pendingOperationBytesByUri
        pendingOperationBytesByUri = emptyMap()
        lifecycleScope.launch {
            val confirmedDeleted = trashCoordinator.verifyPostDelete(uris)
            val totalBatchBytes = uris.sumOf { bytesMap[it] ?: 0L }
            val confirmedBytes = if (result.resultCode == Activity.RESULT_OK && confirmedDeleted == uris.size) {
                totalBatchBytes
            } else {
                0L
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
    private var sessionAuthorizedBytes: Long = 0L
    private lateinit var spaceMetricsText: TextView
    private lateinit var radarConsentCheckBox: CheckBox
    private var activeTab: AppTab = AppTab.HOME

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
    private lateinit var topProPill: MaterialButton

    // Widgets da Aba Início (2027 Dashboard)
    private lateinit var storageRingGauge: StorageRingGaugeView
    private lateinit var storageUsedHeadline: TextView
    private lateinit var storageFreeSubline: TextView
    private lateinit var heroOrbButton: LinearLayout
    private lateinit var heroOrbTitle: TextView
    private lateinit var heroOrbSubtitle: TextView

    // Widgets compartilhados e das demais abas
    private lateinit var storageText: TextView
    private lateinit var statusText: TextView
    private lateinit var pageLabel: TextView
    private lateinit var selectionSummary: TextView
    private lateinit var resultSummary: TextView
    private lateinit var proStatusBadge: TextView
    private lateinit var oaasPipelineStatus: TextView
    private lateinit var oaasFlywheelMetrics: TextView
    private lateinit var recordsContainer: LinearLayout
    private lateinit var selectedContainer: LinearLayout
    private lateinit var volumeSpinner: Spinner
    private lateinit var scanButton: MaterialButton
    private lateinit var permissionButton: MaterialButton
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
    private lateinit var planMonthlyButton: MaterialButton
    private lateinit var planAnnualButton: MaterialButton
    private lateinit var planLifetimeButton: MaterialButton
    private lateinit var restorePurchasesButton: MaterialButton
    private lateinit var manageSubscriptionButton: MaterialButton
    private lateinit var progress: ProgressBar

    private var updatingVolumeSpinner = false
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

        // 1. TOP APP BAR MODERNA (Logo M + Nome Midiastores + Pílula Seja Pro)
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(10))
        }
        val mark = TextView(this).apply {
            text = "S"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(color(R.color.sweep_background))
            background = gradientPill(color(R.color.sweep_primary), color(R.color.sweep_primary_glow), dp(14))
            contentDescription = getString(R.string.app_name)
        }
        topBar.addView(mark, LinearLayout.LayoutParams(dp(42), dp(42)))

        val brandCopy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brandCopy.addView(label(getString(R.string.app_name), 20f, R.color.sweep_text, true))
        brandCopy.addView(label(getString(R.string.brand_tagline), 11f, R.color.sweep_primary_glow, false))
        topBar.addView(brandCopy, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })

        topProPill = MaterialButton(this).apply {
            text = "PRO"
            isAllCaps = false
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            cornerRadius = dp(20)
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_accent_amber))
            setOnClickListener { switchTab(AppTab.PRO) }
        }
        topBar.addView(topProPill, LinearLayout.LayoutParams(-2, dp(38)))
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
        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(color(R.color.sweep_surface))
            setPadding(dp(10), dp(8), dp(10), dp(10))
        }
        navHomeBtn = navTabButton("Início") { switchTab(AppTab.HOME) }
        navFilesBtn = navTabButton("Arquivos") { switchTab(AppTab.FILES) }
        navOaasBtn = navTabButton("Missões 10x") { switchTab(AppTab.OAAS) }
        navProBtn = navTabButton("Planos Pro") { switchTab(AppTab.PRO) }

        bottomNav.addView(navHomeBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) })
        bottomNav.addView(navFilesBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
        bottomNav.addView(navOaasBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
        bottomNav.addView(navProBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(4) })
        root.addView(bottomNav, LinearLayout.LayoutParams(-1, -2))

        return root
    }

    private fun buildHomeDashboardSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        // Card de Armazenamento com Medidor Circular 2027
        val gaugeCard = card()
        val gaugeRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        storageRingGauge = StorageRingGaugeView(this@MainActivity)
        gaugeRow.addView(storageRingGauge, LinearLayout.LayoutParams(dp(116), dp(116)).apply { marginEnd = dp(14) })

        val gaugeInfo = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        gaugeInfo.addView(label(getString(R.string.storage_title), 10f, R.color.sweep_primary_glow, true))
        storageUsedHeadline = label("...", 20f, R.color.sweep_text, true)
        gaugeInfo.addView(storageUsedHeadline, top(4))
        storageFreeSubline = label("...", 13f, R.color.sweep_muted, false)
        gaugeInfo.addView(storageFreeSubline, top(2))
        storageText = label(getString(R.string.storage_unavailable), 11f, R.color.sweep_muted, false).apply {
            visibility = View.GONE
        }
        gaugeInfo.addView(storageText)
        gaugeRow.addView(gaugeInfo, LinearLayout.LayoutParams(0, -2, 1f))
        gaugeCard.addView(gaugeRow)
        gaugeCard.addView(label(getString(R.string.space_accounting_title), 10f, R.color.sweep_primary_glow, true), top(14))
        spaceMetricsText = label(
            getString(R.string.space_metrics_summary, "0 B", "0 B", "0 B", "0 B"),
            12f,
            R.color.sweep_text,
            false,
        )
        gaugeCard.addView(spaceMetricsText, top(4))
        addView(wrap(gaugeCard), bottom(22))

        // ORBE GIGANTE DE 1 TOQUE (Inspirado no Dossiê V3 Selo Ouro)
        heroOrbButton = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = circleGlowBackground(color(R.color.sweep_primary), color(R.color.sweep_surface_alt))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (hasAnyMediaPermission()) {
                    viewModel.startSession()
                    switchTab(AppTab.FILES)
                } else {
                    requestMediaPermissions()
                }
            }
        }
        heroOrbTitle = label("ANALISAR AGORA", 19f, R.color.sweep_background, true).apply {
            gravity = Gravity.CENTER
        }
        heroOrbSubtitle = label("Toque para escanear mídias", 11f, R.color.sweep_background, false).apply {
            gravity = Gravity.CENTER
        }
        heroOrbButton.addView(heroOrbTitle)
        heroOrbButton.addView(heroOrbSubtitle, top(4))
        addView(
            heroOrbButton,
            LinearLayout.LayoutParams(dp(196), dp(196)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(22)
            }
        )

        // Atalhos rápidos de Missões OaaS na Home
        val quickMissionsCard = card()
        quickMissionsCard.addView(label("EXECUÇÃO RÁPIDA (1 TOQUE)", 11f, R.color.sweep_primary_glow, true))
        quickMissionsCard.addView(label("Escolha o resultado desejado e o Midiastores prepara tudo para você:", 12f, R.color.sweep_muted, false), top(4))

        val quickLargeBtn = button("Liberar espaço dos maiores arquivos (>10 MB)") {
            switchTab(AppTab.OAAS)
            runOutcomeMission(DesiredOutcomeMission.FREE_MAX_SPACE_NOW)
        }.apply {
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
        }
        quickMissionsCard.addView(quickLargeBtn, top(10))

        val quickDupBtn = button("Separar cópias de mesmo tamanho (Original protegido)") {
            switchTab(AppTab.OAAS)
            runOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
        }
        quickMissionsCard.addView(quickDupBtn, top(8))

        scanButton = button(getString(R.string.scan_button)) {
            startScanOrRequestAccess()
            switchTab(AppTab.FILES)
        }
        quickMissionsCard.addView(scanButton, top(8))

        permissionButton = button(getString(R.string.permission_continue)) {
            requestMediaPermissions()
        }
        quickMissionsCard.addView(permissionButton, top(6))

        cancelButton = button(getString(R.string.cancel_scan)) { viewModel.cancelLoading() }.apply {
            visibility = View.GONE
        }
        quickMissionsCard.addView(cancelButton, top(6))

        addView(wrap(quickMissionsCard), bottom(14))
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
        val filterRow1 = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
        filterAllButton = button(getString(R.string.filter_all)) { viewModel.selectSmartFilter(SmartFilterMode.ALL_ITEMS) }
        filterLargeButton = button(getString(R.string.filter_large_10mb)) { viewModel.selectSmartFilter(SmartFilterMode.LARGE_FILES_10MB) }
        filterRow1.addView(filterAllButton, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) })
        filterRow1.addView(filterLargeButton, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(4) })
        controls.addView(filterRow1, bottom(6))

        val filterRow2 = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
        filterDuplicatesButton = button(getString(R.string.filter_same_size)) { viewModel.selectSmartFilter(SmartFilterMode.SAME_SIZE_CANDIDATES) }
        filterOldButton = button(getString(R.string.filter_older_30d)) { viewModel.selectSmartFilter(SmartFilterMode.OLDER_THAN_30_DAYS) }
        filterRow2.addView(filterDuplicatesButton, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) })
        filterRow2.addView(filterOldButton, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(4) })
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
        addView(wrap(controls), bottom(14))

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

        val reviewCard = card()
        reviewCard.addView(label(getString(R.string.review_title), 11f, R.color.sweep_primary_glow, true))
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
        addView(wrap(reviewCard), bottom(14))
    }

    private fun buildOaasMissionsSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        val oaasCard = card()
        oaasCard.addView(label(getString(R.string.oaas_card_title), 11f, R.color.sweep_primary_glow, true))
        oaasCard.addView(label(getString(R.string.oaas_card_headline), 18f, R.color.sweep_text, true), top(6))
        oaasCard.addView(label(getString(R.string.oaas_card_subtitle), 12f, R.color.sweep_muted, false), top(4))
        oaasPipelineStatus = label(getString(R.string.oaas_pipeline_idle), 12f, R.color.sweep_primary_glow, true)
        oaasCard.addView(oaasPipelineStatus, top(10))

        oaasMissionLargeButton = button(getString(R.string.oaas_mission_large)) {
            runOutcomeMission(DesiredOutcomeMission.FREE_MAX_SPACE_NOW)
            switchTab(AppTab.FILES)
        }.apply {
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
        }
        oaasCard.addView(oaasMissionLargeButton, top(12))

        oaasMissionDuplicatesButton = button(getString(R.string.oaas_mission_duplicates)) {
            runOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
            switchTab(AppTab.FILES)
        }
        oaasCard.addView(oaasMissionDuplicatesButton, top(8))

        oaasMissionOldButton = button(getString(R.string.oaas_mission_old)) {
            runOutcomeMission(DesiredOutcomeMission.ARCHIVE_OLD_MEDIA_30D)
            switchTab(AppTab.FILES)
        }
        oaasCard.addView(oaasMissionOldButton, top(8))

        oaasFlywheelMetrics = label(
            getString(
                R.string.oaas_flywheel_summary,
                0,
                0,
                android.text.format.Formatter.formatFileSize(this@MainActivity, 0L),
            ),
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
        proCard.addView(label(getString(R.string.pro_card_title), 11f, R.color.sweep_accent_amber, true))
        proStatusBadge = label(getString(R.string.pro_status_free), 17f, R.color.sweep_text, true)
        proCard.addView(proStatusBadge, top(6))
        proCard.addView(label(getString(R.string.pro_card_subtitle), 13f, R.color.sweep_muted, false), top(6))

        planAnnualButton = button(getString(R.string.plan_annual_default)) {
            billingGateway.launchPurchaseFlow(this@MainActivity, BillingStatePolicy.PRODUCT_ANNUAL)
        }.apply {
            setTextColor(color(R.color.sweep_background))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.sweep_primary))
        }
        proCard.addView(planAnnualButton, top(14))

        planMonthlyButton = button(getString(R.string.plan_monthly_default)) {
            billingGateway.launchPurchaseFlow(this@MainActivity, BillingStatePolicy.PRODUCT_MONTHLY)
        }
        proCard.addView(planMonthlyButton, top(8))

        planLifetimeButton = button(getString(R.string.plan_lifetime_default)) {
            billingGateway.launchPurchaseFlow(this@MainActivity, BillingStatePolicy.PRODUCT_LIFETIME)
        }
        proCard.addView(planLifetimeButton, top(8))

        restorePurchasesButton = button(getString(R.string.restore_purchases_button)) {
            billingGateway.startAndReconcile()
            statusText.text = getString(R.string.restoring_purchases_status)
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
        proStatusBadge.text = when (state) {
            ProAccessSummaryState.PRO_GRANTED -> getString(R.string.pro_status_active)
            ProAccessSummaryState.PENDING_PAYMENT -> getString(R.string.pro_status_pending)
            ProAccessSummaryState.RECONCILIATION_REQUIRED -> getString(R.string.pro_status_reconciliation)
            ProAccessSummaryState.FREE_NO_ACTIVE_PURCHASE -> getString(R.string.pro_status_free)
        }
    }

    private fun renderCatalogPrices(catalog: Map<String, PlayCatalogProductUi>) {
        if (!::planAnnualButton.isInitialized) return
        val annual = catalog[BillingStatePolicy.PRODUCT_ANNUAL]
        val monthly = catalog[BillingStatePolicy.PRODUCT_MONTHLY]
        val lifetime = catalog[BillingStatePolicy.PRODUCT_LIFETIME]

        if (annual != null && annual.formattedPrice.isNotBlank()) {
            val realDiscount = BillingStatePolicy.calculateRealAnnualDiscountPercent(
                monthlyPriceMicros = monthly?.priceAmountMicros,
                annualPriceMicros = annual.priceAmountMicros,
                monthlyCurrencyCode = monthly?.priceCurrencyCode,
                annualCurrencyCode = annual.priceCurrencyCode,
            )
            planAnnualButton.text = if (realDiscount != null) {
                getString(R.string.plan_annual_with_price_and_discount, annual.formattedPrice, realDiscount)
            } else {
                getString(R.string.plan_annual_with_price, annual.formattedPrice)
            }
        } else if (annual != null && !annual.isAvailable) {
            planAnnualButton.text = getString(R.string.plan_unavailable)
        }
        if (monthly != null && monthly.formattedPrice.isNotBlank()) {
            planMonthlyButton.text = getString(R.string.plan_monthly_with_price, monthly.formattedPrice)
        } else if (monthly != null && !monthly.isAvailable) {
            planMonthlyButton.text = getString(R.string.plan_unavailable)
        }
        if (lifetime != null && lifetime.formattedPrice.isNotBlank()) {
            planLifetimeButton.text = getString(R.string.plan_lifetime_with_price, lifetime.formattedPrice)
        } else if (lifetime != null && !lifetime.isAvailable) {
            planLifetimeButton.text = getString(R.string.plan_unavailable)
        }
    }

    private fun openPlayStoreSubscriptions() {
        runCatching {
            val uri = Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun startScanOrRequestAccess() {
        if (hasAnyMediaPermission()) viewModel.startSession() else requestMediaPermissions()
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
        val canChangePartialAccess = Build.VERSION.SDK_INT >= 34 &&
            isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
            (!isGranted(Manifest.permission.READ_MEDIA_IMAGES) || !isGranted(Manifest.permission.READ_MEDIA_VIDEO))
        permissionButton.visibility = if (
            state.shouldShowReviewAccessAction && (hasDeniedMediaKind() || canChangePartialAccess)
        ) View.VISIBLE else View.GONE
        photosButton.isEnabled = !state.isLoading && isKindAccessible(state, MediaKind.IMAGE)
        videosButton.isEnabled = !state.isLoading && isKindAccessible(state, MediaKind.VIDEO)
        audioButton.isEnabled = !state.isLoading && isKindAccessible(state, MediaKind.AUDIO)
        filterAllButton.isEnabled = !state.isLoading
        filterLargeButton.isEnabled = !state.isLoading
        filterDuplicatesButton.isEnabled = !state.isLoading
        filterOldButton.isEnabled = !state.isLoading
        previousButton.isEnabled = state.canLoadPrevious
        nextButton.isEnabled = state.canLoadNext

        val countInPage = state.currentPage?.records?.size ?: 0
        heroOrbSubtitle.text = if (state.isLoading) {
            getString(R.string.loading_page)
        } else if (countInPage > 0) {
            "$countInPage mídias prontas na aba Arquivos"
        } else {
            "Toque para escanear fotos, vídeos e áudios"
        }

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
            else -> if (state.status == ScanUiStatus.PARTIAL) {
                val details = state.currentPage?.issues.orEmpty()
                    .groupBy { (it.kind ?: state.selectedKind) to it.code }
                    .mapNotNull { (key, grouped) ->
                        val (kind, code) = key
                        val base = when (code) {
                            com.rdf112.sweep.domain.model.PageIssueCode.REVISION_UNVERIFIABLE -> getString(R.string.warn_revision_unverifiable)
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

        updateVolumeSpinner(state)
        updatePage(state)
        updateReview(state)
        updateVisibleCheckboxes(state)
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
        val shaBadge = record.contentSha256?.take(10)?.let { " - SHA-256: ${it}..." }.orEmpty()
        details.addView(label("${displayVolumeName(record.volumeName)} - $date$shaBadge", 10f, R.color.sweep_muted, false), top(2))
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
        moveToTrashButton.isEnabled = hasSelection && trashCoordinator.supportsSystemTrash()
        permanentDeleteButton.isEnabled = hasSelection && trashCoordinator.supportsSystemTrash()
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

    private fun runOutcomeMission(mission: DesiredOutcomeMission) {
        val currentState = viewModel.state.value
        if (currentState.currentPage == null) {
            startScanOrRequestAccess()
            oaasPipelineStatus.text = getString(R.string.oaas_starting_scan_first)
            return
        }
        val plan = viewModel.executeOutcomeMission(mission) ?: return
        outcomeTelemetry.recordMissionRequested(mission, plan.autoSelectedRecords.size)
        refreshFlywheelSummary()
        val formattedBytes = android.text.format.Formatter.formatFileSize(this, plan.estimatedDeliverableBytes)
        oaasPipelineStatus.text = if (plan.isReadyToDeliver) {
            getString(
                R.string.oaas_plan_ready_status,
                plan.autoSelectedRecords.size,
                formattedBytes,
                plan.protectedReferencesKept,
                plan.unconfirmedRecordsCount,
            )
        } else {
            getString(R.string.oaas_plan_empty_status)
        }
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
        if (selected.isEmpty() || !trashCoordinator.supportsSystemTrash()) return
        lifecycleScope.launch {
            val plan = trashCoordinator.prepareAndRevalidatePlan(selected, MediaOperationMode.PERMANENT_DELETE)
            val firstBatch = plan.uriBatches.firstOrNull()
            if (firstBatch.isNullOrEmpty()) {
                statusText.text = getString(R.string.revalidation_none_eligible, plan.rejectedItems.size)
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
                    statusText.text = getString(R.string.billing_pro_required_message)
                }
            }
        }
    }

    private fun refreshStorageStats() {
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            val total = stat.totalBytes.coerceAtLeast(1L)
            val free = stat.availableBytes.coerceAtLeast(0L)
            val used = (total - free).coerceAtLeast(0L)
            val usedFmt = android.text.format.Formatter.formatFileSize(this, used)
            val freeFmt = android.text.format.Formatter.formatFileSize(this, free)
            val totalFmt = android.text.format.Formatter.formatFileSize(this, total)
            storageText.text = getString(R.string.storage_measurement, usedFmt, freeFmt, totalFmt)
            if (::storageUsedHeadline.isInitialized) {
                storageUsedHeadline.text = "$usedFmt de $totalFmt"
                storageFreeSubline.text = "$freeFmt livres neste aparelho"
                val ratio = (used.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
                storageRingGauge.usedFraction = ratio
                storageRingGauge.centerLabel = "${(ratio * 100).toInt()}%"
                storageRingGauge.bottomLabel = "USADO"
            }
        }.onFailure {
            storageText.text = getString(R.string.storage_unavailable)
            if (::storageUsedHeadline.isInitialized) {
                storageUsedHeadline.text = getString(R.string.storage_unavailable)
                storageFreeSubline.text = getString(R.string.storage_unavailable)
                storageRingGauge.usedFraction = 0f
                storageRingGauge.centerLabel = "--"
                storageRingGauge.bottomLabel = "N/D"
            }
        }
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
    }

    private fun wrap(content: LinearLayout): MaterialCardView = MaterialCardView(this).apply {
        radius = dp(22).toFloat()
        cardElevation = dp(2).toFloat()
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
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun button(text: String, onClick: () -> Unit): MaterialButton = MaterialButton(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 13f
        cornerRadius = dp(16)
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
