package com.babylon.wallet.android.presentation.settings.securitycenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import rdx.works.profile.data.model.BackupState

@Composable
fun SecurityCenterScreen(
    modifier: Modifier = Modifier,
    securityCenterViewModel: SecurityCenterViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onSecurityFactorsClick: () -> Unit,
    onBackupConfigurationClick: () -> Unit,
) {
    val state by securityCenterViewModel.state.collectAsStateWithLifecycle()
    SecurityCenterContent(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onSecurityFactorsClick = onSecurityFactorsClick,
        onBackupConfigurationClick = onBackupConfigurationClick
    )
}

@Composable
private fun SecurityCenterContent(
    modifier: Modifier = Modifier,
    state: SecurityCenterViewModel.SecurityCenterUiState,
    onBackClick: () -> Unit,
    onSecurityFactorsClick: () -> Unit,
    onBackupConfigurationClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars,
                contentColor = RadixTheme.colors.gray1,
                containerColor = RadixTheme.colors.gray5
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                text = stringResource(id = R.string.securityCenter_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Text(
                text = stringResource(id = R.string.securityCenter_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))
            when {
                state.securityFactorsState?.contains(SecurityPromptType.NEEDS_RESTORE) == true -> {
                    NotOkStatusCard(
                        title = stringResource(id = R.string.securityCenter_problem9_heading),
                        subtitle = stringResource(id = R.string.securityCenter_problem9_text)
                    )
                }

                state.securityFactorsState?.contains(SecurityPromptType.NEEDS_BACKUP) == true -> {
                    NotOkStatusCard(
                        title = stringResource(
                            id = R.string.securityCenter_problem3_heading,
                            state.accountsNeedRecovery,
                            state.personasNeedRecovery
                        ),
                        subtitle = stringResource(id = R.string.securityCenter_problem3_text)
                    )
                }

                state.backupState?.isWarningVisible == true -> {
                    NotOkStatusCard(
                        title = stringResource(id = R.string.securityCenter_problem6_heading),
                        subtitle = stringResource(id = R.string.securityCenter_problem6_text)
                    )
                }

                state.securityFactorsState != null && state.backupState?.isWarningVisible == false -> OkStatusCard()
            }
            SecurityFactorsCard(
                onSecurityFactorsClick = onSecurityFactorsClick,
                needsAction = state.securityFactorsState?.isNotEmpty() == true
            )
            BackupConfigurationCard(
                needsAction = state.backupState?.isWarningVisible == true,
                onBackupConfigurationClick = onBackupConfigurationClick
            )
        }
    }
}

@Composable
private fun OkStatusCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.green1, RadixTheme.shapes.roundedRectMedium)
            .padding(horizontal = RadixTheme.dimensions.paddingLarge, vertical = RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(painter = painterResource(id = DSR.ic_security_center), contentDescription = null, tint = RadixTheme.colors.white)
        Text(
            text = stringResource(id = R.string.securityCenter_goodState_heading),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.white
        )
    }
}

@Composable
private fun NotOkStatusCard(modifier: Modifier = Modifier, title: String, subtitle: String) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.orange1.copy(alpha = 0.3f), RadixTheme.shapes.roundedRectMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.orange1, RadixTheme.shapes.roundedRectTopMedium)
                .padding(horizontal = RadixTheme.dimensions.paddingLarge, vertical = RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
        ) {
            Icon(painter = painterResource(id = DSR.ic_warning_error), contentDescription = null, tint = RadixTheme.colors.white)
            Text(
                text = title,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.white
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
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.orange1
            )
            Icon(painter = painterResource(id = DSR.ic_chevron_right), contentDescription = null, tint = RadixTheme.colors.orange1)
        }
    }
}

@Composable
private fun SecurityFactorsCard(
    modifier: Modifier = Modifier,
    onSecurityFactorsClick: () -> Unit,
    needsAction: Boolean
) {
    Row(
        modifier = modifier
            .shadow(6.dp, shape = RadixTheme.shapes.roundedRectMedium)
            .clip(RadixTheme.shapes.roundedRectMedium)
            .clickable {
                onSecurityFactorsClick()
            }
            .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectMedium)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            modifier = Modifier.size(80.dp),
            painter = painterResource(id = DSR.ic_security_factors),
            contentDescription = null,
            tint = Color.Unspecified
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RadixTheme.dimensions.paddingSmall),
            verticalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingSmall, alignment = Alignment.CenterVertically)
        ) {
            Text(
                text = stringResource(id = R.string.securityCenter_securityFactorsItem_title),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
            Text(
                text = stringResource(id = R.string.securityCenter_securityFactorsItem_subtitle),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                Arrangement.spacedBy(space = RadixTheme.dimensions.paddingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = if (needsAction) DSR.ic_warning_error else DSR.ic_check_circle
                val color = if (needsAction) RadixTheme.colors.orange1 else RadixTheme.colors.green1
                val text = if (needsAction) {
                    stringResource(id = R.string.securityCenter_anyItem_actionRequiredStatus)
                } else {
                    stringResource(id = R.string.securityCenter_securityFactorsItem_activeStatus)
                }
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = color
                )
                Text(
                    text = text,
                    style = RadixTheme.typography.body2HighImportance,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun BackupConfigurationCard(needsAction: Boolean, onBackupConfigurationClick: () -> Unit) {
    Row(
        modifier = Modifier
            .shadow(6.dp, shape = RadixTheme.shapes.roundedRectMedium)
            .clip(RadixTheme.shapes.roundedRectMedium)
            .clickable {
                onBackupConfigurationClick()
            }
            .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectMedium)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            modifier = Modifier.size(80.dp),
            painter = painterResource(id = DSR.ic_configuration_backup),
            contentDescription = null,
            tint = Color.Unspecified
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RadixTheme.dimensions.paddingSmall),
            verticalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingSmall, alignment = Alignment.CenterVertically)
        ) {
            Text(
                text = stringResource(id = R.string.configurationBackup_title),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
            Text(
                text = stringResource(id = R.string.configurationBackup_subtitle),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                Arrangement.spacedBy(space = RadixTheme.dimensions.paddingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = if (needsAction) DSR.ic_warning_error else DSR.ic_check_circle
                val color = if (needsAction) RadixTheme.colors.orange1 else RadixTheme.colors.green1
                val text = if (needsAction) {
                    stringResource(id = R.string.securityCenter_anyItem_actionRequiredStatus)
                } else {
                    stringResource(id = R.string.securityCenter_securityFactorsItem_activeStatus)
                }
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = color
                )
                Text(
                    text = text,
                    style = RadixTheme.typography.body2HighImportance,
                    color = color
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SecurityCenterContentPreviewAllOk() {
    RadixWalletTheme {
        SecurityCenterContent(
            state = SecurityCenterViewModel.SecurityCenterUiState(
                securityFactorsState = emptySet(),
                backupState = null,
            ),
            onBackClick = {},
            onSecurityFactorsClick = {},
            onBackupConfigurationClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SecurityCenterContentPreviewAllNotOk() {
    RadixWalletTheme {
        SecurityCenterContent(
            state = SecurityCenterViewModel.SecurityCenterUiState(
                securityFactorsState = setOf(SecurityPromptType.NEEDS_RESTORE),
                backupState = BackupState.Closed
            ),
            onBackClick = {},
            onSecurityFactorsClick = {},
            onBackupConfigurationClick = {}
        )
    }
}