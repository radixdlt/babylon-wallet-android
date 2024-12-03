package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixRadioButton
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun SelectableFactorSourceCard(
    modifier: Modifier = Modifier,
    item: FactorSourceCard,
    isSelected: Boolean,
    isSingleChoice: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    FactorSourceCardView(
        modifier = modifier,
        item = item,
        endContent = {
            SelectorView(
                isSelected = isSelected,
                isSingleChoice = isSingleChoice,
                onSelectedChange = onSelectedChange
            )
        }
    )
}

@Composable
fun RemovableFactorSourceCard(
    modifier: Modifier = Modifier,
    item: FactorSourceCard,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FactorSourceCardView(
            modifier = Modifier.weight(1f),
            item = item
        )

        IconButton(onClick = onRemoveClick) {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                contentDescription = null,
                tint = RadixTheme.colors.gray2
            )
        }
    }
}

@Composable
fun SimpleSelectableFactorSourceCard(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int,
    title: String,
    isSelected: Boolean,
    isSingleChoice: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    SimpleFactorSourceCard(
        modifier = modifier
            .background(
                color = RadixTheme.colors.white,
                shape = RadixTheme.shapes.roundedRectMedium
            ),
        iconRes = iconRes,
        title = title,
        endContent = {
            SelectorView(
                isSelected = isSelected,
                isSingleChoice = isSingleChoice,
                onSelectedChange = onSelectedChange
            )
        }
    )
}

@Composable
private fun SelectorView(
    isSelected: Boolean,
    isSingleChoice: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Row {
        if (isSingleChoice) {
            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

            RadixRadioButton(
                selected = isSelected,
                onClick = { onSelectedChange(true) }
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))
        } else {
            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

            Checkbox(
                checked = isSelected,
                colors = CheckboxDefaults.colors().copy(
                    checkedCheckmarkColor = RadixTheme.colors.white,
                    checkedBorderColor = RadixTheme.colors.gray1,
                    checkedBoxColor = RadixTheme.colors.gray1,
                    uncheckedCheckmarkColor = Color.Transparent,
                    uncheckedBorderColor = RadixTheme.colors.gray2,
                    uncheckedBoxColor = RadixTheme.colors.gray5
                ),
                onCheckedChange = onSelectedChange
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SelectableFactorSourceCardPreview(
    @PreviewParameter(FactorSourceCardPreviewProvider::class) item: FactorSourceCard
) {
    RadixWalletPreviewTheme {
        SelectableFactorSourceCard(
            item = item,
            isSelected = false,
            isSingleChoice = true,
            onSelectedChange = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SimpleSelectableFactorSourceCardPreview() {
    RadixWalletPreviewTheme {
        SimpleSelectableFactorSourceCard(
            iconRes = FactorSourceKind.DEVICE.iconRes(),
            title = "My Phone",
            isSelected = false,
            isSingleChoice = false,
            onSelectedChange = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun RemovableFactorSourceCardPreview() {
    RadixWalletPreviewTheme {
        RemovableFactorSourceCard(
            item = FactorSourceCard(
                kind = FactorSourceKind.DEVICE,
                lastUsedOn = null,
                messages = emptyList(),
                accounts = emptyList(),
                personas = emptyList()
            ),
            onRemoveClick = {}
        )
    }
}