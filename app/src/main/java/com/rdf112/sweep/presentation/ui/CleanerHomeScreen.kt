package com.rdf112.sweep.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A192F),
                        MidiastoreColors.OledBlack,
                        MidiastoreColors.OledBlack,
                    ),
                ),
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cleaner_home_space_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MidiastoreColors.TextPrimary,
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isProActive) MidiastoreColors.AccentBlueSoft else MidiastoreColors.CardSurface,
                    modifier = Modifier
                        .border(1.dp, MidiastoreColors.CardStroke, RoundedCornerShape(999.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onOpenPro),
                ) {
                    Text(
                        text = stringResource(
                            if (isProActive) R.string.cleaner_home_pro_badge else R.string.cleaner_home_free_badge,
                        ),
                        color = if (isProActive) MidiastoreColors.AccentBlue else MidiastoreColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }

        item {
            QuickMissionGrid(
                collections = state.collections,
                onMissionClick = onMissionClick,
            )
        }

        item {
            StorageNowBanner(
                state = state,
                onAnalyze = onAnalyze,
            )
        }

        if (state.collections.isNotEmpty()) {
            item {
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
                        color = MidiastoreColors.TextMuted,
                        modifier = Modifier.clickable { onMissionClick(state.collections.first().mission) },
                    )
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.collections, key = { it.mission.name }) { collection ->
                        MixCarouselCard(
                            collection = collection,
                            onClick = { onMissionClick(collection.mission) },
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
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
                stringResource(R.string.cleaner_home_fallback_duplicates_sub),
                "—",
                0,
            ),
            CleanCollectionUiModel(
                CleanMission.LARGE_VIDEOS,
                stringResource(R.string.cleaner_home_fallback_large_videos_title),
                stringResource(R.string.cleaner_home_fallback_large_videos_sub),
                "—",
                0,
            ),
            CleanCollectionUiModel(
                CleanMission.WHATSAPP,
                stringResource(R.string.cleaner_home_fallback_whatsapp_title),
                stringResource(R.string.cleaner_home_fallback_whatsapp_sub),
                "—",
                0,
            ),
            CleanCollectionUiModel(
                CleanMission.OLD_SCREENSHOTS,
                stringResource(R.string.cleaner_home_fallback_screenshots_title),
                stringResource(R.string.cleaner_home_fallback_screenshots_sub),
                "—",
                0,
            ),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        quickItems.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { item ->
                    QuickTile(
                        item = item,
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
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MidiastoreColors.CardSurfaceAlt)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(gradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = missionShortTag(item.mission),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
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
                fontSize = 11.sp,
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidiastoreColors.CardSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MidiastoreColors.CardStroke, RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cleaner_home_recoverable_eyebrow),
                    color = MidiastoreColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.recoverableLabel,
                    color = MidiastoreColors.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = stringResource(R.string.cleaner_home_used_of_total, state.usedLabel, state.totalLabel),
                    color = MidiastoreColors.TextMuted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
                SegmentedBar(fractions = state.categoryFractions)
            }

            Spacer(Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MidiastoreColors.AccentBlue)
                    .clickable(enabled = !state.isScanning, onClick = onAnalyze),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isScanning) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.28f, size.height * 0.16f)
                            lineTo(size.width * 0.84f, size.height * 0.50f)
                            lineTo(size.width * 0.28f, size.height * 0.84f)
                            close()
                        }
                        drawPath(path = path, color = Color.White)
                    }
                }
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
                    text = stringResource(R.string.cleaner_home_items_count, collection.itemCount),
                    color = Color(0xCCFFFFFF),
                    fontSize = 11.sp,
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
