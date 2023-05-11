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
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView

@Composable
fun TargetAccountCard(
    modifier: Modifier = Modifier,
    onChooseAccountClick: () -> Unit,
    onAddAssetsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isDeletable: Boolean = false,
    selectedAccount: TransferViewModel.State.SelectedAccountForTransfer,
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
        val cardModifier = when (selectedAccount.type) {
            TransferViewModel.State.SelectedAccountForTransfer.Type.NoAccount -> {
                Modifier
            }
            TransferViewModel.State.SelectedAccountForTransfer.Type.ExistingAccount -> {
                Modifier
                    .background(
                        brush = selectedAccount.account?.appearanceID?.let { appearanceId ->
                            Brush.linearGradient(AccountGradientList[appearanceId % AccountGradientList.size])
                        } ?: run {
                            Brush.linearGradient(AccountGradientList[0])
                        },
                        shape = RadixTheme.shapes.roundedRectTopMedium
                    )
            }
            TransferViewModel.State.SelectedAccountForTransfer.Type.ThirdPartyAccount -> {
                Modifier
                    .background(
                        color = RadixTheme.colors.gray2,
                        shape = RadixTheme.shapes.roundedRectTopMedium
                    )
            }
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
            if (selectedAccount.type == TransferViewModel.State.SelectedAccountForTransfer.Type.NoAccount) {
                RadixTextButton(
                    text = stringResource(id = R.string.choose_accounts),
                    textStyle = RadixTheme.typography.body1Header,
                    contentColor = RadixTheme.colors.gray2,
                    onClick = onChooseAccountClick
                )
            } else {
                Text(
                    modifier = Modifier.padding(start = RadixTheme.dimensions.paddingMedium),
                    text = selectedAccount.account?.displayName.orEmpty(),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.white
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            val hasAccount = selectedAccount.type != TransferViewModel.State.SelectedAccountForTransfer.Type.NoAccount
            if (hasAccount) {
                ActionableAddressView(
                    address = selectedAccount.account?.address.orEmpty(),
                    textStyle = RadixTheme.typography.body2HighImportance,
                    textColor = RadixTheme.colors.white.copy(alpha = 0.8f),
                    iconColor = RadixTheme.colors.white.copy(alpha = 0.8f)
                )
            }
            if (isDeletable || hasAccount) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "clear",
                        tint = if (hasAccount) RadixTheme.colors.white else RadixTheme.colors.gray1,
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
            selectedAccount = TransferViewModel.State.SelectedAccountForTransfer()
        )
    }
}
