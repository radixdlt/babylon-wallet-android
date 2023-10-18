package com.babylon.wallet.android.presentation.account.composable

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.ValidatorDetail
import com.babylon.wallet.android.domain.model.XrdResource
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.babylon.wallet.android.presentation.ui.composables.resources.ValidatorDetailsItem
import com.babylon.wallet.android.presentation.ui.composables.resources.poolName
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun LSUBottomSheetDetails(
    lsuUnit: LiquidStakeUnit,
    validatorDetail: ValidatorDetail,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = poolName(lsuUnit.fungibleResource.name),
            onBackClick = onCloseClick,
            modifier = Modifier.fillMaxWidth(),
            contentColor = RadixTheme.colors.gray1,
            backIconType = BackIconType.Close
        )
        Spacer(modifier = Modifier.height(22.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Thumbnail.LSU(
                modifier = Modifier.size(104.dp),
                liquidStakeUnit = lsuUnit
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            TokenBalance(lsuUnit.fungibleResource)
            Divider(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingLarge),
                color = RadixTheme.colors.gray4
            )
            Text(
                text = stringResource(id = R.string.account_poolUnits_details_currentRedeemableValue),
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1
            )
            ValidatorDetailsItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingDefault),
                validator = validatorDetail
            )
            LSUResourceValue(resource = lsuUnit, validatorDetail)
            Divider(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingLarge),
                color = RadixTheme.colors.gray4
            )
            if (!validatorDetail.description.isNullOrBlank()) {
                Text(
                    text = lsuUnit.fungibleResource.description,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
                Divider(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = RadixTheme.dimensions.paddingLarge,
                            bottom = RadixTheme.dimensions.paddingDefault
                        ),
                    color = RadixTheme.colors.gray4
                )
            }
            AddressRow(
                modifier = Modifier.fillMaxWidth(),
                address = lsuUnit.fungibleResource.resourceAddress,
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            if (lsuUnit.fungibleResource.displayTitle.isNotEmpty()) {
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.assetDetails_name),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray2
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = lsuUnit.fungibleResource.displayTitle,
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1
                    )
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }

            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.assetDetails_currentSupply),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    modifier = Modifier.padding(start = RadixTheme.dimensions.paddingDefault),
                    text = lsuUnit.fungibleResource.currentSupplyToDisplay ?: stringResource(id = R.string.assetDetails_supplyUnkown),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            AddressRow(
                modifier = Modifier.fillMaxWidth(),
                address = lsuUnit.validatorAddress,
                label = "Validator address"
            )

            if (lsuUnit.fungibleResource.behaviours.isNotEmpty()) {
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
                    lsuUnit.fungibleResource.behaviours.forEach { behaviour ->
                        Behaviour(
                            icon = behaviour.icon(),
                            name = behaviour.name()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun LSUResourceValue(
    resource: LiquidStakeUnit,
    validatorDetail: ValidatorDetail,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .border(1.dp, RadixTheme.colors.gray4, RadixTheme.shapes.roundedRectMedium)
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
                resourceAddress = XrdResource.officialAddress,
                ownedAmount = null
            )
        )
        Text(
            modifier = Modifier.weight(1f),
            text = Resource.XRD_SYMBOL,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 2
        )
        Text(
            resource.stakeValueInXRD(validatorDetail.totalXrdStake)?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )
    }
}

@Preview
@Composable
fun LSUBottomSheetDetailsPreview() {
    RadixWalletTheme {
        LSUBottomSheetDetails(
            lsuUnit = SampleDataProvider().sampleLSUUnit(),
            onCloseClick = {},
            validatorDetail = ValidatorDetail("address1", "Validator 1", null, null, BigDecimal(100000))
        )
    }
}
