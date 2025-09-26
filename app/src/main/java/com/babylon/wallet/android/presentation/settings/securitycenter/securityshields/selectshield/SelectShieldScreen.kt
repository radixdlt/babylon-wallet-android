package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectshield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableSingleChoiceSecurityShieldCard
import com.babylon.wallet.android.presentation.ui.composables.card.shieldsForDisplaySample
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.none
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun ApplyShieldToEntityScreen(
    onDismiss: () -> Unit,
    onComplete: (SecurityStructureId, AddressOfAccountOrPersona) -> Unit,
    onCreateShieldClick: (AddressOfAccountOrPersona) -> Unit,
    viewModel: SelectShieldViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SelectShieldContent(
        state = state,
        onDismiss = onDismiss,
        onShieldClick = viewModel::onShieldClick,
        onCreateShieldClick = { onCreateShieldClick(viewModel.args.address) },
        onConfirmClick = viewModel::onConfirmClick,
        onDismissMessage = viewModel::onDismissMessage
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is SelectShieldViewModel.Event.Complete -> onComplete(
                    event.securityStructureId,
                    event.entityAddress
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectShieldContent(
    state: SelectShieldViewModel.State,
    onDismiss: () -> Unit,
    onShieldClick: (SecurityStructureId) -> Unit,
    onCreateShieldClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                backIconType = BackIconType.Close,
                containerColor = RadixTheme.colors.backgroundSecondary,
                windowInsets = WindowInsets.statusBarsAndBanner,
                onBackClick = onDismiss
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                RadixBottomBar(
                    text = stringResource(id = R.string.common_confirm),
                    enabled = state.isButtonEnabled,
                    onClick = onConfirmClick
                )
            }
        },
        containerColor = RadixTheme.colors.backgroundSecondary,
        contentWindowInsets = WindowInsets.none
    ) { padding ->
        if (state.isLoading) {
            FullscreenCircularProgressContent()
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    Text(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.commonSecurityShields_select_title),
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.text,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                }

                items(state.shields) {
                    SelectableSingleChoiceSecurityShieldCard(
                        item = it,
                        onSelect = { onShieldClick(it.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    RadixSecondaryButton(
                        text = stringResource(id = R.string.securityShields_createShieldButton),
                        onClick = onCreateShieldClick
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
    @PreviewParameter(SelectShieldPreviewProvider::class) state: SelectShieldViewModel.State
) {
    RadixWalletPreviewTheme {
        SelectShieldContent(
            state = state,
            onDismiss = {},
            onShieldClick = {},
            onCreateShieldClick = {},
            onConfirmClick = {},
            onDismissMessage = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SelectShieldDarkPreview(
    @PreviewParameter(SelectShieldPreviewProvider::class) state: SelectShieldViewModel.State
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        SelectShieldContent(
            state = state,
            onDismiss = {},
            onShieldClick = {},
            onCreateShieldClick = {},
            onConfirmClick = {},
            onDismissMessage = {}
        )
    }
}

@UsesSampleValues
class SelectShieldPreviewProvider : PreviewParameterProvider<SelectShieldViewModel.State> {

    override val values: Sequence<SelectShieldViewModel.State>
        get() = sequenceOf(
            SelectShieldViewModel.State(
                isLoading = false,
                shields = shieldsForDisplaySample.map {
                    Selectable(
                        data = it,
                        selected = false
                    )
                }
            ),
            SelectShieldViewModel.State(
                isLoading = true
            )
        )
}
