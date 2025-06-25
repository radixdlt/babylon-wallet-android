package com.babylon.wallet.android.presentation.dappdir.common.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.HistoryFilterTag
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppFilters
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DAppTagsBottomSheet(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    filters: DAppFilters,
    onDismissRequest: () -> Unit,
    onFilterTagAdded: (String) -> Unit,
    onFilterTagRemoved: (String) -> Unit,
    onAllFilterTagsRemoved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(isVisible) {
        if (isVisible) {
            scope.launch { sheetState.show() }
        } else {
            scope.launch { sheetState.hide() }
        }
    }

    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = sheetState,
        wrapContent = true,
        onDismissRequest = onDismissRequest,
        enableImePadding = true,
        sheetContent = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.dappDirectory_filters_title),
                    onBackClick = onDismissRequest,
                    backIconType = BackIconType.Close,
                    actions = {
                        RadixTextButton(
                            text = stringResource(R.string.transactionHistory_filters_clearAll),
                            onClick = onAllFilterTagsRemoved
                        )
                    }
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                    content = {
                        filters.availableTags.forEach { tag ->
                            val isSelected = remember(filters) {
                                filters.isTagSelected(tag)
                            }

                            HistoryFilterTag(
                                selected = isSelected,
                                text = tag,
                                showCloseIcon = false,
                                onClick = {
                                    if (isSelected) {
                                        onFilterTagRemoved(tag)
                                    } else {
                                        onFilterTagAdded(tag)
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }
    )
}
