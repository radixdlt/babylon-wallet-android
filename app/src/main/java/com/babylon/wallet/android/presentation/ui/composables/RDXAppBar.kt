package com.babylon.wallet.android.presentation.ui.composables

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun RDXAppBar(
    toolbarTitle: String,
    modifier: Modifier = Modifier,
    onMenuItemClicked: () -> Unit
) {
    TopAppBar(
        modifier = modifier,
        backgroundColor = RadixTheme.colors.defaultBackground,
        title = {
            Text(
                text = toolbarTitle,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
        },
        actions = {
            IconButton(onClick = { onMenuItemClicked() }) {
                BadgedBox(badge = { Badge() }, modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)) {
                    Icon(
                        imageVector =
                        ImageVector.vectorResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_settings),
                        contentDescription = null,
                        tint = RadixTheme.colors.gray1
                    )
                }
            }
        },
        elevation = 0.dp
    )
}

@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun RDXAppBarPreview() {
    BabylonWalletTheme {
        RDXAppBar(
            toolbarTitle = "Account",
            onMenuItemClicked = {}
        )
    }
}
