package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import com.babylon.wallet.android.presentation.transfer.assets.ResourcesTabs
import java.math.BigDecimal

fun LazyListScope.assetsView(
    assets: Assets?,
    selectedTab: ResourceTab,
    onTabSelected: (ResourceTab) -> Unit,
    nonFungiblesCollapseState: SnapshotStateList<Boolean>,
    stakeUnitCollapsedState: MutableState<Boolean>,
    action: AssetsViewAction
) {
    item {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ResourcesTabs(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }

    if (assets == null) {
        loadingAssets(selectedTab = selectedTab)
    } else {
        when (selectedTab) {
            ResourceTab.Tokens -> tokensTab(
                assets = assets,
                action = action
            )

            ResourceTab.Nfts -> nftsTab(
                assets = assets,
                collapsedState = nonFungiblesCollapseState,
                action = action
            )

            ResourceTab.PoolUnits -> poolUnitsTab(
                assets = assets,
                stakeUnitCollapsedState = stakeUnitCollapsedState,
                action = action
            )
        }
    }
}

private fun LazyListScope.loadingAssets(
    selectedTab: ResourceTab,
) {
    when (selectedTab) {
        ResourceTab.Tokens -> {}
        ResourceTab.Nfts -> {}
        ResourceTab.PoolUnits -> {}
    }
}

sealed interface AssetsViewAction {

    data class Click(
        val onFungibleClick: (Resource.FungibleResource) -> Unit,
        val onNonFungibleItemClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
        val onLSUClick: (LiquidStakeUnit, ValidatorDetail) -> Unit
    ): AssetsViewAction

    data class Selection(
        val selectedResources: List<String>,
        val onResourceCheckChanged: (String, Boolean) -> Unit
    ): AssetsViewAction {

        fun isSelected(resourceAddress: String) = selectedResources.contains(resourceAddress)

    }
}

@Preview
@Composable
fun AssetsViewWithLoadingAssets() {
    var tabs by remember { mutableStateOf(ResourceTab.Tokens) }
    RadixWalletTheme {
        LazyColumn {
            assetsView(
                assets = null,
                selectedTab = tabs,
                onTabSelected = {},
                nonFungiblesCollapseState = SnapshotStateList(),
                stakeUnitCollapsedState = mutableStateOf(true),
                action = AssetsViewAction.Click(
                    onFungibleClick = {},
                    onNonFungibleItemClick = { _,_ ->},
                    onLSUClick = { _,_ ->},
                )
            )
        }
    }
}

@Preview
@Composable
fun AssetsViewWithEmptyAssets() {
    var tabs by remember { mutableStateOf(ResourceTab.Tokens) }
    RadixWalletTheme {
        LazyColumn {
            assetsView(
                assets = Assets(),
                selectedTab = tabs,
                onTabSelected = {},
                nonFungiblesCollapseState = SnapshotStateList(),
                stakeUnitCollapsedState = mutableStateOf(true),
                action = AssetsViewAction.Click(
                    onFungibleClick = {},
                    onNonFungibleItemClick = { _,_ ->},
                    onLSUClick = { _,_ ->},
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

    val collapsedState = remember(assets.nonFungibles) {
        assets.nonFungibles.map { true }.toMutableStateList()
    }

    val stakeUnitCollapsedState = remember(assets) { mutableStateOf(true) }

    RadixWalletTheme {
        LazyColumn(modifier = Modifier.background(RadixTheme.colors.gray5)) {
            assetsView(
                assets = assets,
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                },
                nonFungiblesCollapseState = collapsedState,
                stakeUnitCollapsedState = stakeUnitCollapsedState,
                action = AssetsViewAction.Click(
                    onFungibleClick = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onLSUClick = { _, _ -> }
                )
            )
        }
    }
}
