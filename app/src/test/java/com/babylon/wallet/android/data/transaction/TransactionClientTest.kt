package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.usecases.GetAccountResourcesUseCase
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.ManifestAstValue
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.repository.AccountRepository
import rdx.works.profile.data.repository.ProfileDataSource
import java.math.BigDecimal

internal class TransactionClientTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val profileDataSource = mockk<ProfileDataSource>()
    private val accountRepository = mockk<AccountRepository>()
    private val getAccountResourceUseCase = mockk<GetAccountResourcesUseCase>()
    private val cache = mockk<HttpCache>()

    private lateinit var transactionClient: TransactionClient

    @Before
    fun setUp() {
        transactionClient = TransactionClient(
            transactionRepository,
            profileDataSource,
            accountRepository,
            getAccountResourceUseCase,
            cache
        )
        coEvery { profileDataSource.getCurrentNetworkId() } returns Radix.Network.nebunet.networkId()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when address exists, finds address involved & signing for set metadata manifest`() =
        runTest {
            val expectedAddress =
                "account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjqn7q2py8ej"
            coEvery {
                getAccountResourceUseCase.getAccounts(
                    addresses = listOf(expectedAddress),
                    isRefreshing = true
                )
            } returns Result.Success(
                listOf(SampleDataProvider().sampleAccountResource(expectedAddress))
            )

            var manifest = ManifestBuilder().addInstruction(
                Instruction.SetMetadata(
                    entityAddress = ManifestAstValue.Address(expectedAddress),
                    ManifestAstValue.String("name"),
                    ManifestAstValue.Enum("RadixDashboard")
                )
            ).build()

            val addressesInvolved = transactionClient.getAddressesInvolvedInATransaction(manifest)
            val addressToLockFee =
                transactionClient.selectAccountAddressToLockFee(addressesInvolved)
            manifest =
                transactionClient.addLockFeeInstructionToManifest(manifest, addressToLockFee!!)
            val addressesNeededToSign = transactionClient.getAddressesNeededToSign(manifest)


            Assert.assertEquals(1, addressesNeededToSign.size)
            Assert.assertEquals(
                expectedAddress,
                addressesNeededToSign.first()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when given address has no funds but there is another address with funds, use the other address for the transaction`() =
        runTest {
            val addressWithNoFunds =
                "account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjqn7q2py8ej"
            val addressWithFunds =
                "account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjdgt5674682"
            coEvery {
                getAccountResourceUseCase.getAccounts(
                    addresses = listOf(addressWithNoFunds),
                    isRefreshing = true
                )
            } returns Result.Success(
                listOf(
                    SampleDataProvider().sampleAccountResource(
                        address = addressWithNoFunds,
                        withFungibleTokens = SampleDataProvider().sampleFungibleTokens(
                            ownerAddress = addressWithNoFunds,
                            amount = BigDecimal.ZERO to "XRD"
                        )
                    )
                )
            )
            coEvery {
                getAccountResourceUseCase.getAccountsFromProfile(isRefreshing = true)
            } returns Result.Success(
                listOf(
                    SampleDataProvider().sampleAccountResource(
                        address = addressWithNoFunds,
                        withFungibleTokens = SampleDataProvider().sampleFungibleTokens(
                            ownerAddress = addressWithNoFunds,
                            amount = BigDecimal.ZERO to "XRD"
                        )
                    ),
                    SampleDataProvider().sampleAccountResource(
                        address = addressWithFunds,
                        withFungibleTokens = SampleDataProvider().sampleFungibleTokens(
                            ownerAddress = addressWithNoFunds,
                            amount = BigDecimal(100000) to "XRD"
                        )
                    )
                )
            )

            var manifest = ManifestBuilder().addInstruction(
                Instruction.SetMetadata(
                    entityAddress = ManifestAstValue.Address(addressWithNoFunds),
                    ManifestAstValue.String("name"),
                    ManifestAstValue.Enum("RadixDashboard")
                )
            ).build()

            val addressesInvolved = transactionClient.getAddressesInvolvedInATransaction(manifest)
            val addressToLockFee =
                transactionClient.selectAccountAddressToLockFee(addressesInvolved)
            manifest =
                transactionClient.addLockFeeInstructionToManifest(manifest, addressToLockFee!!)
            val addressesNeededToSign = transactionClient.getAddressesNeededToSign(manifest)

            Assert.assertEquals(2, addressesNeededToSign.size)
            Assert.assertEquals(
                addressWithFunds,
                addressesNeededToSign.first()
            )

        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when address has no funds, return the respective error`() = runTest {
        val expectedAddress = "account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjqn7q2py8ej"
        coEvery {
            getAccountResourceUseCase.getAccounts(
                addresses = listOf(expectedAddress),
                isRefreshing = true
            )
        } returns Result.Success(
            listOf(
                SampleDataProvider().sampleAccountResource(
                    address = expectedAddress,
                    withFungibleTokens = SampleDataProvider().sampleFungibleTokens(
                        ownerAddress = expectedAddress,
                        amount = BigDecimal.ZERO to "XRD"
                    )
                )
            )
        )
        coEvery {
            getAccountResourceUseCase.getAccountsFromProfile(isRefreshing = true)
        } returns Result.Success(
            listOf(
                SampleDataProvider().sampleAccountResource(
                    address = expectedAddress,
                    withFungibleTokens = SampleDataProvider().sampleFungibleTokens(
                        ownerAddress = expectedAddress,
                        amount = BigDecimal.ZERO to "XRD"
                    )
                )
            )
        )

        val manifest = ManifestBuilder().addInstruction(
            Instruction.SetMetadata(
                entityAddress = ManifestAstValue.Address(expectedAddress),
                ManifestAstValue.String("name"),
                ManifestAstValue.Enum("RadixDashboard")
            )
        ).build()

        val addressesInvolved = transactionClient.getAddressesInvolvedInATransaction(manifest)

        try {
            transactionClient.selectAccountAddressToLockFee(addressesInvolved)
        } catch (exception: Exception) {
            Assert.assertEquals(
                DappRequestException(
                    DappRequestFailure.TransactionApprovalFailure.FailedToFindAccountWithEnoughFundsToLockFee
                ),
                exception
            )
        }

    }
}
