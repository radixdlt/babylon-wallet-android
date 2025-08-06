package com.babylon.wallet.android.presentation.ui.composables.securityfactors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.card.subtitle
import com.babylon.wallet.android.presentation.ui.composables.card.title
import com.radixdlt.sargon.FactorSourceKind

@Composable
fun FactorSourceCategoryHeaderView(
    kind: FactorSourceKind,
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = kind.title(),
            style = RadixTheme.typography.body1Link,
            color = RadixTheme.colors.textSecondary
        )

        Text(
            text = kind.subtitle(),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.textSecondary
        )

        message?.let {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))

            Text(
                text = it,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.warning
            )
        }
    }
}
