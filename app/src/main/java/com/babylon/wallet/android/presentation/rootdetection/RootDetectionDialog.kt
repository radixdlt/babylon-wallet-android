package com.babylon.wallet.android.presentation.rootdetection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.MainActivity
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog

const val ROUTE_ROOT_DETECTION = "root_detection_route"

@Composable
fun RootDetectionContent(
    viewModel: RootDetectionViewModel,
    onAcknowledgeDeviceRooted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = (LocalContext.current as MainActivity)
    BackHandler {
        activity.finish()
    }
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                RootDetectionEvent.RootAcknowledged -> onAcknowledgeDeviceRooted()
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RadixTheme.colors.blue1)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    viewModel.onAcknowledgeClick()
                } else {
                    activity.finish()
                }
            },
            title = {
                Text(
                    text = "Hey, this device looks rooted!  Don’t use the Wallet!", // TODO Crowdin
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            },
            text = {},
            confirmText = "I understand the risk", // TODO Crowdin
            dismissText = stringResource(id = R.string.common_cancel)
        )
    }
}
