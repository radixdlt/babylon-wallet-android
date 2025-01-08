package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.addfactor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableSingleChoiceFactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceKindCard
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.FactorSourceKind

@Composable
fun AddFactorScreen(
    modifier: Modifier = Modifier,
    viewModel: AddFactorViewModel,
    onDismiss: () -> Unit,
    toFactorSetup: (FactorSourceKind) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AddFactorContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onFactorSourceKindSelect = viewModel::onFactorSourceKindSelect,
        onNoHardwareClick = viewModel::onNoHardwareClick,
        onButtonClick = viewModel::onButtonClick,
        onMessageShown = viewModel::onMessageShown
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is AddFactorViewModel.Event.ToFactorSetup -> toFactorSetup(event.kind)
            }
        }
    }
}

@Composable
private fun AddFactorContent(
    modifier: Modifier = Modifier,
    state: AddFactorViewModel.State,
    onDismiss: () -> Unit,
    onFactorSourceKindSelect: (FactorSourceKindCard) -> Unit,
    onNoHardwareClick: () -> Unit,
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
                text = state.mode.button(),
                enabled = state.isButtonEnabled,
                additionalBottomContent = if (state.isModeHardwareOnly) {
                    {
                        RadixTextButton(
                            text = stringResource(id = R.string.shieldSetupPrepareFactors_addHardwareFactor_noDeviceButton),
                            onClick = onNoHardwareClick
                        )
                    }
                } else {
                    null
                }
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
        LazyColumn(
            contentPadding = padding
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = DSR.ic_add_hardware_device),
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                    Text(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                        text = state.mode.title(),
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))

                    Text(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
                        text = state.mode.subtitle(),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
                }
            }

            items(state.factorSourceKinds) { item ->
                SelectableSingleChoiceFactorSourceKindCard(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(bottom = RadixTheme.dimensions.paddingDefault),
                    item = item,
                    onSelect = onFactorSourceKindSelect
                )
            }
        }
    }
}

@Composable
private fun AddFactorViewModel.State.Mode.title(): String = when (this) {
    AddFactorViewModel.State.Mode.HARDWARE_ONLY -> stringResource(id = R.string.shieldSetupPrepareFactors_addHardwareFactor_title)
    AddFactorViewModel.State.Mode.ANY -> stringResource(id = R.string.shieldSetupPrepareFactors_addAnotherFactor_title)
}

@Composable
private fun AddFactorViewModel.State.Mode.subtitle(): AnnotatedString = when (this) {
    AddFactorViewModel.State.Mode.HARDWARE_ONLY -> AnnotatedString(
        stringResource(id = R.string.shieldSetupPrepareFactors_addHardwareFactor_subtitle)
    )
    AddFactorViewModel.State.Mode.ANY -> stringResource(id = R.string.shieldSetupPrepareFactors_addAnotherFactor_subtitle)
        .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold))
}

@Composable
private fun AddFactorViewModel.State.Mode.button(): String = when (this) {
    AddFactorViewModel.State.Mode.HARDWARE_ONLY -> stringResource(id = R.string.shieldSetupPrepareFactors_addHardwareFactor_button)
    AddFactorViewModel.State.Mode.ANY -> stringResource(id = R.string.shieldSetupPrepareFactors_addAnotherFactor_button)
}

@Composable
@Preview
private fun AddFactorPreview(
    @PreviewParameter(AddFactorPreviewProvider::class) state: AddFactorViewModel.State
) {
    RadixWalletPreviewTheme {
        AddFactorContent(
            onDismiss = {},
            state = state,
            onFactorSourceKindSelect = {},
            onNoHardwareClick = {},
            onButtonClick = {},
            onMessageShown = {}
        )
    }
}

class AddFactorPreviewProvider : PreviewParameterProvider<AddFactorViewModel.State> {

    override val values: Sequence<AddFactorViewModel.State>
        get() = sequenceOf(
            AddFactorViewModel.State(
                mode = AddFactorViewModel.State.Mode.HARDWARE_ONLY
            ),
            AddFactorViewModel.State(
                mode = AddFactorViewModel.State.Mode.ANY,
                selected = FactorSourceKind.OFF_DEVICE_MNEMONIC
            )
        )
}
