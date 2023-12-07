package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

private const val TAG_EXPAND_TOGGLE = "expand_toggle"

@Composable
fun ExpandableText(
    modifier: Modifier = Modifier,
    text: String,
    collapsedLines: Int = 5,
    style: TextStyle = TextStyle.Default,
    toggleStyle: TextStyle = TextStyle.Default,
    onClick: (Int) -> Unit = {},
) {
    ExpandableText(
        modifier = modifier,
        text = buildAnnotatedString { append(text) },
        collapsedLines = collapsedLines,
        style = style,
        toggleStyle = toggleStyle,
        onClick = onClick
    )
}

@Composable
fun ExpandableText(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    collapsedLines: Int = 5,
    style: TextStyle = TextStyle.Default,
    toggleStyle: TextStyle = TextStyle.Default,
    onClick: (Int) -> Unit = {}
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isExpanded by remember { mutableStateOf(false) }
    var canExpand by remember { mutableStateOf(false) }

    var resultText by remember { mutableStateOf(text) }
    val expandText = "…${stringResource(id = R.string.common_showMore)}"
    val collapseText = " ${stringResource(id = R.string.common_showLess)}"

    LaunchedEffect(textLayoutResult) {
        val result = textLayoutResult ?: return@LaunchedEffect
        when {
            isExpanded -> {
                resultText = buildAnnotatedString {
                    append(text)
                    pushStringAnnotation(TAG_EXPAND_TOGGLE, "")
                    withStyle(style = toggleStyle.toSpanStyle()) {
                        append(collapseText)
                    }
                    pop()
                }
            }
            !isExpanded && result.hasVisualOverflow -> {
                canExpand = true
                resultText = buildAnnotatedString {
                    val lastVisibleCharIndex = result.getLineEnd(collapsedLines - 1)
                    val croppedText = text.substring(startIndex = 0, endIndex = lastVisibleCharIndex - expandText.length)

                    append(croppedText)
                    pushStringAnnotation(TAG_EXPAND_TOGGLE, "")
                    withStyle(style = toggleStyle.toSpanStyle()) {
                        append(expandText)
                    }
                    pop()
                }
            }
        }
    }

    ClickableText(
        modifier = modifier.animateContentSize(),
        text = resultText,
        onClick = { offset ->
            onClick(offset)
            isExpanded = !isExpanded && canExpand
        },
        onTextLayout = { textLayoutResult = it },
        style = style,
        maxLines = if (!isExpanded) collapsedLines else Int.MAX_VALUE
    )
}

@Preview
@Composable
fun ExpandableTextPreview() {
    val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse nec est maximus, ultricies purus ut, " +
            "molestie neque. Curabitur pellentesque tempus leo non tristique. Vivamus aliquam urna imperdiet orci pharetra interdum. " +
            "Integer molestie, dolor et imperdiet imperdiet, nisl nisl eleifend diam, ut tristique nisl risus et arcu. " +
            "Praesent interdum, ipsum lobortis tristique aliquet, massa augue congue quam, non scelerisque dui risus dapibus diam. " +
            "Fusce fringilla scelerisque enim, vitae mattis lacus pretium ut. Donec aliquam tortor in condimentum commodo. " +
            "Donec euismod commodo egestas. Etiam eu vulputate sem. Morbi tincidunt enim nec odio vehicula lacinia. " +
            "Maecenas pellentesque est turpis, mollis accumsan lacus efficitur at. Integer in velit sapien. " +
            "Phasellus finibus tempus elementum. Vestibulum metus diam, ultricies vitae tortor id, accumsan posuere nulla. " +
            "Morbi efficitur nulla tempus mauris posuere, nec egestas lacus varius. Suspendisse accumsan ultricies volutpat. " +
            "Phasellus luctus magna nisi, sed pharetra augue maximus vel. Nullam at sapien massa. Curabitur sed hendrerit leo. " +
            "Vivamus accumsan at risus sed euismod. Vivamus imperdiet efficitur massa pulvinar congue. " +
            "Curabitur rhoncus convallis tellus, sed vehicula sapien. Nulla rhoncus finibus augue sed accumsan. " +
            "Phasellus dictum leo vel tellus suscipit, nec laoreet eros ullamcorper. Etiam nisi erat, pellentesque vel pretium et, " +
            "dignissim ullamcorper lorem. Duis purus est, auctor ut."

    RadixWalletTheme {
        ExpandableText(
            text = text
        )
    }
}

@Preview
@Composable
fun ExpandableTextWithLessLinesPreview() {
    val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse nec est maximus, ultricies purus ut, " +
            "molestie neque. Curabitur pellentesque tempus leo non tristique."

    RadixWalletTheme {
        ExpandableText(
            text = text
        )
    }
}
