package com.babylon.wallet.android.presentation.transfer.assets

import android.graphics.drawable.ColorDrawable
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun SpendingAssetItem(
    modifier: Modifier = Modifier,
    asset: SpendingAsset,
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
            containerColor = RadixTheme.colors.defaultBackground,
            contentColor = RadixTheme.colors.gray1
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(
            width = if (isEditingState.value) 1.dp else 0.dp,
            color = if (isEditingState.value) RadixTheme.colors.gray1 else RadixTheme.colors.gray1.copy(alpha = 0.01f)
        )
    ) {
        when (asset) {
            is SpendingAsset.Fungible -> FungibleSpendingAsset(
                resource = asset.resource,
                amount = asset.amountString,
                isExceedingBalance = asset.exceedingBalance,
                onAmountChanged = onAmountTyped,
                focusRequester = focusRequester,
                isEditing = isEditingState.value,
                onEditStateChanged = {
                    isEditingState.value = it
                },
                onMaxClicked = onMaxClicked
            )

            is SpendingAsset.NFT -> NonFungibleSpendingAsset(
                nft = asset.item,
                isExceedingBalance = asset.exceedingBalance,
            )

            is SpendingAsset.LSU -> FungibleSpendingAsset(
                resource = asset.resource,
                amount = asset.amountString,
                isExceedingBalance = asset.exceedingBalance,
                onAmountChanged = onAmountTyped,
                focusRequester = focusRequester,
                isEditing = isEditingState.value,
                onEditStateChanged = {
                    isEditingState.value = it
                },
                onMaxClicked = onMaxClicked
            )

            is SpendingAsset.PoolUnit -> FungibleSpendingAsset(
                resource = asset.resource,
                amount = asset.amountString,
                isExceedingBalance = asset.exceedingBalance,
                onAmountChanged = onAmountTyped,
                focusRequester = focusRequester,
                isEditing = isEditingState.value,
                onEditStateChanged = {
                    isEditingState.value = it
                },
                onMaxClicked = onMaxClicked
            )

            is SpendingAsset.StakeClaimNFT -> NonFungibleSpendingAsset(
                nft = asset.item,
                isExceedingBalance = asset.exceedingBalance,
            )
        }
    }
}

@Composable
private fun ColumnScope.FungibleSpendingAsset(
    modifier: Modifier = Modifier,
    resource: Resource.FungibleResource,
    amount: String,
    isExceedingBalance: Boolean,
    onAmountChanged: (String) -> Unit,
    focusRequester: FocusRequester,
    isEditing: Boolean,
    onEditStateChanged: (Boolean) -> Unit,
    onMaxClicked: () -> Unit
) {
    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    Row(
        modifier = modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val placeholder = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb()))

        AsyncImage(
            modifier = Modifier
                .size(24.dp)
                .background(RadixTheme.colors.gray3, shape = RadixTheme.shapes.circle)
                .clip(RadixTheme.shapes.circle),
            model = if (resource.isXrd) {
                R.drawable.ic_xrd_token
            } else {
                rememberImageUrl(fromUrl = resource.iconUrl, size = ImageSize.MEDIUM)
            },
            placeholder = placeholder,
            fallback = placeholder,
            error = placeholder,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))
        Text(
            text = resource.displayTitle,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 2
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))
        val selectionColors = TextSelectionColors(
            handleColor = RadixTheme.colors.gray1,
            backgroundColor = RadixTheme.colors.gray1.copy(alpha = 0.4f)
        )
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            Box(modifier = Modifier.weight(1f)) {
                if (amount.isBlank()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "0.00",
                        style = RadixTheme.typography.header.copy(
                            color = RadixTheme.colors.gray3,
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
                    onValueChange = {
                        onAmountChanged(it.replace(",", ""))
                    },
                    singleLine = true,
                    textStyle = RadixTheme.typography.header.copy(
                        color = RadixTheme.colors.gray1,
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
                    )
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
            text = "Total Sum is over your current balance",
            style = RadixTheme.typography.body2HighImportance.copy(
                fontSize = 12.sp,
                color = RadixTheme.colors.red1,
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
                    .padding(horizontal = RadixTheme.dimensions.paddingXSmall)
                    .clickable { onMaxClicked() },
                text = "Max",
                style = RadixTheme.typography.body1Link.copy(
                    color = RadixTheme.colors.blue1,
                    fontSize = 12.sp
                ),
                textDecoration = TextDecoration.Underline
            )

            resource.amount?.let { amount ->
                Text(
                    text = "- Balance: ${amount.displayableQuantity()}",
                    style = RadixTheme.typography.body2HighImportance.copy(
                        color = RadixTheme.colors.gray2,
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
    nft: Resource.NonFungibleResource.Item,
    isExceedingBalance: Boolean,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 69.dp)
            .padding(RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (nft.imageUrl != null) {
            AsyncImage(
                model = rememberImageUrl(
                    fromUrl = nft.imageUrl,
                    size = ImageSize.SMALL,
                    placeholder = com.babylon.wallet.android.R.drawable.img_placeholder,
                    error = com.babylon.wallet.android.R.drawable.img_placeholder
                ),
                contentDescription = "Nft image",
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .size(55.dp)
                    .clip(RadixTheme.shapes.roundedRectSmall),
            )
        }

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))
        Column {
            Text(
                text = nft.localId.displayable,
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body2Regular
            )

//            Text(
//                text = "Devin Booker - Dunk",
//                color = RadixTheme.colors.gray1,
//                style = RadixTheme.typography.body1HighImportance
//            )

            AnimatedVisibility(
                visible = isExceedingBalance,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "This NFT is already used in another account",
                    style = RadixTheme.typography.body2HighImportance.copy(
                        fontSize = 12.sp,
                        color = RadixTheme.colors.red1,
                        textAlign = TextAlign.Start
                    ),
                    maxLines = 2,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpendingAssetItemsPreview() {
    RadixWalletTheme {
        Column(
            modifier = Modifier
                .background(color = RadixTheme.colors.gray5),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            var firstAmount by remember { mutableStateOf("") }
            SpendingAssetItem(
                asset = SpendingAsset.Fungible(
                    Resource.FungibleResource(
                        resourceAddress = "resource_rdx_abcd",
                        amount = BigDecimal.TEN,
                        nameMetadataItem = NameMetadataItem("Radix"),
                        symbolMetadataItem = SymbolMetadataItem("XRD")
                    ),
                    amountString = firstAmount,
                ),
                onAmountTyped = {
                    firstAmount = it
                },
                onMaxClicked = {
                    firstAmount = "10"
                }
            )

            var secondAmount by remember { mutableStateOf("3.4") }
            SpendingAssetItem(
                asset = SpendingAsset.Fungible(
                    resource = Resource.FungibleResource(
                        resourceAddress = "resource_rdx_abcd",
                        amount = BigDecimal.TEN,
                        nameMetadataItem = NameMetadataItem("Radix"),
                        symbolMetadataItem = SymbolMetadataItem("XRD")
                    ),
                    amountString = secondAmount,
                    exceedingBalance = secondAmount.toBigDecimalOrNull()?.compareTo(BigDecimal.TEN) == 1
                ),
                onAmountTyped = {
                    secondAmount = it
                },
                onMaxClicked = {
                    secondAmount = "10"
                }
            )

            SpendingAssetItem(
                asset = SpendingAsset.NFT(
                    Resource.NonFungibleResource.Item(
                        collectionAddress = "resource_rdx_abcd",
                        localId = Resource.NonFungibleResource.Item.ID.from("<dbooker_dunk_39>"),
                        nameMetadataItem = NameMetadataItem(name = "Local item with ID 39"),
                        iconMetadataItem = IconUrlMetadataItem(
                            url = Uri.parse(
                                "https://c4.wallpaperflare.com/wallpaper/817/534/563/ave-bosque-fantasia-fenix-wallpaper-preview.jpg"
                            )
                        )
                    )
                ),
                onAmountTyped = {
                    secondAmount = it
                },
                onMaxClicked = {
                    secondAmount = "10"
                }
            )

            SpendingAssetItem(
                asset = SpendingAsset.NFT(
                    Resource.NonFungibleResource.Item(
                        collectionAddress = "resource_rdx_abcd",
                        localId = Resource.NonFungibleResource.Item.ID.from("<dbooker_dunk_39>"),
                        nameMetadataItem = NameMetadataItem(name = "Local item with ID 39"),
                        iconMetadataItem = IconUrlMetadataItem(
                            url = Uri.parse(
                                "https://c4.wallpaperflare.com/wallpaper/817/534/563/ave-bosque-fantasia-fenix-wallpaper-preview.jpg"
                            )
                        )
                    ),
                    exceedingBalance = true
                ),
                onAmountTyped = {
                    secondAmount = it
                },
                onMaxClicked = {
                    secondAmount = "10"
                }
            )
        }
    }
}
