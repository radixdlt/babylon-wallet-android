package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun RawManifestView(
    modifier: Modifier = Modifier,
    manifest: String
) {
    Text(
        modifier = modifier,
        text = manifest,
        color = RadixTheme.colors.gray1,
        fontSize = 13.sp,
        fontFamily = FontFamily(Typeface(android.graphics.Typeface.MONOSPACE)),
    )
}
