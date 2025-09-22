package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.card.iconRes
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.name
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FactorSourceLabelsView(
    factorSources: ImmutableList<FactorSource>,
    textStyle: TextStyle = RadixTheme.typography.body2Regular,
    color: Color = RadixTheme.colors.text,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
    ) {
        factorSources.forEach { factorSource ->
            FactorSourceLabelView(
                factorSource = factorSource,
                textStyle = textStyle,
                color = color
            )
        }
    }
}

@Composable
fun FactorSourceLabelView(
    factorSource: FactorSource,
    textStyle: TextStyle = RadixTheme.typography.body2Regular,
    color: Color = RadixTheme.colors.text,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
    ) {
        Text(
            text = factorSource.name,
            style = textStyle,
            color = color
        )

        Icon(
            modifier = Modifier.height(
                with(LocalDensity.current) {
                    textStyle.fontSize.toDp() * 1.2f
                }
            ),
            painter = painterResource(id = factorSource.kind.iconRes()),
            contentDescription = null,
            tint = color
        )
    }
}
