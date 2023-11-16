@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.status.assets.pool

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.AssetMetadataRow
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.Behaviour
import com.babylon.wallet.android.presentation.ui.composables.assets.PoolResourcesValues
import com.babylon.wallet.android.presentation.ui.composables.assets.Tag
import com.babylon.wallet.android.presentation.ui.composables.assets.assetOutlineBorder
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun PoolUnitAssetDialog(
    viewModel: PoolUnitAssetDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    FungibleAssetDialogContent(
        state = state,
        onMessageShown = viewModel::onMessageShown,
        onDismiss = onDismiss
    )
}

@Composable
private fun FungibleAssetDialogContent(
    modifier: Modifier = Modifier,
    state: PoolUnitAssetDialogViewModel.State,
    onMessageShown: () -> Unit,
    onDismiss: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier.fillMaxHeight(fraction = 0.9f),
        title = state.poolUnit?.name.orEmpty(),
        onDismissRequest = onDismiss
    ) {
        Box {
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
                if (state.poolUnit != null) {
                    Thumbnail.PoolUnit(
                        modifier = Modifier.size(104.dp),
                        poolUnit = state.poolUnit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .radixPlaceholder(
                                visible = true,
                                shape = CircleShape
                            )
                    )
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                TokenBalance(
                    modifier = Modifier
                        .fillMaxWidth(fraction = if (state.poolUnit?.stake == null) 0.5f else 1f)
                        .radixPlaceholder(visible = state.poolUnit?.stake == null),
                    fungibleResource = state.poolUnit?.stake
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = RadixTheme.colors.gray4
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = stringResource(id = R.string.account_poolUnits_details_currentRedeemableValue),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                if (state.poolUnit != null) {
                    PoolResourcesValues(
                        poolUnit = state.poolUnit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingMedium)
                    )
                } else {
                    Column(
                        modifier = modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingMedium)
                            .fillMaxWidth()
                            .assetOutlineBorder()
                            .padding(
                                vertical = RadixTheme.dimensions.paddingDefault,
                                horizontal = RadixTheme.dimensions.paddingSmall
                            ),
                        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                                .radixPlaceholder(visible = true)
                        )
                        Divider(color = RadixTheme.colors.gray4)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                                .radixPlaceholder(visible = true)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = RadixTheme.colors.gray4
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                if (!state.poolUnit?.stake?.description.isNullOrBlank()) {
                    Text(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = state.poolUnit?.stake?.description.orEmpty(),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray1
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        color = RadixTheme.colors.gray4
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
                AddressRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    address = state.resourceAddress
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                AssetMetadataRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    key = stringResource(id = R.string.assetDetails_currentSupply)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = RadixTheme.dimensions.paddingDefault)
                            .widthIn(min = RadixTheme.dimensions.paddingXXXLarge * 2)
                            .radixPlaceholder(visible = state.poolUnit?.stake?.currentSupply == null),
                        text = when {
                            state.poolUnit?.stake?.currentSupply != null -> when (state.poolUnit.stake.currentSupply) {
                                BigDecimal.ZERO -> stringResource(id = R.string.assetDetails_supplyUnkown)
                                else -> state.poolUnit.stake.currentSupply.displayableQuantity()
                            }

                            else -> ""
                        },
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.End
                    )
                }

                Column(modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall)) {
                    if (state.poolUnit?.stake?.behaviours == null || state.poolUnit.stake.behaviours.isNotEmpty()) {
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

                    if (state.poolUnit?.stake?.behaviours == null) {
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
                        state.poolUnit.stake.behaviours.forEach { behaviour ->
                            Behaviour(
                                icon = behaviour.icon(),
                                name = behaviour.name()
                            )
                        }
                    }
                }

                if (!state.poolUnit?.stake?.tags.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                        text = stringResource(id = R.string.assetDetails_tags),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray2
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                        content = {
                            state.poolUnit?.stake?.tags?.forEach { tag ->
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

            SnackbarUiMessageHandler(
                message = state.uiMessage,
                onMessageShown = onMessageShown
            )
        }
    }
}
