package com.babylon.wallet.android.presentation.settings.securitycenter.mfafactorinstance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.composables.card.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.id

@Composable
fun MfaFactorInstanceScreen(
    viewModel: MfaFactorInstanceViewModel,
    toAddressDetails: (ActionableAddress) -> Unit,
    toFactorSourceDetails: (FactorSourceId) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is MfaFactorInstanceViewModel.Event.ShowAddressDetails -> {
                    toAddressDetails(event.actionableAddress)
                }

                is MfaFactorInstanceViewModel.Event.ShowFactorSourceDetails -> {
                    toFactorSourceDetails(event.factorSourceId)
                }
            }
        }
    }

    MfaFactorInstanceContent(
        state = state,
        onGetNewInstanceClick = viewModel::onGetNewInstanceClick,
        onFactorSourceClick = viewModel::onFactorSourceClick,
        onMessageShown = viewModel::onMessageShown,
        onBackClick = onBackClick
    )
}

@Composable
private fun MfaFactorInstanceContent(
    state: MfaFactorInstanceViewModel.State,
    onGetNewInstanceClick: () -> Unit,
    onFactorSourceClick: (FactorSourceId) -> Unit,
    onMessageShown: () -> Unit,
    onBackClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackbarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.factorSources_detail_mfaSignatureResourceTitle),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner,
                containerColor = RadixTheme.colors.backgroundSecondary,
                actions = {
                    IconButton(onClick = onGetNewInstanceClick) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(id = R.string.factorSources_detail_mfaGetNewInstance),
                            tint = RadixTheme.colors.icon
                        )
                    }
                }
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary,
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackbarHostState
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(RadixTheme.dimensions.paddingDefault)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                text = stringResource(id = R.string.factorSources_detail_mfaCurrentUsageTitle),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.text
            )

            when {
                state.isLoadingCurrentUsage -> LoadingState()
                state.activeUsages.isEmpty() -> EmptyState()

                else -> Column(
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                ) {
                    state.activeUsages.forEachIndexed { usageIndex, usage ->
                        ActiveUsageCard(
                            usage = usage,
                            usageIndex = usageIndex,
                            onFactorSourceClick = onFactorSourceClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .mfaResourceSurface()
            .padding(RadixTheme.dimensions.paddingDefault)
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .mfaResourceSurface()
            .padding(RadixTheme.dimensions.paddingLarge)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.factorSources_detail_mfaNoActiveResources),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.textSecondary
        )
    }
}

@Composable
private fun ActiveUsageCard(
    usage: MfaFactorInstanceViewModel.State.ActiveUsage,
    usageIndex: Int,
    onFactorSourceClick: (FactorSourceId) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .mfaResourceSurface()
            .padding(RadixTheme.dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        SignatureResourceCard(
            usage = usage,
            usageIndex = usageIndex
        )

        if (usage.accounts.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.factorSources_detail_mfaUsedByTitle),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.textSecondary
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                usage.accounts.forEach { account ->
                    account.profileAccount?.let { profileAccount ->
                        SimpleAccountCard(
                            modifier = Modifier.fillMaxWidth(),
                            account = profileAccount,
                            shape = RadixTheme.shapes.roundedRectSmall
                        )
                    } ?: UsedByFallbackAccountRow(account = account)
                }
            }
        }

        usage.factorSource?.let { factorSource ->
            Text(
                text = stringResource(id = R.string.factorSources_detail_mfaCreatedWithTitle),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.textSecondary
            )

            FactorSourceCardView(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFactorSourceClick(factorSource.id) },
                item = factorSource.toFactorSourceCard(
                    includeDescription = false,
                    includeLastUsedOn = false
                ),
                castsShadow = false,
                containerColor = RadixTheme.colors.cardSecondary
            )
        }
    }
}

@Composable
private fun Modifier.mfaResourceSurface(): Modifier {
    val shape = RadixTheme.shapes.roundedRectMedium

    return this
        .defaultCardShadow(shape = shape)
        .clip(shape)
        .background(color = RadixTheme.colors.card, shape = shape)
}

@Composable
private fun SignatureResourceCard(
    usage: MfaFactorInstanceViewModel.State.ActiveUsage,
    usageIndex: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.cardSecondary,
                shape = RadixTheme.shapes.roundedRectSmall
            )
            .padding(RadixTheme.dimensions.paddingDefault),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_nfts),
            contentDescription = null,
            tint = Color.Unspecified
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
        ) {
            Text(
                text = stringResource(
                    id = R.string.factorSources_detail_mfaCurrentResourceLabel,
                    usageIndex + 1
                ),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.text
            )
            ActionableAddressView(
                globalId = usage.signatureResource,
                showOnlyLocalId = false,
                textStyle = RadixTheme.typography.body2Regular,
                textColor = RadixTheme.colors.textSecondary,
                iconColor = RadixTheme.colors.iconSecondary
            )
        }
    }
}

@Composable
private fun UsedByFallbackAccountRow(
    account: MfaFactorInstanceViewModel.State.UsedByAccount
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.cardSecondary,
                shape = RadixTheme.shapes.roundedRectSmall
            )
            .padding(RadixTheme.dimensions.paddingDefault),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = account.addressBookName ?: stringResource(id = R.string.common_account),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )
        ActionableAddressView(
            address = Address.Account(account.address),
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = RadixTheme.colors.textSecondary,
            iconColor = RadixTheme.colors.iconSecondary
        )
    }
}
