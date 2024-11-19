package com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.sargon.sample
import rdx.works.profile.domain.DeviceFactorSourceData

@Composable
fun SeedPhrasesScreen(
    modifier: Modifier = Modifier,
    viewModel: SeedPhrasesViewModel,
    onBackClick: () -> Unit,
    onNavigateToRecoverMnemonic: () -> Unit,
    onNavigateToSeedPhrase: (FactorSourceId.Hash) -> Unit
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
                    context.biometricAuthenticate { result ->
                        if (result == BiometricAuthenticationResult.Succeeded) {
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
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.displayMnemonics_seedPhrases),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        WarningText(
                            modifier = Modifier.fillMaxWidth(),
                            text = AnnotatedString(stringResource(R.string.displayMnemonics_seedPhraseSecurityInfo))
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
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            val icon = if (mnemonicNeedsRecovery) DSR.ic_warning_error else DSR.ic_seed_phrases
            val tint = if (mnemonicNeedsRecovery) RadixTheme.colors.orange1 else RadixTheme.colors.gray1
            val text = if (mnemonicNeedsRecovery) {
                stringResource(id = R.string.securityProblems_no9_securityFactors)
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
                            if (data.allAccounts.size == 1) {
                                R.string.displayMnemonics_connectedAccountsPersonasLabel_one
                            } else {
                                R.string.displayMnemonics_connectedAccountsPersonasLabel_many
                            }
                        } else {
                            if (data.allAccounts.size == 1) {
                                R.string.displayMnemonics_connectedAccountsLabel_one
                            } else if (data.allAccounts.isEmpty()) {
                                R.string.seedPhrases_seedPhrase_noConnectedAccounts
                            } else {
                                R.string.displayMnemonics_connectedAccountsLabel_many
                            }
                        },
                        data.allAccounts.size
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
            WarningText(
                text = AnnotatedString(stringResource(id = R.string.securityProblems_no3_seedPhrases)),
                contentColor = RadixTheme.colors.orange1
            )
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
        if (data.hasOnlyHiddenAccounts) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge)
                    .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectMedium)
                    .padding(RadixTheme.dimensions.paddingXXLarge),
                text = stringResource(id = R.string.seedPhrases_hiddenAccountsOnly),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
        } else {
            data.activeAccounts.forEach { account ->
                SimpleAccountCard(
                    modifier = Modifier.fillMaxWidth(),
                    account = account
                )
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SeedPhrasesWithAccountsAndPersonasPreview() {
    RadixWalletPreviewTheme {
        SeedPhraseContent(
            onBackClick = {},
            deviceFactorSourceData = persistentListOf(
                DeviceFactorSourceData(
                    deviceFactorSource = FactorSource.Device.sample(),
                    allAccounts = Account.sampleMainnet.all,
                    personas = Persona.sampleMainnet.all
                ),
                DeviceFactorSourceData(
                    deviceFactorSource = FactorSource.Device.sample.other(),
                    allAccounts = persistentListOf(Account.sampleMainnet())
                )
            ),
            onSeedPhraseClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SeedPhrasesWithoutAccountsAndPersonasPreview() {
    RadixWalletPreviewTheme {
        SeedPhraseContent(
            onBackClick = {},
            deviceFactorSourceData = persistentListOf(
                DeviceFactorSourceData(
                    deviceFactorSource = FactorSource.Device.sample(),
                ),
                DeviceFactorSourceData(
                    deviceFactorSource = FactorSource.Device.sample.other(),
                    allAccounts = persistentListOf(Account.sampleMainnet())
                )
            ),
            onSeedPhraseClick = {}
        )
    }
}
