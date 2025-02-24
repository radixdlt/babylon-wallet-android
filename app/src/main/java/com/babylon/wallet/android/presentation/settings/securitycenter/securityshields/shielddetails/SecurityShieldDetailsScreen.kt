@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shielddetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.securityshields.ConfirmationDelay
import com.babylon.wallet.android.presentation.common.securityshields.OrView
import com.babylon.wallet.android.presentation.common.securityshields.display
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RenameBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.card.CollapsibleCommonCard
import com.babylon.wallet.android.presentation.ui.composables.card.CommonCard
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.composables.card.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.card.SimplePersonaCardWithShadow
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ConfirmationRoleWithFactorSources
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PrimaryRoleWithFactorSources
import com.radixdlt.sargon.RecoveryRoleWithFactorSources
import com.radixdlt.sargon.SecurityStructureOfFactorSources
import com.radixdlt.sargon.TimePeriod
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSample
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSampleOther
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityShieldDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityShieldDetailsViewModel,
    onBackClick: () -> Unit
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
            titleRes = R.string.renameLabel_factorSource_title, // TODO crowdin
            subtitleRes = R.string.renameLabel_factorSource_subtitle, // TODO crowdin
            errorValidationMessageRes = R.string.renameLabel_factorSource_empty, // TODO crowdin
            errorTooLongNameMessageRes = R.string.renameLabel_factorSource_tooLong, // TODO crowdin
            onNameChange = viewModel::onRenameSecurityShieldChanged,
            onUpdateNameClick = viewModel::onRenameSecurityShieldUpdateClick,
            onDismiss = viewModel::onRenameSecurityShieldDismissed,
        )
    }

    SecurityShieldDetailsContent(
        modifier = modifier,
        securityShieldName = state.securityShieldName,
        securityStructureOfFactorSources = state.securityStructureOfFactorSources,
        linkedAccounts = state.linkedAccounts,
        linkedPersonas = state.linkedPersonas,
        hasAnyHiddenLinkedEntities = state.hasAnyHiddenLinkedEntities,
        onRenameSecurityShieldClick = viewModel::onRenameSecurityShieldClick,
        onEditFactorsClick = viewModel::onEditFactorsClick,
        onBackClick = onBackClick
    )
}

@Composable
private fun SecurityShieldDetailsContent(
    modifier: Modifier = Modifier,
    securityShieldName: String,
    securityStructureOfFactorSources: SecurityStructureOfFactorSources?,
    linkedAccounts: PersistentList<Account>,
    linkedPersonas: PersistentList<Persona>,
    hasAnyHiddenLinkedEntities: Boolean,
    onRenameSecurityShieldClick: () -> Unit,
    onEditFactorsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var isRegularAccessCardCollapsed by remember { mutableStateOf(true) }
    var isLogInCardCollapsed by remember { mutableStateOf(true) }
    var isRecoveryCardCollapsed by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(color = RadixTheme.colors.gray5),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner,
                contentColor = RadixTheme.colors.gray1,
                containerColor = RadixTheme.colors.gray5
            )
        },
        bottomBar = {
            RadixBottomBar(
                button = {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = "Edit Factors", // TODO crowdin
                        onClick = onEditFactorsClick
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(color = RadixTheme.colors.gray5)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    text = securityShieldName,
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1
                )

                RadixTextButton(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    text = "Rename", // TODO crowdin
                    isWithoutPadding = true,
                    onClick = onRenameSecurityShieldClick
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                securityStructureOfFactorSources?.let {
                    RegularAccessCollapsibleCard(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                        isCollapsed = isRegularAccessCardCollapsed,
                        primaryRoleWithFactorSources = securityStructureOfFactorSources.matrixOfFactors.primaryRole,
                        onToggleCollapse = { isRegularAccessCardCollapsed = !isRegularAccessCardCollapsed }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    LogInAndProveOwnershipCollapsibleCard(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                        isCollapsed = isLogInCardCollapsed,
                        authenticationSigningFactor = securityStructureOfFactorSources.authenticationSigningFactor,
                        onToggleCollapse = { isLogInCardCollapsed = !isLogInCardCollapsed }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    RecoveryCollapsibleCard(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                        isCollapsed = isRecoveryCardCollapsed,
                        recoveryRoleWithFactorSources = securityStructureOfFactorSources.matrixOfFactors.recoveryRole,
                        confirmationRoleWithFactorSources = securityStructureOfFactorSources.matrixOfFactors.confirmationRole,
                        confirmationDelay = securityStructureOfFactorSources.matrixOfFactors.timeUntilDelayedConfirmationIsCallable,
                        onToggleCollapse = { isRecoveryCardCollapsed = !isRecoveryCardCollapsed }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                }
            }

            LinkedEntitiesView(
                modifier = Modifier.fillMaxHeight(1f),
                linkedAccounts = linkedAccounts,
                linkedPersonas = linkedPersonas,
                hasAnyHiddenLinkedEntities = hasAnyHiddenLinkedEntities
            )
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
                        color = RadixTheme.colors.gray1
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                    Text(
                        text = stringResource(R.string.transactionReview_updateShield_regularAccessMessage),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray2
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
                    colors = CardDefaults.cardColors(containerColor = RadixTheme.colors.gray5),
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
                            color = RadixTheme.colors.gray1
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
                                color = RadixTheme.colors.gray1
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
                        color = RadixTheme.colors.gray1
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                    Text(
                        text = stringResource(R.string.transactionReview_updateShield_authSigningMessage),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray2
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
                    colors = CardDefaults.cardColors(containerColor = RadixTheme.colors.gray5),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = RadixTheme.dimensions.paddingSemiLarge)
                    ) {
                        Text(
                            text = stringResource(R.string.transactionReview_updateShield_authSigningThreshold),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray1
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
                        color = RadixTheme.colors.gray1
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                    Text(
                        text = stringResource(R.string.transactionReview_updateShield_startConfirmMessage),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray2
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
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    colors = CardDefaults.cardColors(containerColor = RadixTheme.colors.gray5),
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
                            color = RadixTheme.colors.gray1
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
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                    colors = CardDefaults.cardColors(containerColor = RadixTheme.colors.gray5),
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
                            color = RadixTheme.colors.gray1
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

@Composable
private fun LinkedEntitiesView(
    modifier: Modifier = Modifier,
    linkedAccounts: PersistentList<Account>,
    linkedPersonas: PersistentList<Persona>,
    hasAnyHiddenLinkedEntities: Boolean,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.gray4)
            .padding(top = RadixTheme.dimensions.paddingDefault)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        SecurityShieldStatusText()

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        LinkedAccountsView(linkedAccounts = linkedAccounts)

        LinkedPersonasView(linkedPersonas = linkedPersonas)

        if (hasAnyHiddenLinkedEntities) {
            LinkedHiddenEntitiesText()
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
}

@Composable
private fun SecurityShieldStatusText() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(80.dp),
            painter = painterResource(id = DSR.ic_shield_not_applied),
            contentDescription = null,
            tint = Color.Unspecified
        )
        Text(
            text = "This Security Shield is applied to these Accounts and Personas", // TODO crowdin
            style = RadixTheme.typography.body1Link,
            color = RadixTheme.colors.gray1
        )
    }
}

@Composable
private fun LinkedAccountsView(linkedAccounts: PersistentList<Account>) {
    Column {
        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            text = "Accounts", // TODO crowdin
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        if (linkedAccounts.isNotEmpty()) {
            linkedAccounts.forEach { account ->
                SimpleAccountCard(
                    account = account,
                    shape = RadixTheme.shapes.roundedRectMedium
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        } else {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                text = "No Accounts", // TODO crowdin
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray2,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
    }
}

@Composable
private fun LinkedPersonasView(linkedPersonas: PersistentList<Persona>) {
    Column {
        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            text = "Personas", // TODO crowdin
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        if (linkedPersonas.isNotEmpty()) {
            linkedPersonas.forEach { persona ->
                SimplePersonaCardWithShadow(persona = persona)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        } else {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                text = "No Personas", // TODO crowdin
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray2,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
    }
}

@Composable
private fun LinkedHiddenEntitiesText() {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingDefault),
        text = stringResource(R.string.securityShields_assigned_onlyHiddenEntities), // TODO crowdin
        style = RadixTheme.typography.body1Header,
        color = RadixTheme.colors.gray2,
        textAlign = TextAlign.Center
    )
}

@UsesSampleValues
@Preview
@Composable
private fun SecurityShieldDetailsWithAllLinkedEntitiesPreview() {
    RadixWalletPreviewTheme {
        SecurityShieldDetailsContent(
            securityShieldName = "DPG7000",
            securityStructureOfFactorSources = newSecurityStructureOfFactorSourcesSample(),
            linkedAccounts = persistentListOf(
                Account.sampleMainnet.alice,
                Account.sampleMainnet.bob,
                Account.sampleMainnet.carol
            ),
            linkedPersonas = persistentListOf(
                Persona.sampleMainnet.batman,
                Persona.sampleMainnet.ripley,
            ),
            hasAnyHiddenLinkedEntities = true,
            onRenameSecurityShieldClick = {},
            onEditFactorsClick = {},
            onBackClick = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun SecurityShieldDetailsWithLinkedPersonasPreview() {
    RadixWalletPreviewTheme {
        SecurityShieldDetailsContent(
            securityShieldName = "DPG7000",
            securityStructureOfFactorSources = newSecurityStructureOfFactorSourcesSampleOther(),
            linkedAccounts = persistentListOf(),
            linkedPersonas = persistentListOf(
                Persona.sampleMainnet.batman,
                Persona.sampleMainnet.ripley,
            ),
            hasAnyHiddenLinkedEntities = false,
            onRenameSecurityShieldClick = {},
            onEditFactorsClick = {},
            onBackClick = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun LogInAndProveOwnershipCollapsibleCardPreview() {
    RadixWalletPreviewTheme {
        val securityStructureOfFactorSourcesSample = newSecurityStructureOfFactorSourcesSample()
        LogInAndProveOwnershipCollapsibleCard(
            authenticationSigningFactor = securityStructureOfFactorSourcesSample.authenticationSigningFactor,
            isCollapsed = false,
            onToggleCollapse = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun RecoveryCollapsibleCardPreview() {
    RadixWalletPreviewTheme {
        val securityStructureOfFactorSourcesSample = newSecurityStructureOfFactorSourcesSample()
        RecoveryCollapsibleCard(
            recoveryRoleWithFactorSources = securityStructureOfFactorSourcesSample.matrixOfFactors.recoveryRole,
            confirmationRoleWithFactorSources = securityStructureOfFactorSourcesSample.matrixOfFactors.confirmationRole,
            confirmationDelay = securityStructureOfFactorSourcesSample.matrixOfFactors.timeUntilDelayedConfirmationIsCallable,
            isCollapsed = false,
            onToggleCollapse = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun SecurityShieldDetailsWithLinkedAccountsAndHiddenPreview() {
    RadixWalletPreviewTheme {
        LinkedEntitiesView(
            linkedAccounts = persistentListOf(
                Account.sampleMainnet.alice,
                Account.sampleMainnet.bob,
                Account.sampleMainnet.carol
            ),
            linkedPersonas = persistentListOf(),
            hasAnyHiddenLinkedEntities = true
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun SecurityShieldDetailsWithLinkedPersonasAndHiddenPreview() {
    RadixWalletPreviewTheme {
        LinkedEntitiesView(
            linkedAccounts = persistentListOf(),
            linkedPersonas = persistentListOf(
                Persona.sampleMainnet.batman,
                Persona.sampleMainnet.ripley,
            ),
            hasAnyHiddenLinkedEntities = true
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun SecurityShieldDetailsWithOnlyLinkedHiddenEntitiesPreview() {
    RadixWalletPreviewTheme {
        LinkedEntitiesView(
            linkedAccounts = persistentListOf(),
            linkedPersonas = persistentListOf(),
            hasAnyHiddenLinkedEntities = true
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun SecurityShieldDetailsWithoutLinkedEntitiesPreview() {
    RadixWalletPreviewTheme {
        LinkedEntitiesView(
            linkedAccounts = persistentListOf(),
            linkedPersonas = persistentListOf(),
            hasAnyHiddenLinkedEntities = false
        )
    }
}
