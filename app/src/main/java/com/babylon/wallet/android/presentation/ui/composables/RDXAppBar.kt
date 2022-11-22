package com.babylon.wallet.android.presentation.ui.composables

import android.content.res.Configuration
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RDXAppBar(
    toolbarTitle: String,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit
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
        }
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
            onMenuClick = {}
        )
    }
}
