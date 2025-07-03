package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.password

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.composables.FactorSourcesList
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
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

@Composable
fun PasswordsScreen(
    viewModel: PasswordsViewModel,
    onNavigateToPasswordFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit,
    onNavigateToAddPassword: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is PasswordsViewModel.Event.NavigateToPasswordFactorSourceDetails -> {
                    onNavigateToPasswordFactorSourceDetails(event.factorSourceId)
                }
            }
        }
    }

    PasswordsContent(
        passwordFactorSources = state.passwordFactorSources,
        onPasswordFactorSourceClick = viewModel::onPasswordFactorSourceClick,
        onAddPasswordClick = onNavigateToAddPassword,
        onBackClick = onBackClick,
    )
}

@Composable
private fun PasswordsContent(
    modifier: Modifier = Modifier,
    passwordFactorSources: PersistentList<FactorSourceCard>,
    onPasswordFactorSourceClick: (FactorSourceId) -> Unit,
    onAddPasswordClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.factorSources_card_passwordTitle),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            HorizontalDivider(color = RadixTheme.colors.divider)

            FactorSourcesList(
                factorSources = passwordFactorSources,
                factorSourceDescriptionText = R.string.factorSources_card_passwordDescription,
                addFactorSourceButtonTitle = R.string.factorSources_list_passwordAdd,
                onFactorSourceClick = onPasswordFactorSourceClick,
                onAddFactorSourceClick = onAddPasswordClick
            )
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun PasswordsScreenPreview() {
    RadixWalletTheme {
        PasswordsContent(
            passwordFactorSources = persistentListOf(
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.PASSWORD,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Panathinaikos",
                    includeDescription = false,
                    lastUsedOn = "Today",
                    kind = FactorSourceKind.PASSWORD,
                    messages = persistentListOf(),
                    accounts = persistentListOf(Account.sampleMainnet()),
                    personas = persistentListOf(),
                    hasHiddenEntities = false,
                    supportsBabylon = true,
                    isEnabled = true
                ),
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.PASSWORD,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Gate 13",
                    includeDescription = false,
                    lastUsedOn = "Last year",
                    kind = FactorSourceKind.PASSWORD,
                    messages = persistentListOf(),
                    accounts = persistentListOf(),
                    personas = persistentListOf(),
                    hasHiddenEntities = false,
                    supportsBabylon = true,
                    isEnabled = true
                ),
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.PASSWORD,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Gate13",
                    includeDescription = false,
                    lastUsedOn = "Last year",
                    kind = FactorSourceKind.PASSWORD,
                    messages = persistentListOf(),
                    accounts = persistentListOf(Account.sampleMainnet()),
                    personas = persistentListOf(),
                    hasHiddenEntities = false,
                    supportsBabylon = true,
                    isEnabled = true
                )
            ),
            onPasswordFactorSourceClick = {},
            onAddPasswordClick = {},
            onBackClick = {}
        )
    }
}
