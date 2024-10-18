package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.usecases.transaction.TransactionConfig
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.babylon.wallet.android.fakes.StateRepositoryFake
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AccountOrAddressOf
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.PerAssetFungibleResource
import com.radixdlt.sargon.PerAssetFungibleTransfer
import com.radixdlt.sargon.PerAssetTransfers
import com.radixdlt.sargon.PerAssetTransfersOfFungibleResource
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.perAssetTransfers
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase

class SearchFeePayersUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities()
    private val account1 = profile.activeAccountsOnCurrentNetwork[0]
    private val profileUseCase = GetProfileUseCase(
        profileRepository = FakeProfileRepository(profile),
        dispatcher = testDispatcher
    )
    private var useCase = createUseCase()

    @Test
    fun `when account with enough xrd exists, returns the selected fee payer`() =
        testScope.runTest {
            val manifestData = feePayerCandidates(account1)

            val result = useCase(manifestData, TransactionConfig.DEFAULT_LOCK_FEE.toDecimal192()).getOrThrow()

            assertEquals(
                TransactionFeePayers(
                    selectedAccountAddress = account1.address,
                    candidates = listOf(
                        TransactionFeePayers.FeePayerCandidate(account1, 100.toDecimal192(), true)
                    )
                ),
                result
            )
        }

    @Test
    fun `when account with not enough xrd exists, returns null fee payer and hasEnoughBalance false`() =
        testScope.runTest {
            val manifestData = feePayerCandidates(account1)

            useCase = createUseCase(firstAccountBalance = 0.1)
            val result = useCase(manifestData, TransactionConfig.DEFAULT_LOCK_FEE.toDecimal192()).getOrThrow()

            assertEquals(
                TransactionFeePayers(
                    selectedAccountAddress = null,
                    candidates = listOf(
                        TransactionFeePayers.FeePayerCandidate(account1, 0.1.toDecimal192(), false)
                    )
                ),
                result
            )
        }

    @Test
    fun `when account with xrd does not exist, returns null fee payer and no candidates`() =
        testScope.runTest {
            val manifestData = feePayerCandidates(account1)

            useCase = createUseCase(firstAccountBalance = 0.0)
            val result = useCase(manifestData, 200.toDecimal192()).getOrThrow()

            assertEquals(
                TransactionFeePayers(
                    selectedAccountAddress = null,
                    candidates = emptyList()
                ),
                result
            )
        }

    private fun createUseCase(firstAccountBalance: Double = 100.0): SearchFeePayersUseCase {
        return SearchFeePayersUseCase(
            profileUseCase = profileUseCase,
            stateRepository = object : StateRepositoryFake() {
                override suspend fun getOwnedXRD(accounts: List<Account>): Result<Map<Account, Decimal192>> {
                    return Result.success(accounts.associateWith {
                        if (it == accounts.first()) {
                            firstAccountBalance.toDecimal192()
                        } else {
                            0.toDecimal192()
                        }
                    })
                }
            }
        )
    }

    companion object {
        private fun feePayerCandidates(
            account: Account
        ) = listOf(
            account.address
        )
    }

}
