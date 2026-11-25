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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onShareResultOrInvite: (Long) -> Unit = {},
    onOpenLanguageSelector: () -> Unit = {},
    hasAnnualFreeTrial: Boolean = false,
    modifier: Modifier = Modifier,
    /** The host Activity already renders a Pro pill in its top bar; only standalone hosts should enable this. */
    showProBadge: Boolean = false,
    isTabActive: Boolean = true,
) {
    val defaultCaption = when {
        state.isScanning -> stringResource(R.string.cleaner_home_scanning)
        state.recoverableBytes > 0L -> MariaI18n.homeDefaultGreeting(state.recoverableLabel)
        !state.statusMessage.isNullOrBlank() -> state.statusMessage
        else -> MariaI18n.homeDefaultGreeting(null)
    }
    var customCaption by remember { mutableStateOf<String?>(null) }
    var activeAudioRes by remember { mutableIntStateOf(R.raw.maria_clean) }
    var speechNonce by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MidiastoreColors.OledBlack)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onOpenLanguageSelector) {
                Text(
                    text = stringResource(R.string.app_language_button),
                    color = MidiastoreColors.AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        MariaHeroBanner(
            tabKey = "clean",
            captionText = customCaption ?: defaultCaption,
            rawAudioResId = activeAudioRes,
            isActive = isTabActive,
            speechNonce = speechNonce,
        )

        StorageNowBanner(
            state = state,
            onAnalyze = {
                customCaption = null
                activeAudioRes = R.raw.maria_clean
                speechNonce++
                onAnalyze()
            },
            onShareFreed = { bytes -> onShareResultOrInvite(bytes) },
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
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.limited_access_banner),
                        color = MidiastoreColors.AccentBlue,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp,
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

        // Quadradinhos 2x2 com valores em MB logo acima para aparecerem inteiros na tela inicial!
        QuickMissionGrid(
            collections = state.collections,
            onMissionClick = { mission ->
                when (mission) {
                    CleanMission.DUPLICATES -> {
                        customCaption = MariaI18n.duplicatesTitle()
                        activeAudioRes = R.raw.maria_dup
                    }
                    CleanMission.SIMILAR_PHOTOS -> {
                        customCaption = MariaI18n.similarPhotosTitle() + " • " + MariaI18n.bestShotProtectedSub()
                        activeAudioRes = R.raw.maria_dup
                    }
                    CleanMission.LARGE_VIDEOS -> {
                        customCaption = MariaI18n.largeVideosTitle() + " (> 10 MB)"
                        activeAudioRes = R.raw.maria_videos
                    }
                    CleanMission.WHATSAPP -> {
                        customCaption = MariaI18n.socialChatTileTitle() + " • " + MariaI18n.socialChatTileSub()
                        activeAudioRes = R.raw.maria_whatsapp
                    }
                    CleanMission.OLD_SCREENSHOTS -> {
                        customCaption = MariaI18n.oldScreenshotsTitle() + " • " + MariaI18n.moreThan30DaysSub()
                        activeAudioRes = R.raw.maria_screenshots
                    }
                    CleanMission.BLURRY_PHOTOS -> {
                        customCaption = MariaI18n.blurryPhotosTitle()
                        activeAudioRes = R.raw.maria_clean
                    }
                }
                speechNonce++
                onMissionClick(mission)
            },
        )

        // Keep the optional Pro offer after the core cleanup actions.
        SalesPsychologyProCard(
            onOpenPro = onOpenPro,
            hasAnnualFreeTrial = hasAnnualFreeTrial && !isProActive,
        )

        ShareViralHomeCard(
            freedBytes = state.lifetimeFreedBytes,
            onShareClick = { onShareResultOrInvite(state.lifetimeFreedBytes) },
        )

        Text(
            text = stringResource(R.string.cleaner_home_manual_review),
            color = MidiastoreColors.TextSubtle,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun ShareViralHomeCard(
    freedBytes: Long,
    onShareClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MidiastoreColors.CardSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MidiastoreColors.CardStroke, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onShareClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (freedBytes > 0L) R.string.share_freed_result_button else R.string.share_invite_friend_button,
                ),
                color = Color(0xFF30D158),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "›",
                color = Color(0xFF30D158),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SalesPsychologyProCard(
    onOpenPro: () -> Unit,
    hasAnnualFreeTrial: Boolean = false,
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
                    color = if (hasAnnualFreeTrial) Color(0xFF30D158) else MidiastoreColors.AccentBlue,
                ) {
                    Text(
                        text = if (hasAnnualFreeTrial) {
                            stringResource(R.string.pro_annual_trial_badge)
                        } else {
                            MariaI18n.proCardBadge()
                        },
                        color = if (hasAnnualFreeTrial) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = MariaI18n.proBannerTitle(),
                    color = MidiastoreColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 19.sp,
                )
                Text(
                    text = MariaI18n.proBannerSub(),
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
        collections.take(6)
    } else {
        listOf(
            CleanCollectionUiModel(
                CleanMission.DUPLICATES,
                stringResource(R.string.cleaner_home_fallback_duplicates_title),
                stringResource(R.string.cleaner_home_fallback_duplicates_sub),
                "SHA-256",
                0,
            ),
            CleanCollectionUiModel(
                CleanMission.SIMILAR_PHOTOS,
                MariaI18n.similarPhotosTitle(),
                MariaI18n.bestShotProtectedSub(),
                "dHash",
                0,
            ),
            CleanCollectionUiModel(
                CleanMission.LARGE_VIDEOS,
                MariaI18n.largeVideosTitle(),
                MariaI18n.quickReviewSub(),
                "> 10 MB",
                0,
            ),
            CleanCollectionUiModel(
                CleanMission.OLD_SCREENSHOTS,
                MariaI18n.oldScreenshotsTitle(),
                MariaI18n.moreThan30DaysSub(),
                MariaI18n.plus30DaysBadge(),
                0,
            ),
            CleanCollectionUiModel(
                CleanMission.WHATSAPP,
                MariaI18n.socialChatTileTitle(),
                MariaI18n.socialChatTileSub(),
                stringResource(R.string.cleaner_home_fallback_whatsapp_sub),
                0,
            ),
            CleanCollectionUiModel(
                CleanMission.BLURRY_PHOTOS,
                MariaI18n.blurryPhotosTitle(),
                MariaI18n.quickReviewSub(),
                "Local",
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
                        CleanMission.SIMILAR_PHOTOS -> Color(0xFF0A84FF)
                        CleanMission.LARGE_VIDEOS -> Color(0xFFBF5AF2)
                        CleanMission.OLD_SCREENSHOTS -> Color(0xFF64D2FF)
                        CleanMission.WHATSAPP -> Color(0xFF30D158)
                        CleanMission.BLURRY_PHOTOS -> Color(0xFFFF375F)
                    }
                    val tileTitle = when (item.mission) {
                        CleanMission.WHATSAPP -> MariaI18n.socialChatTileTitle()
                        CleanMission.SIMILAR_PHOTOS -> MariaI18n.similarPhotosTitle()
                        else -> item.title
                    }
                    val tileSub = when {
                        item.itemCount > 0 -> stringResource(R.string.cleaner_home_items_count, item.itemCount)
                        item.mission == CleanMission.WHATSAPP -> MariaI18n.socialChatTileSub()
                        item.mission == CleanMission.SIMILAR_PHOTOS -> MariaI18n.bestShotProtectedSub()
                        else -> item.subtitle
                    }
                    MariaSquareTile(
                        label = tileTitle,
                        highlight = item.sizeLabel,
                        highlightColor = accentColor,
                        subtitle = tileSub,
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
    onShareFreed: (Long) -> Unit = {},
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (hasRecoverable) {
                            state.recoverableLabel
                        } else {
                            stringResource(R.string.storage_used_headline, state.usedLabel)
                        },
                        color = MidiastoreColors.TextPrimary,
                        fontSize = 22.sp,
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
                        fontSize = 11.5.sp,
                    )
                }

                Spacer(Modifier.width(10.dp))

                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isScanning) {
                        CircularProgressIndicator(
                            color = MidiastoreColors.AccentBlue,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(44.dp),
                        )
                    } else {
                        val sweepAngle = (state.usedFraction.coerceIn(0f, 1f)) * 360f
                        Canvas(modifier = Modifier.size(50.dp)) {
                            val strokePx = 5.5.dp.toPx()
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            SegmentedBar(fractions = state.categoryFractions)
            if (state.lifetimeFreedBytes > 0L && state.lifetimeFreedLabel.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x1A30D158),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x4D30D158), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onShareFreed(state.lifetimeFreedBytes) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.lifetime_freed_home_badge, state.lifetimeFreedLabel),
                            color = Color(0xFF30D158),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.share_home_pill_action),
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

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
                    modifier = Modifier.padding(vertical = 2.dp),
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
    CleanMission.SIMILAR_PHOTOS -> Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF082F49)))
    CleanMission.LARGE_VIDEOS -> Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFF3B0764)))
    CleanMission.WHATSAPP -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF064E3B)))
    CleanMission.OLD_SCREENSHOTS -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFF78350F)))
    CleanMission.BLURRY_PHOTOS -> Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF831843)))
}

private fun missionShortTag(mission: CleanMission): String = when (mission) {
    CleanMission.DUPLICATES -> "2X"
    CleanMission.SIMILAR_PHOTOS -> "SIM"
    CleanMission.LARGE_VIDEOS -> "MP4"
    CleanMission.WHATSAPP -> "WA"
    CleanMission.OLD_SCREENSHOTS -> "30D"
    CleanMission.BLURRY_PHOTOS -> "BLUR"
}
