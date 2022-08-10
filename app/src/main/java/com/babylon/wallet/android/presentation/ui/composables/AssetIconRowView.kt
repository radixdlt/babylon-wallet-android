package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.data.mockdata.mockTokenUiList
import com.babylon.wallet.android.presentation.model.TokenUi
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme
import com.babylon.wallet.android.presentation.ui.theme.RadixBackground

private const val MAX_ASSETS_DISPLAYED = 10
private const val RELATIVE_PADDING = 0.7f

@Composable
fun AssetIconRowView(
    assets: List<TokenUi>,
    circleSize: Int = 30,
    fontSize: Int = 10
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {

        var paddingStart = 0f
        for (i in assets.indices) {
            Box(
                modifier = Modifier
                    .offset(paddingStart.dp, 0.dp)
                    .background(RadixBackground, shape = CircleShape)
                    .defaultMinSize(circleSize.dp, circleSize.dp),
                contentAlignment = Alignment.Center
            ) {
                val text = if (i >= MAX_ASSETS_DISPLAYED)
                    "+${assets.size - MAX_ASSETS_DISPLAYED}"
                else
                    assets[i].symbol
                Text(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fontSize.sp,
                    text = text.orEmpty(), // TODO
                )
            }
            paddingStart += circleSize * RELATIVE_PADDING
            if (i >= MAX_ASSETS_DISPLAYED) {
                break
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AssetIconRowPreview() {
    BabylonWalletTheme {
        AssetIconRowView(
            assets = mockTokenUiList
        )
    }
}
