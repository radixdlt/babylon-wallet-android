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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme

@Composable
fun RDXAppBar(
    toolbarTitle: String,
    modifier: Modifier = Modifier,
    onMenuItemClicked: () -> Unit
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = toolbarTitle,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        actions = {
            IconButton(onClick = { onMenuItemClicked() }) {
                BadgedBox(badge = { Badge() }, modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector =
                        ImageVector.vectorResource(id = R.drawable.ic_home_settings),
                        ""
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
