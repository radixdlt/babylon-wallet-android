package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable

@Composable
fun RawManifestView(
    modifier: Modifier = Modifier,
    manifest: String
) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            modifier = Modifier
                .padding(RadixTheme.dimensions.paddingSmall)
                .background(
                    color = RadixTheme.colors.gray4,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
                .throttleClickable {
                    clipboardManager.setText(buildAnnotatedString { append(manifest) })
                }
                .padding(RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    id = R.drawable.ic_copy
                ),
                contentDescription = ""
            )
            Text(
                text = stringResource(id = com.babylon.wallet.android.R.string.common_copy),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        }
        Text(
            text = manifest,
            color = RadixTheme.colors.gray1,
            fontSize = 13.sp,
            fontFamily = FontFamily(Typeface(android.graphics.Typeface.MONOSPACE)),
        )
    }
}
