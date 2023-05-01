package com.babylon.wallet.android.presentation.settings.seedphrase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.StandardOneLineCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.collections.immutable.ImmutableList
import rdx.works.profile.data.model.factorsources.FactorSource

@Composable
fun ShowMnemonicScreen(
    modifier: Modifier = Modifier,
    viewModel: ShowMnemonicViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SeedPhraseContent(
        factorSources = state.factorSources,
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        onBackClick = onBackClick,
        onShowMnemonic = viewModel::onShowMnemonic,
    )
    when (val dialogState = state.visibleMnemonic) {
        is VisibleMnemonic.Shown -> {
            val mnemonic = dialogState.mnemonic
            BasicPromptAlertDialog(
                finish = { confirmed ->
                    viewModel.closeMnemonicDialog(if (confirmed) dialogState.factorSourceID else null)
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.mnemonic),
                        style = RadixTheme.typography.body2Header,
                        color = RadixTheme.colors.gray1
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            id = R.string.mnemonic_and_passphrase,
                            mnemonic.mnemonic,
                            mnemonic.bip39Passphrase.ifEmpty {
                                stringResource(
                                    id = R.string.none
                                )
                            }
                        ),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray1
                    )
                },
                confirmText = stringResource(id = R.string.backed_up_mnemonic)
            )
        }
        else -> {}
    }
}

@Composable
private fun SeedPhraseContent(
    factorSources: ImmutableList<FactorSource>,
    modifier: Modifier,
    onBackClick: () -> Unit,
    onShowMnemonic: (FactorSource.ID) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.background(RadixTheme.colors.defaultBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(id = R.string.mnemonics),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1,
            backIconType = BackIconType.Back
        )
        Divider(color = RadixTheme.colors.gray5)
        LazyColumn(
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.here_are_all_your_mnemonics),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                InfoLink(stringResource(R.string.what_is_a_mnemonic), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
            itemsIndexed(items = factorSources) { _, factorSource ->
                StandardOneLineCard(
                    "",
                    factorSource.hint,
                    modifier = Modifier
                        .shadow(elevation = 8.dp, shape = RadixTheme.shapes.roundedRectMedium)
                        .clip(RadixTheme.shapes.roundedRectMedium)
                        .throttleClickable {
                            context.biometricAuthenticate { authenticatedSuccessfully ->
                                if (authenticatedSuccessfully) {
                                    onShowMnemonic(factorSource.id)
                                }
                            }
                        }
                        .fillMaxWidth()
                        .background(
                            RadixTheme.colors.white,
                            shape = RadixTheme.shapes.roundedRectMedium
                        )
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingLarge,
                            vertical = RadixTheme.dimensions.paddingDefault
                        )
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
        }
    }
}
