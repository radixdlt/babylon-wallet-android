package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.dashedCircle
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun ConnectedDAppsContent(
    connectedDApps: ImmutableList<DAppWithResources>,
    onDAppClick: (DAppWithResources) -> Unit,
    onUnknownDAppsClick: (ImmutableList<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (connectedDApps.isEmpty()) return

    var expanded by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .padding(bottom = RadixTheme.dimensions.paddingLarge)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = RadixTheme.dimensions.paddingMedium)
        ) {
            Row(
                modifier = Modifier
                    .clickable { expanded = !expanded }
            ) {
                Image(
                    modifier = Modifier
                        .size(24.dp)
                        .dashedCircle(24.dp),
                    painter = painterResource(id = DSR.ic_using_dapps),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(RadixTheme.colors.gray2),
                    contentScale = ContentScale.Inside
                )
                Text(
                    modifier = Modifier.padding(start = RadixTheme.dimensions.paddingMedium),
                    text = stringResource(id = R.string.transactionReview_usingDappsHeading).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2,
                    overflow = TextOverflow.Ellipsis,
                )
                val iconRes = if (expanded) {
                    com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_up
                } else {
                    com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_down
                }
                Icon(
                    painter = painterResource(id = iconRes),
                    tint = RadixTheme.colors.gray2,
                    contentDescription = "arrow"
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectMedium),
                horizontalAlignment = Alignment.End
            ) {
                val unverifiedDappsCount = connectedDApps.count { it.verified.not() }
                val verifiedDapps = connectedDApps.filter { it.verified }
                if (unverifiedDappsCount > 0) {
                    ConnectedDappRow(
                        modifier = Modifier.throttleClickable {
                            onUnknownDAppsClick(connectedDApps.map { it.componentAddresses }.flatten().toPersistentList())
                        },
                        dApp = null,
                        name = stringResource(id = R.string.transactionReview_unknownComponents, unverifiedDappsCount)
                    )
                }
                verifiedDapps.forEach { connectedDApp ->
                    ConnectedDappRow(
                        dApp = connectedDApp.dApp,
                        modifier = Modifier.throttleClickable {
                            onDAppClick(connectedDApp)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedDappRow(
    dApp: DApp?,
    name: String = dApp.displayName(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumbnail.DApp(
            modifier = Modifier.size(44.dp),
            dapp = dApp,
            shape = RadixTheme.shapes.roundedRectXSmall
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))
        Text(
            text = name,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectedDAppsContentPreview() {
    ConnectedDAppsContent(
        persistentListOf(
            DAppWithResources(
                dApp = DApp(
                    dAppAddress = "account_tdx_19jd32jd3928jd3892jd329"
                ),
                resources = DAppResources(
                    emptyList(),
                    emptyList()
                ),
                verified = true
            )
        ),
        onDAppClick = {},
        onUnknownDAppsClick = {}
    )
}
