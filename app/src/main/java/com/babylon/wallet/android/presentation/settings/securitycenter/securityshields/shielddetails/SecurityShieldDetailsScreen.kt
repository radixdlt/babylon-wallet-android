@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shielddetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.securityshields.ConfirmationDelay
import com.babylon.wallet.android.presentation.common.securityshields.OrView
import com.babylon.wallet.android.presentation.common.securityshields.display
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RenameBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.card.CollapsibleCommonCard
import com.babylon.wallet.android.presentation.ui.composables.card.CommonCard
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.ConfirmationRoleWithFactorSources
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.PrimaryRoleWithFactorSources
import com.radixdlt.sargon.RecoveryRoleWithFactorSources
import com.radixdlt.sargon.TimePeriod
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSample
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSampleOther

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityShieldDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityShieldDetailsViewModel,
    onBackClick: () -> Unit,
    onEditShield: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SyncSheetState(
        sheetState = bottomSheetState,
        isSheetVisible = state.isRenameBottomSheetVisible,
        onSheetClosed = viewModel::onRenameSecurityShieldDismissed
    )

    if (state.isRenameBottomSheetVisible) {
        RenameBottomSheet(
            sheetState = bottomSheetState,
            renameInput = state.renameSecurityShieldInput,
            titleRes = R.string.renameLabel_securityShield_title,
            subtitleRes = R.string.renameLabel_securityShield_subtitle,
            errorValidationMessageRes = R.string.renameLabel_securityShield_empty,
            errorTooLongNameMessageRes = R.string.renameLabel_securityShield_tooLong,
            onNameChange = viewModel::onRenameSecurityShieldChanged,
            onUpdateNameClick = viewModel::onRenameSecurityShieldUpdateClick,
            onDismiss = viewModel::onRenameSecurityShieldDismissed,
        )
    }

    SecurityShieldDetailsContent(
        modifier = modifier,
        state = state,
        onRenameSecurityShieldClick = viewModel::onRenameSecurityShieldClick,
        onEditFactorsClick = viewModel::onEditFactorsClick,
        onBackClick = onBackClick,
        toggleRegularAccessCard = viewModel::toggleRegularAccessCard,
        toggleLogInCard = viewModel::toggleLogInCard,
        toggleRecoveryCard = viewModel::toggleRecoveryCard
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                SecurityShieldDetailsViewModel.Event.EditShield -> onEditShield()
            }
        }
    }
}

@Composable
private fun SecurityShieldDetailsContent(
    modifier: Modifier = Modifier,
    state: SecurityShieldDetailsViewModel.State,
    onRenameSecurityShieldClick: () -> Unit,
    onEditFactorsClick: () -> Unit,
    onBackClick: () -> Unit,
    toggleRegularAccessCard: () -> Unit,
    toggleLogInCard: () -> Unit,
    toggleRecoveryCard: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner,
                contentColor = RadixTheme.colors.text,
                containerColor = RadixTheme.colors.backgroundSecondary
            )
        },
        bottomBar = {
            RadixBottomBar(
                button = {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(R.string.securityShields_editFactors),
                        onClick = onEditFactorsClick
                    )
                }
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    text = state.securityShieldName,
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.text
                )

                RadixTextButton(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    text = stringResource(R.string.renameLabel_securityShield_title),
                    isWithoutPadding = true,
                    onClick = onRenameSecurityShieldClick
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                state.securityStructureOfFactorSources?.let {
                    RegularAccessCollapsibleCard(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                        isCollapsed = state.isRegularAccessCardCollapsed,
                        primaryRoleWithFactorSources = state.securityStructureOfFactorSources.matrixOfFactors.primaryRole,
                        onToggleCollapse = { toggleRegularAccessCard() }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    LogInAndProveOwnershipCollapsibleCard(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                        isCollapsed = state.isLogInCardCollapsed,
                        authenticationSigningFactor = state.securityStructureOfFactorSources.authenticationSigningFactor,
                        onToggleCollapse = { toggleLogInCard() }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    RecoveryCollapsibleCard(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                        isCollapsed = state.isRecoveryCardCollapsed,
                        recoveryRoleWithFactorSources = state.securityStructureOfFactorSources.matrixOfFactors.recoveryRole,
                        confirmationRoleWithFactorSources = state.securityStructureOfFactorSources.matrixOfFactors.confirmationRole,
                        confirmationDelay = state.securityStructureOfFactorSources.matrixOfFactors.timeUntilDelayedConfirmationIsCallable,
                        onToggleCollapse = { toggleRecoveryCard() }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                }
            }
        }
    }
}

@Composable
private fun RegularAccessCollapsibleCard(
    modifier: Modifier = Modifier,
    isCollapsed: Boolean,
    primaryRoleWithFactorSources: PrimaryRoleWithFactorSources,
    onToggleCollapse: () -> Unit
) {
    Column(modifier = modifier) {
        CollapsibleCommonCard(
            isCollapsed = isCollapsed,
            collapsedItems = 1
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleCollapse() }
                    .padding(RadixTheme.dimensions.paddingLarge),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.transactionReview_updateShield_regularAccessTitle),
                        style = RadixTheme.typography.secondaryHeader,
                        color = RadixTheme.colors.text
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                    Text(
                        text = stringResource(R.string.transactionReview_updateShield_regularAccessMessage),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.textSecondary
                    )
                }
            }
        }

        AnimatedVisibility(visible = isCollapsed.not()) {
            CommonCard(
                roundTopCorners = false,
                roundBottomCorners = true
            ) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    colors = CardDefaults.cardColors(containerColor = RadixTheme.colors.backgroundSecondary),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = RadixTheme.dimensions.paddingSemiLarge)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.transactionReview_updateShield_primaryThersholdMessage,
                                primaryRoleWithFactorSources.threshold.display()
                            ).formattedSpans(
                                SpanStyle(fontWeight = FontWeight.Bold)
                            ),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.text
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                        primaryRoleWithFactorSources.thresholdFactors.forEachIndexed { index, factorSource ->
                            FactorSourceCardView(
                                item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                                castsShadow = false,
                                isOutlined = true
                            )

                            if (index != primaryRoleWithFactorSources.thresholdFactors.lastIndex) {
                                OrView()
                            }
                        }
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                        if (primaryRoleWithFactorSources.overrideFactors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                            Text(
                                text = stringResource(
                                    R.string.transactionReview_updateShield_primaryOverrideMessage,
                                ).formattedSpans(
                                    SpanStyle(fontWeight = FontWeight.Bold)
                                ),
                                style = RadixTheme.typography.body1Regular,
                                color = RadixTheme.colors.text
                            )

                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                            primaryRoleWithFactorSources.overrideFactors.forEachIndexed { index, factorSource ->
                                FactorSourceCardView(
                                    item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                                    castsShadow = false,
                                    isOutlined = true
                                )

                                if (index != primaryRoleWithFactorSources.overrideFactors.lastIndex) {
                                    OrView()
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            }
        }
    }
}

@Composable
private fun LogInAndProveOwnershipCollapsibleCard(
    modifier: Modifier = Modifier,
    isCollapsed: Boolean,
    authenticationSigningFactor: FactorSource,
    onToggleCollapse: () -> Unit
) {
    Column(modifier = modifier) {
        CollapsibleCommonCard(
            isCollapsed = isCollapsed,
            collapsedItems = 1
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleCollapse() }
                    .padding(RadixTheme.dimensions.paddingLarge),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.transactionReview_updateShield_authSigningTitle),
                        style = RadixTheme.typography.secondaryHeader,
                        color = RadixTheme.colors.text
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                    Text(
                        text = stringResource(R.string.transactionReview_updateShield_authSigningMessage),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.textSecondary
                    )
                }
            }
        }

        AnimatedVisibility(visible = isCollapsed.not()) {
            CommonCard(
                roundTopCorners = false,
                roundBottomCorners = true
            ) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    colors = CardDefaults.cardColors(containerColor = RadixTheme.colors.backgroundSecondary),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = RadixTheme.dimensions.paddingSemiLarge)
                    ) {
                        Text(
                            text = stringResource(R.string.transactionReview_updateShield_authSigningThreshold),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.text
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                        FactorSourceCardView(
                            item = authenticationSigningFactor.toFactorSourceCard(includeLastUsedOn = false),
                            castsShadow = false,
                            isOutlined = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            }
        }
    }
}

@Composable
private fun RecoveryCollapsibleCard(
    modifier: Modifier = Modifier,
    isCollapsed: Boolean,
    recoveryRoleWithFactorSources: RecoveryRoleWithFactorSources,
    confirmationRoleWithFactorSources: ConfirmationRoleWithFactorSources,
    confirmationDelay: TimePeriod,
    onToggleCollapse: () -> Unit
) {
    Column(modifier = modifier) {
        CollapsibleCommonCard(
            isCollapsed = isCollapsed,
            collapsedItems = 1
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleCollapse() }
                    .padding(RadixTheme.dimensions.paddingLarge),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.transactionReview_updateShield_startConfirmTitle),
                        style = RadixTheme.typography.secondaryHeader,
                        color = RadixTheme.colors.text
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                    Text(
                        text = stringResource(R.string.transactionReview_updateShield_startConfirmMessage),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.textSecondary
                    )
                }
            }
        }

        AnimatedVisibility(visible = isCollapsed.not()) {
            CommonCard(
                roundTopCorners = false,
                roundBottomCorners = true
            ) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    text = stringResource(R.string.transactionReview_updateShield_startRecoveryTitle),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.text
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    colors = CardDefaults.cardColors(containerColor = RadixTheme.colors.backgroundSecondary),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = RadixTheme.dimensions.paddingSemiLarge)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.transactionReview_updateShield_nonPrimaryOverrideMessage
                            ).formattedSpans(
                                SpanStyle(fontWeight = FontWeight.Bold)
                            ),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.text
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                        recoveryRoleWithFactorSources.overrideFactors.forEachIndexed { index, factorSource ->
                            FactorSourceCardView(
                                item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                                castsShadow = false,
                                isOutlined = true
                            )

                            if (index != recoveryRoleWithFactorSources.overrideFactors.lastIndex) {
                                OrView()
                            }
                        }
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    }
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    text = stringResource(R.string.transactionReview_updateShield_confirmRecoveryTitle),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.text
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    colors = CardDefaults.cardColors(containerColor = RadixTheme.colors.backgroundSecondary),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = RadixTheme.dimensions.paddingSemiLarge)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.transactionReview_updateShield_nonPrimaryOverrideMessage
                            ).formattedSpans(
                                SpanStyle(fontWeight = FontWeight.Bold)
                            ),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.text
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                        confirmationRoleWithFactorSources.overrideFactors.forEachIndexed { index, factorSource ->
                            FactorSourceCardView(
                                item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                                castsShadow = false,
                                isOutlined = true
                            )

                            if (index != confirmationRoleWithFactorSources.overrideFactors.lastIndex) {
                                OrView()
                            }
                        }
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    }
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                ConfirmationDelay(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    delay = confirmationDelay
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            }
        }
    }
}

@UsesSampleValues
@Preview
@Composable
private fun SecurityShieldDetailsLightPreview(
    @PreviewParameter(SecurityShieldDetailsPreviewProvider::class) state: SecurityShieldDetailsViewModel.State
) {
    RadixWalletPreviewTheme {
        SecurityShieldDetailsContent(
            state = state,
            onRenameSecurityShieldClick = {},
            onEditFactorsClick = {},
            onBackClick = {},
            toggleRegularAccessCard = {},
            toggleLogInCard = {},
            toggleRecoveryCard = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun SecurityShieldDetailsDarkPreview(
    @PreviewParameter(SecurityShieldDetailsPreviewProvider::class) state: SecurityShieldDetailsViewModel.State
) {
    RadixWalletPreviewTheme(
        enableDarkTheme = true
    ) {
        SecurityShieldDetailsContent(
            state = state,
            onRenameSecurityShieldClick = {},
            onEditFactorsClick = {},
            onBackClick = {},
            toggleRegularAccessCard = {},
            toggleLogInCard = {},
            toggleRecoveryCard = {}
        )
    }
}

@UsesSampleValues
class SecurityShieldDetailsPreviewProvider : PreviewParameterProvider<SecurityShieldDetailsViewModel.State> {

    override val values: Sequence<SecurityShieldDetailsViewModel.State>
        get() = sequenceOf(
            SecurityShieldDetailsViewModel.State(
                securityShieldName = "My Shield",
                securityStructureOfFactorSources = newSecurityStructureOfFactorSourcesSample()
            ),
            SecurityShieldDetailsViewModel.State(
                securityShieldName = "My Shield 2",
                securityStructureOfFactorSources = newSecurityStructureOfFactorSourcesSampleOther(),
                isRegularAccessCardCollapsed = false,
                isLogInCardCollapsed = false,
                isRecoveryCardCollapsed = false
            )
        )
}
