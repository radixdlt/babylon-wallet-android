package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.StatusMessageText
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.model.factors.StatusMessage
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun FactorSourceKindCardView(
    item: FactorSourceKindCard,
    modifier: Modifier = Modifier,
    endContent: (@Composable () -> Unit)? = null
) {
    CardContainer(
        modifier = modifier
    ) {
        SimpleFactorCardView(
            iconRes = item.kind.iconRes(),
            title = item.kind.title(),
            subtitle = item.kind.subtitle(),
            description = null,
            endContent = endContent
        )

        MessagesView(
            messages = item.messages
        )
    }
}

@Composable
fun FactorSourceCardView(
    item: FactorSourceCard,
    modifier: Modifier = Modifier,
    endContent: (@Composable () -> Unit)? = null
) {
    CardContainer(
        modifier = modifier
    ) {
        SimpleFactorCardView(
            iconRes = item.kind.iconRes(),
            title = item.name,
            subtitle = item.kind.subtitle().takeIf { item.includeDescription },
            description = item.lastUsedOn?.let {
                stringResource(id = R.string.factorSources_card_lastUsed, it)
                    .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold))
            },
            endContent = endContent
        )

        MessagesView(
            messages = item.messages
        )

        LinkedEntitiesView(
            accounts = item.accounts,
            personas = item.personas,
            hasHiddenEntities = item.hasHiddenEntities
        )
    }
}

/**
 * A composable that can be used either to present a factor source or a factor source kind.
 *
 */
@Composable
fun SimpleFactorCardView(
    @DrawableRes iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    description: AnnotatedString? = null,
    endContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.padding(
            vertical = RadixTheme.dimensions.paddingDefault
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    id = iconRes
                ),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

            Column {
                Text(
                    text = title,
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )

                subtitle?.let {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))

                    Text(
                        text = it,
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray2
                    )
                }

                description?.let {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))

                    Text(
                        text = it,
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray2
                    )
                }
            }
        }

        endContent?.invoke()
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
                color = RadixTheme.colors.white,
                shape = RadixTheme.shapes.roundedRectDefault
            )
    ) {
        content()
    }
}

@Composable
private fun MessagesView(
    messages: PersistentList<FactorSourceStatusMessage>
) {
    if (messages.isEmpty()) {
        return
    }

    Column(
        modifier = Modifier.padding(
            start = RadixTheme.dimensions.paddingXXLarge,
            end = RadixTheme.dimensions.paddingXXLarge,
            top = RadixTheme.dimensions.paddingXSmall,
            bottom = RadixTheme.dimensions.paddingDefault
        ),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall)
    ) {
        messages.forEach {
            StatusMessageText(
                message = it.getMessage()
            )
        }
    }
}

@Composable
private fun LinkedEntitiesView(
    accounts: PersistentList<Account>,
    personas: PersistentList<Persona>,
    hasHiddenEntities: Boolean
) {
    if (accounts.isEmpty() && personas.isEmpty()) {
        return
    }

    val accountsText = when {
        accounts.isEmpty() -> null
        accounts.size == 1 -> stringResource(id = R.string.factorSources_card_accountSingular)
        else -> stringResource(id = R.string.factorSources_card_accountPlural, accounts.size)
    }
    val personasText = when {
        personas.isEmpty() -> null
        personas.size == 1 -> stringResource(id = R.string.factorSources_card_personaSingular)
        else -> stringResource(id = R.string.factorSources_card_personaPlural, personas.size)
    }
    val linkedText = if (hasHiddenEntities) {
        when {
            accountsText != null && personasText != null -> stringResource(
                id = R.string.factorSources_card_linkedAccountsAndPersonasSomeHidden,
                accountsText,
                personasText
            )
            accountsText != null -> stringResource(id = R.string.factorSources_card_linkedAccountsOrPersonasSomeHidden, accountsText)
            personasText != null -> stringResource(id = R.string.factorSources_card_linkedAccountsOrPersonasSomeHidden, personasText)
            else -> ""
        }
    } else {
        when {
            accountsText != null && personasText != null -> stringResource(
                id = R.string.factorSources_card_linkedAccountsAndPersonas,
                accountsText,
                personasText
            )
            accountsText != null -> stringResource(id = R.string.factorSources_card_linkedAccountsOrPersonas, accountsText)
            personasText != null -> stringResource(id = R.string.factorSources_card_linkedAccountsOrPersonas, personasText)
            else -> ""
        }
    }

    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.gray5,
                shape = RadixTheme.shapes.roundedRectBottomDefault
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .throttleClickable { isExpanded = !isExpanded }
                .padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingMedium
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = linkedText,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )

            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(
                    id = if (isExpanded) {
                        com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_up
                    } else {
                        com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_down
                    }
                ),
                contentDescription = null,
                tint = RadixTheme.colors.gray2
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = RadixTheme.dimensions.paddingDefault,
                        end = RadixTheme.dimensions.paddingDefault,
                        top = RadixTheme.dimensions.paddingSmall,
                        bottom = RadixTheme.dimensions.paddingDefault
                    ),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                accounts.forEach {
                    SimpleAccountCard(
                        account = it,
                        shape = RadixTheme.shapes.roundedRectMedium
                    )
                }

                personas.forEach {
                    SimplePersonaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = RadixTheme.colors.white,
                                shape = RadixTheme.shapes.roundedRectMedium
                            ),
                        persona = it
                    )
                }

                if (hasHiddenEntities) {
                    HiddenEntityCard(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun FactorSourceKind.iconRes(): Int {
    return when (this) {
        FactorSourceKind.DEVICE -> com.babylon.wallet.android.designsystem.R.drawable.ic_factor_device
        FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> com.babylon.wallet.android.designsystem.R.drawable.ic_factor_ledger_hardware
        FactorSourceKind.OFF_DEVICE_MNEMONIC -> com.babylon.wallet.android.designsystem.R.drawable.ic_factor_passphrase
        FactorSourceKind.ARCULUS_CARD -> com.babylon.wallet.android.designsystem.R.drawable.ic_factor_arculus
        FactorSourceKind.PASSWORD -> com.babylon.wallet.android.designsystem.R.drawable.ic_factor_password
        FactorSourceKind.TRUSTED_CONTACT,
        FactorSourceKind.SECURITY_QUESTIONS -> error("Not supported yet")
    }
}

@Composable
fun FactorSourceKind.title(): String {
    return stringResource(
        id = when (this) {
            FactorSourceKind.DEVICE -> R.string.factorSources_card_deviceTitle
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> R.string.factorSources_card_ledgerTitle
            FactorSourceKind.OFF_DEVICE_MNEMONIC -> R.string.factorSources_card_offDeviceMnemonicTitle
            FactorSourceKind.ARCULUS_CARD -> R.string.factorSources_card_arculusCardTitle
            FactorSourceKind.PASSWORD -> R.string.factorSources_card_passwordTitle
            FactorSourceKind.TRUSTED_CONTACT,
            FactorSourceKind.SECURITY_QUESTIONS -> error("Not supported yet")
        }
    )
}

@Composable
fun FactorSourceKind.subtitle(): String {
    return stringResource(
        id = when (this) {
            FactorSourceKind.DEVICE -> R.string.factorSources_card_deviceDescription
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> R.string.factorSources_card_ledgerDescription
            FactorSourceKind.OFF_DEVICE_MNEMONIC -> R.string.factorSources_card_offDeviceMnemonicDescription
            FactorSourceKind.ARCULUS_CARD -> R.string.factorSources_card_arculusCardDescription
            FactorSourceKind.PASSWORD -> R.string.factorSources_card_passwordDescription
            FactorSourceKind.TRUSTED_CONTACT,
            FactorSourceKind.SECURITY_QUESTIONS -> error("Not supported yet")
        }
    )
}

@Composable
@Preview
@UsesSampleValues
private fun FactorSourceKindCardPreview(
    @PreviewParameter(FactorSourceKindCardPreviewProvider::class) item: FactorSourceKindCard
) {
    RadixWalletPreviewTheme {
        FactorSourceKindCardView(
            item = item
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun FactorSourceCardPreview(
    @PreviewParameter(FactorSourceCardPreviewProvider::class) item: FactorSourceCard
) {
    RadixWalletPreviewTheme {
        FactorSourceCardView(
            item = item
        )
    }
}

@UsesSampleValues
class FactorSourceKindCardPreviewProvider : PreviewParameterProvider<FactorSourceKindCard> {

    override val values: Sequence<FactorSourceKindCard>
        get() = sequenceOf(
            FactorSourceKindCard(
                kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                messages = persistentListOf()
            ),
            FactorSourceKindCard(
                kind = FactorSourceKind.ARCULUS_CARD,
                messages = persistentListOf()
            ),
            FactorSourceKindCard(
                kind = FactorSourceKind.PASSWORD,
                messages = persistentListOf()
            ),
            FactorSourceKindCard(
                kind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
                messages = persistentListOf(
                    FactorSourceStatusMessage.Dynamic(
                        message = StatusMessage(
                            message = "This seed phrase has been written down",
                            type = StatusMessage.Type.SUCCESS
                        )
                    )
                )
            )
        )
}

@UsesSampleValues
class FactorSourceCardPreviewProvider : PreviewParameterProvider<FactorSourceCard> {

    override val values: Sequence<FactorSourceCard>
        get() = sequenceOf(
            FactorSourceCard(
                id = FactorSourceId.Hash.init(
                    kind = FactorSourceKind.DEVICE,
                    mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                ),
                name = "My Phone",
                includeDescription = false,
                lastUsedOn = "Today",
                kind = FactorSourceKind.DEVICE,
                messages = persistentListOf(
                    FactorSourceStatusMessage.PassphraseHint,
                    FactorSourceStatusMessage.Dynamic(
                        message = StatusMessage(
                            message = "Warning text",
                            type = StatusMessage.Type.WARNING
                        )
                    )
                ),
                accounts = persistentListOf(
                    Account.sampleMainnet()
                ),
                personas = persistentListOf(
                    Persona.sampleMainnet(),
                    Persona.sampleStokenet()
                ),
                hasHiddenEntities = true
            ),
            FactorSourceCard(
                id = FactorSourceId.Hash.init(
                    kind = FactorSourceKind.DEVICE,
                    mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                ),
                name = "My Phone",
                includeDescription = false,
                lastUsedOn = "Today",
                kind = FactorSourceKind.DEVICE,
                messages = persistentListOf(
                    FactorSourceStatusMessage.PassphraseHint,
                    FactorSourceStatusMessage.Dynamic(
                        message = StatusMessage(
                            message = "Warning text",
                            type = StatusMessage.Type.WARNING
                        )
                    )
                ),
                accounts = persistentListOf(
                    Account.sampleMainnet()
                ),
                personas = persistentListOf(),
                hasHiddenEntities = true
            )
        )
}
