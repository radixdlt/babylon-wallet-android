package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadixCenteredTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = RadixTheme.colors.gray1,
    actions: @Composable RowScope.() -> Unit = {},
    backIconType: BackIconType = BackIconType.Back,
    containerColor: Color = RadixTheme.colors.defaultBackground
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        windowInsets = WindowInsets(0.dp),
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
                        text = stringResource(id = R.string.cancel),
                        onClick = onBackClick
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                style = RadixTheme.typography.body1Header,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = containerColor)
    )
}

@Preview
@Composable
fun RadixCenteredTopAppBarPreview() {
    RadixWalletTheme {
        RadixCenteredTopAppBar(title = "App bar", onBackClick = {})
    }
}

enum class BackIconType {
    Back, Close, Cancel
}
