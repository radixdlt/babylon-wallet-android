package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.text.style.TextOverflow
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadixCenteredTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = RadixTheme.colors.white,
    actions: @Composable RowScope.() -> Unit = {},
    backIconType: BackIconType = BackIconType.Back
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                val backIcon = when (backIconType) {
                    BackIconType.Back -> R.drawable.ic_arrow_back
                    BackIconType.Close -> R.drawable.ic_close
                }
                Icon(
                    painterResource(id = backIcon),
                    tint = contentColor,
                    contentDescription = "navigate back"
                )
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
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

enum class BackIconType {
    Back, Close
}
