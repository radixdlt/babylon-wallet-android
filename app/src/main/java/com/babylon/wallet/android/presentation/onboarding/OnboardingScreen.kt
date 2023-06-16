@file:OptIn(ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.GradientBrand2
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
import kotlin.math.absoluteValue
import kotlin.math.sign

@ExperimentalAnimationApi
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    modifier: Modifier = Modifier,
    onOnBoardingEnd: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler { onBack() }

    OnboardingScreenContent(
        currentPage = state.currentPagerPage,
        onProceedClick = viewModel::onProceedClick,
        onRestoreWalletClick = viewModel::onRestoreProfileFromBackupClicked,
//        showWarning = state.showWarning,
//        authenticateWithBiometric = state.authenticateWithBiometric,
//        onUserAuthenticated = viewModel::onUserAuthenticated,
//        onAlertClicked = viewModel::onAlertClicked,
        modifier = modifier
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is OnboardingViewModel.OnBoardingEvent.EndOnBoarding -> {
                    onOnBoardingEnd()
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreenContent(
    currentPage: Int,
    onProceedClick: () -> Unit,
    onRestoreWalletClick: () -> Unit,
//    showWarning: Boolean,
//    authenticateWithBiometric: Boolean,
//    onUserAuthenticated: (Boolean) -> Unit,
//    onAlertClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding()
            .background(White)
            .fillMaxSize()
    ) {
        val pagerState = rememberPagerState(initialPage = currentPage)
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
//            val onboardPages = listOf(
//                OnboardingPage(
//                    title = stringResource(id = R.string.onboarding_title_1),
//                    description = stringResource(id = R.string.onboarding_body_1),
//                    R.drawable.img_carousel_asset
//                ),
//                OnboardingPage(
//                    title = stringResource(id = R.string.onboarding_title_2),
//                    description = stringResource(id = R.string.onboarding_body_2),
//                    R.drawable.img_carousel_asset
//                ),
//                OnboardingPage(
//                    title = stringResource(id = R.string.onboarding_title_3),
//                    description = stringResource(id = R.string.onboarding_body_3),
//                    R.drawable.img_carousel_asset
//                ),
//                OnboardingPage(
//                    title = stringResource(id = R.string.onboarding_title_4),
//                    description = "",
//                    R.drawable.img_carousel_asset
//                )
//            )
            RadixOnboardingPagerIndicator(
                pagerState = pagerState,
                pageCount = 5,
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterHorizontally),
                indicatorWidth = 48.dp,
                indicatorHeight = 4.dp,
            )
//
//            HorizontalPager(
//                pageCount = onboardPages.size,
//                state = pagerState,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .weight(1f)
//            ) { page ->
//                OnboardingPageView(page = onboardPages[page])
//            }
//
//            Spacer(modifier = Modifier.weight(1f))
//
//            // TODO I think this could be triggered outside of composable, maybe in main activity or via view model
//            val context = LocalContext.current
//            LaunchedEffect(authenticateWithBiometric) {
//                if (authenticateWithBiometric) {
//                    context.findFragmentActivity()?.let { activity ->
//                        activity.biometricAuthenticate(true) { authenticatedSuccessfully ->
//                            onUserAuthenticated(authenticatedSuccessfully)
//                        }
//                    }
//                }
//            }
//            if (showWarning) {
//                NotSecureAlertDialog(finish = { accepted -> onAlertClicked(accepted) })
//            }
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = /*!pagerState.canScrollForward*/true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RadixPrimaryButton(
                    text = stringResource(id = R.string.onboarding_newUser),
                    onClick = onProceedClick,
                    modifier = Modifier.fillMaxWidth()
                )
                RadixTextButton(
                    text = stringResource(id = R.string.onboarding_restoreFromBackup),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRestoreWalletClick
                )
                Spacer(Modifier.height(RadixTheme.dimensions.paddingXXLarge))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RadixOnboardingPagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
    pageIndexMapping: (Int) -> Int = { it },
    inactiveColor: Color = RadixTheme.colors.gray4,
    indicatorWidth: Dp = 8.dp,
    indicatorHeight: Dp = indicatorWidth,
    spacing: Dp = RadixTheme.dimensions.paddingSmall,
    indicatorShape: Shape = CircleShape,
) {
    val indicatorWidthPx = LocalDensity.current.run { indicatorWidth.roundToPx() }
    val spacingPx = LocalDensity.current.run { spacing.roundToPx() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val indicatorModifier = Modifier
                .size(width = indicatorWidth, height = indicatorHeight)
                .background(color = inactiveColor, shape = indicatorShape)

            repeat(pageCount) {
                Box(indicatorModifier)
            }
        }

        Box(
            Modifier
                .offset {
                    val position = pageIndexMapping(pagerState.currentPage)
                    val offset = pagerState.currentPageOffsetFraction
                    val next = pageIndexMapping(pagerState.currentPage + offset.sign.toInt())
                    val scrollPosition = ((next - position) * offset.absoluteValue + position)
                        .coerceIn(
                            0f,
                            (pageCount - 1)
                                .coerceAtLeast(0)
                                .toFloat()
                        )

                    IntOffset(
                        x = ((spacingPx + indicatorWidthPx) * scrollPosition).toInt(),
                        y = 0
                    )
                }
                .size(width = indicatorWidth / 2, height = indicatorHeight)
                .then(
                    if (pageCount > 0) {
                        Modifier.background(
                            brush = GradientBrand2,
                            shape = indicatorShape,
                        )
                    } else {
                        Modifier
                    }
                )
        )
    }
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    RadixWalletTheme {
        OnboardingScreenContent(
            currentPage = 0,
            onProceedClick = {},
            onRestoreWalletClick = {},
//            showWarning = false,
//            authenticateWithBiometric = true,
//            onUserAuthenticated = {},
//            onAlertClicked = {},
            Modifier.fillMaxSize()
        )
    }
}
