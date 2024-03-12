@file:OptIn(ExperimentalLayoutApi::class)

package com.babylon.wallet.android.presentation.account.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.domain.model.TransactionClass
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.account.history.State
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.utils.truncatedHash

@Composable
fun FiltersDialog(
    modifier: Modifier = Modifier,
    state: State,
    onDismiss: () -> Unit,
    onClearAllFilters: () -> Unit,
    onTransactionTypeFilterSelected: (HistoryFilters.TransactionType?) -> Unit,
    onTransactionClassFilterSelected: (TransactionClass?) -> Unit,
    onResourceFilterSelected: (Resource) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.transactionHistory_filters_title),
                onBackClick = onDismiss,
                backIconType = BackIconType.Close,
                containerColor = RadixTheme.colors.defaultBackground,
                actions = {
                    RadixTextButton(text = stringResource(id = R.string.transactionHistory_filters_clearAll), onClick = onClearAllFilters)
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.background(color = RadixTheme.colors.defaultBackground)
            ) {
                HorizontalDivider(color = RadixTheme.colors.gray5)

                RadixPrimaryButton(
                    text = stringResource(id = R.string.transactionHistory_filters_showResultsButton),
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(RadixTheme.dimensions.paddingDefault)
                        .fillMaxWidth()
                )
            }
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = RadixTheme.dimensions.paddingLarge,
                        end = RadixTheme.dimensions.paddingLarge,
                        bottom = RadixTheme.dimensions.paddingDefault
                    ),
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                TagContainer {
                    HistoryFilters.TransactionType.entries.forEach { entry ->
                        val selected = state.filters.transactionType == entry
                        HistoryFilterTag(selected = selected, text = entry.label(), leadingIcon = {
                            Icon(painter = painterResource(id = entry.icon()), contentDescription = null, tint = Color.Unspecified)
                        }, onClick = {
                            onTransactionTypeFilterSelected(entry)
                        }, onCloseClick = {
                            onTransactionTypeFilterSelected(null)
                        })
                    }
                }
            }
            HorizontalDivider(color = RadixTheme.colors.gray4, modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge))
            FilterTypeSection(label = stringResource(id = R.string.transactionHistory_filters_assetTypeLabel)) {
                ResourcesSection(state, onResourceFilterSelected)
            }
            HorizontalDivider(color = RadixTheme.colors.gray4, modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge))
            FilterTypeSection(label = stringResource(id = R.string.transactionHistory_filters_transactionTypeLabel)) {
                TagContainer {
                    TransactionClass.entries.forEach { entry ->
                        val selected = state.filters.transactionClass == entry
                        HistoryFilterTag(selected = selected, text = entry.description(), onClick = {
                            onTransactionClassFilterSelected(entry)
                        }, onCloseClick = {
                            onTransactionClassFilterSelected(null)
                        })
                    }
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            }
            HorizontalDivider(color = RadixTheme.colors.gray4, modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge))
        }
    }
}

@Composable
private fun ResourcesSection(
    state: State,
    onResourceFilterSelected: (Resource) -> Unit,
) {
    val maxFungiblesInCollapsedState = 12
    val maxNonFungiblesInCollapsedState = 6
    val fungibles = remember(state.fungibleResources) {
        state.fungibleResources
    }
    val nonFungibles = remember(state.nonFungibleResources) {
        state.nonFungibleResources
    }
    val showMoreFungiblesButton by remember {
        derivedStateOf {
            fungibles.size > maxFungiblesInCollapsedState
        }
    }
    val showMoreNonFungiblesButton by remember {
        derivedStateOf {
            nonFungibles.size > maxNonFungiblesInCollapsedState
        }
    }
    var showingAllFungibles by remember { mutableStateOf(fungibles.size < maxFungiblesInCollapsedState) }
    var showingAllNonFungibleResource by remember { mutableStateOf(nonFungibles.size < maxNonFungiblesInCollapsedState) }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (fungibles.isNotEmpty()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.transactionHistory_filters_tokensLabel),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2,
                textAlign = TextAlign.Start
            )
            TagContainer(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
                    .animateContentSize()
            ) {
                fungibles.take(
                    if (showingAllFungibles) {
                        fungibles.size
                    } else {
                        maxFungiblesInCollapsedState
                    }
                ).forEach { fungible ->
                    val selected = state.filters.resource?.resourceAddress == fungible.resourceAddress
                    HistoryFilterTag(
                        selected = selected,
                        text = fungible.displayTitle.ifEmpty { fungible.resourceAddress.truncatedHash() },
                        onClick = {
                            if (selected.not()) {
                                onResourceFilterSelected(fungible)
                            }
                        },
                        onCloseClick = {
                            onResourceFilterSelected(fungible)
                        }
                    )
                }
            }
            if (showMoreFungiblesButton) {
                RadixTextButton(
                    text = if (showingAllFungibles) {
                        "- " + stringResource(id = R.string.transactionHistory_filters_tokenShowLess)
                    } else {
                        "+ " + stringResource(id = R.string.transactionHistory_filters_tokenShowAll)
                    },
                    onClick = {
                        showingAllFungibles = !showingAllFungibles
                    }
                )
            }
        }
        if (nonFungibles.isNotEmpty()) {
            HorizontalDivider(color = RadixTheme.colors.gray4)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.transactionHistory_filters_assetTypeNFTsLabel),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2,
                textAlign = TextAlign.Start
            )
            TagContainer(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
                    .animateContentSize()
            ) {
                nonFungibles.take(
                    if (showingAllNonFungibleResource) {
                        nonFungibles.size
                    } else {
                        maxNonFungiblesInCollapsedState
                    }
                ).forEach { nonFungible ->
                    val selected = state.filters.resource?.resourceAddress == nonFungible.resourceAddress
                    HistoryFilterTag(
                        selected = selected,
                        text = nonFungible.name.ifEmpty { nonFungible.resourceAddress.truncatedHash() },
                        onClick = {
                            if (selected.not()) {
                                onResourceFilterSelected(nonFungible)
                            }
                        },
                        onCloseClick = {
                            onResourceFilterSelected(nonFungible)
                        }
                    )
                }
            }
            if (showMoreNonFungiblesButton) {
                RadixTextButton(
                    text = if (showingAllNonFungibleResource) {
                        "- " + stringResource(id = R.string.transactionHistory_filters_nftShowLess)
                    } else {
                        "+ " + stringResource(id = R.string.transactionHistory_filters_nftShowAll)
                    },
                    onClick = {
                        showingAllNonFungibleResource = !showingAllNonFungibleResource
                    }
                )
            }
        }
    }
}

@Composable
private fun TagContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = RadixTheme.dimensions.paddingSmall),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),

        content = {
            content()
        }
    )
}

@Composable
private fun FilterTypeSection(modifier: Modifier = Modifier, label: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = RadixTheme.dimensions.paddingDefault, horizontal = RadixTheme.dimensions.paddingLarge)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1,
                overflow = TextOverflow.Ellipsis,
            )
            val iconRes = if (expanded) {
                com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_up
            } else {
                com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_down
            }
            Icon(
                painter = painterResource(id = iconRes),
                tint = RadixTheme.colors.gray1,
                contentDescription = "arrow"
            )
        }
        AnimatedVisibility(
            modifier = Modifier.padding(
                horizontal = RadixTheme.dimensions.paddingLarge
            ),
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun HistoryFilters.TransactionType.label(): String {
    return when (this) {
        HistoryFilters.TransactionType.DEPOSIT -> stringResource(id = R.string.transactionHistory_filters_depositsType)
        HistoryFilters.TransactionType.WITHDRAWAL -> stringResource(id = R.string.transactionHistory_filters_withdrawalsType)
    }
}

@Composable
fun HistoryFilters.TransactionType.icon(): Int {
    return when (this) {
        HistoryFilters.TransactionType.DEPOSIT -> DSR.ic_filter_deposit
        HistoryFilters.TransactionType.WITHDRAWAL -> DSR.ic_filter_withdraw
    }
}
