package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.composables.ChooseEntityContent
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models.ChooseEntityEvent
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models.ChooseEntityUiState
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet

@Composable
fun ChooseAccountsScreen(
    modifier: Modifier = Modifier,
    viewModel: ChooseAccountsViewModel,
    onDismiss: () -> Unit,
    onSelected: (AddressOfAccountOrPersona) -> Unit,
    onSkip: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChooseAccountsContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onSelectAccount = viewModel::onSelectItem,
        onContinueClick = viewModel::onContinueClick,
        onSkipClick = viewModel::onSkipClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is ChooseEntityEvent.EntitySelected -> onSelected(event.address)
                ChooseEntityEvent.Skip -> onSkip()
            }
        }
    }
}

@Composable
private fun ChooseAccountsContent(
    modifier: Modifier = Modifier,
    state: ChooseEntityUiState<Account>,
    onDismiss: () -> Unit,
    onSelectAccount: (Account) -> Unit,
    onContinueClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    ChooseEntityContent(
        modifier = modifier.fillMaxSize(),
        title = stringResource(id = R.string.shieldWizardApplyShield_chooseAccounts_title),
        subtitle = stringResource(id = R.string.shieldWizardApplyShield_chooseAccounts_subtitle),
        isButtonEnabled = state.isButtonEnabled,
        hasSkipButton = state.canSkip,
        skipButtonTitle = stringResource(id = R.string.shieldWizardApplyShield_chooseAccounts_skipButton),
        onContinueClick = onContinueClick,
        onDismiss = onDismiss,
        onSkipClick = onSkipClick
    ) {
        items(state.items) { account ->
            AccountSelectionCard(
                modifier = Modifier
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingXSmall
                    )
                    .background(
                        brush = account.data.appearanceId.gradient(),
                        shape = RadixTheme.shapes.roundedRectSmall
                    )
                    .clickable { onSelectAccount(account.data) },
                accountName = account.data.displayName.value,
                address = account.data.address,
                checked = account.selected,
                isSingleChoice = true,
                radioButtonClicked = { onSelectAccount(account.data) }
            )
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun ChooseAccountsPreview(
    @PreviewParameter(ChooseAccountsPreviewProvider::class) state: ChooseEntityUiState<Account>
) {
    RadixWalletPreviewTheme {
        ChooseAccountsContent(
            state = state,
            onDismiss = {},
            onSelectAccount = {},
            onContinueClick = {},
            onSkipClick = {}
        )
    }
}

@UsesSampleValues
class ChooseAccountsPreviewProvider : PreviewParameterProvider<ChooseEntityUiState<Account>> {

    override val values: Sequence<ChooseEntityUiState<Account>>
        get() = sequenceOf(
            ChooseEntityUiState(
                items = listOf(
                    Selectable(Account.sampleMainnet(), true),
                    Selectable(Account.sampleStokenet(), false)
                )
            ),
            ChooseEntityUiState(
                items = listOf(
                    Selectable(Account.sampleMainnet(), true),
                    Selectable(Account.sampleStokenet(), true)
                )
            ),
            ChooseEntityUiState()
        )
}
