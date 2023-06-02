package com.babylon.wallet.android.presentation.settings.incompatibleprofile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.MainActivity
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog

const val ROUTE_INCOMPATIBLE_PROFILE = "incompatible_profile_route"

@Composable
fun IncompatibleProfileContent(
    viewModel: IncompatibleProfileViewModel,
    onProfileDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = (LocalContext.current as MainActivity)
    BackHandler {
        activity.finish()
    }
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                IncompatibleProfileEvent.ProfileDeleted -> onProfileDeleted()
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
                    viewModel.deleteProfile()
                } else {
                    activity.finish()
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.splash_incompatibleProfileVersionAlert_title),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.splash_incompatibleProfileVersionAlert_message),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.splash_incompatibleProfileVersionAlert_delete),
            confirmTextColor = RadixTheme.colors.red1
        )
    }
}
