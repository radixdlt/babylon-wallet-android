package com.babylon.wallet.android.presentation.status.assets.lsu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.account.composable.AssetMetadataRow
import com.babylon.wallet.android.presentation.status.assets.BehavioursSection
import com.babylon.wallet.android.presentation.status.assets.TagsSection
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.ValidatorDetailsItem
import com.babylon.wallet.android.presentation.ui.composables.assets.assetOutlineBorder
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun LSUDialogContent(
    modifier: Modifier = Modifier,
    resourceAddress: String,
    lsu: LiquidStakeUnit?
) {
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
        if (lsu != null) {
            Thumbnail.LSU(
                modifier = Modifier.size(104.dp),
                liquidStakeUnit = lsu
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
                .fillMaxWidth(fraction = if (lsu == null) 0.5f else 1f)
                .radixPlaceholder(visible = lsu == null),
            fungibleResource = lsu?.fungibleResource
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        HorizontalDivider(
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

        if (lsu != null) {
            ValidatorDetailsItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                validator = lsu.validator
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
            lsu = lsu,
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = RadixTheme.colors.gray4
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        if (!lsu?.fungibleResource?.description.isNullOrBlank()) {
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                text = lsu?.fungibleResource?.description.orEmpty(),
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
            address = resourceAddress
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        AddressRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingSmall)
                .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2)
                .radixPlaceholder(visible = lsu == null),
            label = stringResource(id = R.string.assetDetails_validator),
            address = lsu?.validator?.address.orEmpty()
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
                    .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2)
                    .radixPlaceholder(visible = lsu == null),
                text = lsu?.name().orEmpty(),
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
                    .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2)
                    .radixPlaceholder(
                        visible = lsu?.fungibleResource?.currentSupply == null
                    ),
                text = when {
                    lsu?.fungibleResource?.currentSupply != null ->
                        when (lsu.fungibleResource.currentSupply) {
                            BigDecimal.ZERO -> stringResource(id = R.string.assetDetails_supplyUnkown)
                            else -> lsu.fungibleResource.currentSupply.displayableQuantity()
                        }

                    else -> ""
                },
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.End
            )
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        BehavioursSection(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            behaviours = lsu?.fungibleResource?.behaviours
        )

        TagsSection(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            tags = lsu?.fungibleResource?.tags
        )
    }
}

@Composable
private fun LSUResourceValue(
    lsu: LiquidStakeUnit?,
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
                .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2)
                .radixPlaceholder(visible = lsu == null),
            text = lsu?.stakeValue()?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

@Composable
fun LiquidStakeUnit?.name(): String = this?.name?.ifEmpty {
    stringResource(id = R.string.account_poolUnits_unknownPoolUnitName)
} ?: stringResource(id = R.string.account_poolUnits_unknownPoolUnitName)
