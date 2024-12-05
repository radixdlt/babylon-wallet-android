package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.HorizontalPagerIndicator
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun SecurityShieldOnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityShieldOnboardingViewModel,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onSelectFactors: () -> Unit,
    onSetupFactors: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SecurityShieldOnboardingContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onPageChange = viewModel::onPageChange,
        onInfoClick = onInfoClick,
        onButtonClick = viewModel::onButtonClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                SecurityShieldOnboardingViewModel.Event.SelectFactors -> onSelectFactors()
                SecurityShieldOnboardingViewModel.Event.SetupFactors -> onSetupFactors()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SecurityShieldOnboardingContent(
    modifier: Modifier = Modifier,
    state: SecurityShieldOnboardingViewModel.State,
    onDismiss: () -> Unit,
    onPageChange: (Int) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onButtonClick: () -> Unit
) {
    val pagerState = rememberPagerState(
        pageCount = { state.pageCount }
    )

    LaunchedEffect(state.currentPagePosition) {
        if (pagerState.currentPage == state.currentPagePosition) {
            return@LaunchedEffect
        }

        pagerState.animateScrollToPage(state.currentPagePosition)
    }

    LaunchedEffect(pagerState.currentPage) {
        if (state.currentPagePosition == pagerState.currentPage) {
            return@LaunchedEffect
        }

        onPageChange(pagerState.currentPage)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onDismiss,
                windowInsets = WindowInsets.statusBarsAndBanner,
                backIconType = BackIconType.Close
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.background(color = RadixTheme.colors.white)
            ) {
                HorizontalPagerIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            top = RadixTheme.dimensions.paddingDefault,
                            bottom = RadixTheme.dimensions.paddingMedium
                        ),
                    pagerState = pagerState,
                    activeIndicatorWidth = 8.dp,
                    inactiveIndicatorWidth = 8.dp,
                    activeColor = RadixTheme.colors.blue2,
                    inactiveColor = RadixTheme.colors.gray4
                )

                RadixBottomBar(
                    onClick = onButtonClick,
                    text = stringResource(
                        id = if (state.isLastPage) {
                            R.string.shieldSetupOnboarding_startButtonTitle
                        } else {
                            R.string.shieldSetupOnboarding_nextButtonTitle
                        }
                    )
                )
            }
        },
        containerColor = RadixTheme.colors.white
    ) { padding ->
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = pagerState
        ) { page ->
            val currentPage = state.pages[page]

            PageContent(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                imageRes = currentPage.imageRes(),
                titleRes = currentPage.titleRes(),
                descriptionRes = currentPage.descriptionRes()
            ) {
                when (currentPage) {
                    SecurityShieldOnboardingViewModel.State.Page.Introduction -> InfoButton(
                        text = stringResource(id = R.string.infoLink_title_securityshield),
                        onClick = { onInfoClick(GlossaryItem.securityshield) }
                    )
                    SecurityShieldOnboardingViewModel.State.Page.AddFactors -> InfoButton(
                        text = stringResource(id = R.string.infoLink_title_buildsecurityshields),
                        onClick = { onInfoClick(GlossaryItem.buildsecurityshields) }
                    )
                    SecurityShieldOnboardingViewModel.State.Page.ApplyShield -> {}
                }
            }
        }
    }
}

@Composable
private fun PageContent(
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int,
    @StringRes titleRes: Int,
    @StringRes descriptionRes: Int,
    additionalContent: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(id = imageRes),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = stringResource(id = titleRes),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = stringResource(id = descriptionRes),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        additionalContent()
    }
}

private fun SecurityShieldOnboardingViewModel.State.Page.imageRes() = when (this) {
    SecurityShieldOnboardingViewModel.State.Page.Introduction -> DSR.shield_intro_1
    SecurityShieldOnboardingViewModel.State.Page.AddFactors -> DSR.shield_intro_2
    SecurityShieldOnboardingViewModel.State.Page.ApplyShield -> DSR.shield_intro_3
}

private fun SecurityShieldOnboardingViewModel.State.Page.titleRes() = when (this) {
    SecurityShieldOnboardingViewModel.State.Page.Introduction -> R.string.shieldSetupOnboarding_introStep_title
    SecurityShieldOnboardingViewModel.State.Page.AddFactors -> R.string.shieldSetupOnboarding_buildShieldStep_title
    SecurityShieldOnboardingViewModel.State.Page.ApplyShield -> R.string.shieldSetupOnboarding_applyShieldStep_title
}

private fun SecurityShieldOnboardingViewModel.State.Page.descriptionRes() = when (this) {
    SecurityShieldOnboardingViewModel.State.Page.Introduction -> R.string.shieldSetupOnboarding_introStep_subtitle
    SecurityShieldOnboardingViewModel.State.Page.AddFactors -> R.string.shieldSetupOnboarding_buildShieldStep_subtitle
    SecurityShieldOnboardingViewModel.State.Page.ApplyShield -> R.string.shieldSetupOnboarding_applyShieldStep_subtitle
}

@Composable
@Preview
private fun SecurityShieldOnboardingPreview() {
    RadixWalletPreviewTheme {
        SecurityShieldOnboardingContent(
            state = SecurityShieldOnboardingViewModel.State(),
            onDismiss = {},
            onPageChange = {},
            onInfoClick = {},
            onButtonClick = {}
        )
    }
}
