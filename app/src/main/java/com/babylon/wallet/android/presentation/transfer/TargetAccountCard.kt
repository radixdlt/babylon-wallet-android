package com.babylon.wallet.android.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView

@Composable
fun TargetAccountCard(
    modifier: Modifier = Modifier,
    onChooseAccountClick: () -> Unit,
    onAddAssetsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isDeletable: Boolean = false,
    targetAccount: TargetAccount,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = RadixTheme.colors.gray4,
                shape = RadixTheme.shapes.roundedRectMedium
            )
    ) {
        val cardModifier = when (targetAccount) {
            is TargetAccount.Skeleton -> Modifier
            is TargetAccount.Owned -> Modifier
                .background(
                    brush = Brush.linearGradient(
                        getAccountGradientColorsFor(targetAccount.account.appearanceID)
                    ),
                    shape = RadixTheme.shapes.roundedRectTopMedium
                )
            is TargetAccount.Other -> Modifier
                .background(
                    color = RadixTheme.colors.gray2,
                    shape = RadixTheme.shapes.roundedRectTopMedium
                )
        }
        Row(
            modifier = cardModifier
                .padding(
                    start = RadixTheme.dimensions.paddingMedium,
                    end = RadixTheme.dimensions.paddingXSmall,
                    top = RadixTheme.dimensions.paddingXSmall,
                    bottom = RadixTheme.dimensions.paddingXSmall
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (targetAccount) {
                is TargetAccount.Skeleton -> {
                    RadixTextButton(
                        text = stringResource(id = R.string.choose_accounts),
                        textStyle = RadixTheme.typography.body1Header,
                        contentColor = RadixTheme.colors.gray2,
                        onClick = onChooseAccountClick
                    )
                }
                is TargetAccount.Other -> {
                    Text(
                        modifier = Modifier.padding(start = RadixTheme.dimensions.paddingMedium),
                        text = stringResource(id = R.string.unknown),
                        style = RadixTheme.typography.body1Header,
                        color = RadixTheme.colors.white
                    )
                }
                is TargetAccount.Owned -> {
                    Text(
                        modifier = Modifier.padding(start = RadixTheme.dimensions.paddingMedium),
                        text = targetAccount.account.displayName,
                        style = RadixTheme.typography.body1Header,
                        color = RadixTheme.colors.white
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (targetAccount.isAddressValid) {
                ActionableAddressView(
                    address = targetAccount.address,
                    textStyle = RadixTheme.typography.body2HighImportance,
                    textColor = RadixTheme.colors.white.copy(alpha = 0.8f),
                    iconColor = RadixTheme.colors.white.copy(alpha = 0.8f)
                )
            }

            if (isDeletable || targetAccount.isAddressValid) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "clear",
                        tint = if (targetAccount.isAddressValid) RadixTheme.colors.white else RadixTheme.colors.gray1,
                    )
                }
            }
        }

        Divider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.gray4)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RadixTheme.colors.gray5,
                    shape = RadixTheme.shapes.roundedRectBottomMedium
                )
                .padding(RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadixTextButton(
                text = stringResource(id = R.string.add_assets),
                contentColor = RadixTheme.colors.gray2,
                onClick = onAddAssetsClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TargetAccountCardPreview() {
    RadixWalletTheme {
        TargetAccountCard(
            onChooseAccountClick = {},
            onAddAssetsClick = {},
            onDeleteClick = {},
            targetAccount = TargetAccount.Skeleton(index = 0, assets = emptyList())
        )
    }
}
