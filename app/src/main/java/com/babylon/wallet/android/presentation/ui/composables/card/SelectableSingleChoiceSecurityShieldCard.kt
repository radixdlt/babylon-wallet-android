package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.shared.RadioButtonSelectorView
import com.babylon.wallet.android.presentation.ui.model.securityshields.SecurityShieldCard
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun SelectableSingleChoiceSecurityShieldCard(
    modifier: Modifier = Modifier,
    item: Selectable<SecurityShieldCard>,
    onSelect: (SecurityShieldCard) -> Unit
) {
    SecurityShieldCardView(
        modifier = modifier.noIndicationClickable {
            onSelect(item.data)
        },
        item = item.data,
        iconRes = DSR.ic_security_shields,
        iconSize = 36.dp,
        contentPadding = PaddingValues(
            start = RadixTheme.dimensions.paddingDefault,
            top = RadixTheme.dimensions.paddingDefault,
            bottom = RadixTheme.dimensions.paddingDefault
        ),
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
@UsesSampleValues
private fun SelectableSingleChoiceSecurityShieldCardPreview(
    @PreviewParameter(SecurityShieldCardPreviewProvider::class) item: SecurityShieldCard
) {
    RadixWalletPreviewTheme {
        SelectableSingleChoiceSecurityShieldCard(
            item = Selectable(item),
            onSelect = {}
        )
    }
}
