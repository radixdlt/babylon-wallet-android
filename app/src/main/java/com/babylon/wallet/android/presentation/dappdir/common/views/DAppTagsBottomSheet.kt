package com.babylon.wallet.android.presentation.dappdir.common.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.HistoryFilterTag
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppFilters
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
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
            Box {
                Column(
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    RadixCenteredTopAppBar(
                        title = stringResource(R.string.dappDirectory_filters_title),
                        onBackClick = onDismissRequest,
                        backIconType = BackIconType.Close,
                        actions = {
                            if (filters.selectedTags.isNotEmpty()) {
                                RadixTextButton(
                                    text = stringResource(R.string.transactionHistory_filters_clearAll),
                                    onClick = onAllFilterTagsRemoved
                                )
                            }
                        }
                    )

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
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

                RadixBottomBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onClick = onDismissRequest,
                    text = stringResource(id = R.string.common_confirm),
                    insets = WindowInsets(0.dp)
                )
            }
        }
    )
}
