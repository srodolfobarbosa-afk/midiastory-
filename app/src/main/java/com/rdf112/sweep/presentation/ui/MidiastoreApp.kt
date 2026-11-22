package com.rdf112.sweep.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdf112.sweep.presentation.ui.theme.MidiastoreColors
import com.rdf112.sweep.presentation.ui.theme.MidiastoreTheme
import java.util.Locale

@Composable
fun MidiastoreApp(
    uiState: MidiastoreUiState,
    callbacks: MidiastoreCallbacks,
) {
    MidiastoreTheme {
        Scaffold(
            containerColor = MidiastoreColors.OledBlack,
            bottomBar = {
                MidiastoreBottomBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = callbacks.onTabSelected,
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(innerPadding),
            ) {
                when (uiState.selectedTab) {
                    MidiastoreTab.Clean -> CleanerHomeScreen(
                        state = uiState.home,
                        isProActive = uiState.pro.isProActive,
                        onAnalyze = callbacks.onAnalyzeStorage,
                        onMissionClick = callbacks.onMissionClick,
                        onOpenPro = { callbacks.onTabSelected(MidiastoreTab.Pro) },
                    )
                    MidiastoreTab.Files -> FilesScreen(
                        state = uiState.files,
                        onFilterSelected = callbacks.onFilterSelected,
                        onToggleFileSelection = callbacks.onToggleFileSelection,
                    )
                    MidiastoreTab.Pro -> ProScreen(
                        state = uiState.pro,
                        onPurchaseRequested = callbacks.onPurchaseRequested,
                        onRestorePurchases = callbacks.onRestorePurchases,
                    )
                }

                AnimatedVisibility(
                    visible = uiState.selectedTab == MidiastoreTab.Files && uiState.files.selectedCount > 0,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    SelectionMiniBar(
                        selectedCount = uiState.files.selectedCount,
                        selectedBytes = uiState.files.selectedBytes,
                        onClearSelection = callbacks.onClearSelection,
                        onDeleteSelected = callbacks.onDeleteSelected,
                    )
                }

                uiState.errorMessage?.let { msg ->
                    Surface(
                        color = Color(0xFF2A1414),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(14.dp)
                            .border(1.dp, MidiastoreColors.DangerRed, RoundedCornerShape(12.dp))
                            .clickable(onClick = callbacks.onDismissError),
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionMiniBar(
    selectedCount: Int,
    selectedBytes: Long,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (selectedCount == 1) "1 arquivo selecionado" else "$selectedCount arquivos selecionados",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${formatBytesShort(selectedBytes)} prontos para revisar",
                    color = Color(0xFF93C5FD),
                    fontSize = 11.sp,
                )
            }
            Text(
                text = "Limpar",
                color = MidiastoreColors.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClearSelection)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = onDeleteSelected,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MidiastoreColors.DangerRed,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Excluir",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MidiastoreBottomBar(
    selectedTab: MidiastoreTab,
    onTabSelected: (MidiastoreTab) -> Unit,
) {
    NavigationBar(
        containerColor = MidiastoreColors.CardSurface,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        val items = listOf(
            MidiastoreTab.Clean to "Limpar",
            MidiastoreTab.Files to "Arquivos",
            MidiastoreTab.Pro to "Pro",
        )
        items.forEach { (tab, label) ->
            val selected = tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = { TabVectorIcon(tab = tab, active = selected) },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MidiastoreColors.AccentBlue,
                    selectedTextColor = MidiastoreColors.AccentBlue,
                    unselectedIconColor = MidiastoreColors.TextMuted,
                    unselectedTextColor = MidiastoreColors.TextMuted,
                    indicatorColor = MidiastoreColors.AccentBlueSoft,
                ),
            )
        }
    }
}

@Composable
private fun TabVectorIcon(tab: MidiastoreTab, active: Boolean) {
    val tint = if (active) MidiastoreColors.AccentBlue else MidiastoreColors.TextMuted
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = Stroke(width = 1.9.dp.toPx())
        when (tab) {
            MidiastoreTab.Clean -> {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.14f)
                    lineTo(size.width * 0.88f, size.height * 0.48f)
                    lineTo(size.width * 0.88f, size.height * 0.88f)
                    lineTo(size.width * 0.12f, size.height * 0.88f)
                    lineTo(size.width * 0.12f, size.height * 0.48f)
                    close()
                }
                drawPath(path = path, color = tint, style = stroke)
            }
            MidiastoreTab.Files -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.14f, size.height * 0.14f),
                    size = Size(size.width * 0.72f, size.height * 0.72f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = stroke,
                )
                drawCircle(
                    color = tint,
                    radius = size.minDimension * 0.11f,
                    center = Offset(size.width * 0.36f, size.height * 0.38f),
                )
            }
            MidiastoreTab.Pro -> {
                val path = Path().apply {
                    moveTo(size.width * 0.50f, size.height * 0.10f)
                    lineTo(size.width * 0.62f, size.height * 0.38f)
                    lineTo(size.width * 0.92f, size.height * 0.40f)
                    lineTo(size.width * 0.68f, size.height * 0.60f)
                    lineTo(size.width * 0.76f, size.height * 0.88f)
                    lineTo(size.width * 0.50f, size.height * 0.72f)
                    lineTo(size.width * 0.24f, size.height * 0.88f)
                    lineTo(size.width * 0.32f, size.height * 0.60f)
                    lineTo(size.width * 0.08f, size.height * 0.40f)
                    lineTo(size.width * 0.38f, size.height * 0.38f)
                    close()
                }
                drawPath(path = path, color = tint, style = stroke)
            }
        }
    }
}

private fun formatBytesShort(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}
