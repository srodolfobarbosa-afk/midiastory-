package com.rdf112.sweep.presentation.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdf112.sweep.R
import com.rdf112.sweep.presentation.ui.theme.MidiastoreColors

@Composable
fun FilesScreen(
    state: FilesUiState,
    onFilterSelected: (FilesFilter) -> Unit,
    onToggleFileSelection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MidiastoreColors.ElevatedBg),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            FilesMariaHeaderSection(
                isActive = true,
                onSelectPhotos = { onFilterSelected(FilesFilter.ALL) },
                onSelectLargeVideos = { onFilterSelected(FilesFilter.LARGE) },
                onSelectWhatsApp = { onFilterSelected(FilesFilter.WHATSAPP) },
                onSelectScreenshots = { onFilterSelected(FilesFilter.SCREENSHOTS) },
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FilesFilter.entries) { filter ->
                    val active = filter == state.activeFilter
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (active) MidiastoreColors.AccentBlue else MidiastoreColors.CardSurfaceAlt,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onFilterSelected(filter) },
                    ) {
                        Text(
                            text = filterLabel(filter),
                            color = if (active) Color.White else MidiastoreColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (state.isPartialCoverage) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.files_partial_coverage),
                    color = MidiastoreColors.AmberSegment,
                    fontSize = 11.sp,
                )
            }
        }

        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (state.isPartialCoverage) {
                            R.string.files_empty_state_partial_access
                        } else {
                            R.string.files_empty_state_full_access
                        },
                    ),
                    color = MidiastoreColors.TextMuted,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = if (state.selectedCount > 0) 92.dp else 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    val selected = item.id in state.selectedIds
                    TrackMediaRow(
                        item = item,
                        selected = selected,
                        onClick = { onToggleFileSelection(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackMediaRow(
    item: MediaItemUiModel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MidiastoreColors.AccentBlueSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MidiastoreColors.CardSurfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = mediaBadge(item),
                color = MidiastoreColors.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                color = MidiastoreColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.sizeLabel} • ${item.categoryLabel}",
                color = MidiastoreColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) MidiastoreColors.AccentBlue else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (selected) MidiastoreColors.AccentBlue else MidiastoreColors.TextSubtle,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
    }
}

@Composable
private fun filterLabel(filter: FilesFilter): String = stringResource(
    when (filter) {
        FilesFilter.ALL -> R.string.files_filter_all
        FilesFilter.DUPLICATES -> R.string.files_filter_duplicates
        FilesFilter.LARGE -> R.string.files_filter_large
        FilesFilter.SCREENSHOTS -> R.string.files_filter_screenshots
        FilesFilter.WHATSAPP -> R.string.files_filter_whatsapp
    },
)

@Composable
private fun mediaBadge(item: MediaItemUiModel): String {
    val ext = item.displayName.substringAfterLast('.', "").uppercase()
    return if (ext.length in 2..4) ext else stringResource(R.string.files_media_badge)
}
