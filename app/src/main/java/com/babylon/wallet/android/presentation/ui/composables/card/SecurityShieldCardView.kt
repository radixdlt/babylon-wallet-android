package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.StatusMessageText
import com.babylon.wallet.android.presentation.ui.composables.shared.CardContainer
import com.babylon.wallet.android.presentation.ui.model.securityshields.SecurityShieldCard
import com.babylon.wallet.android.presentation.ui.model.securityshields.SecurityShieldStatusMessage
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.SecurityStructureFlag
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.SecurityStructureMetadata
import com.radixdlt.sargon.ShieldForDisplay
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SecurityShieldCardView(
    modifier: Modifier = Modifier,
    item: SecurityShieldCard
) {
    CardContainer {
        Row(
            modifier = modifier
                .padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingLarge
                )
                // keep the same height of the card if status message is not present
                .padding(vertical = if (item.messages.isEmpty()) RadixTheme.dimensions.paddingMedium else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(80.dp),
                painter = painterResource(id = DSR.ic_security_shields),
                contentDescription = null,
                tint = Color.Unspecified
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    text = item.shieldForDisplay.metadata.displayName.value,
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))

                Text(
                    text = stringResource(R.string.securityShields_assigned_title),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray2
                )

                Text(
                    text = linkedEntitiesView(
                        accounts = item.shieldForDisplay.numberOfLinkedAccounts.toInt(),
                        personas = item.shieldForDisplay.numberOfLinkedPersonas.toInt()
                    ),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))

                if (item.messages.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall)) {
                        item.messages.forEach {
                            StatusMessageText(
                                message = it.getMessage()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun linkedEntitiesView(
    accounts: Int,
    personas: Int
): String {
    val accountsText = when (accounts) {
        0 -> stringResource(id = R.string.empty)
        1 -> stringResource(id = R.string.securityShields_assigned_accountSingular)
        else -> stringResource(id = R.string.securityShields_assigned_accountPlural, accounts)
    }
    val personasText = when (personas) {
        0 -> stringResource(id = R.string.empty)
        1 -> stringResource(id = R.string.securityShields_assigned_personaSingular)
        else -> stringResource(id = R.string.securityShields_assigned_personaPlural, personas)
    }
    val linkedText = if (accounts == 0 && personas == 0) {
        stringResource(R.string.common_none)
    } else {
        when {
            accounts != 0 && personas != 0 -> {
                accountsText + " " + stringResource(id = R.string.dot_separator) + " " + personasText
            }
            accounts != 0 -> accountsText
            else -> personasText
        }
    }
    return linkedText
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
val mainShieldForDisplaySample = SecurityShieldCard(
    ShieldForDisplay(
        metadata = SecurityStructureMetadata(
            id = SecurityStructureId.randomUUID(),
            displayName = DisplayName("Panathinaikos"),
            createdOn = Timestamp.now(),
            lastUpdatedOn = Timestamp.now(),
            flags = listOf(SecurityStructureFlag.MAIN)
        ),
        numberOfLinkedAccounts = 2.toUInt(),
        numberOfLinkedHiddenAccounts = 3.toUInt(),
        numberOfLinkedPersonas = 1.toUInt(),
        numberOfLinkedHiddenPersonas = 0.toUInt()
    ),
    messages = persistentListOf(SecurityShieldStatusMessage.AppliedAndWorking)
)

@UsesSampleValues
val otherShieldsForDisplaySample = persistentListOf(
    SecurityShieldCard(
        shieldForDisplay = ShieldForDisplay(
            metadata = SecurityStructureMetadata(
                id = SecurityStructureId.randomUUID(),
                displayName = DisplayName("cool shield"),
                createdOn = Timestamp.now(),
                lastUpdatedOn = Timestamp.now(),
                flags = emptyList()
            ),
            numberOfLinkedAccounts = 21.toUInt(),
            numberOfLinkedHiddenAccounts = 0.toUInt(),
            numberOfLinkedPersonas = 1.toUInt(),
            numberOfLinkedHiddenPersonas = 0.toUInt()
        ),
        messages = persistentListOf(SecurityShieldStatusMessage.AppliedAndWorking)
    ),
    SecurityShieldCard(
        ShieldForDisplay(
            metadata = SecurityStructureMetadata(
                id = SecurityStructureId.randomUUID(),
                displayName = DisplayName.sample.other(),
                createdOn = Timestamp.now(),
                lastUpdatedOn = Timestamp.now(),
                flags = emptyList()
            ),
            numberOfLinkedAccounts = 0.toUInt(),
            numberOfLinkedHiddenAccounts = 0.toUInt(),
            numberOfLinkedPersonas = 1.toUInt(),
            numberOfLinkedHiddenPersonas = 0.toUInt()
        ),
        messages = persistentListOf(SecurityShieldStatusMessage.ActionRequired)
    ),
    SecurityShieldCard(
        ShieldForDisplay(
            metadata = SecurityStructureMetadata(
                id = SecurityStructureId.randomUUID(),
                displayName = DisplayName("666"),
                createdOn = Timestamp.now(),
                lastUpdatedOn = Timestamp.now(),
                flags = emptyList()
            ),
            numberOfLinkedAccounts = 0.toUInt(),
            numberOfLinkedHiddenAccounts = 0.toUInt(),
            numberOfLinkedPersonas = 0.toUInt(),
            numberOfLinkedHiddenPersonas = 0.toUInt()
        ),
        messages = persistentListOf()
    ),
    SecurityShieldCard(
        ShieldForDisplay(
            metadata = SecurityStructureMetadata(
                id = SecurityStructureId.randomUUID(),
                displayName = DisplayName.sample(),
                createdOn = Timestamp.now(),
                lastUpdatedOn = Timestamp.now(),
                flags = emptyList()
            ),
            numberOfLinkedAccounts = 23.toUInt(),
            numberOfLinkedHiddenAccounts = 32.toUInt(),
            numberOfLinkedPersonas = 11.toUInt(),
            numberOfLinkedHiddenPersonas = 10.toUInt()
        ),
        messages = persistentListOf(SecurityShieldStatusMessage.AppliedAndWorking)
    )
)

@UsesSampleValues
class SecurityShieldCardPreviewProvider : PreviewParameterProvider<SecurityShieldCard> {

    override val values: Sequence<SecurityShieldCard>
        get() = otherShieldsForDisplaySample.asSequence()
}
