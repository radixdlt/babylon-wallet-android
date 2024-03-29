package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTabs
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.profile.data.model.apppreferences.Radix
import java.math.BigDecimal

@Suppress("LongParameterList", "MagicNumber")
fun LazyListScope.assetsView(
    assetsViewData: AssetsViewData?,
    isLoadingBalance: Boolean,
    state: AssetsViewState,
    action: AssetsViewAction
) {
    item {
        AssetsTabs(
            selectedTab = state.selectedTab,
            onTabSelected = action.onTabClick
        )
    }

    if (assetsViewData == null) {
        loadingAssets()
    } else {
        when (state.selectedTab) {
            AssetsTab.Tokens -> tokensTab(
                assetsViewData = assetsViewData,
                isLoadingBalance = isLoadingBalance,
                action = action
            )

            AssetsTab.Nfts -> nftsTab(
                assetsViewData = assetsViewData,
                state = state,
                action = action
            )

            AssetsTab.Staking -> stakingTab(
                assetsViewData = assetsViewData,
                isLoadingBalance = isLoadingBalance,
                state = state,
                action = action
            )

            AssetsTab.PoolUnits -> poolUnitsTab(
                assetsViewData = assetsViewData,
                isLoadingBalance = isLoadingBalance,
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

data class AssetsViewState(
    val selectedTab: AssetsTab,
    val collapsedCollections: Map<String, Boolean>
) {
    fun isCollapsed(collectionId: String) = collapsedCollections.getOrDefault(collectionId, true)
    fun onCollectionToggle(collectionId: String): AssetsViewState {
        val isCollapsed = isCollapsed(collectionId)
        val collapsedCollections = collapsedCollections.toMutableMap().apply {
            this[collectionId] = !isCollapsed
        }

        return copy(collapsedCollections = collapsedCollections)
    }
    companion object {
        fun init(selectedTab: AssetsTab = AssetsTab.Tokens): AssetsViewState {
            return AssetsViewState(
                selectedTab = selectedTab,
                collapsedCollections = emptyMap()
            )
        }
    }
}

sealed interface AssetsViewAction {

    val onTabClick: (AssetsTab) -> Unit
    val onCollectionClick: (String) -> Unit
    val onNextNFtsPageRequest: (Resource.NonFungibleResource) -> Unit
    val onStakesRequest: () -> Unit

    data class Click(
        val onFungibleClick: (Resource.FungibleResource) -> Unit,
        val onNonFungibleItemClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
        val onLSUClick: (LiquidStakeUnit) -> Unit,
        val onPoolUnitClick: (PoolUnit) -> Unit,
        val onClaimClick: (List<StakeClaim>) -> Unit,
        override val onTabClick: (AssetsTab) -> Unit,
        override val onCollectionClick: (String) -> Unit,
        override val onNextNFtsPageRequest: (Resource.NonFungibleResource) -> Unit,
        override val onStakesRequest: () -> Unit,
    ) : AssetsViewAction

    data class Selection(
        val selectedResources: List<String>,
        val onFungibleCheckChanged: (Resource.FungibleResource, Boolean) -> Unit,
        val onNFTCheckChanged: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit,
        override val onTabClick: (AssetsTab) -> Unit,
        override val onCollectionClick: (String) -> Unit,
        override val onNextNFtsPageRequest: (Resource.NonFungibleResource) -> Unit,
        override val onStakesRequest: () -> Unit,
    ) : AssetsViewAction {

        fun isSelected(resourceAddress: String) = selectedResources.contains(resourceAddress)
    }
}

@Preview(showBackground = true)
@Composable
fun AssetsViewWithLoadingAssets() {
    RadixWalletTheme {
        LazyColumn {
            assetsView(
                assetsViewData = null,
                isLoadingBalance = false,
                state = AssetsViewState.init(),
                action = AssetsViewAction.Click(
                    onFungibleClick = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onLSUClick = {},
                    onPoolUnitClick = {},
                    onNextNFtsPageRequest = {},
                    onClaimClick = {},
                    onStakesRequest = {},
                    onCollectionClick = {},
                    onTabClick = {}
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AssetsViewWithEmptyAssets() {
    RadixWalletTheme {
        LazyColumn {
            assetsView(
                assetsViewData = null,
                isLoadingBalance = false,
                state = AssetsViewState.init(),
                action = AssetsViewAction.Click(
                    onFungibleClick = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onLSUClick = {},
                    onPoolUnitClick = {},
                    onNextNFtsPageRequest = {},
                    onClaimClick = {},
                    onStakesRequest = {},
                    onCollectionClick = {},
                    onTabClick = {}
                )
            )
        }
    }
}

@Preview
@Composable
fun AssetsViewWithAssets() {
    val assets by remember {
        mutableStateOf(
            Assets(
                tokens = listOf(
                    Token(
                        Resource.FungibleResource(
                            resourceAddress = XrdResource.address(Radix.Gateway.default.network.networkId().value),
                            ownedAmount = BigDecimal(1000),
                            metadata = listOf(
                                Metadata.Primitive(
                                    key = ExplicitMetadataKey.SYMBOL.key,
                                    value = XrdResource.SYMBOL,
                                    valueType = MetadataType.String
                                )
                            )
                        )
                    )
                ) + SampleDataProvider().sampleFungibleResources().map { Token(it) },
                nonFungibles = listOf(
                    Resource.NonFungibleResource(
                        resourceAddress = SampleDataProvider().randomAddress(),
                        amount = 10,
                        items = emptyList(),
                        metadata = listOf(
                            Metadata.Primitive(ExplicitMetadataKey.NAME.key, "abc", MetadataType.String)
                        )
                    ),
                    SampleDataProvider().nonFungibleResource("cde"),
                    with(SampleDataProvider().randomAddress()) {
                        Resource.NonFungibleResource(
                            resourceAddress = this,
                            amount = 1,
                            items = listOf(
                                Resource.NonFungibleResource.Item(
                                    collectionAddress = this,
                                    localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
                                    metadata = listOf(
                                        Metadata.Primitive(
                                            key = ExplicitMetadataKey.NAME.key,
                                            value = "Some NFT",
                                            valueType = MetadataType.String
                                        )
                                    )
                                )
                            ),
                            metadata = listOf(
                                Metadata.Primitive(ExplicitMetadataKey.NAME.key, "abc", MetadataType.String)
                            )
                        )
                    },
                ).map { NonFungibleCollection(it) },
                poolUnits = listOf(
                    PoolUnit(
                        stake = Resource.FungibleResource(
                            resourceAddress = "resource_abcd",
                            ownedAmount = BigDecimal(2.5),
                            divisibility = 18,
                            currentSupply = BigDecimal(1_000_000),
                            metadata = listOf(
                                Metadata.Primitive(
                                    key = ExplicitMetadataKey.NAME.key,
                                    value = "Custom Pool",
                                    valueType = MetadataType.String
                                ),
                                Metadata.Primitive(
                                    key = ExplicitMetadataKey.SYMBOL.key,
                                    value = "CPL",
                                    valueType = MetadataType.String
                                )
                            )
                        ),
                        pool = Pool(
                            address = "pool_abc",
                            metadata = listOf(
                                Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "My Pool", valueType = MetadataType.String),
                                Metadata.Primitive(key = ExplicitMetadataKey.ICON_URL.key, value = "XXX", valueType = MetadataType.Url),
                                Metadata.Primitive(
                                    key = ExplicitMetadataKey.POOL_UNIT.key,
                                    value = "resource_tdx_19jd32jd3928jd3892jd329",
                                    valueType = MetadataType.Address
                                )
                            ),
                            resources = listOf(
                                Resource.FungibleResource(
                                    resourceAddress = XrdResource.address(Radix.Gateway.default.network.networkId().value),
                                    ownedAmount = BigDecimal(1000),
                                    metadata = listOf(
                                        Metadata.Primitive(
                                            key = ExplicitMetadataKey.SYMBOL.key,
                                            value = XrdResource.SYMBOL,
                                            valueType = MetadataType.String
                                        )
                                    )
                                ),
                                Resource.FungibleResource(
                                    resourceAddress = "resource_abcdef",
                                    ownedAmount = BigDecimal(100),
                                    metadata = listOf(
                                        Metadata.Primitive(
                                            key = ExplicitMetadataKey.SYMBOL.key,
                                            value = "CTM",
                                            valueType = MetadataType.String
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                liquidStakeUnits = listOf(
                    LiquidStakeUnit(
                        fungibleResource = Resource.FungibleResource(
                            resourceAddress = "resource_dfgh",
                            ownedAmount = BigDecimal(100),
                            metadata = listOf(
                                Metadata.Primitive(
                                    key = ExplicitMetadataKey.NAME.key,
                                    value = "Liquid Stake Unit",
                                    valueType = MetadataType.String
                                )
                            )
                        ),
                        validator = Validator.sampleMainnet()
                    ),
                    LiquidStakeUnit(
                        fungibleResource = Resource.FungibleResource(
                            resourceAddress = "resource_dfg",
                            ownedAmount = BigDecimal(21),
                            metadata = listOf(
                                Metadata.Primitive(
                                    key = ExplicitMetadataKey.NAME.key,
                                    value = "Liquid Stake Unit",
                                    valueType = MetadataType.String
                                )
                            )
                        ),
                        validator = Validator.sampleMainnet()
                    )
                ),
                stakeClaims = listOf(
                    StakeClaim(
                        nonFungibleResource = Resource.NonFungibleResource(
                            resourceAddress = "resource_stake_claim-abc",
                            amount = 2,
                            items = listOf()
                        ),
                        validator = Validator.sampleMainnet()
                    )
                )
            )
        )
    }
    var state by remember(assets) {
        mutableStateOf(AssetsViewState.init())
    }

    RadixWalletTheme {
        LazyColumn(modifier = Modifier.background(RadixTheme.colors.gray5)) {
            assetsView(
                assetsViewData = null,
                isLoadingBalance = false,
                state = state,
                action = AssetsViewAction.Click(
                    onFungibleClick = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onLSUClick = {},
                    onPoolUnitClick = {},
                    onNextNFtsPageRequest = {},
                    onClaimClick = {},
                    onStakesRequest = {},
                    onTabClick = {
                        state = state.copy(selectedTab = it)
                    },
                    onCollectionClick = {
                        state = state.onCollectionToggle(it)
                    }
                )
            )
        }
    }
}
