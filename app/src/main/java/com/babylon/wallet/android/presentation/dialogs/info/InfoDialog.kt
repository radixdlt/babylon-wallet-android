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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
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
import com.mikepenz.markdown.utils.buildMarkdownAnnotatedString
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.findChildOfType

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
            scrollState = ScrollState(initial = 0),
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
    val context = LocalContext.current

    Column(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxHeight(0.5f)
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
                    } else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                    }
                }
            }
        ) {
            val customHeading2: MarkdownComponent = {
                val content = it.content
                it.node.findChildOfType(MarkdownTokenTypes.ATX_CONTENT)?.let {
                    val styledText = buildAnnotatedString {
                        pushStyle(RadixTheme.typography.title.toSpanStyle().copy(color = RadixTheme.colors.gray1))
                        buildMarkdownAnnotatedString(content, it)
                        pop()
                    }
                    Text(
                        styledText,
                        modifier = Modifier.fillMaxSize().padding(bottom = RadixTheme.dimensions.paddingDefault),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Markdown(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(),
                typography = markdownTypography(
                    text = RadixTheme.typography.body1Regular,
                    h2 = RadixTheme.typography.title
                ),
                content = markdownContent ?: stringResource(id = R.string.empty),
                components = markdownComponents(
                    heading2 = customHeading2
                )
            )
        }
    }
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