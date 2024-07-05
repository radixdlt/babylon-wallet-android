package com.babylon.wallet.android.presentation.onboarding.eula

import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import kotlinx.coroutines.launch

@Composable
fun EulaScreen(
    viewModel: EulaViewModel,
    modifier: Modifier = Modifier,
    onBackClick: (isWithCloudBackupEnabled: Boolean) -> Unit,
    onAccepted: (isWithCloudBackupEnabled: Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is EulaViewModel.EulaEvent.ProceedToCreateNewWallet -> {
                    onAccepted(event.isWithCloudBackupEnabled)
                }

                is EulaViewModel.EulaEvent.NavigateBack -> {
                    onBackClick(event.isWithCloudBackupEnabled)
                }
            }
        }
    }

    EulaContent(
        modifier = modifier,
        onAcceptClick = viewModel::onAcceptClick,
        onBackClick = viewModel::onBackClick
    )
}

@Composable
private fun EulaContent(
    modifier: Modifier = Modifier,
    onAcceptClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var eulaText: String? by remember { mutableStateOf(null) }
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.empty),
                    onBackClick = onBackClick,
                    backIconType = BackIconType.Close,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.onboarding_eula_headerTitle),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.onboarding_eula_headerSubtitle),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = RadixTheme.colors.gray5)
            }
        },
        bottomBar = {
            BottomPrimaryButton(
                text = stringResource(id = R.string.onboarding_eula_accept),
                onClick = onAcceptClick,
                enabled = eulaText != null
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        val scope = rememberCoroutineScope()

        val context = LocalContext.current
        LaunchedEffect(Unit) {
            scope.launch {
                eulaText = context.assets.open("eula/eula.html").bufferedReader().use { it.readText() }
            }
        }

        AnimatedVisibility(
            modifier = Modifier.padding(padding),
            visible = eulaText != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            eulaText?.let {
                EulaView(eula = it)
            }
        }

        if (eulaText == null) {
            FullscreenCircularProgressContent(modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun EulaView(
    modifier: Modifier = Modifier,
    eula: String
) {
    AndroidView(
        modifier = modifier,
        factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.loadWithOverviewMode = true
                settings.textZoom = 100
                setBackgroundColor(Color.TRANSPARENT)
                loadDataWithBaseURL(null, eula, "text/html", "UTF-8", null)
            }
        }
    )
}

@Preview
@Composable
fun EulaScreenPreview() {
    RadixWalletTheme {
        EulaContent(
            onBackClick = {},
            onAcceptClick = {}
        )
    }
}
