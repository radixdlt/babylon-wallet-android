package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.SymbolMetadataItem
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTabs
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import java.math.BigDecimal

@Suppress("LongParameterList")
fun LazyListScope.assetsView(
    assets: Assets?,
    epoch: Long?,
    selectedTab: ResourceTab,
    onTabSelected: (ResourceTab) -> Unit,
    collapsibleAssetsState: SnapshotStateMap<String, Boolean>,
    action: AssetsViewAction
) {
    item {
        Row {
            Spacer(modifier = Modifier.weight(0.15f))
            AssetsTabs(
                modifier = Modifier.weight(0.7f),
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
            Spacer(modifier = Modifier.weight(0.15f))
        }

    }

    if (assets == null) {
        loadingAssets()
    } else {
        when (selectedTab) {
            ResourceTab.Tokens -> tokensTab(
                assets = assets,
                action = action
            )

            ResourceTab.Nfts -> nftsTab(
                assets = assets,
                collapsibleAssetsState = collapsibleAssetsState,
                action = action
            )

            ResourceTab.PoolUnits -> poolUnitsTab(
                assets = assets,
                epoch = epoch,
                collapsibleAssetsState = collapsibleAssetsState,
                action = action
            )
        }
    }
}

private fun LazyListScope.loadingAssets() {
    item {
        Box(
            modifier = Modifier
                .height(256.dp)
                .fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = RadixTheme.colors.gray1
            )
        }
    }
}

sealed interface AssetsViewAction {

    val onNextNFtsPageRequest: (Resource.NonFungibleResource) -> Unit
    val onStakesRequest: () -> Unit

    data class Click(
        val onFungibleClick: (Resource.FungibleResource) -> Unit,
        val onNonFungibleItemClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
        val onLSUClick: (LiquidStakeUnit) -> Unit,
        val onPoolUnitClick: (PoolUnit) -> Unit,
        override val onNextNFtsPageRequest: (Resource.NonFungibleResource) -> Unit,
        override val onStakesRequest: () -> Unit
    ) : AssetsViewAction

    data class Selection(
        val selectedResources: List<String>,
        val onFungibleCheckChanged: (Resource.FungibleResource, Boolean) -> Unit,
        val onNFTCheckChanged: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit,
        override val onNextNFtsPageRequest: (Resource.NonFungibleResource) -> Unit,
        override val onStakesRequest: () -> Unit
    ) : AssetsViewAction {

        fun isSelected(resourceAddress: String) = selectedResources.contains(resourceAddress)
    }
}

@Composable
fun rememberAssetsViewState(assets: Assets?): SnapshotStateMap<String, Boolean> {
    val collections = remember(assets) {
        assets?.nonFungibles?.map { it.resourceAddress }.orEmpty() + listOf(STAKE_COLLECTION_ID)
    }
    return remember(collections) {
        SnapshotStateMap<String, Boolean>().apply {
            putAll(collections.associateWith { true })
        }
    }
}

@Preview
@Composable
fun AssetsViewWithLoadingAssets() {
    val tabs by remember { mutableStateOf(ResourceTab.Tokens) }
    RadixWalletTheme {
        LazyColumn {
            assetsView(
                assets = null,
                epoch = null,
                selectedTab = tabs,
                onTabSelected = {},
                collapsibleAssetsState = SnapshotStateMap(),
                action = AssetsViewAction.Click(
                    onFungibleClick = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onLSUClick = {},
                    onPoolUnitClick = {},
                    onNextNFtsPageRequest = {},
                    onStakesRequest = {}
                )
            )
        }
    }
}

@Preview
@Composable
fun AssetsViewWithEmptyAssets() {
    val tabs by remember { mutableStateOf(ResourceTab.Tokens) }
    RadixWalletTheme {
        LazyColumn {
            assetsView(
                assets = null,
                epoch = null,
                selectedTab = tabs,
                onTabSelected = {},
                collapsibleAssetsState = SnapshotStateMap(),
                action = AssetsViewAction.Click(
                    onFungibleClick = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onLSUClick = {},
                    onPoolUnitClick = {},
                    onNextNFtsPageRequest = {},
                    onStakesRequest = {}
                )
            )
        }
    }
}

@Preview
@Composable
fun AssetsViewWithAssets() {
    var selectedTab by remember { mutableStateOf(ResourceTab.Nfts) }
    val assets by remember {
        mutableStateOf(
            Assets(
                fungibles = listOf(
                    Resource.FungibleResource(
                        resourceAddress = XrdResource.address(),
                        ownedAmount = BigDecimal(1000),
                        symbolMetadataItem = SymbolMetadataItem("XRD")
                    )
                ) + SampleDataProvider().sampleFungibleResources(),
                nonFungibles = listOf(
                    Resource.NonFungibleResource(
                        resourceAddress = SampleDataProvider().randomAddress(),
                        nameMetadataItem = NameMetadataItem("abc"),
                        amount = 10,
                        items = emptyList()
                    ),
                    SampleDataProvider().nonFungibleResource("cde"),
                    with(SampleDataProvider().randomAddress()) {
                        Resource.NonFungibleResource(
                            resourceAddress = this,
                            nameMetadataItem = NameMetadataItem("abc"),
                            amount = 1,
                            items = listOf(
                                Resource.NonFungibleResource.Item(
                                    collectionAddress = this,
                                    localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
                                    nameMetadataItem = NameMetadataItem("Some NFT")
                                )
                            )
                        )
                    },
                ),
                poolUnits = listOf(
                    PoolUnit(
                        stake = Resource.FungibleResource(
                            resourceAddress = "resource_abcd",
                            ownedAmount = BigDecimal(2.5),
                            nameMetadataItem = NameMetadataItem("Custom Pool"),
                            symbolMetadataItem = SymbolMetadataItem("CPL"),
                            divisibility = 18,
                            currentSupply = BigDecimal(1_000_000)
                        ),
                        pool = Pool(
                            address = "pool_abc",
                            resources = listOf(
                                Resource.FungibleResource(
                                    resourceAddress = XrdResource.address(),
                                    ownedAmount = BigDecimal(1000),
                                    symbolMetadataItem = SymbolMetadataItem("XRD")
                                ),
                                Resource.FungibleResource(
                                    resourceAddress = "resource_abcdef",
                                    ownedAmount = BigDecimal(100),
                                    symbolMetadataItem = SymbolMetadataItem("CTM")
                                )
                            )
                        )
                    )
                ),
                validatorsWithStakes = listOf(
                    ValidatorWithStakes(
                        validatorDetail = ValidatorDetail(
                            address = "validator_abc",
                            name = "Awesome Validator",
                            url = null,
                            description = null,
                            totalXrdStake = BigDecimal(1000)
                        ),
                        liquidStakeUnit = LiquidStakeUnit(
                            Resource.FungibleResource(
                                resourceAddress = "resource_dfgh",
                                ownedAmount = BigDecimal(100),
                                nameMetadataItem = NameMetadataItem("Liquid Stake Unit")
                            )
                        ),
                        stakeClaimNft = StakeClaim(
                            nonFungibleResource = Resource.NonFungibleResource(
                                resourceAddress = "resource_stake_claim-abc",
                                amount = 2,
                                items = listOf()
                            )
                        )
                    ),
                    ValidatorWithStakes(
                        validatorDetail = ValidatorDetail(
                            address = "validator_abcd",
                            name = "Another Validator",
                            url = null,
                            description = null,
                            totalXrdStake = BigDecimal(10000)
                        ),
                        liquidStakeUnit = LiquidStakeUnit(
                            Resource.FungibleResource(
                                resourceAddress = "resource_dfg",
                                ownedAmount = BigDecimal(21),
                                nameMetadataItem = NameMetadataItem("Liquid Stake Unit")
                            )
                        )
                    )
                )
            )
        )
    }

    val collapsibleAssetsState = rememberAssetsViewState(assets = assets)

    RadixWalletTheme {
        LazyColumn(modifier = Modifier.background(RadixTheme.colors.gray5)) {
            assetsView(
                assets = assets,
                epoch = null,
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                },
                collapsibleAssetsState = collapsibleAssetsState,
                action = AssetsViewAction.Click(
                    onFungibleClick = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onLSUClick = {},
                    onPoolUnitClick = {},
                    onNextNFtsPageRequest = {},
                    onStakesRequest = {}
                )
            )
        }
    }
}
