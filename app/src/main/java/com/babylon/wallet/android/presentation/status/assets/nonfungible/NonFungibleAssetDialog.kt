@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.babylon.wallet.android.presentation.status.assets.nonfungible

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.AssetMetadataRow
import com.babylon.wallet.android.presentation.account.composable.View
import com.babylon.wallet.android.presentation.status.assets.nonfungible.NonFungibleAssetDialogViewModel.State.ClaimState
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail.NFTAspectRatio
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail.NFTCornerRadius
import com.babylon.wallet.android.presentation.ui.composables.assets.Behaviour
import com.babylon.wallet.android.presentation.ui.composables.assets.Tag
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import rdx.works.core.displayableQuantity

@Composable
fun NonFungibleAssetDialog(
    viewModel: NonFungibleAssetDialogViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    NonFungibleAssetDialogContent(
        state = state,
        onMessageShown = viewModel::onMessageShown,
        onClaimClick = viewModel::onClaimClick,
        onDismiss = onDismiss
    )
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun NonFungibleAssetDialogContent(
    modifier: Modifier = Modifier,
    state: NonFungibleAssetDialogViewModel.State,
    onMessageShown: () -> Unit,
    onDismiss: () -> Unit,
    onClaimClick: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier,
        onDismiss = onDismiss,
        title = if (state.item != null && state.localId != null) {
            state.item.name
        } else if (state.localId == null && state.resource != null) {
            state.resource.name
        } else {
            ""
        }
    ) {
        Box(modifier = Modifier.fillMaxHeight(fraction = 0.9f)) {
            Column(
                modifier = Modifier
                    .background(RadixTheme.colors.defaultBackground)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.localId != null) {
                    if (state.item?.imageUrl != null) {
                        Thumbnail.NFT(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                            nft = state.item,
                            cropped = false
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    } else if (state.item == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(NFTAspectRatio)
                                .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                                .radixPlaceholder(
                                    visible = true,
                                    shape = RoundedCornerShape(NFTCornerRadius)
                                )
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    }

                    if (!state.item?.description.isNullOrBlank()) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                                .fillMaxWidth(),
                            text = state.item?.description.orEmpty(),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.gray1
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                            color = RadixTheme.colors.gray4
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    }

                    if (state.item != null) {
                        AssetMetadataRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                            key = stringResource(id = R.string.assetDetails_NFTDetails_id)
                        ) {
                            ActionableAddressView(
                                modifier = Modifier.padding(start = RadixTheme.dimensions.paddingDefault),
                                address = state.item.globalAddress,
                                textStyle = RadixTheme.typography.body1HighImportance,
                                textColor = RadixTheme.colors.gray1
                            )
                        }
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    }

                    if (state.item?.claimEpoch != null && state.item.claimAmountXrd != null) {
                        val claimState = remember(state.epoch) { state.claimState }

                        RadixPrimaryButton(
                            modifier = Modifier
                                .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                                .fillMaxWidth()
                                .radixPlaceholder(visible = claimState == null),
                            text = state.claimState?.description().orEmpty(),
                            onClick = { onClaimClick() },
                            enabled = claimState is ClaimState.ReadyToClaim
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    }

                    if (!state.item?.nonStandardMetadata.isNullOrEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                            color = RadixTheme.colors.gray4
                        )

                        state.item?.nonStandardMetadata?.forEach { metadata ->
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                            metadata.View(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                            )
                        }

                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    }
                }

                if (state.localId != null) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = RadixTheme.colors.gray4
                    )
                }
                GrayBackgroundWrapper(contentPadding = PaddingValues(bottom = RadixTheme.dimensions.paddingXLarge)) {
                    if (state.resource != null) {
                        Thumbnail.NonFungible(
                            modifier = Modifier
                                .padding(vertical = RadixTheme.dimensions.paddingDefault)
                                .size(104.dp),
                            collection = state.resource,
                            shape = CircleShape
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(vertical = RadixTheme.dimensions.paddingDefault)
                                .size(104.dp)
                                .radixPlaceholder(
                                    visible = true,
                                    shape = CircleShape
                                )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        color = RadixTheme.colors.gray4
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                    if (!state.resource?.description.isNullOrBlank()) {
                        Text(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                            text = state.resource?.description.orEmpty(),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.gray1
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                            color = RadixTheme.colors.gray4
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    }
                    AddressRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                        address = state.resourceAddress
                    )
                    if (!state.resource?.name.isNullOrBlank() && state.localId != null) {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                        AssetMetadataRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                            key = stringResource(id = R.string.assetDetails_name)
                        ) {
                            Text(
                                text = state.resource?.name.orEmpty(),
                                style = RadixTheme.typography.body1HighImportance,
                                color = RadixTheme.colors.gray1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    if (!state.isNewlyCreated) {
                        AssetMetadataRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                            key = stringResource(id = R.string.assetDetails_currentSupply)
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(start = RadixTheme.dimensions.paddingDefault)
                                    .widthIn(min = RadixTheme.dimensions.paddingXXXLarge * 2)
                                    .radixPlaceholder(visible = state.resource?.currentSupply == null),
                                text = when {
                                    state.resource?.currentSupply != null -> when (state.resource.currentSupply) {
                                        0 -> stringResource(id = R.string.assetDetails_supplyUnkown)
                                        else -> state.resource.currentSupply.toString()
                                    }

                                    else -> ""
                                },
                                style = RadixTheme.typography.body1HighImportance,
                                color = RadixTheme.colors.gray1,
                                textAlign = TextAlign.End
                            )
                        }

                        BehavioursSection(state)

                        TagsSection(state)
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

@Composable
private fun BehavioursSection(
    state: NonFungibleAssetDialogViewModel.State
) {
    Column(modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXLarge)) {
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
                    name = behaviour.name()
                )
            }
        }
    }
}

@Composable
private fun TagsSection(state: NonFungibleAssetDialogViewModel.State) {
    if (!state.resource?.tags.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
            text = stringResource(id = R.string.assetDetails_tags),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray2
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
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
    }
}

@Composable
private fun ClaimState.description() = when (this) {
    is ClaimState.ReadyToClaim -> stringResource(id = R.string.assetDetails_staking_readyToClaim, amount.displayableQuantity())
    is ClaimState.Unstaking -> stringResource(
        id = R.string.assetDetails_staking_unstaking,
        amount.displayableQuantity(),
        approximateClaimMinutes
    )
}
