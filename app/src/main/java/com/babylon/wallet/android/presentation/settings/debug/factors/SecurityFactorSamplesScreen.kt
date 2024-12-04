package com.babylon.wallet.android.presentation.settings.debug.factors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.composables.card.RemovableFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableMultiChoiceFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableSingleChoiceFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun SecurityFactorSamplesScreen(
    viewModel: SecurityFactorSamplesViewModel,
    onBackClick: () -> Unit
) {
    val state = viewModel.state.collectAsState().value

    SecurityFactorSamplesContent(
        state = state,
        onBackClick = onBackClick,
        onSelect = viewModel::onSelect,
        onCheckedChange = viewModel::onCheckedChange,
        onRemoveClick = viewModel::onRemoveClick
    )
}

@Composable
private fun SecurityFactorSamplesContent(
    modifier: Modifier = Modifier,
    state: SecurityFactorSamplesViewModel.State,
    onBackClick: () -> Unit,
    onSelect: (FactorSourceCard) -> Unit,
    onCheckedChange: (FactorSourceCard, Boolean) -> Unit,
    onRemoveClick: (FactorSourceCard) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.securityFactors_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            items(state.displayOnlyItems) {
                FactorSourceCardView(
                    item = it
                )
            }

            items(state.singleChoiceItems) {
                SelectableSingleChoiceFactorSourceCard(
                    item = it.item,
                    isSelected = it.isSelected,
                    onSelect = onSelect
                )
            }

            items(state.multiChoiceItems) {
                SelectableMultiChoiceFactorSourceCard(
                    item = it.item,
                    isChecked = it.isSelected,
                    onCheckedChange = onCheckedChange
                )
            }

            items(state.removableItems) {
                RemovableFactorSourceCard(
                    item = it,
                    onRemoveClick = onRemoveClick
                )
            }
        }
    }
}

@Composable
@Preview
private fun SecurityFactorSamplesPreview() {
    RadixWalletPreviewTheme {
        SecurityFactorSamplesContent(
            state = SecurityFactorSamplesViewModel.State(),
            onBackClick = {},
            onSelect = {},
            onCheckedChange = { _, _ -> },
            onRemoveClick = {}
        )
    }
}