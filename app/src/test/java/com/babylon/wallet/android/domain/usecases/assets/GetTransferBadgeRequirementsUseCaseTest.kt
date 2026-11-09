package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.presentation.transfer.BadgeRequirementStatus
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.RestrictedWithdrawal
import com.radixdlt.sargon.SargonOs
import com.radixdlt.sargon.TransferWithdrawProofs
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

    private fun sargonReturns(outcome: TransferWithdrawProofs) {
        coEvery { sargonOs.transferWithdrawProofs(account, listOf(restrictedToken)) } returns outcome
    }

    private fun gatewayKnows(resource: Resource) {
        coEvery {
            stateRepository.getResources(setOf(resource.address), account, any(), any())
        } returns Result.success(listOf(resource))
    }

    @Test
    fun `no restricted token needs nothing`() = runTest(testDispatcher) {
        sargonReturns(TransferWithdrawProofs(proofs = emptyList(), restrictedWithdrawals = emptyList()))

        val result = sut(account, setOf(restrictedToken)).getOrThrow()

        assertEquals(BadgeRequirementStatus.None, result.status)
        assertTrue(result.proofs.isEmpty())
    }

    @Test
    fun `a held badge gives its proof to the manifest`() = runTest(testDispatcher) {
        val proof = ResourceSpecifier.Fungible(resourceAddress = badge, amount = 1.toDecimal192())
        sargonReturns(
            TransferWithdrawProofs(
                proofs = listOf(proof),
                restrictedWithdrawals = listOf(
                    RestrictedWithdrawal(resource = restrictedToken, badges = listOf(badge), isSatisfied = true)
                )
            )
        )
        gatewayKnows(badgeResource)

        val result = sut(account, setOf(restrictedToken)).getOrThrow()

        assertEquals(BadgeRequirementStatus.Success(badgeResource), result.status)
        assertEquals(listOf(proof), result.proofs)
    }

    @Test
    fun `a missing badge is shown`() = runTest(testDispatcher) {
        sargonReturns(
            TransferWithdrawProofs(
                proofs = emptyList(),
                restrictedWithdrawals = listOf(
                    RestrictedWithdrawal(resource = restrictedToken, badges = listOf(badge), isSatisfied = false)
                )
            )
        )
        gatewayKnows(badgeResource)

        val result = sut(account, setOf(restrictedToken)).getOrThrow()

        assertEquals(
            BadgeRequirementStatus.Error(badgeResource, BadgeRequirementStatus.Error.Reason.MissingBadge),
            result.status
        )
    }

    @Test
    fun `a token nobody may withdraw is shown`() = runTest(testDispatcher) {
        val token = Resource.FungibleResource(address = restrictedToken, ownedAmount = null)
        sargonReturns(
            TransferWithdrawProofs(
                proofs = emptyList(),
                restrictedWithdrawals = listOf(
                    RestrictedWithdrawal(resource = restrictedToken, badges = emptyList(), isSatisfied = false)
                )
            )
        )
        gatewayKnows(token)

        val result = sut(account, setOf(restrictedToken)).getOrThrow()

        assertEquals(
            BadgeRequirementStatus.Error(token, BadgeRequirementStatus.Error.Reason.NotWithdrawable),
            result.status
        )
    }

    @Test
    fun `a failure of Sargon is reported`() = runTest(testDispatcher) {
        coEvery { sargonOs.transferWithdrawProofs(any(), any()) } throws IllegalStateException("no connection")

        assertTrue(sut(account, setOf(restrictedToken)).isFailure)
    }
}
