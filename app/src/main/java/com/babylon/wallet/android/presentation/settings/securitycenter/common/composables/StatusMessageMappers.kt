package com.babylon.wallet.android.presentation.settings.securitycenter.common.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem

@Composable
fun buildStatusMessageAnnotatedString(
    message: String,
    glossaryItem: GlossaryItem,
    annotation: String
) = buildAnnotatedString {
    append(message)
    withStyle(
        RadixTheme.typography.body1StandaloneLink.copy(
            fontSize = 14.sp,
            color = RadixTheme.colors.textButton
        ).toSpanStyle()
    ) {
        pushStringAnnotation(
            tag = glossaryItem.name,
            annotation = glossaryItem.name
        )
        append(" ")
        append(annotation)
    }
}
