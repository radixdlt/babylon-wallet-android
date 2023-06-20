@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel

@Composable
fun AccountCollapsibleContent(
    modifier: Modifier = Modifier,
    state: AccountUiState,
    onTransferClick: (String) -> Unit,
    onApplySecuritySettings: () -> Unit
) {
    val accountAddress = remember(state.accountWithResources) {
        state.accountWithResources?.account?.address.orEmpty()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ActionableAddressView(
            address = accountAddress,
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = RadixTheme.colors.white
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
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

        AnimatedVisibility(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(top = RadixTheme.dimensions.paddingLarge),
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
//    LargeTopAppBar(
//        modifier = modifier,
//        title = {
//            Column(
////                modifier = Modifier.fillMaxWidth(),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//
//            }
//        },
//        navigationIcon = {
//            IconButton(onClick = onBackClick) {
//                Icon(
//                    painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_back),
//                    tint = RadixTheme.colors.white,
//                    contentDescription = "navigate back"
//                )
//            }
//        },
//        actions = {
//
//        },
//        windowInsets = WindowInsets(left = 0.dp),
//        colors = TopAppBarDefaults.largeTopAppBarColors(
//            containerColor = Color.Transparent,
//            scrolledContainerColor = Color.Transparent,
//            navigationIconContentColor = RadixTheme.colors.white,
//            titleContentColor = RadixTheme.colors.white,
//            actionIconContentColor = RadixTheme.colors.white
//        ),
//        scrollBehavior = scrollBehavior
//    )
}
