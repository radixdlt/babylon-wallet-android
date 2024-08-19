package com.babylon.wallet.android.presentation.settings.preferences.assetshiding

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.radixdlt.sargon.AssetAddress
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet

@Composable
fun HiddenAssetsScreen(
    viewModel: HiddenAssetsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HiddenAssetsContent(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onUnhideClick = viewModel::onUnhideClick,
        unhide = viewModel::unhide,
        cancelUnhide = viewModel::cancelUnhide
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                HiddenAssetsViewModel.Event.Close -> onBackClick()
            }
        }
    }
}

@Composable
private fun HiddenAssetsContent(
    state: HiddenAssetsViewModel.State,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onUnhideClick: (AssetAddress) -> Unit,
    unhide: (AssetAddress) -> Unit,
    cancelUnhide: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.appSettings_assetHiding_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )

                HorizontalDivider(color = RadixTheme.colors.gray5)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(RadixTheme.colors.gray5),
            contentPadding = padding.plus(PaddingValues(RadixTheme.dimensions.paddingDefault))
        ) {
            item {
                Text(
                    text = stringResource(R.string.appSettings_assetHiding_text),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2
                )
            }

            if (!state.isLoading) {
                item {
                    TitleLayout(text = stringResource(id = R.string.appSettings_assetHiding_tokens))
                }

                assetItems(
                    items = state.tokens,
                    onUnhideClick = onUnhideClick
                )

                item {
                    TitleLayout(text = stringResource(id = R.string.appSettings_assetHiding_nfts))
                }

                assetItems(
                    items = state.nonFungibles,
                    onUnhideClick = onUnhideClick
                )

                item {
                    TitleLayout(text = stringResource(id = R.string.appSettings_assetHiding_poolUnits))
                }

                assetItems(
                    items = state.poolUnits,
                    onUnhideClick = onUnhideClick
                )
            }
        }

        if (state.unhideAsset != null) {
            BasicPromptAlertDialog(
                finish = {
                    if (it) {
                        unhide(state.unhideAsset)
                    } else {
                        cancelUnhide()
                    }
                },
                message = {
                    Text(
                        text = stringResource(id = R.string.appSettings_assetHiding_unhideConfirmationTitle),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1
                    )
                },
                confirmText = stringResource(id = R.string.common_confirm)
            )
        }
    }
}

private fun LazyListScope.assetItems(
    items: List<HiddenAssetsViewModel.State.Asset>,
    onUnhideClick: (AssetAddress) -> Unit
) {
    if (items.isEmpty()) {
        item {
            NoAssets()
        }
    } else {
        itemsIndexed(items = items) { index, item ->
            AssetLayout(
                asset = item,
                onUnhideClick = onUnhideClick
            )

            if (index < items.size) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
        }
    }
}

@Composable
private fun TitleLayout(text: String) {
    Text(
        modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingXLarge),
        text = text,
        style = RadixTheme.typography.secondaryHeader,
        color = RadixTheme.colors.gray2
    )
}

@Composable
private fun AssetLayout(
    asset: HiddenAssetsViewModel.State.Asset,
    onUnhideClick: (AssetAddress) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultCardShadow()
            .background(
                color = RadixTheme.colors.defaultBackground,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingSemiLarge
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumbnail(
            modifier = Modifier.size(44.dp),
            asset = asset
        )

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = asset.name ?: stringResource(id = R.string.dash),
                color = RadixTheme.colors.gray1,
                style = RadixTheme.typography.body1HighImportance
            )

            asset.description?.let {
                Text(
                    text = it,
                    color = RadixTheme.colors.gray2,
                    style = RadixTheme.typography.body2Regular
                )
            }
        }

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

        RadixSecondaryButton(
            text = stringResource(id = R.string.appSettings_assetHiding_unhideButton),
            onClick = { onUnhideClick(asset.address) }
        )
    }
}

@Composable
private fun Thumbnail(
    modifier: Modifier,
    asset: HiddenAssetsViewModel.State.Asset
) {
    when (asset.address) {
        is AssetAddress.Fungible -> Thumbnail.Fungible(
            modifier = modifier,
            isXrd = false,
            icon = asset.icon,
            name = asset.name.orEmpty()
        )
        is AssetAddress.NonFungible -> Thumbnail.NFT(
            modifier = modifier,
            image = asset.icon,
            localId = asset.description
        )
        is AssetAddress.PoolUnit -> Thumbnail.PoolUnit(
            modifier = modifier,
            iconUrl = asset.icon,
            name = asset.name.orEmpty()
        )
    }
}

@Composable
private fun NoAssets() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(83.dp)
            .background(
                color = RadixTheme.colors.gray4,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(RadixTheme.dimensions.paddingDefault),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.appSettings_assetHiding_none),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray2
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun HiddenAssetsContentPreview(
    @PreviewParameter(HiddenAssetsPreviewProvider::class) state: HiddenAssetsViewModel.State
) {
    RadixWalletTheme {
        HiddenAssetsContent(
            state = state,
            onBackClick = {},
            onUnhideClick = {},
            unhide = {},
            cancelUnhide = {}
        )
    }
}

@UsesSampleValues
class HiddenAssetsPreviewProvider : PreviewParameterProvider<HiddenAssetsViewModel.State> {

    override val values: Sequence<HiddenAssetsViewModel.State>
        get() = sequenceOf(
            HiddenAssetsViewModel.State(
                tokens = listOf(
                    HiddenAssetsViewModel.State.Asset(
                        address = AssetAddress.Fungible(ResourceAddress.sampleMainnet.random()),
                        icon = null,
                        name = "BTC",
                        description = null
                    ),
                    HiddenAssetsViewModel.State.Asset(
                        address = AssetAddress.Fungible(ResourceAddress.sampleMainnet.random()),
                        icon = null,
                        name = "rUSD",
                        description = null
                    ),
                    HiddenAssetsViewModel.State.Asset(
                        address = AssetAddress.Fungible(ResourceAddress.sampleMainnet.random()),
                        icon = null,
                        name = "1 Willshire Boulevard",
                        description = null
                    ),
                    HiddenAssetsViewModel.State.Asset(
                        address = AssetAddress.Fungible(ResourceAddress.sampleMainnet.random()),
                        icon = null,
                        name = "TOPSHOT",
                        description = null
                    )
                ),
                nonFungibles = listOf(
                    HiddenAssetsViewModel.State.Asset(
                        address = AssetAddress.NonFungible(NonFungibleGlobalId.sample()),
                        icon = Uri.EMPTY,
                        name = "Devin Booker - Dunk",
                        description = "dbooker_dunk_39"
                    )
                ),
                poolUnits = emptyList()
            ),
            HiddenAssetsViewModel.State(
                tokens = listOf(
                    HiddenAssetsViewModel.State.Asset(
                        address = AssetAddress.Fungible(ResourceAddress.sampleMainnet.random()),
                        icon = null,
                        name = "BTC",
                        description = null
                    )
                ),
                nonFungibles = emptyList(),
                poolUnits = emptyList(),
                unhideAsset = AssetAddress.Fungible(ResourceAddress.sampleMainnet.random())
            ),
            HiddenAssetsViewModel.State(
                tokens = emptyList(),
                nonFungibles = emptyList(),
                poolUnits = listOf(
                    HiddenAssetsViewModel.State.Asset(
                        address = AssetAddress.PoolUnit(PoolAddress.sampleMainnet.random()),
                        icon = null,
                        name = "Pool Unit",
                        description = null,
                    )
                )
            ),
            HiddenAssetsViewModel.State(
                isLoading = true
            )
        )
}
