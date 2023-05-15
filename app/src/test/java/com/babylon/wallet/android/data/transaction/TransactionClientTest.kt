package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.manifest.addLockFeeInstructionToManifest
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.toDomainModel
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.ManifestAstValue
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.profile.domain.signing.GetFactorSourcesAndSigningEntitiesUseCase
import java.math.BigDecimal

internal class TransactionClientTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getAccountsWithResourcesUseCase = mockk<GetAccountsWithResourcesUseCase>()
    private val collectSignersSignaturesUseCase = mockk<CollectSignersSignaturesUseCase>()
    private val getFactorSourcesAndSigningEntitiesUseCase = mockk<GetFactorSourcesAndSigningEntitiesUseCase>()
    private val submitTransactionUseCase = mockk<SubmitTransactionUseCase>()
    private val networkId = Radix.Gateway.hammunet.network.networkId().value
    private val expectedAddress = "account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjqn7q2py8ej"
    private val expectedAddressWithNoFunds = "account_tdx_22_1pzg5a7htq650xh33x23zq9k90j5me3dvd8jql2wrkk8sd64ak7"

    private lateinit var transactionClient: TransactionClient

    @Before
    fun setUp() {
        coEvery { collectSignersSignaturesUseCase.signingEvent } returns emptyFlow()
        transactionClient = TransactionClient(
            transactionRepository,
            getCurrentGatewayUseCase,
            getProfileUseCase,
            collectSignersSignaturesUseCase,
            getFactorSourcesAndSigningEntitiesUseCase,
            getAccountsWithResourcesUseCase,
            submitTransactionUseCase
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
            getAccountsWithResourcesUseCase.getAccounts(
                any(),
                any()
            )
        } returns Result.Success(
            data = listOf(
                SampleDataProvider().sampleAccountWithResources(address = expectedAddress)
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when address exists, finds address involved & signing for set metadata manifest`() =
        runTest {
            coEvery {
                getAccountsWithResourcesUseCase.getAccounts(
                    accountAddresses = listOf(expectedAddress),
                    isRefreshing = true
                )
            } returns Result.Success(
                listOf(SampleDataProvider().sampleAccountWithResources(expectedAddress))
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
            val signingEntities = transactionClient.getSigningEntities(networkId, manifest)

            Assert.assertEquals(1, signingEntities.size)
            Assert.assertEquals(
                expectedAddress,
                signingEntities.first().address
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when given address has no funds but there is another address with funds, use the other address for the transaction`() =
        runTest {
            coEvery {
                getAccountsWithResourcesUseCase.getAccounts(
                    accountAddresses = listOf(expectedAddressWithNoFunds),
                    isRefreshing = true
                )
            } returns Result.Success(
                listOf(
                    SampleDataProvider().sampleAccountWithResources(
                        address = expectedAddressWithNoFunds,
                        withFungibleTokens = SampleDataProvider().sampleFungibleResources(
                            amount = BigDecimal.ZERO to "XRD"
                        )
                    )
                )
            )
            coEvery {
                getAccountsWithResourcesUseCase.getAccountsFromProfile(isRefreshing = true)
            } returns Result.Success(
                listOf(
                    SampleDataProvider().sampleAccountWithResources(
                        address = expectedAddressWithNoFunds,
                        withFungibleTokens = SampleDataProvider().sampleFungibleResources(
                            amount = BigDecimal.ZERO to "XRD"
                        )
                    ),
                    SampleDataProvider().sampleAccountWithResources(
                        address = expectedAddress,
                        withFungibleTokens = SampleDataProvider().sampleFungibleResources(
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
            val signingEntities = transactionClient.getSigningEntities(networkId, manifest)

            Assert.assertEquals(2, signingEntities.size)
            Assert.assertEquals(
                expectedAddress,
                signingEntities.first().address
            )

        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when address has no funds, return the respective error`() = runTest {
        coEvery {
            getAccountsWithResourcesUseCase.getAccounts(
                accountAddresses = listOf(expectedAddress),
                isRefreshing = true
            )
        } returns Result.Success(
            listOf(
                SampleDataProvider().sampleAccountWithResources(
                    address = expectedAddress,
                    withFungibleTokens = SampleDataProvider().sampleFungibleResources(
                        amount = BigDecimal.ZERO to "XRD"
                    )
                )
            )
        )
        coEvery {
            getAccountsWithResourcesUseCase.getAccountsFromProfile(isRefreshing = true)
        } returns Result.Success(
            listOf(
                SampleDataProvider().sampleAccountWithResources(
                    address = expectedAddress,
                    withFungibleTokens = SampleDataProvider().sampleFungibleResources(
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
