package com.babylon.wallet.android.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Resource

@Composable
fun BadgeRequirementCard(
    status: BadgeRequirementStatus,
    modifier: Modifier = Modifier
) {
    if (status is BadgeRequirementStatus.None) return

    val borderStrokeWidth = 1.dp
    val borderColor = when (status) {
        is BadgeRequirementStatus.Loading -> RadixTheme.colors.divider
        is BadgeRequirementStatus.Success -> RadixTheme.colors.ok
        is BadgeRequirementStatus.Error -> RadixTheme.colors.error
        else -> RadixTheme.colors.divider
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultCardShadow(shape = RadixTheme.shapes.roundedRectMedium)
            .border(
                width = borderStrokeWidth,
                color = borderColor,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .background(
                color = RadixTheme.colors.gray5,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(RadixTheme.dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        when (status) {
            is BadgeRequirementStatus.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = RadixTheme.colors.iconSecondary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))
                    Text(
                        text = stringResource(id = R.string.assetTransfer_badge_checking),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.textSecondary
                    )
                }
            }
            is BadgeRequirementStatus.Success -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                ) {
                    status.resource?.let { resource ->
                        Thumbnail.Badge(
                            badge = Badge(resource),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
                    ) {
                        Text(
                            text = status.resource?.name?.takeIf { it.isNotEmpty() } ?: "Unknown Badge",
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.text
                        )
                        status.resource?.address?.let { address ->
                            ActionableAddressView(
                                address = Address.Resource(address),
                                textStyle = RadixTheme.typography.body2HighImportance,
                                textColor = RadixTheme.colors.textSecondary,
                                iconColor = RadixTheme.colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                painter = painterResource(id = DSR.ic_check_circle_outline),
                                tint = RadixTheme.colors.ok,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXXSmall))
                            Text(
                                text = stringResource(id = R.string.assetTransfer_badge_success_title),
                                style = RadixTheme.typography.body2Regular,
                                color = RadixTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
            is BadgeRequirementStatus.Error -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                ) {
                    val resource = status.resource
                    if (resource != null) {
                        Thumbnail.Badge(
                            badge = Badge(resource),
                            modifier = Modifier.size(44.dp)
                        )
                    } else {
                        Icon(
                            modifier = Modifier.size(44.dp),
                            painter = painterResource(id = DSR.ic_warning_error),
                            tint = RadixTheme.colors.error,
                            contentDescription = null
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
                    ) {
                        Text(
                            text = status.resource?.name?.takeIf { it.isNotEmpty() } ?: "Unknown Badge",
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.text
                        )
                        status.resource?.address?.let { address ->
                            ActionableAddressView(
                                address = Address.Resource(address),
                                textStyle = RadixTheme.typography.body2HighImportance,
                                textColor = RadixTheme.colors.textSecondary,
                                iconColor = RadixTheme.colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                painter = painterResource(id = DSR.ic_warning_error),
                                tint = RadixTheme.colors.error,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXXSmall))
                            Text(
                                text = when (status.reason) {
                                    BadgeRequirementStatus.Error.Reason.MissingBadge -> stringResource(id = R.string.assetTransfer_badge_error_missing_message)
                                    BadgeRequirementStatus.Error.Reason.InsufficientBalance -> stringResource(id = R.string.assetTransfer_badge_error_insufficient_message)
                                    BadgeRequirementStatus.Error.Reason.RuleParsingError -> stringResource(id = R.string.assetTransfer_badge_error_parsing_message)
                                },
                                style = RadixTheme.typography.body2Regular,
                                color = RadixTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun BadgeRequirementCardLoadingPreview() {
    RadixWalletPreviewTheme {
        BadgeRequirementCard(
            status = BadgeRequirementStatus.Loading
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun BadgeRequirementCardSuccessPreview() {
    RadixWalletPreviewTheme {
        BadgeRequirementCard(
            status = BadgeRequirementStatus.Success(
                resource = Badge.sample().resource
            )
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun BadgeRequirementCardErrorMissingPreview() {
    RadixWalletPreviewTheme {
        BadgeRequirementCard(
            status = BadgeRequirementStatus.Error(
                resource = Badge.sample().resource,
                reason = BadgeRequirementStatus.Error.Reason.MissingBadge
            )
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun BadgeRequirementCardErrorInsufficientPreview() {
    RadixWalletPreviewTheme {
        BadgeRequirementCard(
            status = BadgeRequirementStatus.Error(
                resource = Badge.sample.other().resource,
                reason = BadgeRequirementStatus.Error.Reason.InsufficientBalance
            )
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun BadgeRequirementCardErrorParsingPreview() {
    RadixWalletPreviewTheme {
        BadgeRequirementCard(
            status = BadgeRequirementStatus.Error(
                resource = null,
                reason = BadgeRequirementStatus.Error.Reason.RuleParsingError
            )
        )
    }
}

