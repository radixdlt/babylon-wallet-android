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
import androidx.compose.material3.IconButtonDefaults
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
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.transaction.model.GuaranteeItem
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun TransactionAccountWithGuaranteesCard(
    modifier: Modifier = Modifier,
    guaranteeItem: GuaranteeItem,
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
                    brush = when (val involvedAccount = guaranteeItem.account) {
                        is InvolvedAccount.Other -> SolidColor(RadixTheme.colors.iconSecondary)
                        is InvolvedAccount.Owned -> involvedAccount.account.appearanceId.gradient()
                    },
                    shape = RadixTheme.shapes.roundedRectTopMedium
                )
                .padding(RadixTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (val involvedAccount = guaranteeItem.account) {
                    is InvolvedAccount.Other -> stringResource(
                        id = com.babylon.wallet.android.R.string.interactionReview_externalAccountName
                    )
                    is InvolvedAccount.Owned -> involvedAccount.account.displayName.value
                },
                style = RadixTheme.typography.body1Header,
                maxLines = 1,
                modifier = Modifier.weight(1f, false),
                color = White
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

            ActionableAddressView(
                address = guaranteeItem.accountAddress.asGeneral(),
                textStyle = RadixTheme.typography.body1Regular,
                textColor = White,
                iconColor = White
            )
        }

        Column(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .background(
                    color = RadixTheme.colors.background,
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
                val fungible = guaranteeItem.transferable.asset.resource as Resource.FungibleResource
                Thumbnail.Fungible(
                    modifier = Modifier.size(44.dp),
                    token = fungible
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = guaranteeItem.transferable.displayTitle(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.text,
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
                            text = stringResource(id = com.babylon.wallet.android.R.string.interactionReview_estimated),
                            style = RadixTheme.typography.body2HighImportance,
                            color = RadixTheme.colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                        Text(
                            modifier = Modifier,
                            text = guaranteeItem.updatedAmount.estimated.formatted(),
                            style = RadixTheme.typography.secondaryHeader,
                            color = RadixTheme.colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                    Row {
                        Text(
                            modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                            text = stringResource(id = com.babylon.wallet.android.R.string.interactionReview_guaranteed),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                        Text(
                            modifier = Modifier,
                            text = guaranteeItem.updatedAmount.guaranteed.formatted(),
                            style = RadixTheme.typography.body2HighImportance,
                            color = RadixTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            HorizontalDivider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.divider)

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
                    color = RadixTheme.colors.text
                )

                IconButton(
                    modifier = Modifier.weight(0.7f),
                    onClick = onGuaranteePercentDecreased,
                    enabled = guaranteeItem.isDecreaseAllowed,
                    colors = IconButtonDefaults.iconButtonColors().copy(
                        contentColor = RadixTheme.colors.icon,
                        disabledContentColor = RadixTheme.colors.icon.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        painterResource(
                            id = com.babylon.wallet.android.R.drawable.ic_minus
                        ),
                        contentDescription = "minus button"
                    )
                }

                RadixTextField(
                    modifier = Modifier.weight(1.1f),
                    onValueChanged = onGuaranteePercentChanged,
                    value = guaranteeItem.typedPercent,
                    singleLine = true,
                    errorHighlight = !guaranteeItem.isInputValid,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.None,
                        keyboardType = KeyboardType.Number
                    )
                )

                IconButton(
                    modifier = Modifier.weight(0.7f),
                    onClick = onGuaranteePercentIncreased,
                    colors = IconButtonDefaults.iconButtonColors().copy(
                        contentColor = RadixTheme.colors.icon,
                        disabledContentColor = RadixTheme.colors.icon.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        painterResource(
                            id = com.babylon.wallet.android.R.drawable.ic_plus
                        ),
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
        val state: MutableState<GuaranteeItem> = remember {
            mutableStateOf(
                requireNotNull(
                    GuaranteeItem.from(
                        involvedAccount = InvolvedAccount.Owned(Account.sampleMainnet()),
                        transferable = Transferable.FungibleType.Token(
                            asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                            amount = BoundedAmount.Predicted(
                                estimated = 10.toDecimal192(),
                                instructionIndex = 1L,
                                offset = 0.9.toDecimal192()
                            ),
                            isNewlyCreated = false
                        )
                    )
                )
            )
        }
        TransactionAccountWithGuaranteesCard(
            guaranteeItem = state.value,
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
