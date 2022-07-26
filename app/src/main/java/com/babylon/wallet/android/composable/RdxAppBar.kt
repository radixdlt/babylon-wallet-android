package com.babylon.wallet.android.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R

@Composable
fun RDXAppBar(toolbarTitle: String, onMenuItemClicked: () -> Unit) {
    TopAppBar(title = {
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
        }
    )
}