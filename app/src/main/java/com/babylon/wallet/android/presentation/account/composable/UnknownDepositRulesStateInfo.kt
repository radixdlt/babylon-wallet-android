package com.babylon.wallet.android.presentation.account.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR

@Composable
fun UnknownDepositRulesStateInfo(modifier: Modifier = Modifier) {
    val inlineContentId = "icon"
    val annotatedText = buildAnnotatedString {
        appendInlineContent(inlineContentId)
        append(
            "Sorry, this Account's third-party exceptions and depositor lists are in an unknown state and cannot be viewed" +
                " or edited because it was imported using only a seed phrase or Ledger. A forthcoming wallet " +
                "update will enable viewing and editing of these lists." // TODO crowdin
        )
    }
    val inlineContent = mapOf(
        inlineContentId to InlineTextContent(Placeholder(24.sp, 20.sp, PlaceholderVerticalAlign.Center)) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = DSR.ic_warning_error),
                contentDescription = null,
                tint = RadixTheme.colors.orange1,
            )
        }
    )
    Column(modifier) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = annotatedText,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray2,
            inlineContent = inlineContent
        )
    }
}
