package com.babylon.wallet.android.presentation.settings.dappdetail

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.PersonaRoundedAvatar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.StandardOneLineCard
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.collections.immutable.ImmutableList

@Composable
fun DAppDetailsSheetContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    dappName: String,
    dappWithMetadata: DAppWithMetadata?,
    associatedFungibleTokens: ImmutableList<Resource.FungibleResource>,
    associatedNonFungibleTokens: ImmutableList<Resource.NonFungibleResource.Item>
) {
    Column(modifier = modifier) {
        RadixCenteredTopAppBar(
            title = dappName,
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        Divider(color = RadixTheme.colors.gray5)
        LazyColumn(
            contentPadding = PaddingValues(vertical = RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            dappWithMetadata?.iconUrl?.let {
                val url = it.toString()
                if (url.isNotEmpty()) {
                    item {
                        PersonaRoundedAvatar(
                            url = url,
                            modifier = Modifier
                                .padding(vertical = RadixTheme.dimensions.paddingDefault)
                                .size(104.dp)
                        )
                        Divider(color = RadixTheme.colors.gray5)
                    }
                }
            }
            dappWithMetadata?.description?.let { description ->
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingDefault),
                        text = description,
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.Start
                    )
                    Divider(color = RadixTheme.colors.gray5)
                }
            }
            dappWithMetadata?.dAppAddress?.let { dappDefinitionAddress ->
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    DappDefinitionAddressRow(
                        dappDefinitionAddress = dappDefinitionAddress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }
            dappWithMetadata?.claimedWebsite?.let {
                item {
                    DAppWebsiteAddressRow(
                        websiteAddress = it,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }
            if (associatedFungibleTokens.isNotEmpty()) {
                item {
                    GrayBackgroundWrapper {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RadixTheme.dimensions.paddingDefault),
                            text = stringResource(id = R.string.authorizedDapps_dAppDetails_tokens),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
            items(associatedFungibleTokens) { fungibleToken ->
                GrayBackgroundWrapper {
                    val placeholder = if (fungibleToken.isXrd) {
                        painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token)
                    } else {
                        rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb()))
                    }
                    StandardOneLineCard(
                        image = fungibleToken.iconUrl.toString(),
                        title = fungibleToken.displayTitle,
                        modifier = Modifier
                            .shadow(elevation = 8.dp, shape = RadixTheme.shapes.roundedRectMedium)
                            .clip(RadixTheme.shapes.roundedRectMedium)
                            .fillMaxWidth()
                            .background(
                                RadixTheme.colors.white,
                                shape = RadixTheme.shapes.roundedRectMedium
                            )
                            .padding(
                                horizontal = RadixTheme.dimensions.paddingLarge,
                                vertical = RadixTheme.dimensions.paddingDefault
                            ),
                        showChevron = false,
                        placeholder = placeholder
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }
            if (associatedNonFungibleTokens.isNotEmpty()) {
                item {
                    GrayBackgroundWrapper {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RadixTheme.dimensions.paddingDefault),
                            text = stringResource(id = R.string.authorizedDapps_dAppDetails_nfts),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
            items(associatedNonFungibleTokens) { nonFungibleToken ->
                GrayBackgroundWrapper {
                    StandardOneLineCard(
                        image = nonFungibleToken.imageUrl.toString(),
                        title = nonFungibleToken.localId.displayable,
                        modifier = Modifier
                            .shadow(elevation = 8.dp, shape = RadixTheme.shapes.roundedRectMedium)
                            .clip(RadixTheme.shapes.roundedRectMedium)
                            .fillMaxWidth()
                            .background(
                                RadixTheme.colors.white,
                                shape = RadixTheme.shapes.roundedRectMedium
                            )
                            .padding(
                                horizontal = RadixTheme.dimensions.paddingLarge,
                                vertical = RadixTheme.dimensions.paddingDefault
                            ),
                        showChevron = false
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }
        }
    }
}
