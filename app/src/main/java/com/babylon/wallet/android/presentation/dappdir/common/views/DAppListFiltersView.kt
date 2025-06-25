package com.babylon.wallet.android.presentation.dappdir.common.views

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.HistoryFilterTag
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppFilters
import com.babylon.wallet.android.presentation.ui.composables.DSR

@Composable
fun DAppListFiltersView(
    filters: DAppFilters,
    isLoading: Boolean,
    isFiltersButtonVisible: Boolean,
    onSearchTermUpdated: (String) -> Unit,
    onFilterTagAdded: (String) -> Unit,
    onFilterTagRemoved: (String) -> Unit,
    onAllFilterTagsRemoved: () -> Unit,
) {
    var isFiltersVisible by remember { mutableStateOf(false) }

    DAppListFiltersContent(
        filters = filters,
        isLoading = isLoading,
        isFiltersButtonVisible = isFiltersButtonVisible,
        onSearchTermUpdated = onSearchTermUpdated,
        onTagFiltersClick = { isFiltersVisible = true },
        onFilterTagRemoved = onFilterTagRemoved
    )

    if (isFiltersVisible) {
        DAppTagsBottomSheet(
            isVisible = isFiltersVisible,
            filters = filters,
            onDismissRequest = { isFiltersVisible = false },
            onFilterTagAdded = onFilterTagAdded,
            onFilterTagRemoved = onFilterTagRemoved,
            onAllFilterTagsRemoved = onAllFilterTagsRemoved
        )
    }

    BackHandler(
        enabled = isFiltersVisible,
        onBack = { isFiltersVisible = false }
    )
}

@Composable
private fun DAppListFiltersContent(
    filters: DAppFilters,
    isLoading: Boolean,
    isFiltersButtonVisible: Boolean,
    onSearchTermUpdated: (String) -> Unit,
    onTagFiltersClick: () -> Unit,
    onFilterTagRemoved: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.background)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = RadixTheme.dimensions.paddingDefault)
                .padding(
                    start = RadixTheme.dimensions.paddingDefault,
                    end = if (isFiltersButtonVisible) {
                        RadixTheme.dimensions.paddingSmall
                    } else {
                        RadixTheme.dimensions.paddingDefault
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadixTextField(
                modifier = Modifier.weight(1f),
                value = filters.searchTerm,
                onValueChanged = onSearchTermUpdated,
                enabled = !isLoading,
                hint = stringResource(R.string.dappDirectory_search_placeholder),
                trailingIcon = {
                    if (filters.searchTerm.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onSearchTermUpdated("")
                            }
                        ) {
                            Icon(
                                painter = painterResource(DSR.ic_close),
                                contentDescription = null,
                                tint = RadixTheme.colors.icon
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(DSR.ic_search),
                            contentDescription = null,
                            tint = if (isLoading) {
                                RadixTheme.colors.backgroundTertiary
                            } else {
                                RadixTheme.colors.icon
                            }
                        )
                    }
                }
            )

            if (isFiltersButtonVisible) {
                AnimatedVisibility(
                    visible = !isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = onTagFiltersClick
                    ) {
                        Icon(
                            painterResource(id = DSR.ic_filter_list),
                            tint = RadixTheme.colors.icon,
                            contentDescription = null
                        )
                    }
                }
            }
        }

        FilterTagsView(
            filters = filters,
            isLoading = isLoading,
            onFilterTagRemoved = onFilterTagRemoved
        )

        HorizontalDivider(color = RadixTheme.colors.divider)
    }
}

@Composable
private fun FilterTagsView(
    filters: DAppFilters,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onFilterTagRemoved: (String) -> Unit
) {
    val tags = remember(filters.selectedTags) {
        filters.selectedTags.toList()
    }
    if (tags.isNotEmpty()) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                RadixTheme.dimensions.paddingMedium
            ),
            contentPadding = PaddingValues(
                start = RadixTheme.dimensions.paddingMedium,
                end = RadixTheme.dimensions.paddingMedium,
                bottom = RadixTheme.dimensions.paddingMedium
            ),
            userScrollEnabled = !isLoading
        ) {
            items(items = tags) { tag ->
                HistoryFilterTag(
                    selected = true,
                    showCloseIcon = true,
                    text = tag,
                    onClick = { onFilterTagRemoved(tag) },
                    onCloseClick = { onFilterTagRemoved(tag) }
                )
            }
        }
    }
}
