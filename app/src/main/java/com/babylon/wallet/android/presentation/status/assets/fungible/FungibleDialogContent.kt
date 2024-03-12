package com.babylon.wallet.android.presentation.status.assets.fungible

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.AssetPrice
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.resources.isXrd
import com.babylon.wallet.android.presentation.account.composable.AssetMetadataRow
import com.babylon.wallet.android.presentation.status.assets.AssetDialogArgs
import com.babylon.wallet.android.presentation.status.assets.BehavioursSection
import com.babylon.wallet.android.presentation.status.assets.TagsSection
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.FiatBalance
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun FungibleDialogContent(
    modifier: Modifier = Modifier,
    token: Token?,
    tokenPrice: AssetPrice.TokenPrice?,
    args: AssetDialogArgs.Fungible,
) {
    val resourceAddress = args.resourceAddress
    val isNewlyCreated = args.isNewlyCreated
    val amount = args.fungibleAmountOf(resourceAddress) ?: token?.resource?.ownedAmount
    Column(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingSemiLarge
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (token?.resource != null) {
            Thumbnail.Fungible(
                modifier = Modifier.size(104.dp),
                token = token.resource
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
        if (amount != null) {
            TokenBalance(
                modifier = Modifier
                    .fillMaxWidth(fraction = if (token?.resource == null) 0.5f else 1f)
                    .radixPlaceholder(visible = token?.resource == null),
                amount = amount,
                symbol = token?.resource?.symbol.orEmpty(),
            )

            val priceFormatted = remember(tokenPrice) {
                tokenPrice?.priceFormatted
            }
            if (priceFormatted != null) {
                FiatBalance(
                    modifier = Modifier.padding(top = RadixTheme.dimensions.paddingSmall),
                    fiatPriceFormatted = priceFormatted,
                    style = RadixTheme.typography.body2HighImportance
                )
            }
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        HorizontalDivider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        if (!token?.resource?.description.isNullOrBlank()) {
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                text = token?.resource?.description.orEmpty(),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            HorizontalDivider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
        AddressRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingSmall),
            address = resourceAddress,
            isNewlyCreatedEntity = isNewlyCreated
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        if (!isNewlyCreated) {
            AssetMetadataRow(
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
                    text = when {
                        token?.resource?.currentSupply != null -> when (token.resource.currentSupply) {
                            BigDecimal.ZERO -> stringResource(id = R.string.assetDetails_supplyUnkown)
                            else -> token.resource.currentSupply.displayableQuantity()
                        }

                        else -> ""
                    },
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            BehavioursSection(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                isXRD = token?.resource?.isXrd ?: false,
                behaviours = token?.resource?.behaviours
            )

            TagsSection(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                tags = token?.resource?.tags,
            )
        }
    }
}
