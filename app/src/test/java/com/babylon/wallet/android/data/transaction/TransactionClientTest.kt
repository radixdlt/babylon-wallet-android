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
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.ManifestAstValue
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
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import java.math.BigDecimal

internal class TransactionClientTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
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
        coEvery { collectSignersSignaturesUseCase.signingState } returns emptyFlow()
        transactionClient = TransactionClient(
            transactionRepository,
            getCurrentGatewayUseCase,
            getProfileUseCase,
            collectSignersSignaturesUseCase,
            getAccountsWithResourcesUseCase,
            submitTransactionUseCase
        )
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.default
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when address exists, finds address involved & signing for set metadata manifest`() =
        runTest {
            var manifest = ManifestBuilder().addInstruction(
                Instruction.SetMetadata(
                    entityAddress = ManifestAstValue.Address(EntityRepositoryFake.addressWithFunds),
                    ManifestAstValue.String("name"),
                    ManifestAstValue.Enum("RadixDashboard")
                )
            ).build()

            val addressToLockFee = transactionClient.findFeePayerInManifest(manifest).getOrThrow().feePayerAddressFromManifest
            manifest = manifest.addLockFeeInstructionToManifest(addressToLockFee!!)
            val signingEntities = transactionClient.getSigningEntities(Radix.Gateway.default.network.id, manifest)

            Assert.assertEquals(1, signingEntities.size)
            Assert.assertEquals(
                EntityRepositoryFake.addressWithFunds,
                signingEntities.first().address
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when given address has no funds but there is another address with funds, use the other address for the transaction`() =
        runTest {
            val manifest = ManifestBuilder().addInstruction(
                Instruction.SetMetadata(
                    entityAddress = ManifestAstValue.Address(EntityRepositoryFake.addressWithNoFunds),
                    ManifestAstValue.String("name"),
                    ManifestAstValue.Enum("RadixDashboard")
                )
            ).build()

            val addressToLockFee = transactionClient.findFeePayerInManifest(manifest).getOrThrow()
            assert(addressToLockFee.feePayerAddressFromManifest == null)
            assert(addressToLockFee.candidates.size == 1)
        }

    @Test
    fun `when address has no funds, return the respective error`() = runTest {
        val manifest = ManifestBuilder().addInstruction(
            Instruction.SetMetadata(
                entityAddress = ManifestAstValue.Address(EntityRepositoryFake.addressWithNoFunds),
                ManifestAstValue.String("name"),
                ManifestAstValue.Enum("RadixDashboard")
            )
        ).build()

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

    private object EntityRepositoryFake : EntityRepository {

        const val addressWithFunds = "account_tdx_21_12ya9jylskaa6gdrfr8nvve3pfc6wyhyw7eg83fwlc7fv2w0eanumcd"
        const val addressWithNoFunds = "account_tdx_21_12xg7tf7aup8lrxkvug0vzatntzww0c6jnntyj6yd4eg5920kpxpzvt"

        val account1 = account(address = addressWithFunds)
        val accountResourcesWithFunds = AccountWithResources(
            account = account1,
            resources = Resources(
                fungibleResources = listOf(
                    Resource.FungibleResource(
                        resourceAddress = Resource.FungibleResource.officialXrdResourceAddress()!!,
                        amount = BigDecimal.TEN,
                        symbolMetadataItem = SymbolMetadataItem("XRD")
                    )
                ),
                nonFungibleResources = emptyList()
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

        override suspend fun getEntityOwnerKeyHashes(entityAddress: String, isRefreshing: Boolean): Result<OwnerKeyHashesMetadataItem?> {
            error("Not needed")
        }

    }

    private object ProfileRepositoryFake: ProfileRepository {
        private val profile = profile(accounts = listOf(EntityRepositoryFake.account1, EntityRepositoryFake.account2))

        override val profileState: Flow<ProfileState> = flowOf(ProfileState.Restored(profile = profile))

        override val inMemoryProfileOrNull: Profile?
            get() = profile

        override suspend fun saveProfile(profile: Profile) {
            TODO("Not yet implemented")
        }

        override suspend fun clear() {
            TODO("Not yet implemented")
        }

        override suspend fun saveRestoringSnapshot(snapshotSerialised: String): Boolean {
            error("Not needed")
        }

        override suspend fun getSnapshotForBackup(): String? {
            error("Not needed")
        }

        override suspend fun isRestoredProfileFromBackupExists(): Boolean {
            error("Not needed")
        }

        override suspend fun getRestoredProfileFromBackup(): Profile? {
            error("Not needed")
        }

        override suspend fun discardBackedUpProfile() {
            error("Not needed")
        }

    }
}
