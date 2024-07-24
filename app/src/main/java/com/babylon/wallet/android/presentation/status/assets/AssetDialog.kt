package com.babylon.wallet.android.presentation.status.assets

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.MetadataKeyView
import com.babylon.wallet.android.presentation.account.composable.MetadataView
import com.babylon.wallet.android.presentation.status.assets.fungible.FungibleDialogContent
import com.babylon.wallet.android.presentation.status.assets.lsu.LSUDialogContent
import com.babylon.wallet.android.presentation.status.assets.nonfungible.NonFungibleAssetDialogContent
import com.babylon.wallet.android.presentation.status.assets.pool.PoolUnitDialogContent
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.LinkText
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.assets.Behaviour
import com.babylon.wallet.android.presentation.ui.composables.assets.Tag
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import kotlinx.collections.immutable.ImmutableList
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.AssetBehaviours
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Tag

@Composable
fun AssetDialog(
    modifier: Modifier = Modifier,
    viewModel: AssetDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    BottomSheetDialogWrapper(
        modifier = modifier,
        title = state.asset?.displayTitle(),
        onDismiss = onDismiss
    ) {
        val isLoadingBalance = if (state.isFiatBalancesEnabled) {
            state.isLoadingBalance
        } else {
            false
        }

        Box(modifier = Modifier.fillMaxHeight(fraction = 0.9f)) {
            when (val asset = state.asset) {
                is Token -> FungibleDialogContent(
                    args = state.args as AssetDialogArgs.Fungible,
                    token = asset,
                    tokenPrice = state.assetPrice as? AssetPrice.TokenPrice,
                    isLoadingBalance = isLoadingBalance
                )

                is LiquidStakeUnit -> LSUDialogContent(
                    args = state.args as AssetDialogArgs.Fungible,
                    lsu = asset,
                    price = state.assetPrice as? AssetPrice.LSUPrice,
                    isLoadingBalance = isLoadingBalance
                )

                is PoolUnit -> PoolUnitDialogContent(
                    args = state.args as AssetDialogArgs.Fungible,
                    poolUnit = asset,
                    poolUnitPrice = state.assetPrice as? AssetPrice.PoolUnitPrice,
                    isLoadingBalance = isLoadingBalance
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
                        price = state.assetPrice as? AssetPrice.StakeClaimPrice,
                        isLoadingBalance = isLoadingBalance,
                        onClaimClick = viewModel::onClaimClick,
                    )
                }

                // When asset is not retrieved yet, we show the placeholders for tokens, or NFTs
                null -> when (val args = state.args) {
                    is AssetDialogArgs.Fungible -> FungibleDialogContent(
                        args = state.args as AssetDialogArgs.Fungible,
                        token = null,
                        tokenPrice = null,
                        isLoadingBalance = state.isLoadingBalance
                    )

                    is AssetDialogArgs.NFT -> NonFungibleAssetDialogContent(
                        resourceAddress = state.args.resourceAddress,
                        localId = args.localId,
                        asset = null,
                        price = null,
                        isNewlyCreated = args.isNewlyCreated,
                        isLoadingBalance = false // we do not need to pass value here because it's for NFTs
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
            item.nameTruncated
        } else {
            resource.name
        }
    }
}

@Composable
fun DescriptionSection(
    modifier: Modifier = Modifier,
    description: String?,
    infoUrl: Uri?
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (!description.isNullOrBlank()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall)
                    .padding(bottom = if (infoUrl != null) RadixTheme.dimensions.paddingSemiLarge else 0.dp),
                text = description,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Start
            )
        }

        if (infoUrl != null) {
            Text(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                text = stringResource(id = R.string.assetDetails_moreInfo),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray2
            )

            LinkText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall)
                    .padding(top = RadixTheme.dimensions.paddingXSmall),
                url = infoUrl
            )
        }

        if (!description.isNullOrBlank() || infoUrl != null) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            HorizontalDivider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
    }
}

@Composable
fun NonStandardMetadataSection(
    modifier: Modifier = Modifier,
    resource: Resource
) {
    val metadata = remember(resource.metadata) {
        resource.nonStandardMetadata
    }

    if (metadata.isNotEmpty()) {
        Column(
            modifier = modifier
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            HorizontalDivider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)

            metadata.forEach { metadata ->
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                MetadataView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    metadata = metadata
                )
            }
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
    tags: ImmutableList<Tag>?
) {
    if (!tags.isNullOrEmpty()) {
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            MetadataKeyView(
                modifier = Modifier.fillMaxWidth(),
                key = stringResource(id = R.string.assetDetails_tags),
                isLocked = false
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
        }
    }
}
