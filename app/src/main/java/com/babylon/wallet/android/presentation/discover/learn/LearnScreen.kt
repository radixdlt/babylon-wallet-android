package com.babylon.wallet.android.presentation.discover.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.discover.common.views.InfoGlossaryItemView
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.clearFocusNestedScrollConnection

@Composable
fun LearnScreen(
    modifier: Modifier = Modifier,
    viewModel: LearnViewModel,
    onBackClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state: LearnViewModel.State by viewModel.state.collectAsStateWithLifecycle()

    LearnContent(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onInfoClick = onInfoClick,
        onSearchQueryChange = viewModel::onSearchQueryChange
    )
}

@Composable
private fun LearnContent(
    state: LearnViewModel.State,
    onBackClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = RadixTheme.colors.background)
            ) {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.learn_title),
                    onBackClick = onBackClick,
                    backIconType = BackIconType.Back,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )

                RadixTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(bottom = RadixTheme.dimensions.paddingDefault),
                    value = state.searchQuery,
                    onValueChanged = onSearchQueryChange,
                    hint = stringResource(R.string.learn_searchHint),
                    trailingIcon = {
                        if (state.isSearchQueryEmpty) {
                            Icon(
                                painter = painterResource(DSR.ic_search),
                                contentDescription = null,
                                tint = RadixTheme.colors.icon
                            )
                        } else {
                            IconButton(
                                onClick = { onSearchQueryChange("") }
                            ) {
                                Icon(
                                    painter = painterResource(DSR.ic_close),
                                    contentDescription = null,
                                    tint = RadixTheme.colors.icon
                                )
                            }
                        }
                    }
                )
            }

            HorizontalDivider(color = RadixTheme.colors.divider)
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(clearFocusNestedScrollConnection()),
            contentPadding = PaddingValues(
                RadixTheme.dimensions.paddingDefault
            ),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            items(state.items) { item ->
                InfoGlossaryItemView(
                    item = item,
                    onClick = onInfoClick
                )
            }
        }
    }
}

@Composable
@Preview
private fun LearnPreview() {
    RadixWalletTheme {
        LearnContent(
            state = LearnViewModel.State(
                items = GlossaryItemsProvider.searchableGlossaryItems
            ),
            onBackClick = {},
            onInfoClick = {},
            onSearchQueryChange = {}
        )
    }
}
