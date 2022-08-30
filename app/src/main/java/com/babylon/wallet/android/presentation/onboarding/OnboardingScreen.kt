package com.babylon.wallet.android.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.MainActivity
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.composables.BabylonButton
import com.babylon.wallet.android.presentation.ui.composables.OnboardingPage
import com.babylon.wallet.android.presentation.ui.composables.OnboardingPageView
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState

@OptIn(ExperimentalLifecycleComposeApi::class)
@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    restoreWalletFromBackup: () -> Unit,
) {
    val state = viewModel.onboardingUiState.collectAsStateWithLifecycle().value
    val pagerState = rememberPagerState(initialPage = state.currentPagerPage)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())

        ) {
            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
                activeColor = colorResource(R.color.purple_500)
            )

            val onboardPages = listOf(
                OnboardingPage(
                    title = stringResource(id = R.string.onboarding_title_1),
                    description = stringResource(id = R.string.onboarding_body_1),
                    R.drawable.img_carousel_asset
                ),
                OnboardingPage(
                    title = stringResource(id = R.string.onboarding_title_2),
                    description = stringResource(id = R.string.onboarding_body_2),
                    R.drawable.img_carousel_asset
                ),
                OnboardingPage(
                    title = stringResource(id = R.string.onboarding_title_3),
                    description = stringResource(id = R.string.onboarding_body_3),
                    R.drawable.img_carousel_asset
                ),
                OnboardingPage(
                    title = stringResource(id = R.string.onboarding_title_4),
                    description = "",
                    R.drawable.img_carousel_asset
                )
            )

            HorizontalPager(
                count = onboardPages.size,
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                OnboardingPageView(page = onboardPages[page])
            }

            Spacer(modifier = Modifier.weight(1f))

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    viewModel.onPageSelected(page, onboardPages.size)
                }
            }

            val context = LocalContext.current as MainActivity
            context.biometricAuthenticate(state.authenticateWithBiometric) { authenticatedSuccessfully ->
                viewModel.onUserAuthenticated(authenticatedSuccessfully)
            }

            AlertDialogView(state.showWarning) { accepted ->
                viewModel.onAlertClicked(accepted)
            }

            AnimatedVisibility(visible = state.showButtons) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth().fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BabylonButton(title = stringResource(id = R.string.im_new_radar_wallet_user)) {
                        // TODO Is Device Factor Present probably in separate ticket
                        viewModel.onProceedClick()
                    }
                    TextButton(
                        onClick = { restoreWalletFromBackup() },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = MaterialTheme.colors.onBackground
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.restore_wallet_from_backup),
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W400
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertDialogView(
    show: Boolean,
    finish: (accepted: Boolean) -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = { finish(false) },
            confirmButton = {
                TextButton(onClick = { finish(true) }) { Text(text = stringResource(id = R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { finish(false) }) { Text(text = stringResource(id = R.string.cancel)) }
            },
            title = { Text(text = stringResource(id = R.string.please_confirm_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.please_confirm_dialog_body)) }
        )
    }
}

//TODO This cannot run when casting LocalContext.current to MainActivity above. Preview activity cannot be casted to FragmentActivity
// I will leave out preview for now to unblock entire feature and do something with it later
//@OptIn(ExperimentalAnimationApi::class, ExperimentalPagerApi::class)
//@Preview(showBackground = true)
//@Preview("large font", fontScale = 2f, showBackground = true)
//@Composable
//fun OnboardingPreview() {
//    val dataStoreManager = DataStoreManager(
//        LocalContext.current.userDataStore
//    )
//    val securityHelper = DeviceSecurityHelper(LocalContext.current)
//
//    val onboardingViewModel = OnboardingViewModel(dataStoreManager, securityHelper)
//    OnboardingScreen(
//        viewModel = onboardingViewModel,
//        restoreWalletFromBackup = {}
//    )
//}
