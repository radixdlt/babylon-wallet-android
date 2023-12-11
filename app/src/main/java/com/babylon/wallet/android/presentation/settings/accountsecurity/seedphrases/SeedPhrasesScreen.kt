package com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DeviceFactorSourceData
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RedWarningText
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.data.model.factorsources.FactorSource

@Composable
fun SeedPhrasesScreen(
    modifier: Modifier = Modifier,
    viewModel: SeedPhrasesViewModel,
    onBackClick: () -> Unit,
    onNavigateToRecoverMnemonic: () -> Unit,
    onNavigateToSeedPhrase: (FactorSource.FactorSourceID.FromHash) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SeedPhraseContent(
        deviceFactorSourceData = state.deviceFactorSourcesWithAccounts,
        modifier = modifier,
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
                    onNavigateToRecoverMnemonic()
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
    Scaffold(
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.displayMnemonics_seedPhrases),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = RadixTheme.colors.gray5)
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
            ) {
                item {
                    GrayBackgroundWrapper {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                        Text(
                            text = stringResource(id = R.string.seedPhrases_message),
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
                            .padding(
                                horizontal = RadixTheme.dimensions.paddingDefault,
                                vertical = RadixTheme.dimensions.paddingMedium
                            )
                            .fillMaxWidth(),
                        data = deviceFactorSourceItem
                    )
                    if (index != deviceFactorSourceData.size - 1) {
                        HorizontalDivider(
                            Modifier.fillMaxWidth(),
                            color = RadixTheme.colors.gray5
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeedPhraseCard(
    modifier: Modifier,
    data: DeviceFactorSourceData
) {
    val mnemonicNeedsRecovery = data.mnemonicState == DeviceFactorSourceData.MnemonicState.NeedRecover
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            val icon = if (mnemonicNeedsRecovery) DSR.ic_warning_error else DSR.ic_seed_phrases
            val tint = if (mnemonicNeedsRecovery) RadixTheme.colors.red1 else RadixTheme.colors.gray1
            val text = if (mnemonicNeedsRecovery) {
                stringResource(id = R.string.displayMnemonics_seedPhraseEntryWarning)
            } else {
                stringResource(id = R.string.displayMnemonics_cautionAlert_revealButtonLabel)
            }
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = tint
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = RadixTheme.typography.body1Header,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        id = if (data.personas.isNotEmpty()) {
                            if (data.accounts.size == 1) {
                                R.string.displayMnemonics_connectedAccountsPersonasLabel_one
                            } else {
                                R.string.displayMnemonics_connectedAccountsPersonasLabel_many
                            }
                        } else {
                            if (data.accounts.size == 1) {
                                R.string.displayMnemonics_connectedAccountsLabel_one
                            } else {
                                R.string.displayMnemonics_connectedAccountsLabel_many
                            }
                        },
                        data.accounts.size
                    ),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painter = painterResource(
                    id = DSR.ic_chevron_right
                ),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        }
        if (data.mnemonicState == DeviceFactorSourceData.MnemonicState.NotBackedUp) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
            RedWarningText(text = AnnotatedString(stringResource(id = R.string.homePage_securityPromptBackup)))
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
        data.accounts.forEach { account ->
            SimpleAccountCard(
                modifier = Modifier.fillMaxWidth(),
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
                    deviceFactorSource = SampleDataProvider().babylonDeviceFactorSource(),
                    accounts = persistentListOf(
                        SampleDataProvider().sampleAccount(),
                        SampleDataProvider().sampleAccount(),
                        SampleDataProvider().sampleAccount()
                    )
                ),
                DeviceFactorSourceData(
                    deviceFactorSource = SampleDataProvider().olympiaDeviceFactorSource(),
                    accounts = persistentListOf(SampleDataProvider().sampleAccount())
                )
            ),
            onSeedPhraseClick = {}
        )
    }
}
