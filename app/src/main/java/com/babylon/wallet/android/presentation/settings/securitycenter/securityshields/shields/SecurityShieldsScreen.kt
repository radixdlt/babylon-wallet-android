@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shields

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.SecurityShieldCardView
import com.babylon.wallet.android.presentation.ui.composables.card.shieldsForDisplaySample
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.annotation.UsesSampleValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityShieldsScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityShieldsViewModel,
    onNavigateToSecurityShieldDetails: (securityShieldId: SecurityStructureId) -> Unit,
    onCreateNewSecurityShieldClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SecurityShieldsContent(
        modifier = modifier,
        state = state,
        onDismissMessage = viewModel::onDismissMessage,
        onSecurityShieldClick = onNavigateToSecurityShieldDetails,
        onCreateNewSecurityShieldClick = onCreateNewSecurityShieldClick,
        onBackClick = onBackClick,
        onInfoClick = onInfoClick
    )
}

@Composable
fun SecurityShieldsContent(
    modifier: Modifier = Modifier,
    state: SecurityShieldsViewModel.State,
    onBackClick: () -> Unit,
    onDismissMessage: () -> Unit,
    onSecurityShieldClick: (SecurityStructureId) -> Unit,
    onCreateNewSecurityShieldClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.securityShields_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner,
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        if (state.isLoading) {
            FullscreenCircularProgressContent()
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault)
            ) {
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }

                items(state.shields) { securityShieldCard ->
                    SecurityShieldCardView(
                        modifier = Modifier.clickable { onSecurityShieldClick(securityShieldCard.id) },
                        item = securityShieldCard
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }

                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                        text = stringResource(R.string.securityShields_createShieldButton),
                        onClick = onCreateNewSecurityShieldClick,
                        throttleClicks = true
                    )

                    InfoButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(align = Alignment.CenterHorizontally)
                            .padding(
                                horizontal = RadixTheme.dimensions.paddingDefault,
                                vertical = RadixTheme.dimensions.paddingLarge
                            ),
                        text = stringResource(R.string.infoLink_title_securityshields),
                        onClick = {
                            onInfoClick(GlossaryItem.securityshields)
                        }
                    )
                }
            }
        }
    }

    state.errorMessage?.let {
        ErrorAlertDialog(
            errorMessage = it,
            cancel = onDismissMessage,
        )
    }
}

@UsesSampleValues
@Composable
@Preview
private fun SecurityShieldsLightPreview(
    @PreviewParameter(SecurityShieldsPreviewProvider::class) state: SecurityShieldsViewModel.State
) {
    RadixWalletPreviewTheme {
        SecurityShieldsContent(
            state = state,
            onBackClick = {},
            onDismissMessage = {},
            onSecurityShieldClick = {},
            onCreateNewSecurityShieldClick = {},
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Composable
@Preview
private fun SecurityShieldsDarkPreview(
    @PreviewParameter(SecurityShieldsPreviewProvider::class) state: SecurityShieldsViewModel.State
) {
    RadixWalletPreviewTheme(
        enableDarkTheme = true
    ) {
        SecurityShieldsContent(
            state = state,
            onBackClick = {},
            onDismissMessage = {},
            onSecurityShieldClick = {},
            onCreateNewSecurityShieldClick = {},
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
class SecurityShieldsPreviewProvider : PreviewParameterProvider<SecurityShieldsViewModel.State> {

    override val values: Sequence<SecurityShieldsViewModel.State>
        get() = sequenceOf(
            SecurityShieldsViewModel.State(
                isLoading = false,
                shields = shieldsForDisplaySample
            ),
            SecurityShieldsViewModel.State(
                isLoading = false
            ),
            SecurityShieldsViewModel.State(
                isLoading = true
            )
        )
}
