@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.TwoRowsTopAppBar
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.metadata.MetadataType

@Composable
fun TransactionPreviewHeader(
    modifier: Modifier = Modifier,
    isPreAuthorization: Boolean,
    isRawManifestPreviewable: Boolean,
    isRawManifestVisible: Boolean,
    proposingDApp: State.ProposingDApp?,
    onBackClick: () -> Unit,
    onRawManifestClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val title = stringResource(
        id = if (isPreAuthorization) {
            R.string.preAuthorizationReview_title
        } else {
            R.string.transactionReview_title
        }
    )
    val isToggleButtonVisible = !isPreAuthorization && isRawManifestPreviewable
    TwoRowsTopAppBar(
        modifier = modifier,
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = RadixTheme.dimensions.paddingXXLarge)
                    .padding(end = RadixTheme.dimensions.paddingXLarge)
            ) {
                val someDApp = remember(proposingDApp) { (proposingDApp as? State.ProposingDApp.Some) }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CompositionLocalProvider(
                            value = LocalDensity provides Density(
                                density = LocalDensity.current.density,
                                fontScale = 1f
                            )
                        ) {
                            Text(
                                modifier = Modifier.weight(1.5f),
                                text = title,
                                color = RadixTheme.colors.gray1,
                                textAlign = TextAlign.Start,
                                maxLines = 2,
                            )
                        }
                    }

                    if (proposingDApp !is State.ProposingDApp.Internal) {
                        val dApp = (proposingDApp as? State.ProposingDApp.Some)?.dApp

                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dAppName = dApp.displayName()

                            if (dApp?.iconUrl != null) {
                                Thumbnail.DApp(
                                    modifier = Modifier
                                        .size(24.dp),
                                    dapp = someDApp?.dApp,
                                    shape = RadixTheme.shapes.roundedRectSmall
                                )

                                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))
                            }

                            Text(
                                text = stringResource(id = R.string.interactionReview_subtitle, dAppName),
                                style = RadixTheme.typography.body2HighImportance,
                                color = RadixTheme.colors.gray1,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        titleTextStyle = RadixTheme.typography.title,
        smallTitle = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        },
        smallTitleTextStyle = RadixTheme.typography.secondaryHeader,
        navigationIcon = {
            IconButton(
                modifier = Modifier
                    .padding(start = RadixTheme.dimensions.paddingDefault),
                onClick = onBackClick
            ) {
                Icon(
                    painterResource(
                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_close
                    ),
                    tint = RadixTheme.colors.gray1,
                    contentDescription = "close"
                )
            }
        },
        actions = {
            if (isToggleButtonVisible) {
                TransactionRawManifestToggle(
                    modifier = Modifier.padding(end = RadixTheme.dimensions.paddingXLarge),
                    isToggleOn = isRawManifestVisible,
                    onRawManifestClick = onRawManifestClick
                )
            } else {
                // Need to add the same space as the navigation icon to center the title
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault + 40.dp))
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        titleBottomPadding = RadixTheme.dimensions.paddingSemiLarge,
        windowInsets = WindowInsets.statusBarsAndBanner,
        maxHeight = 200.dp,
        pinnedHeight = 82.dp,
        scrollBehavior = scrollBehavior
    )
}

@Preview(showBackground = true)
@UsesSampleValues
@Composable
fun TransactionPreviewHeaderPreview() {
    RadixWalletTheme {
        TransactionPreviewHeader(
            isPreAuthorization = false,
            proposingDApp = State.ProposingDApp.Some(
                dApp = DApp(
                    dAppAddress = AccountAddress.sampleMainnet(),
                    metadata = listOf(
                        rdx.works.core.domain.resources.metadata.Metadata.Primitive(
                            key = rdx.works.core.domain.resources.ExplicitMetadataKey.ICON_URL.key,
                            valueType = MetadataType.Url,
                            value = "https://example.com/icon.png"
                        )
                    )
                )
            ),
            isRawManifestPreviewable = true,
            isRawManifestVisible = false,
            onBackClick = {},
            onRawManifestClick = {},
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        )
    }
}
