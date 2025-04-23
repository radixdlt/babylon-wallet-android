package com.babylon.wallet.android.presentation.ui.composables.securityfactors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableSingleChoiceFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.card.subtitle
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

// can be used to list any factor sources (selectable factor source cards) in ChooseFactorSourceBottomSheet
@Composable
fun SelectableFactorSourcesListView(
    modifier: Modifier = Modifier,
    isButtonEnabled: Boolean,
    factorSourceKind: FactorSourceKind,
    factorSources: PersistentList<Selectable<FactorSourceCard>>,
    onFactorSourceSelect: (FactorSourceCard) -> Unit,
    onAddFactorSourceClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = RadixTheme.colors.gray5)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    modifier = Modifier.padding(
                        top = RadixTheme.dimensions.paddingDefault,
                        bottom = RadixTheme.dimensions.paddingSmall
                    ),
                    text = factorSourceKind.subtitle(),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
            }

            items(factorSources) {
                SelectableSingleChoiceFactorSourceCard(
                    item = it,
                    onSelect = onFactorSourceSelect
                )
            }

            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(align = Alignment.CenterHorizontally)
                        .padding(bottom = RadixTheme.dimensions.paddingLarge),
                    text = factorSourceKind.addButtonTitle(),
                    onClick = onAddFactorSourceClick,
                    throttleClicks = true
                )
            }
        }

        RadixBottomBar(
            dividerColor = RadixTheme.colors.gray4,
            button = {
                RadixPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(R.string.common_continue),
                    enabled = isButtonEnabled,
                    onClick = onContinueClick
                )
            },
        )
    }
}

@Composable
private fun FactorSourceKind.addButtonTitle(): String = stringResource(
    id = when (this) {
        FactorSourceKind.DEVICE -> R.string.factorSources_list_deviceAdd
        FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> R.string.factorSources_list_ledgerAdd
        FactorSourceKind.OFF_DEVICE_MNEMONIC -> R.string.factorSources_list_offDeviceMnemonicAdd
        FactorSourceKind.ARCULUS_CARD -> R.string.factorSources_list_arculusCardAdd
        FactorSourceKind.PASSWORD -> R.string.factorSources_list_passwordAdd
    }
)

@Composable
@Preview(showBackground = true)
@UsesSampleValues
private fun SelectableFactorSourcesListPreview() {
    RadixWalletPreviewTheme {
        SelectableFactorSourcesListView(
            factorSourceKind = FactorSourceKind.DEVICE,
            factorSources = persistentListOf(
                Selectable(
                    data = FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.DEVICE,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "My Phone 666",
                        includeDescription = false,
                        lastUsedOn = "Today",
                        kind = FactorSourceKind.DEVICE,
                        messages = persistentListOf(),
                        accounts = persistentListOf(),
                        personas = persistentListOf(),
                        hasHiddenEntities = false,
                        supportsBabylon = true,
                        isEnabled = true
                    )
                ),
                Selectable(
                    data = FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.DEVICE,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "My Phone 999",
                        includeDescription = false,
                        lastUsedOn = "Yesterday",
                        kind = FactorSourceKind.DEVICE,
                        messages = persistentListOf(FactorSourceStatusMessage.SecurityPrompt.LostFactorSource),
                        accounts = persistentListOf(
                            Account.sampleMainnet()
                        ),
                        personas = persistentListOf(),
                        hasHiddenEntities = false,
                        supportsBabylon = true,
                        isEnabled = true
                    )
                )
            ),
            isButtonEnabled = false,
            onFactorSourceSelect = {},
            onAddFactorSourceClick = {},
            onContinueClick = {}
        )
    }
}
