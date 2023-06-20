package com.babylon.wallet.android.presentation.account.composable

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import com.babylon.wallet.android.presentation.ui.composables.InfoLink

@Composable
fun EmptyResourcesContent(
    modifier: Modifier = Modifier,
    tab: ResourceTab
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingLarge)
    ) {
        Image(
            modifier = Modifier.height(130.dp), // need certain space for icon in order to have similar heights between tabs
            painter = painterResource(id = tab.toEmptyIconRes()),
            contentDescription = null
        )

        Text(
            text = stringResource(id = tab.toEmptyTitleRes()),
            style = RadixTheme.typography.header,
            color = RadixTheme.colors.gray1
        )

        InfoLink(
            text = stringResource(id = tab.toEmptyInfoRes()),
        )
    }
}

@DrawableRes
private fun ResourceTab.toEmptyIconRes() = when (this) {
    ResourceTab.Tokens -> R.drawable.ic_empty_fungibles
    ResourceTab.Nfts -> R.drawable.ic_empty_non_fungibles
}

@StringRes
private fun ResourceTab.toEmptyTitleRes() = when (this) {
    ResourceTab.Tokens -> R.string.assetDetails_tokenDetails_noTokens
    ResourceTab.Nfts -> R.string.assetDetails_NFTDetails_noNfts
}

@StringRes
private fun ResourceTab.toEmptyInfoRes() = when (this) {
    ResourceTab.Tokens -> R.string.assetDetails_tokenDetails_whatAreTokens
    ResourceTab.Nfts -> R.string.assetDetails_NFTDetails_whatAreNfts
}
