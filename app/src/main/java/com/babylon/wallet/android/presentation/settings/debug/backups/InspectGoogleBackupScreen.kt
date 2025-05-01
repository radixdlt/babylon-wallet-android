@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.settings.debug.backups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import rdx.works.profile.domain.backup.CloudBackupFileEntity
import java.time.format.DateTimeFormatter

@Composable
fun InspectGoogleBackupsScreen(
    viewModel: InspectGoogleBackupsViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = viewModel::onMessageShown
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.settings_debugSettings_inspectCloudBackups),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        val pullToRefreshState = rememberPullToRefreshState()
        Box(
            modifier = Modifier
                .padding(padding)
                .pullToRefresh(
                    state = pullToRefreshState,
                    isRefreshing = state.isLoading,
                    onRefresh = viewModel::onRefresh
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    DeviceId(state = state)
                }

                item {
                    UserStatus(state = state)
                }

                items(state.files) {
                    GoogleDriveFile(entity = it)
                }
            }

            PullToRefreshDefaults.Indicator(
                modifier = Modifier
                    .align(Alignment.TopCenter),
                state = pullToRefreshState,
                isRefreshing = state.isLoading,
                color = RadixTheme.colors.text,
                containerColor = RadixTheme.colors.background
            )
        }
    }
}

@Composable
private fun UserStatus(
    modifier: Modifier = Modifier,
    state: InspectGoogleBackupsViewModel.State
) {
    Row(
        modifier = modifier.padding(RadixTheme.dimensions.paddingDefault),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_personas),
            contentDescription = null,
            tint = RadixTheme.colors.iconSecondary
        )

        val emailText = remember(state) {
            if (state.accountEmail == null && !state.isLoading) {
                "Logged out"
            } else if (state.accountEmail != null) {
                requireNotNull(state.accountEmail)
            } else {
                ""
            }
        }

        Text(
            text = emailText,
            color = RadixTheme.colors.textSecondary,
            style = RadixTheme.typography.body2Regular
        )
    }
}

@Composable
private fun DeviceId(
    modifier: Modifier = Modifier,
    state: InspectGoogleBackupsViewModel.State
) {
    Row(
        modifier = modifier.padding(RadixTheme.dimensions.paddingDefault),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_developer_mode),
            contentDescription = null,
            tint = RadixTheme.colors.iconSecondary
        )

        Text(
            text = state.deviceId.toString(),
            color = RadixTheme.colors.textSecondary,
            style = RadixTheme.typography.body2Regular
        )
    }
}

@Composable
private fun GoogleDriveFile(
    modifier: Modifier = Modifier,
    entity: CloudBackupFileEntity
) {
    Surface(
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .padding(bottom = RadixTheme.dimensions.paddingMedium),
        color = RadixTheme.colors.cardOnSecondary,
        shadowElevation = if (RadixTheme.config.isDarkTheme) {
            0.dp
        } else {
            6.dp
        },
        shape = RadixTheme.shapes.roundedRectMedium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            FileProperty(key = "ID", value = entity.id.id)
            FileProperty(key = "ProfileId", value = entity.header.id.toString())
            FileProperty(
                key = "Backed up",
                value = remember(entity.lastBackup) {
                    entity.lastBackup.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                }
            )
            FileProperty(
                key = "Profile Modified",
                value = remember(entity.header.lastModified) {
                    entity.header.lastModified.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                }
            )
            FileProperty(key = "Claiming Device ID", value = entity.header.lastUsedOnDevice.id.toString())
            FileProperty(key = "Claiming Device Name", value = entity.header.lastUsedOnDevice.description)
            FileProperty(key = "Accounts", value = entity.header.contentHint.numberOfAccountsOnAllNetworksInTotal.toString())
            FileProperty(key = "Personas", value = entity.header.contentHint.numberOfPersonasOnAllNetworksInTotal.toString())
        }
    }
}

@Composable
private fun FileProperty(
    modifier: Modifier = Modifier,
    key: String,
    value: String
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = key,
            maxLines = 1,
            color = RadixTheme.colors.text
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingLarge))
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            color = RadixTheme.colors.text
        )
    }
}
