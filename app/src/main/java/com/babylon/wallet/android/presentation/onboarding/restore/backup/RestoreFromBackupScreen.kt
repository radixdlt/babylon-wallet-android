package com.babylon.wallet.android.presentation.onboarding.restore.backup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.onboarding.restore.backup.RestoreFromBackupViewModel.Event
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.utils.formattedSpans
import com.babylon.wallet.android.utils.toDateString

@Composable
fun RestoreFromBackupScreen(
    viewModel: RestoreFromBackupViewModel,
    onBack: () -> Unit,
    onRestored: (Boolean) -> Unit
) {
    val state by viewModel.state.collectAsState()

    RestoreFromBackupContent(
        state = state,
        onBackClick = viewModel::onBackClick,
        onRestoringProfileCheckChanged = viewModel::toggleRestoringProfileCheck,
        onSubmit = viewModel::onSubmit
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is Event.OnDismiss -> onBack()
                is Event.OnRestored -> onRestored(it.needsMnemonicRecovery)
            }
        }
    }
}

@Composable
private fun RestoreFromBackupContent(
    modifier: Modifier = Modifier,
    state: RestoreFromBackupViewModel.State,
    onBackClick: () -> Unit,
    onRestoringProfileCheckChanged: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    BackHandler(onBack = onBackClick)
    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            RadixCenteredTopAppBar(
                title = "",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Column {
                Divider(color = RadixTheme.colors.gray5)

                RadixPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.common_continue),
                    onClick = onSubmit,
                    enabled = state.isContinueEnabled
                )
            }
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.recoverProfileBackup_header_title),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.recoverProfileBackup_header_subtitle),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Regular
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                text = stringResource(id = R.string.androidRecoverProfileBackup_choose_title),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )

            Surface(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .clickable(enabled = state.restoringProfileHeader != null) {
                        onRestoringProfileCheckChanged(!state.isRestoringProfileChecked)
                    },
                color = RadixTheme.colors.gray5,
                elevation = if (state.restoringProfileHeader != null) 8.dp else 0.dp,
                shape = RadixTheme.shapes.roundedRectMedium,
            ) {
                if (state.restoringProfileHeader != null) {
                    Row(
                        modifier = Modifier
                            .padding(RadixTheme.dimensions.paddingDefault),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = RadixTheme.dimensions.paddingXSmall)
                        ) {
                            Text(
                                text = stringResource(
                                    id = R.string.recoverProfileBackup_backupFrom,
                                    state.restoringProfileHeader.lastUsedOnDevice.description
                                ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                                color = RadixTheme.colors.gray2,
                                style = RadixTheme.typography.body2Regular
                            )

                            Text(
                                text = stringResource(
                                    id = R.string.recoverProfileBackup_lastModified,
                                    state.restoringProfileHeader.lastUsedOnDevice.date.toDateString()
                                ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                                color = RadixTheme.colors.gray2,
                                style = RadixTheme.typography.body2Regular
                            )

                            Text(
                                text = stringResource(
                                    id = R.string.recoverProfileBackup_numberOfAccounts,
                                    state.restoringProfileHeader.contentHint.numberOfAccountsOnAllNetworksInTotal
                                ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                                color = RadixTheme.colors.gray2,
                                style = RadixTheme.typography.body2Regular
                            )

                            Text(
                                text = stringResource(
                                    id = R.string.recoverProfileBackup_numberOfPersonas,
                                    state.restoringProfileHeader.contentHint.numberOfPersonasOnAllNetworksInTotal
                                ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                                color = RadixTheme.colors.gray2,
                                style = RadixTheme.typography.body2Regular
                            )

                            if (!state.restoringProfileHeader.isCompatible) {
                                Text(
                                    text = stringResource(id = R.string.recoverProfileBackup_incompatibleWalletDataLabel),
                                    color = RadixTheme.colors.red1,
                                    style = RadixTheme.typography.body2Regular
                                )
                            }
                        }

                        Checkbox(
                            checked = state.isRestoringProfileChecked,
                            onCheckedChange = onRestoringProfileCheckChanged,
                            colors = CheckboxDefaults.colors(
                                checkedColor = RadixTheme.colors.gray1,
                                uncheckedColor = RadixTheme.colors.gray2,
                                checkmarkColor = Color.White
                            )
                        )
                    }
                } else {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingXLarge),
                        text = stringResource(id = R.string.androidRecoverProfileBackup_noBackupsAvailable),
                        color = RadixTheme.colors.gray2,
                        textAlign = TextAlign.Center,
                        style = RadixTheme.typography.secondaryHeader
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun RestoreFromBackupPreviewBackupExists() {
    RadixWalletTheme {
        var state by remember {
            mutableStateOf(
                RestoreFromBackupViewModel.State(
                    restoringProfileHeader = SampleDataProvider().sampleProfile().header
                )
            )
        }
        RestoreFromBackupContent(
            state = state,
            onBackClick = {},
            onRestoringProfileCheckChanged = {
                state = state.copy(isRestoringProfileChecked = it)
            },
            onSubmit = {}
        )
    }
}

@Preview
@Composable
fun RestoreFromBackupPreviewNoBackupExists() {
    RadixWalletTheme {
        RestoreFromBackupContent(
            state = RestoreFromBackupViewModel.State(
                restoringProfileHeader = null
            ),
            onBackClick = {},
            onRestoringProfileCheckChanged = {},
            onSubmit = {}
        )
    }
}
