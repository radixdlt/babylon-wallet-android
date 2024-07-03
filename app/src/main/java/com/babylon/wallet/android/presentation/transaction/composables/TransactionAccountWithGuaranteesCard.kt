package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.transaction.model.AccountWithPredictedGuarantee
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun TransactionAccountWithGuaranteesCard(
    modifier: Modifier = Modifier,
    accountWithGuarantee: AccountWithPredictedGuarantee,
    onGuaranteePercentChanged: (String) -> Unit,
    onGuaranteePercentIncreased: () -> Unit,
    onGuaranteePercentDecreased: () -> Unit,
) {
    Column(
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = when (accountWithGuarantee) {
                        is AccountWithPredictedGuarantee.Other -> SolidColor(RadixTheme.colors.gray2)
                        is AccountWithPredictedGuarantee.Owned -> accountWithGuarantee.account.appearanceId.gradient()
                    },
                    shape = RadixTheme.shapes.roundedRectTopMedium
                )
                .padding(RadixTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (accountWithGuarantee) {
                    is AccountWithPredictedGuarantee.Other -> stringResource(
                        id = com.babylon.wallet.android.R.string.transactionReview_externalAccountName
                    )

                    is AccountWithPredictedGuarantee.Owned -> accountWithGuarantee.account.displayName.value
                },
                style = RadixTheme.typography.body1Header,
                maxLines = 1,
                modifier = Modifier.weight(1f, false),
                color = RadixTheme.colors.white
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

            ActionableAddressView(
                address = Address.Account(accountWithGuarantee.address),
                textStyle = RadixTheme.typography.body1Regular,
                textColor = RadixTheme.colors.white,
                iconColor = RadixTheme.colors.white
            )
        }

        Column(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .background(
                    color = RadixTheme.colors.white,
                    shape = RadixTheme.shapes.roundedRectBottomMedium
                )
                .padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingMedium
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                val fungible = accountWithGuarantee.transferable.resource as Resource.FungibleResource
                Thumbnail.Fungible(
                    modifier = Modifier.size(44.dp),
                    token = fungible
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = fungible.displayTitle,
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                            text = stringResource(id = com.babylon.wallet.android.R.string.transactionReview_estimated),
                            style = RadixTheme.typography.body2HighImportance,
                            color = RadixTheme.colors.gray1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                        Text(
                            modifier = Modifier,
                            text = accountWithGuarantee.transferable.amount.formatted(),
                            style = RadixTheme.typography.secondaryHeader,
                            color = RadixTheme.colors.gray1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                    Row {
                        Text(
                            modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                            text = stringResource(id = com.babylon.wallet.android.R.string.transactionReview_guaranteed),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.gray2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                        Text(
                            modifier = Modifier,
                            text = accountWithGuarantee.guaranteedAmount.formatted(),
                            style = RadixTheme.typography.body2HighImportance,
                            color = RadixTheme.colors.gray2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            HorizontalDivider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.gray4)

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(2f),
                    text = stringResource(
                        id = com.babylon.wallet.android.R.string.transactionReview_guarantees_setGuaranteedMinimum
                    ).replace("%%", "%"),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )

                IconButton(
                    modifier = Modifier.weight(0.7f),
                    onClick = onGuaranteePercentDecreased
                ) {
                    Icon(
                        painterResource(
                            id = com.babylon.wallet.android.R.drawable.ic_minus
                        ),
                        tint = RadixTheme.colors.gray1,
                        contentDescription = "minus button"
                    )
                }

                RadixTextField(
                    modifier = Modifier.weight(1.1f),
                    onValueChanged = onGuaranteePercentChanged,
                    value = accountWithGuarantee.guaranteeAmountString,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.None,
                        keyboardType = KeyboardType.Number
                    )
                )

                IconButton(
                    modifier = Modifier.weight(0.7f),
                    onClick = onGuaranteePercentIncreased
                ) {
                    Icon(
                        painterResource(
                            id = com.babylon.wallet.android.R.drawable.ic_plus
                        ),
                        tint = RadixTheme.colors.gray1,
                        contentDescription = "plus button"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
}

@UsesSampleValues
@Preview("default")
@Preview("large font", fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun TransactionAccountWithGuaranteesCardPreview() {
    RadixWalletTheme {
        val state: MutableState<AccountWithPredictedGuarantee> = remember {
            mutableStateOf(
                AccountWithPredictedGuarantee.Owned(
                    account = Account.sampleMainnet(),
                    transferable = TransferableAsset.Fungible.Token(
                        amount = 10.toDecimal192(),
                        resource = Resource.FungibleResource.sampleMainnet(),
                        isNewlyCreated = false
                    ),
                    instructionIndex = 1L,
                    guaranteeAmountString = "100"
                )
            )
        }
        TransactionAccountWithGuaranteesCard(
            accountWithGuarantee = state.value,
            onGuaranteePercentChanged = { value ->
                state.value = state.value.change(value)
            },
            onGuaranteePercentDecreased = {
                state.value = state.value.decrease()
            },
            onGuaranteePercentIncreased = {
                state.value = state.value.increase()
            }
        )
    }
}
