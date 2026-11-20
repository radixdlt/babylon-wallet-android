package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.presentation.transfer.BadgeRequirementStatus
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.RestrictedWithdrawal
import com.radixdlt.sargon.SargonOs
import com.radixdlt.sargon.RestrictedWithdrawalStatus
import com.radixdlt.sargon.TransferRequiredBadges
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.core.domain.resources.Resource

@OptIn(UsesSampleValues::class)
class GetTransferBadgeRequirementsUseCaseTest {

    private val sargonOs = mockk<SargonOs>()
    private val sargonOsManager = mockk<SargonOsManager>().also {
        every { it.sargonOs } returns sargonOs
    }
    private val stateRepository = mockk<StateRepository>()
    private val testDispatcher = StandardTestDispatcher()

    private val sut = GetTransferBadgeRequirementsUseCase(
        sargonOsManager = sargonOsManager,
        stateRepository = stateRepository,
        dispatcher = testDispatcher
    )

    private val account = AccountAddress.sampleMainnet()
    private val restrictedToken = ResourceAddress.sampleMainnet.xrd
    private val badge = ResourceAddress.sampleMainnet.candy
    private val badgeResource = Resource.FungibleResource(address = badge, ownedAmount = 1.toDecimal192())

    private fun sargonReturns(outcome: TransferRequiredBadges) {
        coEvery { sargonOs.transferRequiredBadges(account, listOf(restrictedToken)) } returns outcome
    }

    private fun gatewayKnows(resource: Resource) {
        coEvery {
            stateRepository.getResources(setOf(resource.address), account, any(), any())
        } returns Result.success(listOf(resource))
    }

    @Test
    fun `no restricted token needs nothing`() = runTest(testDispatcher) {
        sargonReturns(TransferRequiredBadges(requiredBadges = emptyList(), restrictedWithdrawals = emptyList()))

        val result = sut(account, setOf(restrictedToken)).getOrThrow()

        assertEquals(BadgeRequirementStatus.None, result.status)
        assertTrue(result.requiredBadges.isEmpty())
    }

    @Test
    fun `a held badge gives its proof to the manifest`() = runTest(testDispatcher) {
        val proof = ResourceSpecifier.Fungible(resourceAddress = badge, amount = 1.toDecimal192())
        sargonReturns(
            TransferRequiredBadges(
                requiredBadges = listOf(proof),
                restrictedWithdrawals = listOf(
                    RestrictedWithdrawal(resource = restrictedToken, badges = listOf(badge), status = RestrictedWithdrawalStatus.SATISFIED)
                )
            )
        )
        gatewayKnows(badgeResource)

        val result = sut(account, setOf(restrictedToken)).getOrThrow()

        assertEquals(BadgeRequirementStatus.Success(badgeResource), result.status)
        assertEquals(listOf(proof), result.requiredBadges)
    }

    @Test
    fun `a missing badge is shown`() = runTest(testDispatcher) {
        sargonReturns(
            TransferRequiredBadges(
                requiredBadges = emptyList(),
                restrictedWithdrawals = listOf(
                    RestrictedWithdrawal(
                        resource = restrictedToken,
                        badges = listOf(badge),
                        status = RestrictedWithdrawalStatus.MISSING_BADGES
                    )
                )
            )
        )
        gatewayKnows(badgeResource)

        val result = sut(account, setOf(restrictedToken)).getOrThrow()

        assertEquals(
            BadgeRequirementStatus.Error(
                resource = badgeResource,
                reason = BadgeRequirementStatus.Error.Reason.MissingBadge,
                restrictedResource = restrictedToken
            ),
            result.status
        )
    }

    @Test
    fun `a token nobody may withdraw is shown`() = runTest(testDispatcher) {
        val token = Resource.FungibleResource(address = restrictedToken, ownedAmount = null)
        sargonReturns(
            TransferRequiredBadges(
                requiredBadges = emptyList(),
                restrictedWithdrawals = listOf(
                    RestrictedWithdrawal(
                        resource = restrictedToken,
                        badges = emptyList(),
                        status = RestrictedWithdrawalStatus.NOT_WITHDRAWABLE
                    )
                )
            )
        )
        gatewayKnows(token)

        val result = sut(account, setOf(restrictedToken)).getOrThrow()

        assertEquals(
            BadgeRequirementStatus.Error(
                resource = token,
                reason = BadgeRequirementStatus.Error.Reason.NotWithdrawable,
                restrictedResource = restrictedToken
            ),
            result.status
        )
    }

    @Test
    fun `a rule a transfer cannot meet is shown as not withdrawable, whatever the badges`() = runTest(testDispatcher) {
        val token = Resource.FungibleResource(address = restrictedToken, ownedAmount = null)
        sargonReturns(
            TransferRequiredBadges(
                requiredBadges = emptyList(),
                restrictedWithdrawals = listOf(
                    RestrictedWithdrawal(
                        resource = restrictedToken,
                        badges = listOf(badge),
                        status = RestrictedWithdrawalStatus.NOT_WITHDRAWABLE
                    )
                )
            )
        )
        gatewayKnows(token)

        val result = sut(account, setOf(restrictedToken)).getOrThrow()

        assertEquals(
            BadgeRequirementStatus.Error(
                resource = token,
                reason = BadgeRequirementStatus.Error.Reason.NotWithdrawable,
                restrictedResource = restrictedToken
            ),
            result.status
        )
    }

    @Test
    fun `an NFT the Gateway does not know is drawn as an NFT collection`() = runTest(testDispatcher) {
        val nft = ResourceAddress.sampleMainnet.nonFungibleGCMembership
        coEvery { sargonOs.transferRequiredBadges(account, listOf(nft)) } returns TransferRequiredBadges(
            requiredBadges = emptyList(),
            restrictedWithdrawals = listOf(
                RestrictedWithdrawal(resource = nft, badges = emptyList(), status = RestrictedWithdrawalStatus.NOT_WITHDRAWABLE)
            )
        )
        coEvery { stateRepository.getResources(setOf(nft), account, any(), any()) } returns Result.failure(RuntimeException())

        val status = sut(account, setOf(nft)).getOrThrow().status as BadgeRequirementStatus.Error

        assertEquals(Resource.NonFungibleResource(address = nft, amount = 0, items = emptyList()), status.resource)
        assertEquals(BadgeRequirementStatus.Error.Reason.NotWithdrawable, status.reason)
    }

    @Test
    fun `a rule met by the account's own signature shows no badge`() = runTest(testDispatcher) {
        sargonReturns(
            TransferRequiredBadges(
                requiredBadges = emptyList(),
                restrictedWithdrawals = listOf(
                    RestrictedWithdrawal(
                        resource = restrictedToken,
                        badges = emptyList(),
                        status = RestrictedWithdrawalStatus.SATISFIED
                    )
                )
            )
        )

        val result = sut(account, setOf(restrictedToken)).getOrThrow()

        assertEquals(BadgeRequirementStatus.None, result.status)
        assertTrue(result.requiredBadges.isEmpty())
    }

    @Test
    fun `a failure of Sargon is reported`() = runTest(testDispatcher) {
        coEvery { sargonOs.transferRequiredBadges(any(), any()) } throws IllegalStateException("no connection")

        assertTrue(sut(account, setOf(restrictedToken)).isFailure)
    }
}
