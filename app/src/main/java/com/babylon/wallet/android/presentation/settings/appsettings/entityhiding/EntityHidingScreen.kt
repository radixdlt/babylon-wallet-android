package com.babylon.wallet.android.presentation.settings.appsettings.entityhiding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar

@Composable
fun EntityHidingScreen(
    viewModel: EntityHidingViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                Event.Close -> onBackClick()
            }
        }
    }
    EntityHidingContent(
        modifier = modifier,
        onBackClick = onBackClick,
        hiddenAccounts = state.hiddenAccounts,
        hiddenPersonas = state.hiddenPersonas,
        onUnhideAllAccounts = viewModel::onUnhideAllAccounts
    )
}

@Composable
private fun EntityHidingContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    hiddenAccounts: Int,
    hiddenPersonas: Int,
    onUnhideAllAccounts: () -> Unit
) {
    var showUnhideAllPrompt by remember { mutableStateOf(false) }
    if (showUnhideAllPrompt) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    onUnhideAllAccounts()
                }
                showUnhideAllPrompt = false
            },
            text = {
                Text(
                    text = stringResource(id = R.string.appSettings_entityHiding_unhideAllConfirmation),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.common_continue)
        )
    }
    Scaffold(modifier = modifier, topBar = {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.appSettings_entityHiding_title),
            onBackClick = onBackClick,
            windowInsets = WindowInsets.statusBars
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RadixTheme.colors.gray5)
                .padding(padding)
        ) {
            Divider(color = RadixTheme.colors.gray5)
            Text(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
                text = stringResource(R.string.appSettings_entityHiding_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray2
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingMedium),
                text = stringResource(
                    if (hiddenAccounts == 1) R.string.appSettings_entityHiding_hiddenAccount else R.string.appSettings_entityHiding_hiddenAccounts,
                    hiddenAccounts
                ),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray2,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingMedium),
                text = stringResource(
                    if (hiddenAccounts == 1) R.string.appSettings_entityHiding_hiddenPersona else R.string.appSettings_entityHiding_hiddenPersonas,
                    hiddenPersonas
                ),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray2,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
                text = stringResource(R.string.appSettings_entityHiding_unhideAllSection),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray2
            )
            Box(
                modifier = Modifier
                    .background(RadixTheme.colors.defaultBackground)
                    .padding(RadixTheme.dimensions.paddingLarge)
            ) {
                RadixSecondaryButton(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.appSettings_entityHiding_unhideAllButton),
                    onClick = {
                        showUnhideAllPrompt = true
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EntityHidingContentPreview() {
    RadixWalletTheme {
        EntityHidingContent(
            onBackClick = {},
            hiddenAccounts = 1,
            hiddenPersonas = 2,
            onUnhideAllAccounts = {}
        )
    }
}
