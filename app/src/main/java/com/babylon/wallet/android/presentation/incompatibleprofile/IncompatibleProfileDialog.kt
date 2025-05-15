@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.babylon.wallet.android.presentation.incompatibleprofile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.MainActivity
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.Blue1
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.incompatibleprofile.IncompatibleProfileViewModel.Event
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.utils.Constants
import com.babylon.wallet.android.utils.openEmail
import com.radixdlt.sargon.CommonException

const val ROUTE_INCOMPATIBLE_PROFILE = "incompatible_profile_route"

@Composable
fun IncompatibleProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: IncompatibleProfileViewModel,
    onProfileDeleted: () -> Unit,
) {
    val activity = (LocalContext.current as MainActivity)
    BackHandler {
        activity.finish()
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is Event.ProfileDeleted -> onProfileDeleted()
                is Event.OnSendLogsToSupport -> activity.openEmail(
                    recipientAddress = Constants.RADIX_SUPPORT_EMAIL_ADDRESS,
                    subject = Constants.RADIX_SUPPORT_EMAIL_SUBJECT,
                    body = it.body
                )
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    IncompatibleWalletContent(
        modifier = modifier,
        state = state,
        onDismiss = {
            activity.finish()
        },
        onDeleteProfile = viewModel::deleteProfile,
        onSendLogs = viewModel::sendLogsToSupportClick
    )
}

@Composable
private fun IncompatibleWalletContent(
    modifier: Modifier = Modifier,
    state: IncompatibleProfileViewModel.State,
    onDismiss: () -> Unit,
    onDeleteProfile: () -> Unit,
    onSendLogs: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Blue1)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        BasicAlertDialog(
            onDismissRequest = onDismiss
        ) {
            Surface(
                shape = RadixTheme.shapes.roundedRectSmall,
                color = RadixTheme.colors.background,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge),
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.splash_incompatibleProfileVersionAlert_title),
                        style = RadixTheme.typography.body2Header,
                        color = RadixTheme.colors.text
                    )

                    Text(
                        modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingLarge),
                        text = stringResource(id = R.string.splash_incompatibleProfileVersionAlert_message),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.text
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        RadixTextButton(
                            text = stringResource(id = R.string.common_cancel),
                            onClick = onDismiss,
                            contentColor = RadixTheme.colors.textButton
                        )

                        if (state.incompatibleCause != null) {
                            RadixTextButton(
                                text = stringResource(id = R.string.troubleshooting_contactSupport_title),
                                onClick = onSendLogs,
                                contentColor = RadixTheme.colors.textButton
                            )
                        }

                        RadixTextButton(
                            text = stringResource(id = R.string.splash_incompatibleProfileVersionAlert_delete),
                            onClick = onDeleteProfile,
                            contentColor = RadixTheme.colors.error
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun IncompatibleWalletWithCausePreviewLight() {
    RadixWalletPreviewTheme {
        IncompatibleWalletContent(
            state = IncompatibleProfileViewModel.State(
                incompatibleCause = CommonException.Unknown()
            ),
            onDismiss = {},
            onDeleteProfile = {},
            onSendLogs = {}
        )
    }
}

@Preview
@Composable
private fun IncompatibleWalletWithCausePreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        IncompatibleWalletContent(
            state = IncompatibleProfileViewModel.State(
                incompatibleCause = CommonException.Unknown()
            ),
            onDismiss = {},
            onDeleteProfile = {},
            onSendLogs = {}
        )
    }
}

@Preview
@Composable
private fun IncompatibleWalletWithoutCausePreviewLight() {
    RadixWalletPreviewTheme {
        IncompatibleWalletContent(
            state = IncompatibleProfileViewModel.State(),
            onDismiss = {},
            onDeleteProfile = {},
            onSendLogs = {}
        )
    }
}

@Preview
@Composable
private fun IncompatibleWalletWithoutCausePreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        IncompatibleWalletContent(
            state = IncompatibleProfileViewModel.State(),
            onDismiss = {},
            onDeleteProfile = {},
            onSendLogs = {}
        )
    }
}
