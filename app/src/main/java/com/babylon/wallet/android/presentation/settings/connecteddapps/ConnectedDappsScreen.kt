package com.babylon.wallet.android.presentation.settings.connecteddapps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.data.model.pernetwork.OnNetwork

@Composable
fun ConnectedDappsScreen(
    viewModel: ConnectedDappsViewModel,
    onBackClick: () -> Unit,
    onDappClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConnectedDappsContent(
        onBackClick = onBackClick,
        dapps = state.dapps,
        onDappClick = onDappClick,
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    )
}

@Composable
private fun ConnectedDappsContent(
    onBackClick: () -> Unit,
    dapps: ImmutableList<OnNetwork.ConnectedDapp>,
    onDappClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.connected_dapps),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        Divider(color = RadixTheme.colors.gray5)
        LazyColumn(
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.here_are_all_the_decentralized_apps),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                DappInfoLink(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            items(dapps) { dapp ->
                ConnectedDappCard(
                    modifier = Modifier
                        .shadow(elevation = 8.dp, shape = RadixTheme.shapes.roundedRectMedium)
                        .clip(RadixTheme.shapes.roundedRectMedium)
                        .throttleClickable {
                            onDappClick(dapp.dAppDefinitionAddress)
                        }
                        .fillMaxWidth()
                        .background(RadixTheme.colors.white, shape = RadixTheme.shapes.roundedRectMedium)
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingLarge,
                            vertical = RadixTheme.dimensions.paddingDefault
                        ),
                    dapp = dapp
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
        }
    }
}

@Composable
private fun DappInfoLink(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.ic_info_outline
            ),
            contentDescription = null,
            tint = RadixTheme.colors.blue1
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.what_is_dapp),
            style = RadixTheme.typography.body1StandaloneLink,
            color = RadixTheme.colors.blue1
        )
    }
}

@Composable
private fun ConnectedDappCard(dapp: OnNetwork.ConnectedDapp, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        AsyncImage(
            model = "",
            placeholder = painterResource(id = R.drawable.img_placeholder),
            fallback = painterResource(id = R.drawable.img_placeholder),
            error = painterResource(id = R.drawable.img_placeholder),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RadixTheme.shapes.circle)
        )
        Text(
            modifier = Modifier.weight(1f),
            text = dapp.displayName,
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right
            ),
            contentDescription = null,
            tint = RadixTheme.colors.gray1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectedDappsContentPreview() {
    RadixWalletTheme {
        ConnectedDappsContent(
            onBackClick = {},
            dapps = persistentListOf(),
            onDappClick = {}
        )
    }
}
