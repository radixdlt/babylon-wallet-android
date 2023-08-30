package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.manifest.addLockFeeInstructionToManifest
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.OwnerKeyHashesMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.TransactionManifest
import com.radixdlt.ret.knownAddresses
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.core.ret.BabylonManifestBuilder
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase

internal class TransactionClientTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val getProfileUseCase = GetProfileUseCase(ProfileRepositoryFake)
    private val getAccountsWithResourcesUseCase = GetAccountsWithResourcesUseCase(
        EntityRepositoryFake,
        getProfileUseCase
    )
    private val collectSignersSignaturesUseCase = mockk<CollectSignersSignaturesUseCase>()
    private val submitTransactionUseCase = mockk<SubmitTransactionUseCase>()

    private lateinit var transactionClient: TransactionClient

    @Before
    fun setUp() {
        coEvery { collectSignersSignaturesUseCase.interactionState } returns emptyFlow()
        transactionClient = TransactionClient(
            transactionRepository,
            getProfileUseCase,
            collectSignersSignaturesUseCase,
            getAccountsWithResourcesUseCase,
            submitTransactionUseCase
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when address exists, finds address involved & signing for set metadata manifest`() =
        runTest {
            var manifest = manifestWithAddress(EntityRepositoryFake.addressWithFunds)

            val addressToLockFee = transactionClient.findFeePayerInManifest(
                manifest,
                TransactionConfig.DEFAULT_LOCK_FEE.toBigDecimal()
            ).getOrThrow().feePayerAddressFromManifest
            manifest = manifest.addLockFeeInstructionToManifest(addressToLockFee!!, TransactionConfig.DEFAULT_LOCK_FEE.toBigDecimal())
            val signingEntities = transactionClient.getSigningEntities(manifest)
            Assert.assertEquals(1, signingEntities.size)
            Assert.assertEquals(
                EntityRepositoryFake.addressWithFunds,
                signingEntities.first().address
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when given address has funds, verify feePayerAddress exists and all accounts as candidates`() =
        runTest {
            val manifest = manifestWithAddress(EntityRepositoryFake.addressWithFunds)

            val addressToLockFee = transactionClient.findFeePayerInManifest(
                manifest,
                TransactionConfig.DEFAULT_LOCK_FEE.toBigDecimal()
            ).getOrThrow()
            assert(addressToLockFee.feePayerAddressFromManifest != null)
            assert(addressToLockFee.candidates.size == 2)
        }

    @Test
    fun `when address has no funds, return no feePayer and all accounts as candidates`() = runTest {
        val manifest = manifestWithAddress(EntityRepositoryFake.addressWithNoFunds)

        val feePayerResult = transactionClient.findFeePayerInManifest(
            manifest,
            TransactionConfig.DEFAULT_LOCK_FEE.toBigDecimal()
        ).getOrNull()
        assert(feePayerResult?.feePayerAddressFromManifest == null)
        assert(feePayerResult?.candidates?.size == 2)
    }

    private object EntityRepositoryFake : EntityRepository {

        const val addressWithFunds = "account_tdx_22_12yktr4z690lpn874q2zrdggqrjrcqp0zynx629kxy4gfufzw23d3f6"
        const val addressWithNoFunds = "account_tdx_22_12ya2hy3wsmrm88svkeugan753f5atlhk3dzz09n9gm46qvx69rjqcm"

        val account1 = account(address = addressWithFunds)
        val accountResourcesWithFunds = AccountWithResources(
            account = account1,
            resources = Resources(
                fungibleResources = listOf(
                    Resource.FungibleResource(
                        resourceAddress = Resource.FungibleResource.officialXrdResourceAddresses().first(),
                        amount = 30.toBigDecimal(),
                        symbolMetadataItem = SymbolMetadataItem("XRD")
                    )
                ),
                nonFungibleResources = emptyList(),
                poolUnits = emptyList()
            )
        )

        val account2 = account(address = addressWithNoFunds)
        val accountResourcesWithNoFunds = AccountWithResources(
            account = account2,
            resources = Resources.EMPTY
        )


        override suspend fun getAccountsWithResources(
            accounts: List<Network.Account>,
            explicitMetadataForAssets: Set<ExplicitMetadataKey>,
            isRefreshing: Boolean
        ): Result<List<AccountWithResources>> = Result.Success(accounts.map {
            when (it.address) {
                addressWithFunds -> accountResourcesWithFunds
                addressWithNoFunds -> accountResourcesWithNoFunds
                else -> error("Not faked account with address ${it.address}")
            }
        })

        override suspend fun getEntityOwnerKeyHashes(entityAddress: String, isRefreshing: Boolean, stateVersion: Long?): Result<OwnerKeyHashesMetadataItem?> {
            error("Not needed")
        }
    }

    private fun manifestWithAddress(
        address: String,
        networkId: Int = Radix.Gateway.default.network.id
    ): TransactionManifest = BabylonManifestBuilder()
        .withdrawFromAccount(
            fromAddress = Address(address),
            fungible = knownAddresses(networkId = networkId.toUByte()).resourceAddresses.xrd,
            amount = Decimal("10")
        ).build(networkId)

    private object ProfileRepositoryFake: ProfileRepository {
        private val profile = profile(accounts = listOf(EntityRepositoryFake.account1, EntityRepositoryFake.account2))

        override val profileState: Flow<ProfileState> = flowOf(ProfileState.Restored(profile = profile))

        override val inMemoryProfileOrNull: Profile?
            get() = profile

        override suspend fun saveProfile(profile: Profile) {
            error("Not needed")
        }

        override suspend fun clear() {
            error("Not needed")
        }

        override fun deriveProfileState(content: String): ProfileState {
            error("Not needed")
        }
    }
}
