package com.babylon.wallet.android.presentation.ui.composables.securityfactors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.FactorSourceCategory
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.StatusMessageText
import com.babylon.wallet.android.presentation.ui.composables.card.iconRes
import com.babylon.wallet.android.presentation.ui.composables.card.subtitle
import com.babylon.wallet.android.presentation.ui.composables.card.title
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.model.factors.SecurityFactorTypeUiItem
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.radixdlt.sargon.FactorSourceKind
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SecurityFactorTypesListView(
    modifier: Modifier = Modifier,
    description: @Composable (() -> Unit)? = null,
    onInfoClick: ((GlossaryItem) -> Unit)? = null,
    items: PersistentList<SecurityFactorTypeUiItem>,
    onSecurityFactorTypeItemClick: (SecurityFactorTypeUiItem.Item) -> Unit
) {
    LazyColumn(
        modifier = modifier.background(color = RadixTheme.colors.gray5)
    ) {
        if (description != null) {
            item {
                description()
            }
        } else {
            item {
                HorizontalDivider(color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXXLarge))
            }
        }

        itemsIndexed(items) { index, item ->
            when (item) {
                is SecurityFactorTypeUiItem.Header -> HeaderView(
                    item = item
                )
                is SecurityFactorTypeUiItem.Item -> {
                    DefaultSettingsItem(
                        title = item.factorSourceKind.title(),
                        info = item.factorSourceKind.subtitle(),
                        onClick = { onSecurityFactorTypeItemClick(item) },
                        isEnabled = item.isEnabled,
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = item.factorSourceKind.iconRes()),
                                contentDescription = null,
                                tint = RadixTheme.colors.gray1
                            )
                        },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = RadixTheme.colors.gray1
                            )
                        },
                        warningView = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                            ) {
                                item.messages.forEach { message ->
                                    StatusMessageText(
                                        modifier = if (message == FactorSourceStatusMessage.CannotBeUsedHere) {
                                            Modifier.noIndicationClickable { onInfoClick?.invoke(GlossaryItem.buildingshield) }
                                        } else {
                                            Modifier
                                        },
                                        message = message.getMessage()
                                    )
                                }
                            }
                        }
                    )

                    val isLastItem = items.getOrNull(index + 1) !is SecurityFactorTypeUiItem.Item
                    if (!isLastItem) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                            color = RadixTheme.colors.gray4
                        )
                    } else {
                        HorizontalDivider(color = RadixTheme.colors.gray4)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault))
        }
    }
}

@Composable
private fun HeaderView(item: SecurityFactorTypeUiItem.Header) {
    item.category.title()?.let { title ->
        Text(
            text = title,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray2,
            modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
        )
    }
}

@Composable
private fun FactorSourceCategory.title(): String? {
    return when (this) {
        FactorSourceCategory.Hardware -> stringResource(id = R.string.securityFactors_hardware)
        FactorSourceCategory.Information -> stringResource(id = R.string.securityFactors_information)
        FactorSourceCategory.Identity,
        FactorSourceCategory.Contact,
        FactorSourceCategory.Custodian -> null
    }
}

@Preview(showBackground = true)
@Composable
private fun SecurityFactorTypesListPreview(
    @PreviewParameter(SecurityFactorTypesListPreviewProvider::class) items: PersistentList<SecurityFactorTypeUiItem>
) {
    RadixWalletTheme {
        SecurityFactorTypesListView(
            items = items,
            onSecurityFactorTypeItemClick = {}
        )
    }
}

class SecurityFactorTypesListPreviewProvider : PreviewParameterProvider<PersistentList<SecurityFactorTypeUiItem>> {

    val value = values.first()

    override val values: Sequence<PersistentList<SecurityFactorTypeUiItem>>
        get() = sequenceOf(
            persistentListOf(
                SecurityFactorTypeUiItem.Item(
                    factorSourceKind = FactorSourceKind.DEVICE,
                    messages = persistentListOf(FactorSourceStatusMessage.SecurityPrompt.EntitiesNotRecoverable)
                ),
                SecurityFactorTypeUiItem.Header(
                    category = FactorSourceCategory.Hardware
                ),
                SecurityFactorTypeUiItem.Item(
                    factorSourceKind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    messages = persistentListOf()
                ),
                SecurityFactorTypeUiItem.Item(
                    factorSourceKind = FactorSourceKind.ARCULUS_CARD,
                    messages = persistentListOf()
                ),
                SecurityFactorTypeUiItem.Header(
                    category = FactorSourceCategory.Information
                ),
                SecurityFactorTypeUiItem.Item(
                    factorSourceKind = FactorSourceKind.PASSWORD,
                    messages = persistentListOf(FactorSourceStatusMessage.CannotBeUsedHere),
                    isEnabled = false
                ),
                SecurityFactorTypeUiItem.Item(
                    factorSourceKind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
                    messages = persistentListOf()
                )
            )
        )
}
