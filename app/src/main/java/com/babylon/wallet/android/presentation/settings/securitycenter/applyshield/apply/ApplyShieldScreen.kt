package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.toPersistentList

@Composable
fun ApplyShieldScreen(
    modifier: Modifier = Modifier,
    backIconType: BackIconType,
    entityAddress: AddressOfAccountOrPersona,
    viewModel: ApplyShieldViewModel,
    onDismiss: () -> Unit,
    onShieldApplied: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ApplyShieldContent(
        modifier = modifier,
        state = state,
        backIconType = backIconType,
        onDismiss = onDismiss,
        onMessageShown = viewModel::onMessageShown,
        onApplyClick = { viewModel.onApplyClick(entityAddress) }
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is ApplyShieldViewModel.Event -> onShieldApplied()
            }
        }
    }
}

@Composable
private fun ApplyShieldContent(
    modifier: Modifier = Modifier,
    backIconType: BackIconType,
    state: ApplyShieldViewModel.State,
    onDismiss: () -> Unit,
    onMessageShown: () -> Unit,
    onApplyClick: () -> Unit
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
                backIconType = backIconType,
                onBackClick = onDismiss,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                RadixBottomBar(
                    text = stringResource(id = R.string.shieldWizardApplyShield_applyShield_saveButton),
                    isLoading = state.isLoading,
                    enabled = !state.isLoading,
                    onClick = onApplyClick
                )
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        if (state.isLoading) {
            FullscreenCircularProgressContent(
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    text = stringResource(id = R.string.shieldWizardApplyShield_applyShield_title),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                    text = stringResource(id = R.string.shieldWizardApplyShield_applyShield_subtitle),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))

                Column(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingXLarge
                    ),
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                ) {
                    state.factors.forEach {
                        FactorSourceCardView(
                            item = it
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun ApplyShieldLightPreview(
    @PreviewParameter(ApplyShieldPreviewProvider::class) state: ApplyShieldViewModel.State
) {
    RadixWalletPreviewTheme {
        ApplyShieldContent(
            state = state,
            backIconType = BackIconType.Back,
            onDismiss = {},
            onMessageShown = {},
            onApplyClick = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun ApplyShieldDarkPreview(
    @PreviewParameter(ApplyShieldPreviewProvider::class) state: ApplyShieldViewModel.State
) {
    RadixWalletPreviewTheme(
        enableDarkTheme = true
    ) {
        ApplyShieldContent(
            state = state,
            backIconType = BackIconType.Back,
            onDismiss = {},
            onMessageShown = {},
            onApplyClick = {}
        )
    }
}

@UsesSampleValues
class ApplyShieldPreviewProvider : PreviewParameterProvider<ApplyShieldViewModel.State> {

    override val values: Sequence<ApplyShieldViewModel.State>
        get() = sequenceOf(
            ApplyShieldViewModel.State(
                isLoading = false,
                factors = FactorSource.sample.all.map {
                    it.toFactorSourceCard(includeLastUsedOn = false)
                }.toPersistentList()
            ),
            ApplyShieldViewModel.State(
                isLoading = true
            )
        )
}
