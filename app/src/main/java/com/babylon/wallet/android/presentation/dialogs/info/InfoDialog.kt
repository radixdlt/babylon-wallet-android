package com.babylon.wallet.android.presentation.dialogs.info

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.InfoViewModel.Companion.GLOSSARY_ANCHOR
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownPadding
import com.mikepenz.markdown.utils.MARKDOWN_TAG_URL
import com.mikepenz.markdown.utils.buildMarkdownAnnotatedString
import kotlinx.coroutines.launch
import org.intellij.markdown.MarkdownElementTypes.INLINE_LINK
import org.intellij.markdown.MarkdownElementTypes.LINK_DESTINATION
import org.intellij.markdown.MarkdownElementTypes.LINK_LABEL
import org.intellij.markdown.MarkdownElementTypes.LINK_TEXT
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode

typealias DSR = com.babylon.wallet.android.designsystem.R.drawable

@Composable
fun InfoDialog(
    modifier: Modifier = Modifier,
    viewModel: InfoViewModel,
    onDismiss: () -> Unit
) {
    val glossaryItem by viewModel.glossaryItem.collectAsStateWithLifecycle()

    BottomSheetDialogWrapper(
        modifier = modifier,
        showDragHandle = true,
        dragToDismissEnabled = true,
        onDismiss = onDismiss
    ) {
        InfoDialogContent(
            scrollState = rememberScrollState(),
            markdownContent = glossaryItem?.resolveTextFromGlossaryItem(),
            drawableRes = glossaryItem?.resolveIconFromGlossaryItem(),
            onGlossaryItemClick = viewModel::onGlossaryItemClick,
        )
    }
}

@Composable
private fun InfoDialogContent(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    markdownContent: String?,
    @DrawableRes drawableRes: Int?,
    onGlossaryItemClick: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val customHeading2: MarkdownComponent = { markdownComponentModel ->
        markdownComponentModel.node.findChildOfType(MarkdownTokenTypes.ATX_CONTENT)?.let { markDownNode ->
            val styledText = buildAnnotatedString {
                pushStyle(RadixTheme.typography.title.toSpanStyle().copy(color = RadixTheme.colors.gray1))
                buildMarkdownAnnotatedString(markdownComponentModel.content, markDownNode)
                pop()
            }
            Text(
                styledText,
                modifier = Modifier.fillMaxSize(),
                textAlign = TextAlign.Center
            )
        }
    }

    val linkColor = RadixTheme.colors.blue2
    val linkAnnotator = markdownAnnotator { content, child ->
        if (child.type == INLINE_LINK) {
            val linkText = child.findChildOfType(LINK_TEXT)?.children?.innerList()?.firstOrNull()
            val destination = child.findChildOfType(LINK_DESTINATION)
                ?.getTextInNode(content)
                ?.toString()
            val linkLabel = child.findChildOfType(LINK_LABEL)
                ?.getTextInNode(content)?.toString()
            val annotation = destination ?: linkLabel
            if (annotation != null) pushStringAnnotation(MARKDOWN_TAG_URL, annotation)

            pushStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = linkColor
                )
            )

            append(linkText?.getTextInNode(content).toString())
            pop()
            true // return true to consume this ASTNode child
        } else {
            false
        }
    }

    Column(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxHeight(0.9f)
            .verticalScroll(scrollState)
            .padding(horizontal = RadixTheme.dimensions.paddingLarge)
            .padding(top = RadixTheme.dimensions.paddingSmall)
            .padding(bottom = RadixTheme.dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        drawableRes?.let {
            Icon(
                painterResource(id = drawableRes),
                tint = Color.Unspecified,
                contentDescription = null
            )
        }

        CompositionLocalProvider(
            LocalUriHandler provides object : UriHandler {
                override fun openUri(uri: String) {
                    if (uri.contains(GLOSSARY_ANCHOR)) {
                        onGlossaryItemClick(uri.drop(GLOSSARY_ANCHOR.length))
                        coroutineScope.launch {
                            // dialog updated with new glossary item therefore scroll to top
                            scrollState.animateScrollTo(0)
                        }
                    } else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                    }
                }
            }
        ) {
            Markdown(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(),
                typography = markdownTypography(
                    h2 = RadixTheme.typography.title,
                    paragraph = RadixTheme.typography.body1Regular.copy(color = RadixTheme.colors.gray1),

                ),
                padding = markdownPadding(
                    block = RadixTheme.dimensions.paddingSmall,
                ),
                components = markdownComponents(
                    heading2 = customHeading2
                ),
                annotator = linkAnnotator,
                content = markdownContent ?: stringResource(id = R.string.empty),
            )
        }
    }
}

// Helper function to drop the first and last element
// in order to not render the brackets of a link
internal fun List<ASTNode>.innerList(): List<ASTNode> {
    if (this.size <= 1) return emptyList()
    return this.subList(1, this.size - 1)
}

@Composable
private fun GlossaryItem.resolveTextFromGlossaryItem() = when (this) {
    GlossaryItem.web3 -> stringResource(R.string.infoLink_glossary_web3)
    GlossaryItem.radixnetwork -> stringResource(R.string.infoLink_glossary_radixnetwork)
    GlossaryItem.radixwallet -> stringResource(R.string.infoLink_glossary_radixwallet)
    GlossaryItem.radixconnect -> stringResource(R.string.infoLink_glossary_radixconnect)
    GlossaryItem.radixconnector -> stringResource(R.string.infoLink_glossary_radixconnector)
    GlossaryItem.xrd -> stringResource(R.string.infoLink_glossary_xrd)
    GlossaryItem.dashboard -> stringResource(R.string.infoLink_glossary_dashboard)
    GlossaryItem.dapps -> stringResource(R.string.infoLink_glossary_dapps)
    GlossaryItem.connectbutton -> stringResource(R.string.infoLink_glossary_connectbutton)
    GlossaryItem.dex -> stringResource(R.string.infoLink_glossary_dex)
    GlossaryItem.accounts -> stringResource(R.string.infoLink_glossary_accounts)
    GlossaryItem.personas -> stringResource(R.string.infoLink_glossary_personas)
    GlossaryItem.tokens -> stringResource(R.string.infoLink_glossary_tokens)
    GlossaryItem.nfts -> stringResource(R.string.infoLink_glossary_nfts)
    GlossaryItem.claimnfts -> stringResource(R.string.infoLink_glossary_claimnfts)
    GlossaryItem.networkstaking -> stringResource(R.string.infoLink_glossary_networkstaking)
    GlossaryItem.poolunits -> stringResource(R.string.infoLink_glossary_poolunits)
    GlossaryItem.liquidstakeunits -> stringResource(R.string.infoLink_glossary_liquidstakeunits)
    GlossaryItem.badges -> stringResource(R.string.infoLink_glossary_badges)
    GlossaryItem.behaviors -> stringResource(R.string.infoLink_glossary_behaviors)
    GlossaryItem.transfers -> stringResource(R.string.infoLink_glossary_transfers)
    GlossaryItem.transactions -> stringResource(R.string.infoLink_glossary_transactions)
    GlossaryItem.transactionfee -> stringResource(R.string.infoLink_glossary_transactionfee)
    GlossaryItem.guarantees -> stringResource(R.string.infoLink_glossary_guarantees)
    GlossaryItem.payingaccount -> stringResource(R.string.infoLink_glossary_payingaccount)
    GlossaryItem.validators -> stringResource(R.string.infoLink_glossary_validators)
    GlossaryItem.bridging -> stringResource(R.string.infoLink_glossary_bridging)
    GlossaryItem.gateways -> stringResource(R.string.infoLink_glossary_gateways)
    GlossaryItem.preauthorizations -> stringResource(id = R.string.infoLink_glossary_preauthorizations)
    GlossaryItem.possibledappcalls -> stringResource(id = R.string.infoLink_glossary_possibledappcalls)
    GlossaryItem.securityshields -> "" // TODO crowdin
    GlossaryItem.buildingshield -> "" // TODO crowdin
    GlossaryItem.nohardwaredevice -> "" // TODO crowdin
}

@Composable
private fun GlossaryItem.resolveIconFromGlossaryItem() = when (this) {
    GlossaryItem.xrd -> DSR.ic_xrd_token
    GlossaryItem.nfts -> DSR.ic_nfts
    GlossaryItem.networkstaking -> DSR.ic_lsu
    GlossaryItem.poolunits -> DSR.ic_pool_units
    GlossaryItem.liquidstakeunits -> DSR.icon_liquid_stake_units
    GlossaryItem.tokens -> DSR.icon_tokens
    GlossaryItem.badges -> DSR.ic_badge
    else -> null
}

@Preview(showBackground = false)
@Composable
private fun InfoTokensPreview() {
    RadixWalletPreviewTheme {
        InfoDialogContent(
            modifier = Modifier.fillMaxHeight(),
            scrollState = rememberScrollState(),
            markdownContent = stringResource(R.string.infoLink_glossary_tokens),
            drawableRes = DSR.icon_tokens,
            onGlossaryItemClick = {},
        )
    }
}

@Preview(showBackground = false)
@Composable
private fun InfoNFTsPreview() {
    RadixWalletPreviewTheme {
        InfoDialogContent(
            modifier = Modifier.fillMaxHeight(),
            scrollState = rememberScrollState(),
            markdownContent = stringResource(R.string.infoLink_glossary_nfts),
            drawableRes = DSR.ic_nfts,
            onGlossaryItemClick = {},
        )
    }
}
