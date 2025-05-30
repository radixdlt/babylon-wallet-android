package com.babylon.wallet.android.presentation.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.domain.usecases.securityproblems.SecurityPromptType
import com.babylon.wallet.android.presentation.account.AccountViewModel.State
import com.babylon.wallet.android.presentation.ui.composables.AccountPromptLabel
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.toText
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address

/**
 * TODO
 * Not removing this for now as it might be used when we propery revisit collapsing toolbar scrolling with MotionLayout
 */
@OptIn(ExperimentalMotionApi::class)
@Composable
fun AccountTopBar(
    modifier: Modifier = Modifier,
    state: State,
    lazyListState: LazyListState,
    onBackClick: () -> Unit,
    onAccountPreferenceClick: (AccountAddress) -> Unit,
    onTransferClick: (AccountAddress) -> Unit,
    onApplySecuritySettings: (SecurityPromptType) -> Unit
) {
    val accountAddress = state.accountWithAssets?.account?.address
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
            content = remember {
                context.resources.openRawResource(
                    R.raw.account_top_bar_scene
                ).readBytes().decodeToString()
            }
        ),
        progress = progress
    ) {
        IconButton(
            modifier = Modifier.layoutId("backButton"),
            onClick = onBackClick
        ) {
            Icon(
                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_back),
                tint = White,
                contentDescription = "navigate back"
            )
        }

        Text(
            modifier = Modifier
                .layoutId("titleLabel")
                .padding(bottom = RadixTheme.dimensions.paddingDefault),
            text = state.accountWithAssets?.account?.displayName?.value.orEmpty(),
            style = RadixTheme.typography.body1Header.copy(textAlign = TextAlign.Center),
            color = White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        IconButton(
            modifier = Modifier.layoutId("moreButton"),
            onClick = {
                if (accountAddress != null) {
                    onAccountPreferenceClick(accountAddress)
                }
            }
        ) {
            Icon(
                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_more_horiz),
                tint = White,
                contentDescription = "account settings"
            )
        }

        if (progress != 1f) {
            if (accountAddress != null) {
                ActionableAddressView(
                    address = Address.Account(accountAddress),
                    modifier = Modifier
                        .layoutId("accountAddressView")
                        .padding(bottom = RadixTheme.dimensions.paddingXXLarge),
                    textStyle = RadixTheme.typography.body2HighImportance,
                    textColor = White
                )
            }

            AnimatedVisibility(
                modifier = Modifier
                    .layoutId("transferButton")
                    .padding(bottom = RadixTheme.dimensions.paddingLarge),
                visible = state.isTransferEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                RadixSecondaryButton(
                    text = stringResource(id = R.string.account_transfer),
                    onClick = {
                        if (accountAddress != null) {
                            onTransferClick(accountAddress)
                        }
                    },
                    containerColor = White.copy(alpha = 0.2f),
                    contentColor = White,
                    shape = RadixTheme.shapes.circle,
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_transfer),
                            tint = White,
                            contentDescription = null
                        )
                    }
                )
            }

            AnimatedVisibility(
                modifier = Modifier
                    .layoutId("securityPrompt")
                    .padding(bottom = RadixTheme.dimensions.paddingLarge),
                visible = state.securityPrompts != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    state.securityPrompts?.forEach { securityPromptType ->
                        AccountPromptLabel(
                            modifier = Modifier.fillMaxWidth().padding(bottom = RadixTheme.dimensions.paddingMedium),
                            onClick = {
                                onApplySecuritySettings(securityPromptType)
                            },
                            text = securityPromptType.toText()
                        )
                    }
                }
            }
        }
    }
}
