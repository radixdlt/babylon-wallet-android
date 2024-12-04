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
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixRadioButton
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun SelectableSingleChoiceFactorSourceCard(
    modifier: Modifier = Modifier,
    item: FactorSourceCard,
    isSelected: Boolean,
    onSelect: (FactorSourceCard) -> Unit
) {
    FactorSourceCardView(
        modifier = modifier,
        item = item,
        endContent = {
            RadioButtonSelectorView(
                isSelected = isSelected,
                onSelectedChange = { onSelect(item) }
            )
        }
    )
}

@Composable
fun SelectableMultiChoiceFactorSourceCard(
    modifier: Modifier = Modifier,
    item: FactorSourceCard,
    isChecked: Boolean,
    onCheckedChange: (FactorSourceCard, Boolean) -> Unit
) {
    FactorSourceCardView(
        modifier = modifier,
        item = item,
        endContent = {
            CheckboxSelectorView(
                isChecked = isChecked,
                onCheckedChange = { onCheckedChange(item, it) }
            )
        }
    )
}

@Composable
fun RemovableFactorSourceCard(
    modifier: Modifier = Modifier,
    item: FactorSourceCard,
    onRemoveClick: (FactorSourceCard) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FactorSourceCardView(
            modifier = Modifier.weight(1f),
            item = item
        )

        IconButton(
            onClick = { onRemoveClick(item) }
        ) {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                contentDescription = null,
                tint = RadixTheme.colors.gray2
            )
        }
    }
}

@Composable
fun SimpleSelectableMultiChoiceFactorSourceCard(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
            CheckboxSelectorView(
                isChecked = isChecked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun RadioButtonSelectorView(
    isSelected: Boolean,
    onSelectedChange: () -> Unit
) {
    Row {
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

        RadixRadioButton(
            selected = isSelected,
            onClick = onSelectedChange
        )

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))
    }
}

@Composable
private fun CheckboxSelectorView(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row {
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

        Checkbox(
            checked = isChecked,
            colors = CheckboxDefaults.colors().copy(
                checkedCheckmarkColor = RadixTheme.colors.white,
                checkedBorderColor = RadixTheme.colors.gray1,
                checkedBoxColor = RadixTheme.colors.gray1,
                uncheckedCheckmarkColor = Color.Transparent,
                uncheckedBorderColor = RadixTheme.colors.gray2,
                uncheckedBoxColor = RadixTheme.colors.gray5
            ),
            onCheckedChange = onCheckedChange
        )

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))
    }
}

@Composable
@Preview
private fun SelectableSingleChoiceFactorSourceCardPreview() {
    RadixWalletPreviewTheme {
        SelectableSingleChoiceFactorSourceCard(
            item = FactorSourceCard(
                kind = FactorSourceKind.DEVICE,
                header = FactorSourceCard.Header.New,
                messages = emptyList(),
                accounts = emptyList(),
                personas = emptyList()
            ),
            isSelected = false,
            onSelect = {}
        )
    }
}

@Composable
@Preview
private fun SelectableMultiChoiceFactorSourceCardPreview() {
    RadixWalletPreviewTheme {
        SelectableMultiChoiceFactorSourceCard(
            item = FactorSourceCard(
                kind = FactorSourceKind.ARCULUS_CARD,
                header = FactorSourceCard.Header.New,
                messages = emptyList(),
                accounts = emptyList(),
                personas = emptyList()
            ),
            isChecked = true,
            onCheckedChange = { _, _ -> }
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SimpleSelectableFactorSourceCardPreview() {
    RadixWalletPreviewTheme {
        SimpleSelectableMultiChoiceFactorSourceCard(
            iconRes = FactorSourceKind.DEVICE.iconRes(),
            title = "My Phone",
            isChecked = false,
            onCheckedChange = {}
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
                header = FactorSourceCard.Header.New,
                messages = emptyList(),
                accounts = emptyList(),
                personas = emptyList()
            ),
            onRemoveClick = {}
        )
    }
}