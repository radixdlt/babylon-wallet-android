package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.extensions.formatted
import kotlinx.collections.immutable.ImmutableList
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Resource

@Composable
fun PresentingProofsContent(
    badges: ImmutableList<Badge>,
    modifier: Modifier = Modifier,
    onClick: (Badge) -> Unit
) {
    if (badges.isNotEmpty()) {
        Column(
            modifier = modifier.padding(RadixTheme.dimensions.paddingDefault)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingSmall
                )
            ) {
                Text(
                    text = stringResource(id = R.string.transactionReview_presentingHeading).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2,
                    overflow = TextOverflow.Ellipsis,
                )
                // TODO later
//                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
//                Icon(
//                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_info_outline),
//                    contentDescription = null,
//                    tint = RadixTheme.colors.gray3
//                )
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
                        badge.name?.let { name ->
                            Text(
                                text = name,
                                style = RadixTheme.typography.body1HighImportance,
                                color = RadixTheme.colors.gray1
                            )
                        }

                        (badge.resource as? Resource.NonFungibleResource)?.items?.firstOrNull()?.let { nft ->
                            val subtitle = remember(nft) {
                                nft.name ?: nft.localId.formatted()
                            }

                            Text(
                                text = subtitle,
                                style = RadixTheme.typography.body2Regular,
                                color = RadixTheme.colors.gray2
                            )
                        }
                    }

                    (badge.resource as? Resource.FungibleResource)?.ownedAmount?.let { amount ->
                        Text(
                            text = amount.formatted(),
                            style = RadixTheme.typography.secondaryHeader,
                            color = RadixTheme.colors.gray1,
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
                color = RadixTheme.colors.gray4
            )
        }
    }
}
