package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.manifest.addLockFeeInstructionToManifest
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.ManifestAstValue
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import java.math.BigDecimal

internal class TransactionClientTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getAccountsWithResourcesUseCase = mockk<GetAccountsWithResourcesUseCase>()
    private val collectSignersSignaturesUseCase = mockk<CollectSignersSignaturesUseCase>()
    private val submitTransactionUseCase = mockk<SubmitTransactionUseCase>()
    private val networkId = 242

    private lateinit var transactionClient: TransactionClient

    private val addressWithFunds = "account_sim1cyvgx33089ukm2pl97pv4max0x40ruvfy4lt60yvya744cve475w0q"
    private val accountWithFunds = AccountWithResources(
        account = account(address = addressWithFunds),
        resources = Resources(
            fungibleResources = listOf(Resource.FungibleResource(
                resourceAddress = "resource_rdx_abc",
                amount = BigDecimal.TEN,
                symbolMetadataItem = SymbolMetadataItem("XRD")
            )),
            nonFungibleResources = emptyList()
        )
    )
    private val addressWithNoFunds = "account_sim1cyzfj6p254jy6lhr237s7pcp8qqz6c8ahq9mn6nkdjxxxat5syrgz9"
    private val accountWithNoFunds = AccountWithResources(
        account = account(address = addressWithNoFunds),
        resources = Resources.EMPTY
    )

    @Before
    fun setUp() {
        coEvery { collectSignersSignaturesUseCase.signingState } returns emptyFlow()
        transactionClient = TransactionClient(
            transactionRepository,
            getCurrentGatewayUseCase,
            getProfileUseCase,
            collectSignersSignaturesUseCase,
            getAccountsWithResourcesUseCase,
            submitTransactionUseCase
        )
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.hammunet
        coEvery { getAccountsWithResourcesUseCase(accounts = listOf(), isRefreshing = true) } returns Result.Success(emptyList())
    }

    @Ignore("until we have the validated addresses from iOS")
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when address exists, finds address involved & signing for set metadata manifest`() =
        runTest {
            var manifest = ManifestBuilder().addInstruction(
                Instruction.SetMetadata(
                    entityAddress = ManifestAstValue.Address(addressWithFunds),
                    ManifestAstValue.String("name"),
                    ManifestAstValue.Enum("RadixDashboard")
                )
            ).build()

            every { getProfileUseCase() } returns flowOf(
                profile(
                    accounts = listOf(
                        accountWithFunds.account,
                        accountWithNoFunds.account
                    )
                )
            )
            coEvery {
                getAccountsWithResourcesUseCase(accounts = listOf(accountWithFunds.account), isRefreshing = true)
            } returns Result.Success(
                data = listOf(accountWithFunds)
            )

            val addressToLockFee = transactionClient.findFeePayerInManifest(manifest).getOrThrow().feePayerAddressFromManifest
            manifest = manifest.addLockFeeInstructionToManifest(addressToLockFee!!)
            val signingEntities = transactionClient.getSigningEntities(networkId, manifest)

            Assert.assertEquals(1, signingEntities.size)
            Assert.assertEquals(
                addressWithFunds,
                signingEntities.first().address
            )
        }

    @Ignore("until we have the validated addresses from iOS")
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when given address has no funds but there is another address with funds, use the other address for the transaction`() =
        runTest {
            val manifest = ManifestBuilder().addInstruction(
                Instruction.SetMetadata(
                    entityAddress = ManifestAstValue.Address(addressWithNoFunds),
                    ManifestAstValue.String("name"),
                    ManifestAstValue.Enum("RadixDashboard")
                )
            ).build()

            every { getProfileUseCase() } returns flowOf(profile(accounts = listOf(accountWithNoFunds.account, accountWithFunds.account)))
            coEvery {
                getAccountsWithResourcesUseCase(accounts = listOf(accountWithNoFunds.account), isRefreshing = true)
            } returns Result.Success(
                data = listOf(accountWithNoFunds)
            )

            coEvery {
                getAccountsWithResourcesUseCase(accounts = listOf(accountWithFunds.account), isRefreshing = true)
            } returns Result.Success(
                data = listOf(accountWithFunds)
            )

            val addressToLockFee = transactionClient.findFeePayerInManifest(manifest).getOrThrow()
            assert(addressToLockFee.feePayerAddressFromManifest == null)
            assert(addressToLockFee.candidates.size == 1)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when address has no funds, return the respective error`() = runTest {
        val manifest = ManifestBuilder().addInstruction(
            Instruction.SetMetadata(
                entityAddress = ManifestAstValue.Address(addressWithNoFunds),
                ManifestAstValue.String("name"),
                ManifestAstValue.Enum("RadixDashboard")
            )
        ).build()

        every { getProfileUseCase() } returns flowOf(profile(accounts = listOf(accountWithNoFunds.account)))
        coEvery {
            getAccountsWithResourcesUseCase(accounts = listOf(accountWithNoFunds.account), isRefreshing = true)
        } returns Result.Success(
            data = listOf(accountWithNoFunds)
        )

        try {
            transactionClient.findFeePayerInManifest(manifest)
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
