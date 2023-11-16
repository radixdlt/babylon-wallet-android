@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.status.assets.fungible

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.resources.isXrd
import com.babylon.wallet.android.presentation.account.composable.AssetMetadataRow
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.Behaviour
import com.babylon.wallet.android.presentation.ui.composables.assets.Tag
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun FungibleAssetDialog(
    viewModel: FungibleAssetDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    FungibleAssetDialogContent(
        state = state,
        onMessageShown = viewModel::onMessageShown,
        onDismiss = onDismiss
    )
}

@Composable
private fun FungibleAssetDialogContent(
    modifier: Modifier = Modifier,
    state: FungibleAssetDialogViewModel.State,
    onMessageShown: () -> Unit,
    onDismiss: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier.fillMaxHeight(fraction = 0.9f),
        title = state.resource?.name.orEmpty(),
        onDismissRequest = onDismiss
    ) {
        Box {
            Column(
                modifier = Modifier
                    .background(RadixTheme.colors.defaultBackground)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingSemiLarge
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.resource != null) {
                    Thumbnail.Fungible(
                        modifier = Modifier.size(104.dp),
                        token = state.resource
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
                if (state.accountAddress != null) {
                    TokenBalance(
                        modifier = Modifier
                            .fillMaxWidth(fraction = if (state.resource == null) 0.5f else 1f)
                            .radixPlaceholder(visible = state.resource == null),
                        fungibleResource = state.resource
                    )
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                if (!state.resource?.description.isNullOrBlank()) {
                    Text(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                        text = state.resource?.description.orEmpty(),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray1
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
                AddressRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    address = state.resourceAddress
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                AssetMetadataRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    key = stringResource(id = R.string.assetDetails_currentSupply)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = RadixTheme.dimensions.paddingDefault)
                            .widthIn(min = RadixTheme.dimensions.paddingXXXLarge * 2)
                            .radixPlaceholder(visible = state.resource?.currentSupply == null),
                        text = when {
                            state.resource?.currentSupply != null -> when (state.resource.currentSupply) {
                                BigDecimal.ZERO -> stringResource(id = R.string.assetDetails_supplyUnkown)
                                else -> state.resource.currentSupply.displayableQuantity()
                            }

                            else -> ""
                        },
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.End
                    )
                }

                Column(modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall)) {
                    if (state.resource?.behaviours == null || state.resource.behaviours.isNotEmpty()) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = RadixTheme.dimensions.paddingDefault,
                                    bottom = RadixTheme.dimensions.paddingSmall
                                ),
                            text = stringResource(id = R.string.assetDetails_behavior),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2
                        )
                    }

                    if (state.resource?.behaviours == null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(RadixTheme.dimensions.paddingLarge)
                                .radixPlaceholder(visible = true)
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(RadixTheme.dimensions.paddingLarge)
                                .radixPlaceholder(visible = true)
                        )
                    } else {
                        state.resource.behaviours.forEach { behaviour ->
                            Behaviour(
                                icon = behaviour.icon(),
                                name = behaviour.name(state.resource.isXrd)
                            )
                        }
                    }
                }

                if (!state.resource?.tags.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                        text = stringResource(id = R.string.assetDetails_tags),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray2
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                        content = {
                            state.resource?.tags?.forEach { tag ->
                                Tag(
                                    modifier = Modifier
                                        .padding(RadixTheme.dimensions.paddingXSmall)
                                        .border(
                                            width = 1.dp,
                                            color = RadixTheme.colors.gray4,
                                            shape = RadixTheme.shapes.roundedTag
                                        )
                                        .padding(RadixTheme.dimensions.paddingSmall),
                                    tag = tag
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }

            SnackbarUiMessageHandler(
                message = state.uiMessage,
                onMessageShown = onMessageShown
            )
        }
    }
}

@Preview
@Composable
fun FungibleTokenBottomSheetDetailsPreview() {
    RadixWalletTheme {
        val resource = remember {
            SampleDataProvider().sampleFungibleResources().first()
        }
        FungibleAssetDialogContent(
            modifier = Modifier.background(RadixTheme.colors.defaultBackground),
            state = FungibleAssetDialogViewModel.State(
                resourceAddress = resource.resourceAddress,
                accountAddress = null,
                resource = resource
            ),
            onMessageShown = {},
            onDismiss = {}
        )
    }
}
