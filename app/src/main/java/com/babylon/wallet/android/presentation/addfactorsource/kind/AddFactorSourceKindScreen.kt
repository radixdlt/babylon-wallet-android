package com.babylon.wallet.android.presentation.addfactorsource.kind

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableSingleChoiceFactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceKindCard
import com.radixdlt.sargon.FactorSourceKind
import kotlinx.collections.immutable.persistentListOf

@Composable
fun AddFactorSourceKindScreen(
    modifier: Modifier = Modifier,
    viewModel: AddFactorSourceKindViewModel,
    onDismiss: () -> Unit,
    onComplete: (FactorSourceKind) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AddFactorSourceKindContent(
        modifier = modifier,
        state = state,
        onBackClick = viewModel::onBackClick,
        onSelectFactorSourceKindCard = viewModel::onSelectFactorSourceKindCard,
        onAddClick = viewModel::onAddClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                AddFactorSourceKindViewModel.Event.Dismiss -> onDismiss()
                is AddFactorSourceKindViewModel.Event.Complete -> onComplete(event.factorSourceKind)
            }
        }
    }
}

@Composable
private fun AddFactorSourceKindContent(
    modifier: Modifier = Modifier,
    state: AddFactorSourceKindViewModel.State,
    onBackClick: () -> Unit,
    onSelectFactorSourceKindCard: (FactorSourceKindCard) -> Unit,
    onAddClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.empty),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onAddClick,
                text = "Add Security Factor", // TODO localise
                enabled = state.isButtonEnabled
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = RadixTheme.dimensions.paddingDefault,
                end = RadixTheme.dimensions.paddingDefault,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + RadixTheme.dimensions.paddingSemiLarge
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(id = R.string.addFactorSource_selectKind_title),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(id = R.string.addFactorSource_selectKind_description),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }

            items(state.items) { item ->
                SelectableSingleChoiceFactorSourceKindCard(
                    modifier = Modifier
                        .padding(bottom = RadixTheme.dimensions.paddingDefault),
                    item = item,
                    onSelect = onSelectFactorSourceKindCard
                )
            }
        }
    }
}

@Composable
@Preview
private fun AddFactorSourceKindPreview() {
    RadixWalletPreviewTheme {
        AddFactorSourceKindContent(
            state = AddFactorSourceKindViewModel.State(
                isLoading = false,
                items = listOf(
                    Selectable(
                        data = FactorSourceKindCard(
                            kind = FactorSourceKind.DEVICE,
                            messages = persistentListOf()
                        )
                    ),
                    Selectable(
                        data = FactorSourceKindCard(
                            kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                            messages = persistentListOf()
                        )
                    )
                )
            ),
            onBackClick = {},
            onSelectFactorSourceKindCard = {},
            onAddClick = {}
        )
    }
}
