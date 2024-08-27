package com.babylon.wallet.android.presentation.dialogs.assets.fungible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.MetadataView
import com.babylon.wallet.android.presentation.dialogs.assets.AssetDialogArgs
import com.babylon.wallet.android.presentation.dialogs.assets.BehavioursSection
import com.babylon.wallet.android.presentation.dialogs.assets.DescriptionSection
import com.babylon.wallet.android.presentation.dialogs.assets.NonStandardMetadataSection
import com.babylon.wallet.android.presentation.dialogs.assets.TagsSection
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.ShimmeringView
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.FiatBalanceView
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.Token

@Composable
fun FungibleDialogContent(
    modifier: Modifier = Modifier,
    token: Token?,
    tokenPrice: AssetPrice.TokenPrice?,
    args: AssetDialogArgs.Fungible,
    isLoadingBalance: Boolean,
    canBeHidden: Boolean,
    onHideClick: (() -> Unit)? = null
) {
    val resourceAddress = args.resourceAddress
    val isNewlyCreated = args.isNewlyCreated
    val amount = args.fungibleAmountOf(resourceAddress) ?: token?.resource?.ownedAmount
    Column(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = RadixTheme.dimensions.paddingSemiLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FungibleIconSection(token = token)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            if (amount != null) {
                TokenBalance(
                    modifier = Modifier
                        .fillMaxWidth(fraction = if (token?.resource == null) 0.5f else 1f)
                        .radixPlaceholder(visible = token?.resource == null),
                    amount = amount,
                    symbol = token?.resource?.symbol.orEmpty(),
                )

                val fiatPrice = tokenPrice?.price
                if (isLoadingBalance) {
                    ShimmeringView(
                        modifier = Modifier
                            .padding(top = RadixTheme.dimensions.paddingXXSmall)
                            .height(12.dp)
                            .fillMaxWidth(0.2f),
                        isVisible = true
                    )
                } else if (fiatPrice != null) {
                    FiatBalanceView(
                        modifier = Modifier.padding(top = RadixTheme.dimensions.paddingSmall),
                        fiatPrice = fiatPrice,
                        textStyle = RadixTheme.typography.body2HighImportance
                    )
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            HorizontalDivider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            DescriptionSection(
                modifier = Modifier.fillMaxWidth(),
                description = token?.resource?.description,
                infoUrl = token?.resource?.infoUrl
            )

            AddressRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                address = Address.Resource(resourceAddress),
                isNewlyCreatedEntity = isNewlyCreated
            )
            if (!token?.resource?.name.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                MetadataView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    key = stringResource(id = R.string.assetDetails_name)
                ) {
                    Text(
                        text = token?.resource?.name.orEmpty(),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1
                    )
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            if (!isNewlyCreated) {
                MetadataView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    key = stringResource(id = R.string.assetDetails_currentSupply)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = RadixTheme.dimensions.paddingDefault)
                            .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2)
                            .radixPlaceholder(visible = token?.resource?.currentSupply == null),
                        text = when (val supply = token?.resource?.currentSupply) {
                            null -> ""
                            0.toDecimal192() -> stringResource(id = R.string.assetDetails_supplyUnkown)
                            else -> supply.formatted()
                        },
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.End
                    )
                }

                token?.resource?.divisibility?.let {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    MetadataView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                        key = stringResource(id = R.string.assetDetails_divisibility)
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(start = RadixTheme.dimensions.paddingDefault)
                                .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2),
                            text = it.value.toString(),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.gray1,
                            textAlign = TextAlign.End
                        )
                    }
                }

                BehavioursSection(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    isXRD = token?.resource?.isXrd ?: false,
                    behaviours = token?.resource?.behaviours
                )

                TagsSection(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    tags = token?.resource?.tags
                )

                token?.resource?.let { resource ->
                    NonStandardMetadataSection(resource = resource)
                }
            }
        }

        if (canBeHidden) {
            Spacer(modifier = Modifier.weight(1f))

            RadixBottomBar(
                modifier = Modifier.padding(top = RadixTheme.dimensions.paddingLarge),
                button = {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.assetDetails_hideAsset),
                        onClick = { onHideClick?.invoke() }
                    )
                }
            )
        }
    }
}

@Composable
private fun FungibleIconSection(
    modifier: Modifier = Modifier,
    token: Token?
) {
    if (token?.resource != null) {
        Thumbnail.Fungible(
            modifier = modifier.size(104.dp),
            token = token.resource
        )
    } else {
        Box(
            modifier = modifier
                .size(104.dp)
                .radixPlaceholder(
                    visible = true,
                    shape = CircleShape
                )
        )
    }
}
