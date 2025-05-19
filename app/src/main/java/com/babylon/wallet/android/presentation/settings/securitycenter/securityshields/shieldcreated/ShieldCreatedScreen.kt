package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldcreated

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.PromptLabel
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.SecurityStructureId

@Composable
fun ShieldCreatedScreen(
    modifier: Modifier = Modifier,
    viewModel: ShieldCreatedViewModel,
    onDismiss: () -> Unit,
    onApply: (SecurityStructureId) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ShieldCreatedContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onApplyClick = viewModel::onApplyClick,
        onSkipClick = onDismiss
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is ShieldCreatedViewModel.Event.ApplyShield -> onApply(event.securityStructureId)
            }
        }
    }
}

@Composable
private fun ShieldCreatedContent(
    modifier: Modifier = Modifier,
    state: ShieldCreatedViewModel.State,
    onDismiss: () -> Unit,
    onApplyClick: () -> Unit,
    onSkipClick: () -> Unit
) {
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
            RadixBottomBar(
                onClick = onApplyClick,
                text = stringResource(id = R.string.shieldWizardApplyShield_shieldCreated_applyButton),
                enabled = state.isButtonEnabled,
                additionalBottomContent = {
                    RadixTextButton(
                        text = stringResource(id = R.string.shieldWizardApplyShield_shieldCreated_skipButton),
                        onClick = onSkipClick
                    )
                }
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        if (state.isLoading) {
            FullscreenCircularProgressContent()
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = DSR.ic_shield_created),
                    contentDescription = null
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
                    text = stringResource(id = R.string.shieldWizardApplyShield_shieldCreated_title, state.shieldName),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                    text = stringResource(id = R.string.shieldWizardApplyShield_shieldCreated_subtitle),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                Row(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                        .fillMaxWidth()
                        .background(
                            color = RadixTheme.colors.backgroundSecondary,
                            shape = RadixTheme.shapes.roundedRectMedium
                        )
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingLarge,
                            vertical = RadixTheme.dimensions.paddingDefault
                        ),
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = DSR.ic_info_outline),
                        contentDescription = null,
                        tint = RadixTheme.colors.text
                    )

                    Text(
                        text = stringResource(id = R.string.shieldWizardApplyShield_shieldCreated_note),
                        style = RadixTheme.typography.body2HighImportance,
                        color = RadixTheme.colors.text
                    )
                }

                if (state.hasInsufficientXrd) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                    PromptLabel(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                        text = stringResource(id = R.string.shieldWizardApplyShield_shieldCreated_notEnoughXrd)
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun ShieldCreatedPreview(
    @PreviewParameter(ShieldCreatedPreviewProvider::class) state: ShieldCreatedViewModel.State
) {
    RadixWalletPreviewTheme {
        ShieldCreatedContent(
            state = state,
            onDismiss = {},
            onApplyClick = {},
            onSkipClick = {}
        )
    }
}

class ShieldCreatedPreviewProvider : PreviewParameterProvider<ShieldCreatedViewModel.State> {

    override val values: Sequence<ShieldCreatedViewModel.State>
        get() = sequenceOf(
            ShieldCreatedViewModel.State(
                shieldName = "My Shield 1",
                hasInsufficientXrd = false
            ),
            ShieldCreatedViewModel.State(
                shieldName = "My Shield 2",
                hasInsufficientXrd = true
            ),
            ShieldCreatedViewModel.State(
                isLoading = true
            )
        )
}
