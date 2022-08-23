package com.babylon.wallet.android.presentation.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.DataStoreManager
import com.babylon.wallet.android.di.ApplicationModule.userDataStore
import com.babylon.wallet.android.presentation.ui.composables.BabylonButton
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.Dispatchers

@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    newRadarWalletUserClick: () -> Unit,
    restoreWalletFromBackup: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            activeColor = colorResource(R.color.purple_500)
        )

        val onboardPages = listOf(
            Page(
                title = stringResource(id = R.string.onboarding_title_1),
                description = stringResource(id = R.string.onboarding_body_1),
                R.drawable.img_carousel_asset
            ),
            Page(
                title = stringResource(id = R.string.onboarding_title_2),
                description = stringResource(id = R.string.onboarding_body_2),
                R.drawable.img_carousel_asset
            ),
            Page(
                title = stringResource(id = R.string.onboarding_title_3),
                description = stringResource(id = R.string.onboarding_body_3),
                R.drawable.img_carousel_asset
            )
        )

        HorizontalPager(
            count = onboardPages.size,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
        ) { page ->
            PageUI(page = onboardPages[page])
        }

        AnimatedVisibility(visible = pagerState.currentPage == 2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BabylonButton(title = stringResource(id = R.string.im_new_radar_wallet_user)) {
                    viewModel.setShowOnboarding(false)
                    newRadarWalletUserClick()
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

@Composable
fun PageUI(page: Page) {
    Column(
        modifier = Modifier.padding(horizontal = 40.dp, vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.W600,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.W400
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(page.image),
            contentDescription = null,
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

data class Page(
    val title: String,
    val description: String,
    @DrawableRes val image: Int
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalPagerApi::class)
@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun OnboardingPreview() {
    val dataStoreManager = DataStoreManager(
        LocalContext.current.userDataStore
    )

    val onboardingViewModel = OnboardingViewModel(Dispatchers.IO, dataStoreManager)
    OnboardingScreen(
        viewModel = onboardingViewModel,
        newRadarWalletUserClick = {},
        restoreWalletFromBackup = {}
    )
}