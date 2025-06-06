package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.displaySubtitle
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.extensions.formatted
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Resource

@Composable
fun PresentingProofsContent(
    badges: ImmutableList<Badge>,
    modifier: Modifier = Modifier,
    onInfoClick: (GlossaryItem) -> Unit,
    onClick: (Badge) -> Unit
) {
    if (badges.isNotEmpty()) {
        Column(
            modifier = modifier
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingSmall
                    )
                    .height(intrinsicSize = IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
            ) {
                Text(
                    text = stringResource(id = R.string.interactionReview_presentingHeading).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.textSecondary,
                    overflow = TextOverflow.Ellipsis,
                )
                InfoButton(
                    modifier = Modifier
                        .fillMaxHeight()
                        .height(1.dp),
                    text = stringResource(R.string.empty),
                    color = RadixTheme.colors.iconTertiary,
                    onClick = {
                        onInfoClick(GlossaryItem.badges)
                    }
                )
            }

            badges.forEach { badge ->
                Row(
                    modifier = Modifier
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingSmall
                        )
                        .throttleClickable { onClick(badge) },
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Thumbnail.Badge(
                        modifier = Modifier.size(34.dp),
                        badge = badge
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = badge.displayTitle(),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.text
                        )

                        badge.displaySubtitle()?.let { subtitle ->
                            Text(
                                text = subtitle,
                                style = RadixTheme.typography.body2Regular,
                                color = RadixTheme.colors.textSecondary
                            )
                        }
                    }

                    (badge.resource as? Resource.FungibleResource)?.ownedAmount?.let { amount ->
                        Text(
                            text = amount.formatted(),
                            style = RadixTheme.typography.secondaryHeader,
                            color = RadixTheme.colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                color = RadixTheme.colors.divider
            )
        }
    }
}

@Preview
@Composable
private fun PresentingProofsPreview() {
    RadixWalletPreviewTheme {
        PresentingProofsContent(
            badges = persistentListOf(),
            onInfoClick = {},
            onClick = {}
        )
    }
}
