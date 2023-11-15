@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.status.assets.fungible

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.resources.isXrd
import com.babylon.wallet.android.presentation.ui.composables.assets.Behaviour
import com.babylon.wallet.android.presentation.ui.composables.assets.Tag
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder

private const val ROUTE = "fungible_asset_dialog"
private const val ARG_RESOURCE_ADDRESS = "resource_address"
private const val ARG_ACCOUNT_ADDRESS = "account_address"

fun NavController.fungibleAssetDialog(resourceAddress: String, accountAddress: String? = null) {
    val accountAddressParam = if (accountAddress != null) "&$ARG_ACCOUNT_ADDRESS=$accountAddress" else ""
    navigate(route = "$ROUTE?$ARG_RESOURCE_ADDRESS=$resourceAddress$accountAddressParam")
}

fun NavGraphBuilder.fungibleAssetDialog(
    onDismiss: () -> Unit
) {
    dialog(
        route = "$ROUTE?$ARG_RESOURCE_ADDRESS={$ARG_RESOURCE_ADDRESS}&$ARG_ACCOUNT_ADDRESS={$ARG_ACCOUNT_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_RESOURCE_ADDRESS) {
                type = NavType.StringType
            },
            navArgument(ARG_ACCOUNT_ADDRESS) {
                type = NavType.StringType
                nullable = true
            }
        )
    ) {
        FungibleAssetDialog(onDismiss = onDismiss)
    }
}

internal class FungibleAssetDialogArgs(
    val resourceAddress: String,
    val accountAddress: String?
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        resourceAddress = requireNotNull(savedStateHandle[ARG_RESOURCE_ADDRESS]),
        accountAddress = savedStateHandle[ARG_ACCOUNT_ADDRESS]
    )
}

@Composable
private fun FungibleAssetDialog(
    viewModel: FungibleAssetDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    FungibleAssetDialogContent(
        state = state,
        onDismiss = onDismiss
    )
}

@Composable
private fun FungibleAssetDialogContent(
    modifier: Modifier = Modifier,
    state: FungibleAssetDialogViewModel.State,
    onDismiss: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = Modifier
            .fillMaxHeight(fraction = 0.9f),
        title = state.resource?.name.orEmpty(),
        onDismissRequest = onDismiss
    ) {
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
            Thumbnail.Fungible(
                modifier = Modifier.size(104.dp),
                token = state.resource
            )
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

            if (!state.resource?.description.isNullOrBlank()) {
                Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = state.resource?.description.orEmpty(),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            AddressRow(
                modifier = Modifier.fillMaxWidth(),
                address = state.resourceAddress
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.assetDetails_currentSupply),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2
                )
                Text(
                    modifier = Modifier
                        .padding(start = RadixTheme.dimensions.paddingDefault)
                        .widthIn(min = RadixTheme.dimensions.paddingXXXLarge * 2)
                        .radixPlaceholder(visible = state.resource?.currentSupply == null),
                    text = state.resource?.currentSupplyToDisplay.orEmpty(),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            Column {
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

                if (state.resource?.behaviours == null) {
                    Box(
                        modifier
                            .fillMaxWidth()
                            .height(RadixTheme.dimensions.paddingLarge)
                            .radixPlaceholder(visible = true)
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                    Box(
                        modifier
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
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.assetDetails_tags),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
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
            onDismiss = {}
        )
    }
}
