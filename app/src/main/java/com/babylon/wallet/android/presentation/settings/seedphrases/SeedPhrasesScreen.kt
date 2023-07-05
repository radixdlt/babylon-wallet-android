package com.babylon.wallet.android.presentation.settings.seedphrases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun SeedPhrasesScreen(
    modifier: Modifier = Modifier,
    viewModel: SeedPhrasesViewModel,
    onBackClick: () -> Unit,
    onNavigateToRecoverMnemonic: (FactorSource.FactorSourceID.FromHash) -> Unit,
    onNavigateToSeedPhrase: (FactorSource.FactorSourceID.FromHash) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SeedPhraseContent(
        deviceFactorSourceData = state.deviceFactorSourcesWithAccounts,
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        onBackClick = onBackClick,
        onSeedPhraseClick = viewModel::onSeedPhraseClick,
    )

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is SeedPhrasesViewModel.Effect.OnRequestToShowMnemonic -> {
                    context.biometricAuthenticate { authenticated ->
                        if (authenticated) {
                            onNavigateToSeedPhrase(it.factorSourceID)
                        }
                    }
                }

                is SeedPhrasesViewModel.Effect.OnRequestToRecoverMnemonic -> {
                    context.biometricAuthenticate { authenticated ->
                        if (authenticated) {
                            onNavigateToRecoverMnemonic(it.factorSourceID)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeedPhraseContent(
    deviceFactorSourceData: PersistentList<DeviceFactorSourceData>,
    onBackClick: () -> Unit,
    onSeedPhraseClick: (DeviceFactorSourceData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier.background(RadixTheme.colors.defaultBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(id = R.string.displayMnemonics_seedPhrases),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1,
            backIconType = BackIconType.Back
        )
        Divider(color = RadixTheme.colors.gray5)
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
        ) {
            item {
                GrayBackgroundWrapper {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                    Text(
                        text = stringResource(id = R.string.displayMnemonics_cautionAlert_message),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray2
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    InfoLink(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.displayMnemonics_seedPhraseSecurityInfo),
                        contentColor = RadixTheme.colors.orange1,
                        iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                }
            }
            itemsIndexed(items = deviceFactorSourceData.toList()) { index, deviceFactorSourceItem ->
                SeedPhraseCard(
                    modifier = Modifier
                        .throttleClickable {
                            onSeedPhraseClick(deviceFactorSourceItem)
                        }
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingMedium)
                        .fillMaxWidth(),
                    accounts = deviceFactorSourceItem.accounts,
                    mnemonicState = deviceFactorSourceItem.mnemonicState
                )
                if (index != deviceFactorSourceData.size - 1) {
                    Divider(
                        Modifier.fillMaxWidth(),
                        color = RadixTheme.colors.gray5
                    )
                }
            }
        }
    }
}

@Composable
private fun SeedPhraseCard(
    modifier: Modifier,
    accounts: ImmutableList<Network.Account>,
    mnemonicState: DeviceFactorSourceData.MnemonicState
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_seed_phrases),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.displayMnemonics_cautionAlert_revealButtonLabel),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        id = if (accounts.size == 1) {
                            R.string.displayMnemonics_connectedAccountsLabel_one
                        } else {
                            R.string.displayMnemonics_connectedAccountsLabel_many
                        },
                        accounts.size
                    ),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right
                ),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        }
        val securityPromptRes = when (mnemonicState) {
            DeviceFactorSourceData.MnemonicState.NotBackedUp -> R.string.displayMnemonics_backUpWarning
            DeviceFactorSourceData.MnemonicState.NeedRecover -> R.string.homePage_applySecuritySettings
            else -> null
        }
        securityPromptRes?.let { promptRes ->
            ApplySecuritySettingsLabel(
                modifier = Modifier.fillMaxWidth(),
                onClick = null,
                text = stringResource(id = promptRes),
                labelColor = RadixTheme.colors.gray4.copy(alpha = 0.6f),
                contentColor = RadixTheme.colors.gray1
            )
        }
        accounts.forEachIndexed { index, account ->
            SimpleAccountCard(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(getAccountGradientColorsFor(account.appearanceID)),
                        RadixTheme.shapes.roundedRectSmall
                    )
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    ),
                account = account
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountPreferencePreview() {
    RadixWalletTheme {
        SeedPhraseContent(
            onBackClick = {},
            deviceFactorSourceData = persistentListOf(
                DeviceFactorSourceData(
                    FactorSource.FactorSourceID.FromHash(
                        kind = FactorSourceKind.DEVICE,
                        body = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5")
                    ),
                    persistentListOf(SampleDataProvider().sampleAccount())
                ),
                DeviceFactorSourceData(
                    FactorSource.FactorSourceID.FromHash(
                        kind = FactorSourceKind.DEVICE,
                        body = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5")
                    ),
                    persistentListOf(SampleDataProvider().sampleAccount())
                )
            ),
            onSeedPhraseClick = {}
        )
    }
}
