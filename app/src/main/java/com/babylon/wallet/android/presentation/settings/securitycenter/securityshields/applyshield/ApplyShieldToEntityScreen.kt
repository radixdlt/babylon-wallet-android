package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.applyshield

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixRadioButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.SecurityShieldCardView
import com.babylon.wallet.android.presentation.ui.composables.card.shieldsForDisplaySample
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun ApplyShieldToEntityScreen(
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    viewModel: ApplyShieldToEntityViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ApplyShieldToEntityContent(
        state = state,
        onBackClick = onDismiss,
        onShieldClick = viewModel::onShieldClick,
        onConfirmClick = viewModel::onConfirmClick,
        onDismissMessage = viewModel::onDismissMessage
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                ApplyShieldToEntityViewModel.Event.Complete -> onComplete()
            }
        }
    }
}

@Composable
private fun ApplyShieldToEntityContent(
    state: ApplyShieldToEntityViewModel.State,
    onBackClick: () -> Unit,
    onShieldClick: (SecurityStructureId) -> Unit,
    onConfirmClick: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = "",
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner,
                containerColor = RadixTheme.colors.backgroundSecondary
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                RadixBottomBar(
                    text = stringResource(id = R.string.common_confirm),
                    enabled = state.isButtonEnabled,
                    isLoading = state.isApplyLoading,
                    onClick = onConfirmClick
                )
            }
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        if (state.isLoading) {
            FullscreenCircularProgressContent()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = padding + PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault)
            ) {
                item {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                            .padding(bottom = RadixTheme.dimensions.paddingLarge),
                        text = "Select Security Shield", // TODO crowdin
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.text,
                        textAlign = TextAlign.Center
                    )
                }

                items(state.shields) { shield ->
                    SecurityShieldCardView(
                        modifier = Modifier.clickable { onShieldClick(shield.id) },
                        item = shield,
                        endContent = {
                            RadixRadioButton(
                                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                                selected = shield.id == state.selectedId,
                                onClick = { onShieldClick(shield.id) }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }
        }
    }

    state.errorMessage?.let {
        ErrorAlertDialog(
            errorMessage = it,
            cancel = onDismissMessage
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SelectShieldLightPreview(
    @PreviewParameter(ApplyShieldToEntityPreviewProvider::class) state: ApplyShieldToEntityViewModel.State
) {
    RadixWalletPreviewTheme {
        ApplyShieldToEntityContent(
            state = state,
            onBackClick = {},
            onShieldClick = {},
            onConfirmClick = {},
            onDismissMessage = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SelectShieldDarkPreview(
    @PreviewParameter(ApplyShieldToEntityPreviewProvider::class) state: ApplyShieldToEntityViewModel.State
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        ApplyShieldToEntityContent(
            state = state,
            onBackClick = {},
            onShieldClick = {},
            onConfirmClick = {},
            onDismissMessage = {}
        )
    }
}

@UsesSampleValues
class ApplyShieldToEntityPreviewProvider : PreviewParameterProvider<ApplyShieldToEntityViewModel.State> {

    override val values: Sequence<ApplyShieldToEntityViewModel.State>
        get() = sequenceOf(
            ApplyShieldToEntityViewModel.State(
                isLoading = false,
                shields = shieldsForDisplaySample
            ),
            ApplyShieldToEntityViewModel.State(
                isLoading = true
            )
        )
}
