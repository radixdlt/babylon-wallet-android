package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.manifest.addLockFeeInstructionToManifest
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.toDomainModel
import com.babylon.wallet.android.domain.usecases.GetAccountResourcesUseCase
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.ManifestAstValue
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.GetAccountSignersUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import java.math.BigDecimal

internal class TransactionClientTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getAccountSignersUseCase = mockk<GetAccountSignersUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getAccountResourceUseCase = mockk<GetAccountResourcesUseCase>()
    private val cache = mockk<HttpCache>()
    private val networkId = Radix.Gateway.hammunet.network.networkId().value
    private val expectedAddress = "account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjqn7q2py8ej"
    private val expectedAddressWithNoFunds = "account_tdx_22_1pzg5a7htq650xh33x23zq9k90j5me3dvd8jql2wrkk8sd64ak7"

    private lateinit var transactionClient: TransactionClient

    @Before
    fun setUp() {
        transactionClient = TransactionClient(
            transactionRepository,
            getCurrentGatewayUseCase,
            getProfileUseCase,
            getAccountSignersUseCase,
            getAccountResourceUseCase,
            cache
        )
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.hammunet
        every { getProfileUseCase() } returns flowOf(
            profile(
                accounts = listOf(
                    account("my1", expectedAddress),
                    account("my2", expectedAddressWithNoFunds)
                )
            )
        )
        coEvery {
            getAccountResourceUseCase.getAccounts(
                any(),
                any()
            )
        } returns Result.Success(
            data = listOf(
                account(address = expectedAddress).toDomainModel()
                    .copy(fungibleTokens = SampleDataProvider().sampleFungibleTokens().toPersistentList())
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when address exists, finds address involved & signing for set metadata manifest`() =
        runTest {
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

            val addressToLockFee =
                transactionClient.selectAccountAddressToLockFee(networkId, manifest)
            manifest =
                manifest.addLockFeeInstructionToManifest(addressToLockFee!!)
            val addressesNeededToSign = transactionClient.getAddressesNeededToSign(networkId, manifest)


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
            coEvery {
                getAccountResourceUseCase.getAccounts(
                    addresses = listOf(expectedAddressWithNoFunds),
                    isRefreshing = true
                )
            } returns Result.Success(
                listOf(
                    SampleDataProvider().sampleAccountResource(
                        address = expectedAddressWithNoFunds,
                        withFungibleTokens = SampleDataProvider().sampleFungibleTokens(
                            ownerAddress = expectedAddressWithNoFunds,
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
                        address = expectedAddressWithNoFunds,
                        withFungibleTokens = SampleDataProvider().sampleFungibleTokens(
                            ownerAddress = expectedAddressWithNoFunds,
                            amount = BigDecimal.ZERO to "XRD"
                        )
                    ),
                    SampleDataProvider().sampleAccountResource(
                        address = expectedAddress,
                        withFungibleTokens = SampleDataProvider().sampleFungibleTokens(
                            ownerAddress = expectedAddressWithNoFunds,
                            amount = BigDecimal(100000) to "XRD"
                        )
                    )
                )
            )

            var manifest = ManifestBuilder().addInstruction(
                Instruction.SetMetadata(
                    entityAddress = ManifestAstValue.Address(expectedAddressWithNoFunds),
                    ManifestAstValue.String("name"),
                    ManifestAstValue.Enum("RadixDashboard")
                )
            ).build()

            val addressToLockFee =
                transactionClient.selectAccountAddressToLockFee(networkId, manifest)
            manifest =
                manifest.addLockFeeInstructionToManifest(addressToLockFee!!)
            val addressesNeededToSign = transactionClient.getAddressesNeededToSign(networkId, manifest)

            Assert.assertEquals(2, addressesNeededToSign.size)
            Assert.assertEquals(
                expectedAddress,
                addressesNeededToSign.first()
            )

        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when address has no funds, return the respective error`() = runTest {
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

        try {
            transactionClient.selectAccountAddressToLockFee(networkId, manifest)
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
