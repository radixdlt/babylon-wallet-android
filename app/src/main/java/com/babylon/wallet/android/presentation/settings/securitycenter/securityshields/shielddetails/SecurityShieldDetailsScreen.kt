package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shielddetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.transaction.composables.ShieldConfigView
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RenameBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSample
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSampleOther

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityShieldDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityShieldDetailsViewModel,
    onBackClick: () -> Unit,
    onApplyShieldClick: (SecurityStructureId) -> Unit,
    onFactorClick: (FactorSourceId) -> Unit,
    onEditShield: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SyncSheetState(
        sheetState = bottomSheetState,
        isSheetVisible = state.isRenameBottomSheetVisible,
        onSheetClosed = viewModel::onRenameSecurityShieldDismissed
    )

    if (state.isRenameBottomSheetVisible) {
        RenameBottomSheet(
            sheetState = bottomSheetState,
            renameInput = state.renameSecurityShieldInput,
            titleRes = R.string.renameLabel_securityShield_title,
            subtitleRes = R.string.renameLabel_securityShield_subtitle,
            errorValidationMessageRes = R.string.renameLabel_securityShield_empty,
            errorTooLongNameMessageRes = R.string.renameLabel_securityShield_tooLong,
            onNameChange = viewModel::onRenameSecurityShieldChanged,
            onUpdateNameClick = viewModel::onRenameSecurityShieldUpdateClick,
            onDismiss = viewModel::onRenameSecurityShieldDismissed,
        )
    }

    SecurityShieldDetailsContent(
        modifier = modifier,
        state = state,
        onRenameSecurityShieldClick = viewModel::onRenameSecurityShieldClick,
        onFactorClick = onFactorClick,
        onEditFactorsClick = viewModel::onEditFactorsClick,
        onApplyShieldClick = onApplyShieldClick,
        onBackClick = onBackClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                SecurityShieldDetailsViewModel.Event.EditShield -> onEditShield()
            }
        }
    }
}

@Composable
private fun SecurityShieldDetailsContent(
    modifier: Modifier = Modifier,
    state: SecurityShieldDetailsViewModel.State,
    onRenameSecurityShieldClick: () -> Unit,
    onFactorClick: (FactorSourceId) -> Unit,
    onEditFactorsClick: () -> Unit,
    onApplyShieldClick: (SecurityStructureId) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner,
                contentColor = RadixTheme.colors.text,
                containerColor = RadixTheme.colors.backgroundSecondary
            )
        },
        bottomBar = {
            if (state.securityStructureOfFactorSources != null && state.isEditable) {
                RadixBottomBar(
                    button = {
                        RadixSecondaryButton(
                            modifier = Modifier
                                .padding(bottom = RadixTheme.dimensions.paddingDefault)
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            text = stringResource(R.string.securityShields_editFactors),
                            onClick = onEditFactorsClick
                        )
                    },
                    additionalBottomContent = {
                        RadixPrimaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            text = "Apply", // TODO crowdin
                            onClick = { onApplyShieldClick(state.securityStructureOfFactorSources.metadata.id) }
                        )
                    }
                )
            }
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.securityStructureOfFactorSources == null) {
                FullscreenCircularProgressContent()
            } else {
                Column {
                    if (state.isEditable) {
                        Text(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                            text = state.securityShieldName,
                            style = RadixTheme.typography.title,
                            color = RadixTheme.colors.text
                        )

                        RadixTextButton(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                            text = stringResource(R.string.renameLabel_securityShield_title),
                            isWithoutPadding = true,
                            onClick = onRenameSecurityShieldClick
                        )
                    }

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    ShieldConfigView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                            .background(
                                color = RadixTheme.colors.background,
                                shape = RadixTheme.shapes.roundedRectMedium
                            ),
                        securityStructure = state.securityStructureOfFactorSources,
                        onFactorClick = onFactorClick
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                }
            }
        }
    }
}

@UsesSampleValues
@Preview
@Composable
private fun SecurityShieldDetailsLightPreview(
    @PreviewParameter(SecurityShieldDetailsPreviewProvider::class) state: SecurityShieldDetailsViewModel.State
) {
    RadixWalletPreviewTheme {
        SecurityShieldDetailsContent(
            state = state,
            onRenameSecurityShieldClick = {},
            onFactorClick = {},
            onEditFactorsClick = {},
            onApplyShieldClick = {},
            onBackClick = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun SecurityShieldDetailsDarkPreview(
    @PreviewParameter(SecurityShieldDetailsPreviewProvider::class) state: SecurityShieldDetailsViewModel.State
) {
    RadixWalletPreviewTheme(
        enableDarkTheme = true
    ) {
        SecurityShieldDetailsContent(
            state = state,
            onRenameSecurityShieldClick = {},
            onFactorClick = {},
            onEditFactorsClick = {},
            onApplyShieldClick = {},
            onBackClick = {}
        )
    }
}

@UsesSampleValues
class SecurityShieldDetailsPreviewProvider : PreviewParameterProvider<SecurityShieldDetailsViewModel.State> {

    override val values: Sequence<SecurityShieldDetailsViewModel.State>
        get() = sequenceOf(
            SecurityShieldDetailsViewModel.State(
                securityShieldName = "My Shield",
                securityStructureOfFactorSources = newSecurityStructureOfFactorSourcesSample(),
                isEditable = true
            ),
            SecurityShieldDetailsViewModel.State(
                securityShieldName = "My Shield 2",
                securityStructureOfFactorSources = newSecurityStructureOfFactorSourcesSampleOther(),
                isEditable = true
            ),
            SecurityShieldDetailsViewModel.State(
                securityStructureOfFactorSources = newSecurityStructureOfFactorSourcesSampleOther(),
                isEditable = false
            )
        )
}
