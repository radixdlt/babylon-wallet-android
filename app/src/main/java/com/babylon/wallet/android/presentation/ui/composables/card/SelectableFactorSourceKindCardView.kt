package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.shared.RadioButtonSelectorView
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
