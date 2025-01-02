package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixRadioButton
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.radixdlt.sargon.FactorSourceKind
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SelectableSingleChoiceFactorSourceKindCard(
    modifier: Modifier = Modifier,
    item: Selectable<FactorSourceKindCard>,
    onSelect: (FactorSourceKindCard) -> Unit
) {
    FactorSourceKindCardView(
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
@Preview
private fun SelectableSingleChoiceFactorSourceKindCardPreview() {
    RadixWalletPreviewTheme {
        SelectableSingleChoiceFactorSourceKindCard(
            item = Selectable(
                FactorSourceKindCard(
                    kind = FactorSourceKind.DEVICE,
                    messages = persistentListOf()
                )
            ),
            onSelect = {}
        )
    }
}
