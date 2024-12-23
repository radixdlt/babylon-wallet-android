package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.preparefactors

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun PrepareFactorsScreen(
    modifier: Modifier = Modifier,
    viewModel: PrepareFactorsViewModel,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    toAddAnotherFactor: () -> Unit,
    toAddHardwareDevice: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PrepareFactorsContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onInfoClick = onInfoClick,
        onButtonClick = viewModel::onButtonClick,
        onMessageShown = viewModel::onMessageShown
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                PrepareFactorsViewModel.Event.AddAnotherFactor -> toAddAnotherFactor()
                PrepareFactorsViewModel.Event.AddHardwareDevice -> toAddHardwareDevice()
            }
        }
    }
}

@Composable
private fun PrepareFactorsContent(
    modifier: Modifier = Modifier,
    state: PrepareFactorsViewModel.State,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onButtonClick: () -> Unit,
    onMessageShown: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.message,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onDismiss,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onButtonClick,
                text = stringResource(R.string.shieldSetupPrepareFactors_intro_button)
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.white
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = DSR.ic_prepare_factors),
                contentDescription = null
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))

            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = stringResource(id = R.string.shieldSetupPrepareFactors_intro_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
                text = stringResource(id = R.string.shieldSetupPrepareFactors_intro_subtitleTop)
                    .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
                text = stringResource(id = R.string.shieldSetupPrepareFactors_intro_subtitleBottom),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))

            InfoButton(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
                text = stringResource(id = R.string.infoLink_title_buildingshield),
                onClick = { onInfoClick(GlossaryItem.nohardwaredevice) }
            )
        }
    }
}

@Composable
@Preview
private fun PrepareFactorsPreview() {
    RadixWalletPreviewTheme {
        PrepareFactorsContent(
            state = PrepareFactorsViewModel.State(),
            onDismiss = {},
            onInfoClick = {},
            onButtonClick = {},
            onMessageShown = {}
        )
    }
}
