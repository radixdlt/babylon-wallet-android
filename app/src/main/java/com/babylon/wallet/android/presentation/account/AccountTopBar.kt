package com.babylon.wallet.android.presentation.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.MotionLayout
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar

@Composable
fun AccountTopBar(
    modifier: Modifier = Modifier,
    state: AccountUiState,
    onBackClick: () -> Unit,
    onAccountPreferenceClick: (String) -> Unit,
    onTransferClick: (String) -> Unit,
    onApplySecuritySettings: () -> Unit
) {
    val accountAddress = remember(state.accountWithResources) {
        state.accountWithResources?.account?.address.orEmpty()
    }

    ConstraintLayout(
        modifier = modifier.fillMaxWidth(),
    ) {
        val (topBar, accountAddressView, transferButton, securityPrompt) = createRefs()

        RadixCenteredTopAppBar(
            modifier = Modifier.constrainAs(topBar) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(parent.top)
                width = Dimension.fillToConstraints
            },
            title = state.accountWithResources?.account?.displayName.orEmpty(),
            onBackClick = onBackClick,
            actions = {
                IconButton(
                    onClick = { onAccountPreferenceClick(state.accountWithResources?.account?.address.orEmpty()) }
                ) {
                    Icon(
                        painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_more_horiz),
                        tint = RadixTheme.colors.white,
                        contentDescription = "account settings"
                    )
                }
            },
            containerColor = Color.Transparent,
            contentColor = RadixTheme.colors.white
        )

        ActionableAddressView(
            modifier = Modifier.constrainAs(accountAddressView) {
                linkTo(parent.start, parent.end)
                top.linkTo(topBar.bottom)
            }.padding(bottom = RadixTheme.dimensions.paddingXLarge),
            address = accountAddress,
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = RadixTheme.colors.white
        )

        AnimatedVisibility(
            modifier = Modifier.constrainAs(transferButton) {
                linkTo(parent.start, parent.end)
                top.linkTo(accountAddressView.bottom)
            }.padding(bottom = 24.dp),
            visible = state.isTransferEnabled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            RadixSecondaryButton(
                text = stringResource(id = R.string.account_transfer),
                onClick = { onTransferClick(accountAddress) },
                containerColor = RadixTheme.colors.white.copy(alpha = 0.2f),
                contentColor = RadixTheme.colors.white,
                shape = RadixTheme.shapes.circle
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_transfer),
                    tint = RadixTheme.colors.white,
                    contentDescription = null
                )
            }
        }

        AnimatedVisibility(
            modifier = Modifier.constrainAs(securityPrompt) {
                linkTo(parent.start, parent.end, startMargin = 24.dp, endMargin = 24.dp)
                linkTo(transferButton.bottom, parent.bottom, bottomMargin = 24.dp)
                width = Dimension.fillToConstraints
            },
            visible = state.isSecurityPromptVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ApplySecuritySettingsLabel(
                modifier = Modifier.fillMaxWidth(),
                onClick = onApplySecuritySettings,
                text = stringResource(id = R.string.homePage_applySecuritySettings)
            )
        }
    }
}
