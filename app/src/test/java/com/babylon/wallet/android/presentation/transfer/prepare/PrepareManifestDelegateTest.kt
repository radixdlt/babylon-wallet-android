package com.babylon.wallet.android.presentation.transfer.prepare

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.presentation.transfer.BadgeRequirementStatus
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.core.domain.resources.Resource
import rdx.works.profile.data.repository.MnemonicRepository

@OptIn(UsesSampleValues::class)
class PrepareManifestDelegateTest {

    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val mnemonicRepository = mockk<MnemonicRepository>().also {
        coEvery { it.mnemonicExist(any()) } returns true
    }

    private val badge = ResourceAddress.sampleMainnet.random()
    private val proof = ResourceSpecifier.Fungible(resourceAddress = badge, amount = 1.toDecimal192())

    @Test
    fun `the badges are proven when the check succeeded`() = runTest {
        val instructions = submit(badgeRequirementStatus = BadgeRequirementStatus.Success(resource = null))

        assertTrue(instructions, instructions.contains("create_proof_of_amount"))
        assertTrue(instructions.contains(badge.string))
    }

    @Test
    fun `no badge is proven when the check did not succeed`() = runTest {
        listOf(
            BadgeRequirementStatus.None,
            BadgeRequirementStatus.Loading,
            BadgeRequirementStatus.Error(resource = null, reason = BadgeRequirementStatus.Error.Reason.CheckFailed)
        ).forEach { status ->
            val instructions = submit(badgeRequirementStatus = status)

            assertFalse(status.toString(), instructions.contains("create_proof"))
        }
    }

    private suspend fun TestScope.submit(badgeRequirementStatus: BadgeRequirementStatus): String {
        val request = slot<DappToWalletInteraction>()
        coEvery { incomingRequestRepository.add(capture(request)) } just Runs

        val delegate = PrepareManifestDelegate(
            incomingRequestRepository = incomingRequestRepository,
            mnemonicRepository = mnemonicRepository
        )
        delegate(
            scope = this,
            state = MutableStateFlow(
                TransferViewModel.State(
                    fromAccount = Account.sampleMainnet(),
                    targetAccounts = persistentListOf(
                        TargetAccount.Owned(
                            account = Account.sampleMainnet.other(),
                            id = "target",
                            spendingAssets = persistentSetOf(
                                SpendingAsset.Fungible(
                                    resource = Resource.FungibleResource(
                                        address = ResourceAddress.sampleMainnet.candy,
                                        ownedAmount = 10.toDecimal192()
                                    ),
                                    amountString = "1"
                                )
                            )
                        )
                    ),
                    badgeRequirementStatus = badgeRequirementStatus,
                    resolvedRequiredBadges = listOf(proof)
                )
            )
        )

        delegate.onSubmit()

        return (request.captured as TransactionRequest).unvalidatedManifestData.instructions
    }
}
