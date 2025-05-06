@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.composable.RadixCheckboxDefaults
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.designsystem.composable.RadixRadioButton
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
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
        modifier = modifier.noIndicationClickable(item.data.isEnabled) { onSelect(item.data) },
        item = item.data,
        endContent = {
            RadioButtonSelectorView(
                isEnabled = item.data.isEnabled,
                isSelected = item.selected,
                onSelectedChange = { onSelect(item.data) }
            )
        }
    )
}

@Composable
private fun RadioButtonSelectorView(
    isEnabled: Boolean = true,
    isSelected: Boolean,
    onSelectedChange: () -> Unit
) {
    Row {
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

        RadixRadioButton(
            enabled = isEnabled,
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
                tint = RadixTheme.colors.iconSecondary
            )
        }
    }
}

@Composable
fun SimpleSelectableSingleChoiceFactorSourceCard(
    modifier: Modifier = Modifier,
    item: Selectable<FactorSourceCard>,
    onSelect: (FactorSourceCard) -> Unit
) {
    CardContainer(
        modifier = Modifier
    ) {
        SimpleFactorCardView(
            modifier = modifier
                .noIndicationClickable { onSelect(item.data) },
            iconRes = item.data.kind.iconRes(),
            title = item.data.name,
            endContent = {
                RadioButtonSelectorView(
                    isSelected = item.selected,
                    onSelectedChange = { onSelect(item.data) }
                )
            }
        )
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
                color = RadixTheme.colors.cardOnSecondary,
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
            colors = RadixCheckboxDefaults.colors(),
            onCheckedChange = onCheckedChange
        )

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))
    }
}

@Composable
private fun CardContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .defaultCardShadow(elevation = 6.dp)
            .clip(RadixTheme.shapes.roundedRectMedium)
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.cardOnSecondary,
                shape = RadixTheme.shapes.roundedRectDefault
            )
    ) {
        content()
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
                    hasHiddenEntities = false,
                    supportsBabylon = true,
                    isEnabled = true
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
                    hasHiddenEntities = false,
                    supportsBabylon = true,
                    isEnabled = true
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
private fun SimpleSelectableSingleChoiceFactorSourceCardPreview() {
    RadixWalletPreviewTheme {
        SimpleSelectableSingleChoiceFactorSourceCard(
            item = Selectable(
                FactorSourceCard(
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
                    hasHiddenEntities = false,
                    supportsBabylon = true,
                    isEnabled = true
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
private fun SimpleSelectableMultiChoiceFactorSourceCardPreview() {
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
                hasHiddenEntities = false,
                supportsBabylon = true,
                isEnabled = true
            ),
            onRemoveClick = {}
        )
    }
}
