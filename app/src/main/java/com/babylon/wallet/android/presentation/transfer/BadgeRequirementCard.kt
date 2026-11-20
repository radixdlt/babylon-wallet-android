package com.babylon.wallet.android.presentation.transfer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.isNonFungible
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Resource

/**
 * Shows whether the account can withdraw the restricted tokens of a transfer. Nothing is shown while
 * the check runs (the send button shows its progress) or when no badge is involved.
 */
@Composable
fun BadgeRequirementCard(
    status: BadgeRequirementStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (status) {
        is BadgeRequirementStatus.Success -> BadgeRequirementCardContent(
            modifier = modifier,
            borderColor = RadixTheme.colors.ok,
            resource = status.resource,
            title = status.resource?.name?.takeIf { it.isNotBlank() }
                ?: stringResource(id = R.string.assetTransfer_badge_unknown),
            statusIcon = DSR.ic_check_circle_outline,
            statusIconTint = RadixTheme.colors.ok,
            message = stringResource(id = R.string.assetTransfer_badge_success_title)
        )

        is BadgeRequirementStatus.Error -> BadgeRequirementCardContent(
            modifier = modifier,
            borderColor = RadixTheme.colors.error,
            resource = status.resource,
            title = status.title(),
            statusIcon = DSR.ic_warning_error,
            statusIconTint = RadixTheme.colors.error,
            message = stringResource(id = status.messageRes()),
            onRetry = onRetry.takeIf { status.reason == BadgeRequirementStatus.Error.Reason.CheckFailed }
        )

        BadgeRequirementStatus.None, BadgeRequirementStatus.Loading -> Unit
    }
}

/**
 * The badge's name, or the restricted token's name when nobody may withdraw it. When the token has no
 * name only its address is shown; when the rules could not be checked there is nothing to name.
 */
@Composable
private fun BadgeRequirementStatus.Error.title(): String? {
    val name = resource?.name?.takeIf { it.isNotBlank() }
    return when (reason) {
        BadgeRequirementStatus.Error.Reason.MissingBadge -> name ?: stringResource(id = R.string.assetTransfer_badge_unknown)
        BadgeRequirementStatus.Error.Reason.NotWithdrawable -> name
        BadgeRequirementStatus.Error.Reason.CheckFailed -> null
    }
}

@Suppress("LongParameterList")
@Composable
private fun BadgeRequirementCardContent(
    borderColor: Color,
    resource: Resource?,
    title: String?,
    @DrawableRes statusIcon: Int,
    statusIconTint: Color,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultCardShadow(shape = RadixTheme.shapes.roundedRectMedium)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .background(
                color = RadixTheme.colors.cardSecondary,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        when (resource) {
            is Resource.FungibleResource -> Thumbnail.Fungible(
                modifier = Modifier.size(44.dp),
                token = resource
            )
            is Resource.NonFungibleResource -> Thumbnail.NonFungible(
                modifier = Modifier.size(44.dp),
                collection = resource
            )
            null -> Icon(
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
            title?.let {
                Text(
                    text = it,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.text
                )
            }
            resource?.address?.let { address ->
                ActionableAddressView(
                    address = Address.Resource(address),
                    textStyle = RadixTheme.typography.body2HighImportance,
                    textColor = RadixTheme.colors.textSecondary,
                    iconColor = RadixTheme.colors.textSecondary
                )
            }
            if (resource != null) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = statusIcon),
                    tint = statusIconTint,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXXSmall))
                Text(
                    text = message,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.textSecondary
                )
            }
            onRetry?.let {
                RadixTextButton(
                    text = stringResource(id = R.string.common_retry),
                    onClick = it,
                    isWithoutPadding = true,
                    throttleClicks = true
                )
            }
        }
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
            ),
            onRetry = {}
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
            ),
            onRetry = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun BadgeRequirementCardErrorNotWithdrawablePreview() {
    RadixWalletPreviewTheme {
        BadgeRequirementCard(
            status = BadgeRequirementStatus.Error(
                resource = Badge.sample.other().resource,
                reason = BadgeRequirementStatus.Error.Reason.NotWithdrawable,
                restrictedResource = Badge.sample.other().resource.address
            ),
            onRetry = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun BadgeRequirementCardErrorCheckFailedPreview() {
    RadixWalletPreviewTheme {
        BadgeRequirementCard(
            status = BadgeRequirementStatus.Error(
                resource = null,
                reason = BadgeRequirementStatus.Error.Reason.CheckFailed
            ),
            onRetry = {}
        )
    }
}

/** The message names what the restricted resource really is: a token or an NFT. */
@StringRes
internal fun BadgeRequirementStatus.Error.messageRes(): Int {
    val isNft = restrictedResource?.isNonFungible == true
    return when (reason) {
        BadgeRequirementStatus.Error.Reason.MissingBadge -> if (isNft) {
            R.string.assetTransfer_badge_error_missing_nft_message
        } else {
            R.string.assetTransfer_badge_error_missing_message
        }
        BadgeRequirementStatus.Error.Reason.NotWithdrawable -> if (isNft) {
            R.string.assetTransfer_badge_error_notWithdrawable_nft_message
        } else {
            R.string.assetTransfer_badge_error_notWithdrawable_message
        }
        BadgeRequirementStatus.Error.Reason.CheckFailed -> R.string.assetTransfer_badge_error_checkFailed_message
    }
}
