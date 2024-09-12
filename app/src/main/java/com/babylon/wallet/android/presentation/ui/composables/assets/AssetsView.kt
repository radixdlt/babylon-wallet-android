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
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTabs
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

fun LazyListScope.assetsView(
    assetsViewData: AssetsViewData?,
    isLoadingBalance: Boolean,
    state: AssetsViewState,
    action: AssetsViewAction,
    onInfoClick: (GlossaryItem) -> Unit
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
                action = action,
                onInfoClick = onInfoClick
            )

            AssetsTab.Nfts -> nftsTab(
                assetsViewData = assetsViewData,
                state = state,
                action = action,
                onInfoClick = onInfoClick
            )

            AssetsTab.Staking -> stakingTab(
                assetsViewData = assetsViewData,
                isLoadingBalance = isLoadingBalance,
                state = state,
                action = action,
                onInfoClick = onInfoClick
            )

            AssetsTab.PoolUnits -> poolUnitsTab(
                assetsViewData = assetsViewData,
                isLoadingBalance = isLoadingBalance,
                action = action,
                onInfoClick = onInfoClick
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
    val collapsedCollections: Map<String, Boolean>,
    val fetchingNFTsPerCollection: Set<ResourceAddress>,
) {
    fun isCollapsed(collectionId: String) = collapsedCollections.getOrDefault(collectionId, true)

    fun onCollectionToggle(collectionId: String): AssetsViewState {
        val isCollapsed = isCollapsed(collectionId)
        val collapsedCollections = collapsedCollections.toMutableMap().apply {
            this[collectionId] = !isCollapsed
        }

        return copy(collapsedCollections = collapsedCollections)
    }

    fun nextPagePending(collectionId: ResourceAddress): AssetsViewState = copy(
        fetchingNFTsPerCollection = fetchingNFTsPerCollection.toMutableSet().apply { add(collectionId) }
    )

    fun nextPageReceived(collectionId: ResourceAddress): AssetsViewState = copy(
        fetchingNFTsPerCollection = fetchingNFTsPerCollection.toMutableSet().apply { remove(collectionId) }
    )

    companion object {
        fun init(selectedTab: AssetsTab = AssetsTab.Tokens): AssetsViewState {
            return AssetsViewState(
                selectedTab = selectedTab,
                collapsedCollections = emptyMap(),
                fetchingNFTsPerCollection = emptySet()
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
        val selectedResources: List<ResourceAddress>,
        val selectedNFTs: List<NonFungibleGlobalId>,
        val onFungibleCheckChanged: (Resource.FungibleResource, Boolean) -> Unit,
        val onNFTCheckChanged: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit,
        override val onTabClick: (AssetsTab) -> Unit,
        override val onCollectionClick: (String) -> Unit,
        override val onNextNFtsPageRequest: (Resource.NonFungibleResource) -> Unit,
        override val onStakesRequest: () -> Unit,
    ) : AssetsViewAction {

        fun isSelected(resourceAddress: ResourceAddress) = selectedResources.contains(resourceAddress)

        fun isSelected(globalId: NonFungibleGlobalId) = selectedNFTs.contains(globalId)
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
                ),
                onInfoClick = {}
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
                ),
                onInfoClick = {}
            )
        }
    }
}

@UsesSampleValues
@Preview
@Composable
fun AssetsViewWithAssets() {
    val assets by remember {
        mutableStateOf(
            Assets(
                tokens = listOf(
                    Token(Resource.FungibleResource.sampleMainnet())
                ),
                nonFungibles = listOf(
                    Resource.NonFungibleResource.sampleMainnet.random(),
                    Resource.NonFungibleResource.sampleMainnet.random(),
                    Resource.NonFungibleResource.sampleMainnet.random(),
                ).map { NonFungibleCollection(it) },
                poolUnits = listOf(
                    PoolUnit.sampleMainnet()
                ),
                liquidStakeUnits = listOf(
                    LiquidStakeUnit.sampleMainnet(),
                    LiquidStakeUnit.sampleMainnet.other()
                ),
                stakeClaims = listOf(
                    StakeClaim.sampleMainnet()
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
                ),
                onInfoClick = {}
            )
        }
    }
}
