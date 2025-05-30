package com.babylon.wallet.android.presentation.account.composable

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.composables.InfoButton

@Composable
fun EmptyResourcesContent(
    modifier: Modifier = Modifier,
    tab: AssetsTab,
    onInfoClick: (GlossaryItem) -> Unit
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
            color = RadixTheme.colors.text
        )

        InfoButton(
            text = stringResource(id = tab.toEmptyInfoRes()),
            onClick = {
                onInfoClick(tab.toInfoTag())
            }
        )
    }
}

@DrawableRes
private fun AssetsTab.toEmptyIconRes() = when (this) {
    AssetsTab.Tokens -> R.drawable.ic_empty_fungibles
    AssetsTab.Nfts -> com.babylon.wallet.android.designsystem.R.drawable.ic_nfts
    AssetsTab.Staking -> com.babylon.wallet.android.designsystem.R.drawable.ic_lsu
    AssetsTab.PoolUnits -> com.babylon.wallet.android.designsystem.R.drawable.ic_pool_units
}

@StringRes
private fun AssetsTab.toEmptyTitleRes() = when (this) {
    AssetsTab.Tokens -> R.string.assetDetails_tokenDetails_noTokens
    AssetsTab.Nfts -> R.string.assetDetails_NFTDetails_noNfts
    AssetsTab.Staking -> R.string.assetDetails_stakingDetails_noStakes
    AssetsTab.PoolUnits -> R.string.assetDetails_poolUnitDetails_noPoolUnits
}

@Suppress("UnusedPrivateMember") // it will be used soon
@StringRes
private fun AssetsTab.toEmptyInfoRes() = when (this) {
    AssetsTab.Tokens -> R.string.infoLink_title_tokens
    AssetsTab.Nfts -> R.string.infoLink_title_nfts
    AssetsTab.Staking -> R.string.infoLink_title_networkstaking
    AssetsTab.PoolUnits -> R.string.infoLink_title_poolunits
}
