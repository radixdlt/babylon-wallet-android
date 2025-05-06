package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun ChooseEntityContent(
    modifier: Modifier = Modifier,
    onSkipClick: (() -> Unit)? = null,
    title: String,
    subtitle: String,
    isButtonEnabled: Boolean,
    isSelectAllVisible: Boolean,
    selectedAll: Boolean,
    hasSkipButton: Boolean,
    onDismiss: () -> Unit,
    onSelectAllToggleClick: () -> Unit,
    onContinueClick: () -> Unit,
    content: LazyListScope.() -> Unit
) {
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
                onClick = onContinueClick,
                enabled = isButtonEnabled,
                text = stringResource(R.string.common_continue),
                additionalBottomContent = if (hasSkipButton) {
                    {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                        RadixTextButton(
                            text = stringResource(id = R.string.shieldWizardApplyShield_chooseEntities_skipButton),
                            onClick = { onSkipClick?.invoke() }
                        )
                    }
                } else {
                    null
                }
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = RadixTheme.dimensions.paddingXLarge,
                            end = RadixTheme.dimensions.paddingXLarge,
                            top = RadixTheme.dimensions.paddingXXLarge,
                            bottom = RadixTheme.dimensions.paddingSemiLarge
                        )
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = title,
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.text,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                        text = subtitle,
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.text,
                        textAlign = TextAlign.Center
                    )

                    if (isSelectAllVisible) {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                        RadixTextButton(
                            modifier = Modifier.align(Alignment.End),
                            text = stringResource(
                                id = remember(selectedAll) {
                                    if (selectedAll) {
                                        R.string.shieldWizardApplyShield_chooseEntities_deselectAllButton
                                    } else {
                                        R.string.shieldWizardApplyShield_chooseEntities_selectAllButton
                                    }
                                }
                            ),
                            onClick = onSelectAllToggleClick
                        )
                    }
                }
            }

            content()
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun ChooseEntityContentPreview() {
    RadixWalletPreviewTheme {
        ChooseEntityContent(
            modifier = Modifier.fillMaxSize(),
            title = "Choose Accounts",
            subtitle = "Choose the Accounts you want to apply this Shield to.",
            isButtonEnabled = false,
            isSelectAllVisible = true,
            selectedAll = false,
            hasSkipButton = true,
            onDismiss = {},
            onContinueClick = {},
            onSelectAllToggleClick = {}
        ) {}
    }
}
