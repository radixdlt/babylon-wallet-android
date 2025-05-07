package com.babylon.wallet.android.presentation.settings.securitycenter

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.presentation.settings.toProblemHeading
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.PromptLabel
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun SecurityCenterScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityCenterViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    toSecurityShields: () -> Unit,
    toSecurityShieldsOnboarding: () -> Unit,
    onSecurityFactorsClick: () -> Unit,
    onBackupConfigurationClick: () -> Unit,
    onRecoverEntitiesClick: () -> Unit,
    onBackupEntities: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SecurityCenterContent(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onSecurityShieldsClick = viewModel::onSecurityShieldsClick,
        onSecurityFactorsClick = onSecurityFactorsClick,
        onBackupConfigurationClick = onBackupConfigurationClick,
        onRecoverEntitiesClick = onRecoverEntitiesClick,
        onBackupEntities = onBackupEntities
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                SecurityCenterViewModel.Event.ToSecurityShields -> toSecurityShields()
                SecurityCenterViewModel.Event.ToSecurityShieldsOnboarding -> toSecurityShieldsOnboarding()
            }
        }
    }
}

@Composable
private fun SecurityCenterContent(
    modifier: Modifier = Modifier,
    state: SecurityCenterViewModel.SecurityCenterUiState,
    onBackClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER")
    onSecurityShieldsClick: () -> Unit,
    onSecurityFactorsClick: () -> Unit,
    onBackupConfigurationClick: () -> Unit,
    onRecoverEntitiesClick: () -> Unit,
    onBackupEntities: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner,
                contentColor = RadixTheme.colors.text,
                containerColor = RadixTheme.colors.backgroundSecondary
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                text = stringResource(id = R.string.securityCenter_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text
            )
            Text(
                text = stringResource(id = R.string.securityCenter_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text
            )
            Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))

            when (state) {
                is SecurityCenterViewModel.SecurityCenterUiState.Data -> {
                    AnimatedVisibility(visible = state.hasSecurityProblems, enter = fadeIn()) {
                        Column(verticalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingDefault)) {
                            state.securityProblems.forEach { problem ->
                                val title = problem.toProblemHeading()
                                when (problem) {
                                    is SecurityProblem.EntitiesNotRecoverable -> {
                                        NotOkStatusCard(
                                            modifier = Modifier
                                                .clip(RadixTheme.shapes.roundedRectMedium)
                                                .clickable {
                                                    onBackupEntities()
                                                },
                                            title = title,
                                            subtitle = stringResource(id = R.string.securityProblems_no3_securityCenterBody)
                                        )
                                    }

                                    is SecurityProblem.SeedPhraseNeedRecovery -> {
                                        NotOkStatusCard(
                                            modifier = Modifier
                                                .clip(RadixTheme.shapes.roundedRectMedium)
                                                .clickable { onRecoverEntitiesClick() },
                                            title = title,
                                            subtitle = stringResource(id = R.string.securityProblems_no9_securityCenterBody)
                                        )
                                    }

                                    is SecurityProblem.CloudBackupNotWorking -> {
                                        when (problem) {
                                            is SecurityProblem.CloudBackupNotWorking.Disabled -> {
                                                val text = if (problem.hasManualBackup) {
                                                    stringResource(id = R.string.securityProblems_no7_securityCenterBody)
                                                } else {
                                                    stringResource(id = R.string.securityProblems_no6_securityCenterBody)
                                                }
                                                NotOkStatusCard(
                                                    modifier = Modifier
                                                        .clip(RadixTheme.shapes.roundedRectMedium)
                                                        .clickable { onBackupConfigurationClick() },
                                                    title = title,
                                                    subtitle = text
                                                )
                                            }
                                            is SecurityProblem.CloudBackupNotWorking.ServiceError -> {
                                                NotOkStatusCard(
                                                    modifier = Modifier
                                                        .clip(RadixTheme.shapes.roundedRectMedium)
                                                        .clickable { onBackupConfigurationClick() },
                                                    title = title,
                                                    subtitle = stringResource(id = R.string.securityProblems_no5_securityCenterBody)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!state.hasSecurityProblems) {
                        RecoverableStatusCard(text = stringResource(id = R.string.securityCenter_goodState_heading))
                    }

//                    SecurityCenterCard(
//                        onClick = onSecurityShieldsClick,
//                        title = stringResource(id = R.string.securityCenter_securityShieldsItem_title),
//                        subtitle = stringResource(id = R.string.securityCenter_securityShieldsItem_subtitle),
//                        iconRes = DSR.ic_security_shields,
//                        needsAction = state.hasSecurityShieldsProblems,
//                        positiveStatus = stringResource(id = R.string.securityCenter_securityShieldsItem_shieldedStatus)
//                    )

                    SecurityCenterCard(
                        onClick = onSecurityFactorsClick,
                        title = stringResource(id = R.string.securityCenter_securityFactorsItem_title),
                        subtitle = stringResource(id = R.string.securityCenter_securityFactorsItem_subtitle),
                        iconRes = DSR.ic_security_factors,
                        needsAction = state.hasSecurityRelatedProblems,
                        positiveStatus = stringResource(id = R.string.securityCenter_securityFactorsItem_activeStatus)
                    )

                    SecurityCenterCard(
                        onClick = onBackupConfigurationClick,
                        iconRes = DSR.ic_configuration_backup,
                        title = stringResource(id = R.string.securityCenter_configurationBackupItem_title),
                        subtitle = stringResource(id = R.string.securityCenter_configurationBackupItem_subtitle),
                        needsAction = state.hasCloudBackupProblems,
                        positiveStatus = stringResource(id = R.string.securityCenter_configurationBackupItem_backedUpStatus)
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                }
                SecurityCenterViewModel.SecurityCenterUiState.Loading -> {}
            }
        }
    }
}

// TODO Theme
@Composable
fun RecoverableStatusCard(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.ok, RadixTheme.shapes.roundedRectMedium)
            .padding(horizontal = RadixTheme.dimensions.paddingLarge, vertical = RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            painter = painterResource(id = DSR.ic_security_center),
            contentDescription = null,
            tint = White
        )
        Text(
            text = text,
            style = RadixTheme.typography.body1Regular,
            color = White
        )
    }
}

// TODO Theme
@Composable
private fun NotOkStatusCard(modifier: Modifier = Modifier, title: String, subtitle: String) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.warningSecondary, RadixTheme.shapes.roundedRectMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.warning, RadixTheme.shapes.roundedRectTopMedium)
                .padding(horizontal = RadixTheme.dimensions.paddingLarge, vertical = RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_warning_error),
                contentDescription = null,
                tint = White
            )
            Text(
                text = title,
                style = RadixTheme.typography.body1Header,
                color = White
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge, vertical = RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = subtitle,
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.warning
            )
            Icon(
                painter = painterResource(id = DSR.ic_chevron_right),
                contentDescription = null,
                tint = RadixTheme.colors.warning
            )
        }
    }
}

@Composable
private fun SecurityCenterCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    needsAction: Boolean,
    positiveStatus: String
) {
    Row(
        modifier = modifier
            .defaultCardShadow(
                elevation = 6.dp,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .clip(
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .clickable { onClick() }
            .background(
                color = RadixTheme.colors.background, // TODO Theme (card)
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingLarge
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            modifier = Modifier.size(80.dp),
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.Unspecified // TODO Theme
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RadixTheme.dimensions.paddingSmall),
            verticalArrangement = Arrangement.spacedBy(
                space = RadixTheme.dimensions.paddingSmall,
                alignment = Alignment.CenterVertically
            )
        ) {
            Text(
                text = title,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.text
            )

            Text(
                text = subtitle,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary
            )

            val promptColor = if (needsAction) RadixTheme.colors.warning else RadixTheme.colors.ok

            PromptLabel(
                modifier = Modifier.fillMaxWidth(),
                text = if (needsAction) {
                    stringResource(id = R.string.securityCenter_anyItem_actionRequiredStatus)
                } else {
                    positiveStatus
                },
                textColor = promptColor,
                iconRes = if (needsAction) DSR.ic_warning_error else DSR.ic_check_circle,
                iconTint = promptColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SecurityCenterNoProblemsPreview() {
    RadixWalletTheme {
        SecurityCenterContent(
            state = SecurityCenterViewModel.SecurityCenterUiState.Data(
                securityProblems = emptySet()
            ),
            onBackClick = {},
            onSecurityShieldsClick = {},
            onSecurityFactorsClick = {},
            onBackupConfigurationClick = {},
            onRecoverEntitiesClick = {},
            onBackupEntities = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SecurityCenterWithSecurityProblem5Preview() {
    RadixWalletTheme {
        SecurityCenterContent(
            state = SecurityCenterViewModel.SecurityCenterUiState.Data(
                securityProblems = setOf(
                    SecurityProblem.CloudBackupNotWorking.ServiceError(isAnyActivePersonaAffected = true)
                )
            ),
            onBackClick = {},
            onSecurityShieldsClick = {},
            onSecurityFactorsClick = {},
            onBackupConfigurationClick = {},
            onRecoverEntitiesClick = {},
            onBackupEntities = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SecurityCenterWithSecurityProblem9Preview() {
    RadixWalletTheme {
        SecurityCenterContent(
            state = SecurityCenterViewModel.SecurityCenterUiState.Data(
                securityProblems = setOf(
                    SecurityProblem.SeedPhraseNeedRecovery(isAnyActivePersonaAffected = true)
                )
            ),
            onBackClick = {},
            onSecurityShieldsClick = {},
            onSecurityFactorsClick = {},
            onBackupConfigurationClick = {},
            onRecoverEntitiesClick = {},
            onBackupEntities = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SecurityCenterWithSecurityProblems2And7Preview() {
    RadixWalletTheme {
        SecurityCenterContent(
            state = SecurityCenterViewModel.SecurityCenterUiState.Data(
                securityProblems = setOf(
                    SecurityProblem.CloudBackupNotWorking.ServiceError(isAnyActivePersonaAffected = true),
                    SecurityProblem.EntitiesNotRecoverable(
                        accountsNeedBackup = 7,
                        personasNeedBackup = 2,
                        hiddenAccountsNeedBackup = 1,
                        hiddenPersonasNeedBackup = 3
                    )
                )
            ),
            onBackClick = {},
            onSecurityShieldsClick = {},
            onSecurityFactorsClick = {},
            onBackupConfigurationClick = {},
            onRecoverEntitiesClick = {},
            onBackupEntities = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SecurityCenterWithSecurityProblems2And7And9AndOnlyHiddenEntitiesPreview() {
    RadixWalletTheme {
        SecurityCenterContent(
            state = SecurityCenterViewModel.SecurityCenterUiState.Data(
                securityProblems = setOf(
                    SecurityProblem.CloudBackupNotWorking.ServiceError(isAnyActivePersonaAffected = false),
                    SecurityProblem.EntitiesNotRecoverable(
                        accountsNeedBackup = 0,
                        personasNeedBackup = 0,
                        hiddenAccountsNeedBackup = 1,
                        hiddenPersonasNeedBackup = 3
                    ),
                    SecurityProblem.SeedPhraseNeedRecovery(isAnyActivePersonaAffected = false)
                )
            ),
            onBackClick = {},
            onSecurityShieldsClick = {},
            onSecurityFactorsClick = {},
            onBackupConfigurationClick = {},
            onRecoverEntitiesClick = {},
            onBackupEntities = {}
        )
    }
}
