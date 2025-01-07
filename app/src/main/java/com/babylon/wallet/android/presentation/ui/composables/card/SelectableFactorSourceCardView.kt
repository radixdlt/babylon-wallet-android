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
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixRadioButton
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SelectableSingleChoiceFactorSourceCard(
    modifier: Modifier = Modifier,
    item: Selectable<FactorSourceCard>,
    onSelect: (FactorSourceCard) -> Unit
) {
    FactorSourceCardView(
        modifier = modifier.noIndicationClickable { onSelect(item.data) },
        item = item.data,
        endContent = {
            RadioButtonSelectorView(
                isSelected = item.selected,
                onSelectedChange = { onSelect(item.data) }
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
fun SelectableMultiChoiceFactorSourceCard(
    modifier: Modifier = Modifier,
    item: Selectable<FactorSourceCard>,
    onCheckedChange: (FactorSourceCard, Boolean) -> Unit
) {
    FactorSourceCardView(
        modifier = modifier.noIndicationClickable { onCheckedChange(item.data, !item.selected) },
        item = item.data,
        endContent = {
            CheckboxSelectorView(
                isChecked = item.selected,
                onCheckedChange = { onCheckedChange(item.data, it) }
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
    SimpleFactorCardView(
        modifier = modifier
            .background(
                color = RadixTheme.colors.white,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .noIndicationClickable { onCheckedChange(!isChecked) },
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
@UsesSampleValues
private fun SelectableSingleChoiceFactorSourceCardPreview() {
    RadixWalletPreviewTheme {
        SelectableSingleChoiceFactorSourceCard(
            item = Selectable(
                data = FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.ARCULUS_CARD,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Arculus Card Secret",
                    includeDescription = false,
                    lastUsedOn = "Today",
                    kind = FactorSourceKind.ARCULUS_CARD,
                    messages = persistentListOf(FactorSourceStatusMessage.SecurityPrompt.WriteDownSeedPhrase),
                    accounts = persistentListOf(Account.sampleMainnet()),
                    personas = persistentListOf(),
                    hasHiddenEntities = false
                ),
                selected = true
            ),
            onSelect = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SelectableMultiChoiceFactorSourceCardPreview() {
    RadixWalletPreviewTheme {
        SelectableMultiChoiceFactorSourceCard(
            item = Selectable(
                data = FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.ARCULUS_CARD,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Arculus Card Secret",
                    includeDescription = false,
                    lastUsedOn = "Today",
                    kind = FactorSourceKind.ARCULUS_CARD,
                    messages = persistentListOf(),
                    accounts = persistentListOf(),
                    personas = persistentListOf(),
                    hasHiddenEntities = false
                ),
                selected = true
            ),
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
                id = FactorSourceId.Hash.init(
                    kind = FactorSourceKind.DEVICE,
                    mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                ),
                name = "My Phone",
                includeDescription = true,
                lastUsedOn = null,
                kind = FactorSourceKind.DEVICE,
                messages = persistentListOf(),
                accounts = persistentListOf(),
                personas = persistentListOf(),
                hasHiddenEntities = false
            ),
            onRemoveClick = {}
        )
    }
}
