package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.SecurityFactorTypesListPreviewProvider
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.SecurityFactorTypesListView
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.SecurityFactorTypeUiItem
import com.radixdlt.sargon.FactorSourceKind

@Composable
fun SecurityFactorTypesScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityFactorTypesViewModel,
    onSecurityFactorTypeClick: (FactorSourceKind) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SecurityFactorTypesContent(
        modifier = modifier.fillMaxSize(),
        state = state,
        onSecurityFactorTypeClick = { onSecurityFactorTypeClick(it.factorSourceKind) },
        onBackClick = onBackClick,
    )
}

@Composable
private fun SecurityFactorTypesContent(
    modifier: Modifier = Modifier,
    state: SecurityFactorTypesViewModel.State,
    onSecurityFactorTypeClick: (SecurityFactorTypeUiItem.Item) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.securityFactors_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.backgroundTertiary)
            }
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        SecurityFactorTypesListView(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            description = {
                Text(
                    text = stringResource(id = R.string.securityFactors_subtitle),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.textSecondary,
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
                )
            },
            items = state.items,
            onSecurityFactorTypeItemClick = onSecurityFactorTypeClick
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SecurityFactorTypesPreview() {
    RadixWalletTheme {
        SecurityFactorTypesContent(
            modifier = Modifier,
            state = SecurityFactorTypesViewModel.State(
                items = SecurityFactorTypesListPreviewProvider().value
            ),
            onSecurityFactorTypeClick = {},
            onBackClick = {}
        )
    }
}
