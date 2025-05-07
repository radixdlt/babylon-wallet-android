package com.babylon.wallet.android.presentation.transfer.assets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.model.displaySubtitle
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.extensions.toDecimal192OrNull
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun SpendingAssetItem(
    modifier: Modifier = Modifier,
    asset: SpendingAsset,
    onItemClick: () -> Unit,
    onAmountTyped: (String) -> Unit,
    onMaxClicked: () -> Unit
) {
    val isEditingState = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    OutlinedCard(
        modifier = modifier.clickable(enabled = asset is SpendingAsset.Fungible) {
            focusRequester.requestFocus()
        },
        shape = RadixTheme.shapes.roundedRectSmall,
        colors = CardDefaults.outlinedCardColors(
            containerColor = RadixTheme.colors.card,
            contentColor = RadixTheme.colors.text
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(
            width = if (isEditingState.value) 1.dp else 0.dp,
            color = if (isEditingState.value) RadixTheme.colors.border else Color.Transparent
        )
    ) {
        when (asset) {
            is SpendingAsset.Fungible -> FungibleSpendingAsset(
                asset = asset,
                amount = asset.amountString,
                isExceedingBalance = asset.exceedingBalance,
                onAmountChanged = onAmountTyped,
                focusRequester = focusRequester,
                isEditing = isEditingState.value,
                onEditStateChanged = {
                    isEditingState.value = it
                },
                onMaxClicked = onMaxClicked,
                onItemClick = onItemClick
            )

            is SpendingAsset.NFT -> NonFungibleSpendingAsset(
                asset = asset,
                isExceedingBalance = asset.exceedingBalance,
                onItemClick = onItemClick
            )
        }
    }
}

@Composable
private fun ColumnScope.FungibleSpendingAsset(
    modifier: Modifier = Modifier,
    asset: SpendingAsset.Fungible,
    amount: String,
    isExceedingBalance: Boolean,
    onAmountChanged: (String) -> Unit,
    focusRequester: FocusRequester,
    isEditing: Boolean,
    onEditStateChanged: (Boolean) -> Unit,
    onMaxClicked: () -> Unit,
    onItemClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    Row(
        modifier = modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumbnail.Fungible(
            modifier = Modifier
                .size(24.dp)
                .throttleClickable {
                    onItemClick()
                },
            token = asset.resource
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))
        Text(
            modifier = Modifier
                .weight(1f)
                .throttleClickable {
                    onItemClick()
                },
            text = asset.displayTitle(),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.text,
            maxLines = 2
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))
        val selectionColors = TextSelectionColors(
            handleColor = RadixTheme.colors.text,
            backgroundColor = RadixTheme.colors.text.copy(alpha = 0.4f)
        )
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                if (amount.isBlank()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "0.00",
                        style = RadixTheme.typography.header.copy(
                            color = RadixTheme.colors.textSecondary,
                            textAlign = TextAlign.End
                        )
                    )
                }
                val focusManager = LocalFocusManager.current
                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            onEditStateChanged(focusState.isFocused)
                        }
                        .focusRequester(focusRequester),
                    value = amount,
                    onValueChange = onAmountChanged,
                    singleLine = true,
                    textStyle = RadixTheme.typography.header.copy(
                        color = RadixTheme.colors.text,
                        textAlign = TextAlign.End
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    cursorBrush = SolidColor(RadixTheme.colors.text)
                )
            }
        }
    }

    AnimatedVisibility(
        visible = isExceedingBalance,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.assetTransfer_error_insufficientBalance),
            style = RadixTheme.typography.body2HighImportance.copy(
                fontSize = 12.sp,
                color = RadixTheme.colors.error,
                textAlign = TextAlign.End
            ),
            maxLines = 2,
        )
    }

    AnimatedVisibility(
        visible = isEditing,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingXXSmall)
                    .clickable { onMaxClicked() },
                text = stringResource(id = R.string.common_max),
                style = RadixTheme.typography.body1Link.copy(
                    color = RadixTheme.colors.textButton,
                    fontSize = 12.sp
                ),
                textDecoration = TextDecoration.Underline
            )

            asset.resource.ownedAmount?.let { amount ->
                Text(
                    text = "- Balance: ${amount.formatted()}",
                    style = RadixTheme.typography.body2HighImportance.copy(
                        color = RadixTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
}

@Composable
private fun NonFungibleSpendingAsset(
    modifier: Modifier = Modifier,
    asset: SpendingAsset.NFT,
    isExceedingBalance: Boolean,
    onItemClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingDefault)
            .throttleClickable {
                onItemClick()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Thumbnail.NonFungible(
            modifier = Modifier.size(24.dp),
            collection = asset.resource
        )

        Column {
            Text(
                text = asset.displayTitle(),
                color = RadixTheme.colors.text,
                style = RadixTheme.typography.body1HighImportance
            )

            Text(
                text = asset.displaySubtitle(),
                color = RadixTheme.colors.textSecondary,
                style = RadixTheme.typography.body2Regular
            )

            AnimatedVisibility(
                visible = isExceedingBalance,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.assetTransfer_error_resourceAlreadyAdded),
                    style = RadixTheme.typography.body2HighImportance.copy(
                        fontSize = 12.sp,
                        color = RadixTheme.colors.error,
                        textAlign = TextAlign.Start
                    ),
                    maxLines = 2,
                )
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SpendingAssetItemsPreview() {
    RadixWalletTheme {
        Column(
            modifier = Modifier
                .background(color = RadixTheme.colors.backgroundSecondary),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            var firstAmount by remember { mutableStateOf("") }
            SpendingAssetItem(
                asset = SpendingAsset.Fungible(
                    Resource.FungibleResource.sampleMainnet(),
                    amountString = firstAmount,
                ),
                onAmountTyped = {
                    firstAmount = it
                },
                onMaxClicked = {
                    firstAmount = "10"
                },
                onItemClick = {}
            )

            var secondAmount by remember { mutableStateOf("3.4") }
            SpendingAssetItem(
                asset = SpendingAsset.Fungible(
                    resource = Resource.FungibleResource.sampleMainnet.other(),
                    amountString = secondAmount,
                    exceedingBalance = secondAmount.toDecimal192OrNull()?.compareTo(10.toDecimal192()) == 1
                ),
                onAmountTyped = {
                    secondAmount = it
                },
                onMaxClicked = {
                    secondAmount = "10"
                },
                onItemClick = {}
            )

            val item = Resource.NonFungibleResource.Item(
                collectionAddress = ResourceAddress.sampleMainnet.random(),
                localId = NonFungibleLocalId.init("<dbooker_dunk_39>"),
                metadata = listOf(
                    Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, "Local item with ID 39", valueType = MetadataType.String),
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.KEY_IMAGE_URL.key,
                        "https://c4.wallpaperflare.com/wallpaper/817/534/563/ave-bosque-fantasia-fenix-wallpaper-preview.jpg",
                        valueType = MetadataType.Url
                    )
                )
            )
            val collection = Resource.NonFungibleResource.sampleMainnet().let {
                it.copy(
                    amount = it.amount + 1,
                    items = it.items.toMutableList().apply {
                        add(item)
                    }
                )
            }
            SpendingAssetItem(
                asset = SpendingAsset.NFT(
                    resource = collection,
                    item = item
                ),
                onAmountTyped = {
                    secondAmount = it
                },
                onMaxClicked = {
                    secondAmount = "10"
                },
                onItemClick = {}
            )

            SpendingAssetItem(
                asset = SpendingAsset.NFT(
                    resource = collection,
                    item = item,
                    exceedingBalance = true
                ),
                onAmountTyped = {
                    secondAmount = it
                },
                onMaxClicked = {
                    secondAmount = "10"
                },
                onItemClick = {}
            )
        }
    }
}
