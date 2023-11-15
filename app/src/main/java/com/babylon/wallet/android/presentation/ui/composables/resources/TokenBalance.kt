package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.resources.Resource
import rdx.works.core.displayableQuantity

@Composable
fun TokenBalance(
    modifier: Modifier = Modifier,
    fungibleResource: Resource.FungibleResource?,
    align: TextAlign = TextAlign.Center
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            if (fungibleResource?.ownedAmount != null) {
                append(fungibleResource.ownedAmount.displayableQuantity())
                withStyle(style = RadixTheme.typography.header.toSpanStyle()) {
                    append(" ${fungibleResource.symbol}")
                }
            }
        },
        style = RadixTheme.typography.title,
        color = RadixTheme.colors.gray1,
        textAlign = align
    )
}
