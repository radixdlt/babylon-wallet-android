package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.shared.CardContainer
import com.babylon.wallet.android.presentation.ui.composables.shared.RadioButtonSelectorView
import com.babylon.wallet.android.presentation.ui.model.securityshields.SecurityShieldCard
import com.babylon.wallet.android.presentation.ui.modifier.enabledOpacity
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun SelectableSingleChoiceSecurityShieldCard(
    modifier: Modifier = Modifier,
    item: Selectable<SecurityShieldCard>,
    onSelect: (SecurityShieldCard) -> Unit
) {
    SimpleSecurityShieldCardView(
        modifier = modifier.noIndicationClickable { onSelect(item.data) },
        title = item.data.name.value,
        iconRes = DSR.ic_security_shields,
        endContent = {
            RadioButtonSelectorView(
                isSelected = item.selected,
                onSelectedChange = { onSelect(item.data) }
            )
        }
    )
}

@Composable
private fun SimpleSecurityShieldCardView(
    modifier: Modifier = Modifier,
    title: String,
    @DrawableRes iconRes: Int,
    isEnabled: Boolean = true,
    endContent: (@Composable () -> Unit)? = null
) {
    CardContainer(modifier = modifier) {
        Row(
            modifier = Modifier
                .enabledOpacity(isEnabled)
                .padding(start = RadixTheme.dimensions.paddingDefault)
                .padding(vertical = RadixTheme.dimensions.paddingSemiLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(36.dp),
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.text
            )

            Box(
                Modifier.enabledOpacity(isEnabled)
            ) {
                endContent?.invoke()
            }
        }
    }
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
