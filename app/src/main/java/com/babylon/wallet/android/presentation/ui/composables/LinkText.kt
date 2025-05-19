package com.babylon.wallet.android.presentation.ui.composables

import android.net.Uri
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.openUrl

@Composable
fun LinkText(
    modifier: Modifier = Modifier,
    url: Uri,
    clickable: Boolean = true,
    linkStyle: TextStyle = RadixTheme.typography.body1StandaloneLink,
    linkColor: Color = RadixTheme.colors.textButton,
    linkIconColor: Color = RadixTheme.colors.iconSecondary
) {
    LinkText(
        modifier = modifier,
        url = url.toString(),
        clickable = clickable,
        linkStyle = linkStyle,
        linkColor = linkColor,
        linkIconColor = linkIconColor
    )
}

@Composable
fun LinkText(
    modifier: Modifier = Modifier,
    url: String,
    clickable: Boolean = true,
    linkStyle: TextStyle = RadixTheme.typography.body1StandaloneLink,
    linkColor: Color = RadixTheme.colors.textButton,
    linkIconColor: Color = RadixTheme.colors.iconSecondary
) {
    val context = LocalContext.current
    Text(
        modifier = modifier.throttleClickable(enabled = clickable) {
            context.openUrl(url)
        },
        text = buildAnnotatedString {
            append(url)
            append("  ")
            appendInlineContent(id = "link_icon")
        },
        style = linkStyle,
        color = linkColor,
        inlineContent = mapOf(
            "link_icon" to InlineTextContent(
                Placeholder(
                    linkStyle.fontSize,
                    linkStyle.fontSize,
                    PlaceholderVerticalAlign.TextCenter
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_external_link),
                    contentDescription = null,
                    tint = linkIconColor
                )
            }
        ),
        textAlign = TextAlign.Start,
    )
}
