package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Badge
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import kotlinx.collections.immutable.ImmutableList

@Composable
fun PresentingProofsContent(
    badges: ImmutableList<Badge>,
    modifier: Modifier = Modifier
) {
    if (badges.isNotEmpty()) {
        Column(modifier = modifier.padding(RadixTheme.dimensions.paddingDefault)) {
            Row(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
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

            Column {
                badges.forEach { badge ->
                    Row(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Thumbnail.Badge(
                            modifier = Modifier.size(24.dp),
                            badge = badge
                        )
                        Text(
                            text = badge.name.orEmpty(),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.gray1
                        )
                    }

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }

            Divider(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                color = RadixTheme.colors.gray4
            )
        }
    }
}
