package com.babylon.wallet.android.presentation.status.assets

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.AssetBehaviours
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.resources.Tag
import com.babylon.wallet.android.presentation.status.assets.fungible.FungibleDialogContent
import com.babylon.wallet.android.presentation.status.assets.lsu.LSUDialogContent
import com.babylon.wallet.android.presentation.status.assets.nonfungible.NonFungibleAssetDialogContent
import com.babylon.wallet.android.presentation.status.assets.pool.PoolUnitDialogContent
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.assets.Behaviour
import com.babylon.wallet.android.presentation.ui.composables.assets.Tag
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder

@Composable
fun AssetDialog(
    viewModel: AssetDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    BottomSheetDialogWrapper(
        title = state.asset?.displayTitle(),
        onDismiss = onDismiss
    ) {
        Box(modifier = Modifier.fillMaxHeight(fraction = 0.9f)) {
            when (val asset = state.asset) {
                is Token -> FungibleDialogContent(
                    resourceAddress = state.args.resourceAddress,
                    isAmountPresent = state.args.isAmountPresent,
                    isNewlyCreated = state.args.isNewlyCreated,
                    token = asset
                )

                is LiquidStakeUnit -> LSUDialogContent(
                    resourceAddress = state.args.resourceAddress,
                    lsu = asset
                )

                is PoolUnit -> PoolUnitDialogContent(
                    resourceAddress = state.args.resourceAddress,
                    poolUnit = asset
                )
                // Includes NFTs and stake claims
                is Asset.NonFungible -> {
                    val args = state.args as? AssetDialogArgs.NFT
                    NonFungibleAssetDialogContent(
                        resourceAddress = state.args.resourceAddress,
                        localId = args?.localId,
                        asset = asset,
                        isNewlyCreated = state.args.isNewlyCreated,
                        claimState = state.claimState,
                        accountContext = state.accountContext,
                        onClaimClick = viewModel::onClaimClick
                    )
                }

                // When asset is not retrieved yet, we show the placeholders for tokens, or NFTs
                null -> when (val args = state.args) {
                    is AssetDialogArgs.Fungible -> FungibleDialogContent(
                        resourceAddress = args.resourceAddress,
                        isAmountPresent = args.isAmountPresent,
                        isNewlyCreated = args.isNewlyCreated,
                        token = null
                    )

                    is AssetDialogArgs.NFT -> NonFungibleAssetDialogContent(
                        resourceAddress = state.args.resourceAddress,
                        localId = args.localId,
                        asset = null,
                        isNewlyCreated = args.isNewlyCreated
                    )
                }
            }

            SnackbarUiMessageHandler(
                message = state.uiMessage,
                onMessageShown = viewModel::onMessageShown
            )
        }
    }
}


@Composable
fun Asset.displayTitle() = when (this) {
    is Token -> resource.name
    is LiquidStakeUnit -> name
    is PoolUnit -> displayTitle
    is Asset.NonFungible -> {
        val item = resource.items.firstOrNull()
        if (item != null) {
            item.name
        } else {
            resource.name
        }
    }
}

@Composable
fun BehavioursSection(
    modifier: Modifier = Modifier,
    behaviours: AssetBehaviours?,
    isXRD: Boolean = false
) {
    Column(modifier = modifier) {
        if (behaviours == null || behaviours.isNotEmpty()) {
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

        if (behaviours == null) {
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
            behaviours.forEach { behaviour ->
                Behaviour(
                    icon = behaviour.icon(),
                    name = behaviour.name(isXRD)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSection(
    modifier: Modifier = Modifier,
    tags: List<Tag>?
) {
    if (!tags.isNullOrEmpty()) {
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.assetDetails_tags),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                content = {
                    tags.forEach { tag ->
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
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
        }
    }
}