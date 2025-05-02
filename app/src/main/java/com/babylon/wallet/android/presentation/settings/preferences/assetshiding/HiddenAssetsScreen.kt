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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceIdentifier
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet

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
        onMessageShown = viewModel::onMessageShown,
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
    onMessageShown: () -> Unit,
    onUnhideClick: (ResourceIdentifier) -> Unit,
    unhide: (ResourceIdentifier) -> Unit,
    cancelUnhide: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.message,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.hiddenAssets_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding.plus(PaddingValues(RadixTheme.dimensions.paddingDefault))
        ) {
            item {
                Text(
                    text = stringResource(R.string.hiddenAssets_text),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.textSecondary
                )
            }

            if (!state.isLoading) {
                item {
                    TitleLayout(text = stringResource(id = R.string.hiddenAssets_fungibles))
                }

                resourceItems(
                    items = state.tokens,
                    onUnhideClick = onUnhideClick
                )

                item {
                    TitleLayout(text = stringResource(id = R.string.hiddenAssets_nonFungibles))
                }

                resourceItems(
                    items = state.nonFungibles,
                    onUnhideClick = onUnhideClick
                )

                item {
                    TitleLayout(text = stringResource(id = R.string.hiddenAssets_poolUnits))
                }

                resourceItems(
                    items = state.poolUnits,
                    onUnhideClick = onUnhideClick
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = RadixTheme.colors.gray1
                )
            }
        }

        if (state.unhideResource != null) {
            BasicPromptAlertDialog(
                finish = {
                    if (it) {
                        unhide(state.unhideResource)
                    } else {
                        cancelUnhide()
                    }
                },
                message = {
                    Text(
                        text = stringResource(
                            id = when (state.unhideResource) {
                                is ResourceIdentifier.NonFungible -> R.string.hiddenAssets_unhideConfirmation_collection
                                else -> R.string.hiddenAssets_unhideConfirmation_asset
                            }
                        ),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.text
                    )
                },
                confirmText = stringResource(id = R.string.common_confirm)
            )
        }
    }
}

private fun LazyListScope.resourceItems(
    items: List<HiddenAssetsViewModel.State.Resource>,
    onUnhideClick: (ResourceIdentifier) -> Unit
) {
    if (items.isEmpty()) {
        item {
            NoAssets()
        }
    } else {
        itemsIndexed(items = items) { index, item ->
            ResourceLayout(
                resource = item,
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
        color = RadixTheme.colors.textSecondary
    )
}

@Composable
private fun ResourceLayout(
    resource: HiddenAssetsViewModel.State.Resource,
    onUnhideClick: (ResourceIdentifier) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultCardShadow()
            .background(
                color = RadixTheme.colors.cardOnSecondary,
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
            resource = resource
        )

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = resource.name ?: stringResource(id = R.string.dash),
                color = RadixTheme.colors.text,
                style = RadixTheme.typography.body1HighImportance
            )

            ActionableAddressView(
                address = resource.address,
                isVisitableInDashboard = true,
                textStyle = RadixTheme.typography.body2Regular,
                textColor = RadixTheme.colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

        RadixSecondaryButton(
            text = stringResource(id = R.string.hiddenAssets_unhide),
            onClick = { onUnhideClick(resource.identifier) }
        )
    }
}

@Composable
private fun Thumbnail(
    modifier: Modifier,
    resource: HiddenAssetsViewModel.State.Resource
) {
    when (resource.identifier) {
        is ResourceIdentifier.Fungible -> Thumbnail.Fungible(
            modifier = modifier,
            isXrd = false,
            icon = resource.icon,
            name = resource.name.orEmpty()
        )
        is ResourceIdentifier.NonFungible -> Thumbnail.NonFungible(
            modifier = modifier,
            image = resource.icon,
            name = resource.name
        )
        is ResourceIdentifier.PoolUnit -> Thumbnail.PoolUnit(
            modifier = modifier,
            iconUrl = resource.icon,
            name = resource.name.orEmpty()
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
                color = RadixTheme.colors.backgroundTertiary,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(RadixTheme.dimensions.paddingDefault),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.common_none),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.textSecondary
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
            onMessageShown = {},
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
                    HiddenAssetsViewModel.State.Resource(
                        address = Address.Resource(ResourceAddress.sampleMainnet.random()),
                        identifier = ResourceIdentifier.Fungible(ResourceAddress.sampleMainnet.random()),
                        icon = null,
                        name = "BTC"
                    ),
                    HiddenAssetsViewModel.State.Resource(
                        address = Address.Resource(ResourceAddress.sampleMainnet.random()),
                        identifier = ResourceIdentifier.Fungible(ResourceAddress.sampleMainnet.random()),
                        icon = null,
                        name = "rUSD"
                    ),
                    HiddenAssetsViewModel.State.Resource(
                        address = Address.Resource(ResourceAddress.sampleMainnet.random()),
                        identifier = ResourceIdentifier.Fungible(ResourceAddress.sampleMainnet.random()),
                        icon = null,
                        name = "1 Willshire Boulevard"
                    ),
                    HiddenAssetsViewModel.State.Resource(
                        address = Address.Resource(ResourceAddress.sampleMainnet.random()),
                        identifier = ResourceIdentifier.Fungible(ResourceAddress.sampleMainnet.random()),
                        icon = null,
                        name = "TOPSHOT"
                    )
                ),
                nonFungibles = listOf(
                    HiddenAssetsViewModel.State.Resource(
                        address = Address.Resource(ResourceAddress.sampleMainnet.random()),
                        identifier = ResourceIdentifier.NonFungible(ResourceAddress.sampleStokenet()),
                        icon = Uri.EMPTY,
                        name = "Devin Booker - Dunk"
                    )
                ),
                poolUnits = emptyList()
            ),
            HiddenAssetsViewModel.State(
                tokens = listOf(
                    HiddenAssetsViewModel.State.Resource(
                        address = Address.Resource(ResourceAddress.sampleMainnet.random()),
                        identifier = ResourceIdentifier.Fungible(ResourceAddress.sampleMainnet.random()),
                        icon = null,
                        name = "BTC"
                    )
                ),
                nonFungibles = emptyList(),
                poolUnits = emptyList(),
                unhideResource = ResourceIdentifier.Fungible(ResourceAddress.sampleMainnet.random())
            ),
            HiddenAssetsViewModel.State(
                tokens = emptyList(),
                nonFungibles = emptyList(),
                poolUnits = listOf(
                    HiddenAssetsViewModel.State.Resource(
                        address = Address.Pool(PoolAddress.sampleMainnet.random()),
                        identifier = ResourceIdentifier.PoolUnit(PoolAddress.sampleMainnet.random()),
                        icon = null,
                        name = "Pool Unit"
                    )
                )
            ),
            HiddenAssetsViewModel.State(
                isLoading = true
            )
        )
}
