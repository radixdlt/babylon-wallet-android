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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.AssetMetadataRow
import com.babylon.wallet.android.presentation.status.assets.AssetDialogArgs
import com.babylon.wallet.android.presentation.status.assets.BehavioursSection
import com.babylon.wallet.android.presentation.status.assets.TagsSection
import com.babylon.wallet.android.presentation.ui.composables.ShimmeringView
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.FiatBalanceView
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import rdx.works.core.displayableQuantity
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.isXrd
import java.math.BigDecimal

@Composable
fun FungibleDialogContent(
    modifier: Modifier = Modifier,
    token: Token?,
    tokenPrice: AssetPrice.TokenPrice?,
    args: AssetDialogArgs.Fungible,
    isLoadingBalance: Boolean
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
                    text = when (val supply = token?.resource?.currentSupply) {
                        null -> ""
                        BigDecimal.ZERO -> stringResource(id = R.string.assetDetails_supplyUnkown)
                        else -> supply.displayableQuantity()
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
