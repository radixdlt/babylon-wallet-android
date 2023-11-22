@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.status.assets.lsu

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
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.account.composable.AssetMetadataRow
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.Behaviour
import com.babylon.wallet.android.presentation.ui.composables.assets.Tag
import com.babylon.wallet.android.presentation.ui.composables.assets.assetOutlineBorder
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.babylon.wallet.android.presentation.ui.composables.resources.ValidatorDetailsItem
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun LSUAssetDialog(
    viewModel: LSUAssetDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LSUAssetDialogContent(
        state = state,
        onMessageShown = viewModel::onMessageShown,
        onDismiss = onDismiss
    )
}

@Composable
private fun LSUAssetDialogContent(
    modifier: Modifier = Modifier,
    state: LSUAssetDialogViewModel.State,
    onMessageShown: () -> Unit,
    onDismiss: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier.fillMaxHeight(fraction = 0.9f),
        title = state.validatorWithStakes?.name().orEmpty(),
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
                if (state.validatorWithStakes?.liquidStakeUnit != null) {
                    Thumbnail.LSU(
                        modifier = Modifier.size(104.dp),
                        liquidStakeUnit = state.validatorWithStakes.liquidStakeUnit
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
                        .fillMaxWidth(fraction = if (state.validatorWithStakes?.liquidStakeUnit == null) 0.5f else 1f)
                        .radixPlaceholder(visible = state.validatorWithStakes?.liquidStakeUnit == null),
                    fungibleResource = state.validatorWithStakes?.liquidStakeUnit?.fungibleResource
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

                if (state.validatorWithStakes != null) {
                    ValidatorDetailsItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                        validator = state.validatorWithStakes.validatorDetail
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .padding(horizontal = RadixTheme.dimensions.paddingMedium)
                            .radixPlaceholder(visible = true)
                    )
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                LSUResourceValue(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                    validatorWithStakes = state.validatorWithStakes,
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = RadixTheme.colors.gray4
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

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
                    key = stringResource(id = R.string.assetDetails_name)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = RadixTheme.dimensions.paddingDefault)
                            .widthIn(min = RadixTheme.dimensions.paddingXXXLarge * 2)
                            .radixPlaceholder(visible = state.validatorWithStakes == null),
                        text = state.validatorWithStakes?.name().orEmpty(),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.End
                    )
                }
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
                            .radixPlaceholder(
                                visible = state.validatorWithStakes?.liquidStakeUnit?.fungibleResource?.currentSupply == null
                            ),
                        text = when {
                            state.validatorWithStakes?.liquidStakeUnit?.fungibleResource?.currentSupply != null ->
                                when (state.validatorWithStakes.liquidStakeUnit.fungibleResource.currentSupply) {
                                    BigDecimal.ZERO -> stringResource(id = R.string.assetDetails_supplyUnkown)
                                    else -> state.validatorWithStakes.liquidStakeUnit.fungibleResource.currentSupply.displayableQuantity()
                                }

                            else -> ""
                        },
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                AssetMetadataRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    key = stringResource(id = R.string.assetDetails_validator)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = RadixTheme.dimensions.paddingDefault)
                            .widthIn(min = RadixTheme.dimensions.paddingXXXLarge * 2)
                            .radixPlaceholder(visible = state.validatorWithStakes == null),
                        text = state.validatorWithStakes?.validatorDetail?.name.orEmpty(),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.End
                    )
                }

                BehavioursSection(state)

                TagsSection(state)
            }

            SnackbarUiMessageHandler(
                message = state.uiMessage,
                onMessageShown = onMessageShown
            )
        }
    }
}

@Composable
private fun BehavioursSection(
    state: LSUAssetDialogViewModel.State
) {
    Column(modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall)) {
        if (state.validatorWithStakes?.liquidStakeUnit?.fungibleResource?.behaviours == null ||
            state.validatorWithStakes.liquidStakeUnit.fungibleResource.behaviours.isNotEmpty()
        ) {
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

        if (state.validatorWithStakes?.liquidStakeUnit?.fungibleResource?.behaviours == null) {
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
            state.validatorWithStakes.liquidStakeUnit.fungibleResource.behaviours.forEach { behaviour ->
                Behaviour(
                    icon = behaviour.icon(),
                    name = behaviour.name()
                )
            }
        }
    }
}

@Composable
private fun TagsSection(state: LSUAssetDialogViewModel.State) {
    if (!state.validatorWithStakes?.liquidStakeUnit?.fungibleResource?.tags.isNullOrEmpty()) {
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
                state.validatorWithStakes?.liquidStakeUnit?.fungibleResource?.tags?.forEach { tag ->
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

@Composable
fun ValidatorWithStakes.name(): String = liquidStakeUnit.name.ifEmpty {
    stringResource(id = R.string.account_poolUnits_unknownPoolUnitName)
}

@Composable
private fun LSUResourceValue(
    validatorWithStakes: ValidatorWithStakes?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .assetOutlineBorder()
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingLarge
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Thumbnail.Fungible(
            modifier = Modifier.size(44.dp),
            token = Resource.FungibleResource(
                resourceAddress = XrdResource.address(),
                ownedAmount = null
            )
        )
        Text(
            modifier = Modifier.weight(1f),
            text = XrdResource.SYMBOL,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 2
        )

        Text(
            modifier = Modifier
                .widthIn(min = RadixTheme.dimensions.paddingXXXLarge * 2)
                .radixPlaceholder(visible = validatorWithStakes == null),
            text = validatorWithStakes?.stakeValue()?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}
