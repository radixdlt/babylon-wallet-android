package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadixCenteredTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    contentColor: Color = RadixTheme.colors.text,
    actions: @Composable RowScope.() -> Unit = {},
    backIconType: BackIconType = BackIconType.Back,
    containerColor: Color = RadixTheme.colors.background,
    titleIcon: (@Composable () -> Unit)? = null
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        windowInsets = windowInsets,
        navigationIcon = {
            when (backIconType) {
                BackIconType.Back -> IconButton(onClick = onBackClick) {
                    Icon(
                        painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_back),
                        tint = contentColor,
                        contentDescription = "navigate back"
                    )
                }
                BackIconType.Close -> IconButton(onClick = onBackClick) {
                    Icon(
                        painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                        tint = contentColor,
                        contentDescription = "navigate back"
                    )
                }
                BackIconType.Cancel -> {
                    UnderlineTextButton(
                        text = stringResource(id = R.string.common_cancel),
                        onClick = onBackClick
                    )
                }
                BackIconType.None -> {}
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
            ) {
                titleIcon?.invoke()

                Text(
                    text = title,
                    style = RadixTheme.typography.body1Header,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = containerColor)
    )
}

@Preview
@Composable
fun RadixCenteredTopAppBarDefaultPreview() {
    RadixWalletTheme {
        RadixCenteredTopAppBar(title = "App bar", onBackClick = {})
    }
}

@Preview
@Composable
fun RadixCenteredTopAppBarBackPreview() {
    RadixWalletTheme {
        RadixCenteredTopAppBar(
            title = "App bar",
            backIconType = BackIconType.Back,
            onBackClick = {}
        )
    }
}

@Preview
@Composable
fun RadixCenteredTopAppBarClosePreview() {
    RadixWalletTheme {
        RadixCenteredTopAppBar(
            title = "App bar",
            backIconType = BackIconType.Close,
            onBackClick = {}
        )
    }
}

@Preview
@Composable
fun RadixCenteredTopAppBarCancelPreview() {
    RadixWalletTheme {
        RadixCenteredTopAppBar(
            title = "App bar",
            backIconType = BackIconType.Cancel,
            onBackClick = {}
        )
    }
}

@Preview
@Composable
fun RadixCenteredTopAppBarNonePreview() {
    RadixWalletTheme {
        RadixCenteredTopAppBar(
            title = "App bar",
            backIconType = BackIconType.None,
            onBackClick = {}
        )
    }
}

enum class BackIconType {
    Back, Close, Cancel, None
}
