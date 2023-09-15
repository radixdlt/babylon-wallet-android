package com.babylon.wallet.android.presentation.transaction.composables

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ConnectedDAppsContent(
    connectedDApps: ImmutableList<DAppWithMetadataAndAssociatedResources>,
    onDAppClick: (DAppWithMetadataAndAssociatedResources) -> Unit,
    showStrokeLine: Boolean,
    modifier: Modifier = Modifier
) {
    if (connectedDApps.isEmpty()) return

    var expanded by rememberSaveable { mutableStateOf(true) }

    Box(
        modifier = modifier
            .padding(
                bottom = RadixTheme.dimensions.paddingMedium
            )
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
        ) {
            Text(
                text = stringResource(id = R.string.transactionReview_usingDappsHeading),
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
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            val unverifiedDappsCount = connectedDApps.count { it.verified.not() }
            val verifiedDapps = connectedDApps.filter { it.verified }
            if (unverifiedDappsCount > 0) {
                ConnectedDappRow(null, stringResource(id = R.string.transactionReview_unknownComponents, unverifiedDappsCount))
            }
            verifiedDapps.forEach { connectedDApp ->
                ConnectedDappRow(
                    connectedDApp.dAppWithMetadata.iconUrl,
                    connectedDApp.dAppWithMetadata.displayName(),
                    modifier = Modifier
                        .throttleClickable {
                            onDAppClick(connectedDApp)
                        }
                )
            }
        }
    }

    if (showStrokeLine) {
        StrokeLine(height = 20.dp)
    }
}

@Composable
private fun ConnectedDappRow(
    imageUrl: Uri?,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val strokeWidth = with(LocalDensity.current) { 2.dp.toPx() }
    val cornerRadius = with(LocalDensity.current) { 8.dp.toPx() }
    val strokeInterval = with(LocalDensity.current) { 6.dp.toPx() }
    val strokeColor = RadixTheme.colors.gray3
    Row(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = strokeColor,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect
                            .dashPathEffect(
                                floatArrayOf(strokeInterval, strokeInterval),
                                0f
                            )
                    ),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val placeholder = painterResource(id = R.drawable.ic_unknown_component)
        AsyncImage(
            model = rememberImageUrl(
                fromUrl = imageUrl,
                size = ImageSize.SMALL
            ),
            placeholder = placeholder,
            fallback = placeholder,
            error = placeholder,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RadixTheme.shapes.circle)
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))
        Text(
            text = displayName,
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
            DAppWithMetadataAndAssociatedResources(
                dAppWithMetadata = DAppWithMetadata(
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
        showStrokeLine = true
    )
}
