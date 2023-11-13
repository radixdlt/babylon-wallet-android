package com.babylon.wallet.android.presentation.account.composable

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
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.PoolResourcesValues
import com.babylon.wallet.android.presentation.ui.composables.assets.poolName
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance

@Composable
fun PoolUnitBottomSheetDetails(
    poolUnit: PoolUnit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = poolName(poolUnit.stake.name),
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
            Thumbnail.PoolUnit(
                modifier = Modifier.size(104.dp),
                poolUnit = poolUnit
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            TokenBalance(poolUnit.stake)
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
            PoolResourcesValues(
                poolUnit = poolUnit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RadixTheme.dimensions.paddingLarge, bottom = RadixTheme.dimensions.paddingDefault)
            )
            Divider(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingLarge),
                color = RadixTheme.colors.gray4
            )
            if (poolUnit.stake.description.isNotBlank()) {
                Text(
                    text = poolUnit.stake.description,
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
                address = poolUnit.stake.resourceAddress
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            if (poolUnit.stake.displayTitle.isNotEmpty()) {
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
                        text = poolUnit.stake.displayTitle,
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
                    modifier = Modifier
                        .padding(start = RadixTheme.dimensions.paddingDefault),
                    text = poolUnit.stake.currentSupplyToDisplay ?: stringResource(id = R.string.assetDetails_supplyUnkown),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            if (!poolUnit.stake.behaviours.isNullOrEmpty()) {
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
                    poolUnit.stake.behaviours.forEach { behaviour ->
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

@Preview
@Composable
fun PoolUnitBottomSheetDetailsPreview() {
    RadixWalletTheme {
        PoolUnitBottomSheetDetails(
            poolUnit = SampleDataProvider().samplePoolUnit(),
            onCloseClick = {}
        )
    }
}
