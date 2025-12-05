package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shielddetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dialogs.info.DSR
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transaction.composables.ShieldConfigView
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RenameBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.babylon.wallet.android.presentation.ui.model.shared.TimedRecoveryDisplayData
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSample
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSampleOther
import com.radixdlt.sargon.samples.sampleMainnet
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityShieldDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityShieldDetailsViewModel,
    onBackClick: () -> Unit,
    onApplyShieldClick: (SecurityStructureId) -> Unit,
    onFactorClick: (FactorSourceId) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onEditShield: () -> Unit,
    onTimedRecoveryClick: (AddressOfAccountOrPersona) -> Unit
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
        onTimedRecoveryClick = onTimedRecoveryClick,
        onApplyShieldClick = onApplyShieldClick,
        onInfoClick = onInfoClick,
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
    onTimedRecoveryClick: (AddressOfAccountOrPersona) -> Unit,
    onApplyShieldClick: (SecurityStructureId) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
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
            if (state.securityStructureOfFactorSources != null) {
                RadixBottomBar(
                    button = {
                        Column(
                            modifier = Modifier
                                .padding(
                                    bottom = if (state.isShieldApplied) {
                                        0.dp
                                    } else {
                                        RadixTheme.dimensions.paddingDefault
                                    }
                                )
                        ) {
                            RadixSecondaryButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                text = stringResource(R.string.securityShields_editFactors),
                                enabled = state.isEditShieldEnabled,
                                onClick = onEditFactorsClick
                            )

                            state.timedRecovery?.let { timedRecovery ->
                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                                RadixSecondaryButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                    text = when {
                                        timedRecovery.remainingTime != null -> {
                                            "Recovery in ${timedRecovery.formattedTime}" // TODO crowdin
                                        }

                                        else -> "Recovery ready to confirm" // TODO crowdin
                                    },
                                    leadingContent = {
                                        Icon(
                                            modifier = Modifier.size(18.dp),
                                            painter = painterResource(id = DSR.hourglass),
                                            contentDescription = null,
                                            tint = RadixTheme.colors.icon
                                        )
                                    },
                                    onClick = { onTimedRecoveryClick(timedRecovery.entityAddress) }
                                )
                            }
                        }
                    },
                    additionalBottomContent = if (state.isShieldApplied) {
                        null
                    } else {
                        {
                            RadixPrimaryButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                text = stringResource(id = R.string.securityShields_apply),
                                onClick = { onApplyShieldClick(state.securityStructureOfFactorSources.metadata.id) }
                            )
                        }
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
                    if (!state.isShieldApplied) {
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
                        onFactorClick = onFactorClick,
                        onInfoClick = onInfoClick
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
            onInfoClick = {},
            onBackClick = {},
            onTimedRecoveryClick = {}
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
            onInfoClick = {},
            onBackClick = {},
            onTimedRecoveryClick = {}
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
                isShieldApplied = false
            ),
            SecurityShieldDetailsViewModel.State(
                securityShieldName = "My Shield 2",
                securityStructureOfFactorSources = newSecurityStructureOfFactorSourcesSampleOther(),
                isShieldApplied = false
            ),
            SecurityShieldDetailsViewModel.State(
                securityStructureOfFactorSources = newSecurityStructureOfFactorSourcesSampleOther(),
                isShieldApplied = true,
                timedRecovery = TimedRecoveryDisplayData(
                    remainingTime = 5.hours,
                    entityAddress = AddressOfAccountOrPersona.sampleMainnet()
                )
            )
        )
}
