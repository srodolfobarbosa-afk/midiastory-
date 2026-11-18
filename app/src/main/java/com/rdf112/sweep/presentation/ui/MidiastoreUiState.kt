package com.rdf112.sweep.presentation.ui

enum class MidiastoreTab { Clean, Files, Pro }

enum class CleanMission { DUPLICATES, LARGE_VIDEOS, WHATSAPP, OLD_SCREENSHOTS, BLURRY_PHOTOS }

enum class FilesFilter { ALL, DUPLICATES, LARGE, SCREENSHOTS, WHATSAPP }

data class CleanCollectionUiModel(
    val mission: CleanMission,
    val title: String,
    val subtitle: String,
    val sizeLabel: String,
    val itemCount: Int,
)

data class HomeUiState(
    val usedLabel: String = "—",
    val totalLabel: String = "—",
    val freeLabel: String = "",
    val recoverableBytes: Long = 0L,
    val recoverableLabel: String = "0 MB",
    val categoryFractions: List<Float> = emptyList(),
    val collections: List<CleanCollectionUiModel> = emptyList(),
    val isScanning: Boolean = false,
)

data class MediaItemUiModel(
    val id: Long,
    val displayName: String,
    val sizeBytes: Long,
    val sizeLabel: String,
    val categoryLabel: String,
    val mimeType: String = "",
)

data class FilesUiState(
    val activeFilter: FilesFilter = FilesFilter.ALL,
    val items: List<MediaItemUiModel> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isPartialCoverage: Boolean = false,
) {
    val selectedCount: Int get() = selectedIds.size
    val selectedBytes: Long
        get() = items.asSequence()
            .filter { it.id in selectedIds }
            .sumOf { it.sizeBytes }
}

data class ProPlanUiModel(
    val productId: String,
    val title: String,
    val priceLabel: String,
    val description: String,
    val isFeatured: Boolean = false,
)

data class ProUiState(
    val isProActive: Boolean = false,
    val plans: List<ProPlanUiModel> = emptyList(),
    val isPurchasing: Boolean = false,
)

data class MidiastoreUiState(
    val selectedTab: MidiastoreTab = MidiastoreTab.Clean,
    val home: HomeUiState = HomeUiState(),
    val files: FilesUiState = FilesUiState(),
    val pro: ProUiState = ProUiState(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class MidiastoreCallbacks(
    val onTabSelected: (MidiastoreTab) -> Unit,
    val onAnalyzeStorage: () -> Unit,
    val onMissionClick: (CleanMission) -> Unit,
    val onFilterSelected: (FilesFilter) -> Unit,
    val onToggleFileSelection: (Long) -> Unit,
    val onClearSelection: () -> Unit,
    val onDeleteSelected: () -> Unit,
    val onPurchaseRequested: (String) -> Unit,
    val onRestorePurchases: () -> Unit,
    val onDismissError: () -> Unit = {},
)
