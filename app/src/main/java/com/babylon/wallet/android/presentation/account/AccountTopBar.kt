@file:OptIn(ExperimentalMotionApi::class)

package com.babylon.wallet.android.presentation.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel

@Composable
fun AccountTopBar(
    modifier: Modifier = Modifier,
    state: AccountUiState,
    lazyListState: LazyListState,
    onBackClick: () -> Unit,
    onAccountPreferenceClick: (String) -> Unit,
    onTransferClick: (String) -> Unit,
    onApplySecuritySettings: () -> Unit
) {
    val accountAddress = remember(state.accountWithResources) {
        state.accountWithResources?.account?.address.orEmpty()
    }

    val progressTargetValue by remember {
        derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) 0f else 1f }
    }
    val progress by animateFloatAsState(
        targetValue = progressTargetValue,
        animationSpec = tween(300)
    )

    val context = LocalContext.current
    MotionLayout(
        modifier = modifier
            .fillMaxWidth(),
        motionScene = MotionScene(
            content = remember { context.resources.openRawResource(R.raw.account_top_bar_scene).readBytes().decodeToString() }
        ),
        progress = progress
    ) {
        IconButton(
            modifier = Modifier.layoutId("backButton"),
            onClick = onBackClick
        ) {
            Icon(
                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_back),
                tint = RadixTheme.colors.white,
                contentDescription = "navigate back"
            )
        }

        Text(
            modifier = Modifier
                .layoutId("titleLabel")
                .padding(bottom = RadixTheme.dimensions.paddingDefault),
            text = state.accountWithResources?.account?.displayName.orEmpty(),
            style = RadixTheme.typography.body1Header.copy(textAlign = TextAlign.Center),
            color = RadixTheme.colors.white,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        IconButton(
            modifier = Modifier.layoutId("moreButton"),
            onClick = { onAccountPreferenceClick(state.accountWithResources?.account?.address.orEmpty()) }
        ) {
            Icon(
                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_more_horiz),
                tint = RadixTheme.colors.white,
                contentDescription = "account settings"
            )
        }

        if (progress != 1f) {
            ActionableAddressView(
                modifier = Modifier
                    .layoutId("accountAddressView")
                    .padding(bottom = RadixTheme.dimensions.paddingXLarge),
                address = accountAddress,
                textStyle = RadixTheme.typography.body2HighImportance,
                textColor = RadixTheme.colors.white
            )

            AnimatedVisibility(
                modifier = Modifier
                    .layoutId("transferButton")
                    .padding(bottom = 24.dp),
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
                modifier = Modifier
                    .layoutId("securityPrompt"),
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
}
