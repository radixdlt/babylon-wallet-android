package com.babylon.wallet.android.presentation.discover.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.discover.common.views.InfoGlossaryItemView
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun InfoListScreen(
    modifier: Modifier = Modifier,
    viewModel: InfoListViewModel,
    onBackClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state: InfoListViewModel.State by viewModel.state.collectAsStateWithLifecycle()

    InfoListContent(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onInfoClick = onInfoClick
    )
}

@Composable
private fun InfoListContent(
    state: InfoListViewModel.State,
    onBackClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.learn_title),
                onBackClick = onBackClick,
                backIconType = BackIconType.Back,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
private fun InfoListPreview() {
    RadixWalletTheme {
        InfoListContent(
            state = InfoListViewModel.State(),
            onBackClick = {},
            onInfoClick = {}
        )
    }
}