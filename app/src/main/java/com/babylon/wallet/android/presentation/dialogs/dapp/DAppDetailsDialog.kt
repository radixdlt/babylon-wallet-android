package com.babylon.wallet.android.presentation.dialogs.dapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.DAppWebsiteAddressRow
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.DappDefinitionAddressRow
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.card.FungibleCard
import com.babylon.wallet.android.presentation.ui.composables.card.NonFungibleCard
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import rdx.works.core.domain.resources.Resource

@Composable
fun DAppDetailsDialog(
    viewModel: DAppDetailsDialogViewModel,
    onFungibleClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource) -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    DAppDetailsDialogContent(
        state = state,
        onMessageShown = viewModel::onMessageShown,
        onFungibleClick = onFungibleClick,
        onNonFungibleClick = onNonFungibleClick,
        onDismiss = onDismiss
    )
}

@Composable
private fun DAppDetailsDialogContent(
    modifier: Modifier = Modifier,
    state: DAppDetailsDialogViewModel.State,
    onFungibleClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource) -> Unit,
    onMessageShown: () -> Unit,
    onDismiss: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier,
        title = state.dAppWithResources?.dApp.displayName(),
        onDismiss = onDismiss
    ) {
        Box(modifier = Modifier.fillMaxHeight(fraction = 0.9f)) {
            Column(
                modifier = Modifier
                    .background(RadixTheme.colors.defaultBackground)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        vertical = RadixTheme.dimensions.paddingSemiLarge
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.dAppWithResources != null) {
                    Thumbnail.DApp(
                        modifier = Modifier.size(104.dp),
                        dapp = state.dAppWithResources.dApp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .radixPlaceholder(
                                visible = true,
                                shape = CircleShape
                            )
                    )
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                HorizontalDivider(color = RadixTheme.colors.gray5)

                state.dAppWithResources?.dApp?.description?.let { description ->

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingDefault),
                        text = description,
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.Start
                    )
                    HorizontalDivider(color = RadixTheme.colors.gray5)
                }

                state.dAppWithResources?.dApp?.dAppAddress?.let { dAppAddress ->
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    DappDefinitionAddressRow(
                        dappDefinitionAddress = dAppAddress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }

                if (state.isWebsiteValidating || state.validatedWebsite != null) {
                    DAppWebsiteAddressRow(
                        website = state.validatedWebsite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }

                if (state.dAppWithResources?.fungibleResources?.isNotEmpty() == true) {
                    GrayBackgroundWrapper {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RadixTheme.dimensions.paddingDefault),
                            text = stringResource(id = R.string.authorizedDapps_dAppDetails_tokens),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                state.dAppWithResources?.fungibleResources?.forEach { resource ->
                    GrayBackgroundWrapper {
                        FungibleCard(
                            fungible = resource,
                            showChevron = false,
                            onClick = { onFungibleClick(resource) }
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    }
                }

                if (state.dAppWithResources?.nonFungibleResources?.isNotEmpty() == true) {
                    GrayBackgroundWrapper {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RadixTheme.dimensions.paddingDefault),
                            text = stringResource(id = R.string.authorizedDapps_dAppDetails_nfts),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                state.dAppWithResources?.nonFungibleResources?.forEach { resource ->
                    GrayBackgroundWrapper {
                        NonFungibleCard(
                            nonFungible = resource,
                            showChevron = false,
                            onClick = { onNonFungibleClick(resource) }
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    }
                }
            }

            SnackbarUiMessageHandler(
                message = state.uiMessage,
                onMessageShown = onMessageShown
            )
        }
    }
}
