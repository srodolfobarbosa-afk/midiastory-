package com.rdf112.sweep.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdf112.sweep.R
import com.rdf112.sweep.presentation.ui.theme.MidiastoreColors

@Composable
fun CleanerHomeScreen(
    state: HomeUiState,
    isProActive: Boolean,
    onAnalyze: () -> Unit,
    onMissionClick: (CleanMission) -> Unit,
    onOpenPro: () -> Unit,
    onManagePermissions: () -> Unit = {},
    modifier: Modifier = Modifier,
    /** The host Activity already renders a Pro pill in its top bar; only standalone hosts should enable this. */
    showProBadge: Boolean = false,
    isTabActive: Boolean = true,
) {
    val mariaCaption = when {
        state.isScanning -> stringResource(R.string.cleaner_home_scanning)
        state.recoverableBytes > 0L -> "Olá! Encontrei ${state.recoverableLabel} prontos para liberar com segurança."
        !state.statusMessage.isNullOrBlank() -> state.statusMessage
        else -> "Olá! Encontrei arquivos prontos para liberar espaço com segurança."
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MidiastoreColors.OledBlack)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MariaHeroBanner(
            tabKey = "clean",
            captionText = mariaCaption,
            rawAudioResId = R.raw.maria_clean,
            isActive = isTabActive,
        )

        StorageNowBanner(
            state = state,
            onAnalyze = onAnalyze,
        )

        if (state.needsMediaPermissionPrompt) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MidiastoreColors.AccentBlueSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MidiastoreColors.AccentBlue, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onManagePermissions),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.limited_access_banner),
                        color = MidiastoreColors.AccentBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "›",
                        color = MidiastoreColors.AccentBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        QuickMissionGrid(
            collections = state.collections,
            onMissionClick = onMissionClick,
        )

        if (!isProActive) {
            SalesPsychologyProCard(onOpenPro = onOpenPro)
        }

        // The carousel only adds value once a scan produced real counts; with placeholder data it just
        // duplicated the quick grid and showed clipped "0 items" cards below the fold.
        val scannedCollections = state.collections.filter { it.itemCount > 0 }
        if (scannedCollections.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cleaner_home_mixes_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MidiastoreColors.TextPrimary,
                )
                Text(
                    text = stringResource(R.string.cleaner_home_review),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidiastoreColors.AccentBlue,
                    modifier = Modifier.clickable { onMissionClick(scannedCollections.first().mission) },
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(scannedCollections, key = { it.mission.name }) { collection ->
                    MixCarouselCard(
                        collection = collection,
                        onClick = { onMissionClick(collection.mission) },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.cleaner_home_manual_review),
            color = MidiastoreColors.TextSubtle,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 28.dp),
        )
    }
}

@Composable
private fun SalesPsychologyProCard(
    onOpenPro: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidiastoreColors.CardSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x7A0A84FF), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenPro),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x380A84FF),
                            MidiastoreColors.CardSurface,
                        ),
                    ),
                )
                .padding(horizontal = 15.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MidiastoreColors.AccentBlue,
                ) {
                    Text(
                        text = "PRO + IA NATIVA",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = "Libere limpeza completa + Gemini Nano",
                    color = MidiastoreColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 19.sp,
                )
                Text(
                    text = "A partir de R$ 14,99 • Anual e Vitalício",
                    color = MidiastoreColors.TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "›",
                color = MidiastoreColors.AccentBlue,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun QuickMissionGrid(
    collections: List<CleanCollectionUiModel>,
    onMissionClick: (CleanMission) -> Unit,
) {
    val quickItems = if (collections.isNotEmpty()) {
        collections.take(4)
    } else {
        listOf(
            CleanCollectionUiModel(
                CleanMission.DUPLICATES,
                stringResource(R.string.cleaner_home_fallback_duplicates_title),
                "WhatsApp & Galeria",
                "SHA-256",
                0,
            ),
            CleanCollectionUiModel(
                CleanMission.LARGE_VIDEOS,
                "Vídeos grandes",
                "Revisão rápida",
                "> 10 MB",
                0,
            ),
            CleanCollectionUiModel(
                CleanMission.OLD_SCREENSHOTS,
                "Capturas antigas",
                "Mais de 30 dias",
                "+30 dias",
                0,
            ),
            CleanCollectionUiModel(
                CleanMission.WHATSAPP,
                stringResource(R.string.cleaner_home_fallback_whatsapp_title),
                "Fotos, vídeos e áudios",
                "Mídias",
                0,
            ),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        quickItems.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { item ->
                    val accentColor = when (item.mission) {
                        CleanMission.DUPLICATES -> Color(0xFFFF9F0A)
                        CleanMission.LARGE_VIDEOS -> Color(0xFFBF5AF2)
                        CleanMission.OLD_SCREENSHOTS -> Color(0xFF64D2FF)
                        CleanMission.WHATSAPP -> Color(0xFF30D158)
                        CleanMission.BLURRY_PHOTOS -> Color(0xFFFF375F)
                    }
                    MariaSquareTile(
                        label = item.title,
                        highlight = item.sizeLabel,
                        highlightColor = accentColor,
                        subtitle = if (item.itemCount > 0) "${item.itemCount} itens" else item.subtitle,
                        onClick = { onMissionClick(item.mission) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickTile(
    item: CleanCollectionUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = missionBrush(item.mission)
    Row(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MidiastoreColors.CardSurface)
            .border(1.dp, MidiastoreColors.CardStroke, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(60.dp)
                .background(gradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = missionShortTag(item.mission),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 9.dp)
                .weight(1f),
        ) {
            Text(
                text = item.title,
                color = MidiastoreColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (item.itemCount > 0) "${item.sizeLabel} • ${item.itemCount}" else item.subtitle,
                color = MidiastoreColors.TextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StorageNowBanner(
    state: HomeUiState,
    onAnalyze: () -> Unit,
) {
    val hasRecoverable = state.recoverableBytes > 0L
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidiastoreColors.CardSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MidiastoreColors.CardStroke, RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (hasRecoverable) {
                                R.string.cleaner_home_recoverable_eyebrow
                            } else {
                                R.string.cleaner_home_eyebrow
                            },
                        ),
                        color = MidiastoreColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (hasRecoverable) {
                            state.recoverableLabel
                        } else {
                            stringResource(R.string.storage_used_headline, state.usedLabel)
                        },
                        color = MidiastoreColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = if (state.freeLabel.isNotBlank()) {
                            stringResource(R.string.cleaner_home_free_storage, state.freeLabel) +
                                " • " + state.totalLabel
                        } else {
                            stringResource(R.string.cleaner_home_used_of_total, state.usedLabel, state.totalLabel)
                        },
                        color = MidiastoreColors.TextMuted,
                        fontSize = 12.sp,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isScanning) {
                        CircularProgressIndicator(
                            color = MidiastoreColors.AccentBlue,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp),
                        )
                    } else {
                        val sweepAngle = (state.usedFraction.coerceIn(0f, 1f)) * 360f
                        Canvas(modifier = Modifier.size(58.dp)) {
                            val strokePx = 6.dp.toPx()
                            drawArc(
                                color = MidiastoreColors.CardSurfaceAlt,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokePx),
                            )
                            drawArc(
                                color = MidiastoreColors.AccentBlue,
                                startAngle = -90f,
                                sweepAngle = sweepAngle.coerceAtLeast(12f),
                                useCenter = false,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                            )
                        }
                        Text(
                            text = if (state.usedPercent > 0) "${state.usedPercent}%" else "—",
                            color = MidiastoreColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SegmentedBar(fractions = state.categoryFractions)
            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onAnalyze,
                enabled = !state.isScanning,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MidiastoreColors.AccentBlue,
                    contentColor = Color.White,
                    disabledContainerColor = MidiastoreColors.CardSurfaceAlt,
                    disabledContentColor = MidiastoreColors.TextMuted,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = when {
                        state.isScanning -> stringResource(R.string.cleaner_home_scanning)
                        !state.primaryActionLabel.isNullOrBlank() -> state.primaryActionLabel
                        else -> stringResource(R.string.cleaner_home_analyze)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            if (!state.statusMessage.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = state.statusMessage,
                    color = MidiastoreColors.TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SegmentedBar(fractions: List<Float>) {
    val palette = listOf(
        MidiastoreColors.AccentBlue,
        MidiastoreColors.PurpleSegment,
        MidiastoreColors.GreenSegment,
        MidiastoreColors.AmberSegment,
    )
    val safeFractions = if (fractions.isEmpty()) listOf(0.4f, 0.25f, 0.1f) else fractions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MidiastoreColors.CardSurfaceAlt),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        safeFractions.take(4).forEachIndexed { idx, frac ->
            val clamped = frac.coerceIn(0.03f, 1f)
            Box(
                modifier = Modifier
                    .weight(clamped)
                    .height(6.dp)
                    .background(palette[idx % palette.size]),
            )
        }
        val remaining = (1f - safeFractions.take(4).sum()).coerceAtLeast(0.05f)
        Spacer(Modifier.weight(remaining))
    }
}

@Composable
private fun MixCarouselCard(
    collection: CleanCollectionUiModel,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(146.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(146.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(missionBrush(collection.mission))
                .border(1.dp, MidiastoreColors.CardStroke, RoundedCornerShape(12.dp))
                .padding(12.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column {
                Text(
                    text = collection.sizeLabel,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = if (collection.itemCount > 0) {
                        stringResource(R.string.cleaner_home_items_count, collection.itemCount)
                    } else {
                        collection.subtitle
                    },
                    color = Color(0xCCFFFFFF),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = collection.title,
            color = MidiastoreColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = collection.subtitle,
            color = MidiastoreColors.TextMuted,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun missionBrush(mission: CleanMission): Brush = when (mission) {
    CleanMission.DUPLICATES -> Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFF003B7A)))
    CleanMission.LARGE_VIDEOS -> Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFF3B0764)))
    CleanMission.WHATSAPP -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF064E3B)))
    CleanMission.OLD_SCREENSHOTS -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFF78350F)))
    CleanMission.BLURRY_PHOTOS -> Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF831843)))
}

private fun missionShortTag(mission: CleanMission): String = when (mission) {
    CleanMission.DUPLICATES -> "2X"
    CleanMission.LARGE_VIDEOS -> "MP4"
    CleanMission.WHATSAPP -> "WA"
    CleanMission.OLD_SCREENSHOTS -> "30D"
    CleanMission.BLURRY_PHOTOS -> "BLUR"
}
