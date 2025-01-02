package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.radixdlt.sargon.FactorSourceId
import kotlinx.collections.immutable.PersistentList

@Composable
fun FactorSourcesList(
    modifier: Modifier = Modifier,
    mainFactorSource: FactorSourceCard?,
    factorSources: PersistentList<FactorSourceCard>,
    @StringRes factorSourceDescriptionText: Int,
    @StringRes addFactorSourceButtonTitle: Int,
    onFactorSourceClick: (FactorSourceId) -> Unit,
    onAddFactorSourceClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        item {
            Text(
                modifier = Modifier.padding(
                    top = RadixTheme.dimensions.paddingDefault,
                    bottom = RadixTheme.dimensions.paddingLarge
                ),
                text = stringResource(id = factorSourceDescriptionText),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
        }
        mainFactorSource?.let {
            item {
                Text(
                    modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.factorSources_list_default),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
                FactorSourceCardView(
                    modifier = Modifier
                        .padding(bottom = RadixTheme.dimensions.paddingMedium)
                        .clickable { onFactorSourceClick(it.id) },
                    item = it
                )
            }
        }

        if (factorSources.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(id = R.string.factorSources_list_others),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
            }
        }

        items(factorSources) {
            FactorSourceCardView(
                modifier = Modifier.clickable { onFactorSourceClick(it.id) },
                item = it
            )
        }

        item {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            RadixSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(align = Alignment.CenterHorizontally),
                text = stringResource(id = addFactorSourceButtonTitle),
                onClick = onAddFactorSourceClick,
                throttleClicks = true
            )
            InfoButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(align = Alignment.CenterHorizontally)
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingLarge
                    ),
                text = "Learn about biometrics/PIN", // TODO
                onClick = {
                    onInfoClick(GlossaryItem.biometricsPIN)
                }
            )
        }
    }
}
