package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.InvolvedComponentDetails
import com.babylon.wallet.android.presentation.ui.composables.assets.dashedCircleBorder
import com.babylon.wallet.android.presentation.ui.composables.displayName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.domain.DApp

@Composable
fun ConnectedDAppsContent(
    connectedDApps: ImmutableList<Pair<String, DApp?>>,
    onDAppClick: (DApp) -> Unit,
    onUnknownComponentsClick: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (connectedDApps.isEmpty()) return

    var expanded by rememberSaveable { mutableStateOf(true) }

    Column {
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
                        .dashedCircleBorder(RadixTheme.colors.gray3),
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
            ) {
                val (verifiedDApps, unknownComponents) = remember(connectedDApps) {
                    connectedDApps.mapNotNull { it.second } to connectedDApps.mapNotNull { if (it.second == null) it.first else null }
                }

                verifiedDApps.forEach { dApp ->
                    InvolvedComponentDetails(
                        iconSize = 44.dp,
                        dApp = dApp,
                        text = dApp.displayName(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDAppClick(dApp) }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                }
                if (unknownComponents.isNotEmpty()) {
                    InvolvedComponentDetails(
                        iconSize = 44.dp,
                        dApp = null,
                        text = stringResource(id = R.string.transactionReview_unknownComponents, unknownComponents.size),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUnknownComponentsClick(unknownComponents) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectedDAppsContentPreview() {
    ConnectedDAppsContent(
        persistentListOf(
            "component_tdx_19jd32jd3928jd3892jd329" to DApp(dAppAddress = "account_tdx_19jd32jd3928jd3892jd329"),
            "component_tdx_19jd32jd3928jd3892jd330" to null
        ),
        onDAppClick = {},
        onUnknownComponentsClick = {}
    )
}
