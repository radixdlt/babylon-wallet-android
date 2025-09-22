package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.FactorSourceLabelsView
import com.babylon.wallet.android.presentation.ui.composables.shared.CardContainer
import com.babylon.wallet.android.presentation.ui.model.securityshields.SecurityShieldCard
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun SecurityShieldCardView(
    modifier: Modifier = Modifier,
    item: SecurityShieldCard,
    endContent: (@Composable () -> Unit)? = null
) {
    CardContainer {
        Row(
            modifier = modifier
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(80.dp),
                painter = painterResource(id = DSR.ic_shield_not_applied),
                contentDescription = null,
                tint = Color.Unspecified
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    text = item.name.value,
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.text
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))

                FactorSourceLabelsView(
                    factorSources = item.factorSources
                )
            }

            endContent?.invoke()
        }
    }
}

@UsesSampleValues
@Composable
@Preview
private fun SecurityShieldCardPreview(
    @PreviewParameter(SecurityShieldCardPreviewProvider::class) item: SecurityShieldCard
) {
    RadixWalletPreviewTheme {
        SecurityShieldCardView(
            item = item
        )
    }
}

@UsesSampleValues
val shieldsForDisplaySample = persistentListOf(
    SecurityShieldCard(
        id = SecurityStructureId.randomUUID(),
        name = DisplayName("My Shield"),
        factorSources = FactorSource.sample.all.toPersistentList()
    ),
    SecurityShieldCard(
        id = SecurityStructureId.randomUUID(),
        name = DisplayName("My Shield 3"),
        factorSources = persistentListOf(FactorSource.sample())
    ),
    SecurityShieldCard(
        id = SecurityStructureId.randomUUID(),
        name = DisplayName("My Shield 4"),
        factorSources = persistentListOf(FactorSource.sample.other())
    ),
    SecurityShieldCard(
        id = SecurityStructureId.randomUUID(),
        name = DisplayName("My Shield 5"),
        factorSources = persistentListOf()
    )
)

@UsesSampleValues
class SecurityShieldCardPreviewProvider : PreviewParameterProvider<SecurityShieldCard> {

    override val values: Sequence<SecurityShieldCard>
        get() = shieldsForDisplaySample.asSequence()
}
