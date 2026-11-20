package com.babylon.wallet.android.presentation.transfer

import com.babylon.wallet.android.R
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(UsesSampleValues::class)
class BadgeRequirementMessageTest {

    private val token = ResourceAddress.sampleMainnet.xrd
    private val nft = ResourceAddress.sampleMainnet.nonFungibleGCMembership

    private fun error(reason: BadgeRequirementStatus.Error.Reason, restricted: ResourceAddress?) =
        BadgeRequirementStatus.Error(resource = null, reason = reason, restrictedResource = restricted)

    @Test
    fun `a missing badge names a token or an NFT`() {
        val reason = BadgeRequirementStatus.Error.Reason.MissingBadge
        assertEquals(R.string.assetTransfer_badge_error_missing_message, error(reason, token).messageRes())
        assertEquals(R.string.assetTransfer_badge_error_missing_nft_message, error(reason, nft).messageRes())
    }

    @Test
    fun `a resource nobody may withdraw names a token or an NFT`() {
        val reason = BadgeRequirementStatus.Error.Reason.NotWithdrawable
        assertEquals(R.string.assetTransfer_badge_error_notWithdrawable_message, error(reason, token).messageRes())
        assertEquals(R.string.assetTransfer_badge_error_notWithdrawable_nft_message, error(reason, nft).messageRes())
    }

    @Test
    fun `a failed check names no resource`() {
        assertEquals(
            R.string.assetTransfer_badge_error_checkFailed_message,
            error(BadgeRequirementStatus.Error.Reason.CheckFailed, null).messageRes()
        )
    }
}
