package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.data.transaction.model.TransactionFeePayers
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AccountOrAddressOf
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PerAssetFungibleResource
import com.radixdlt.sargon.PerAssetFungibleTransfer
import com.radixdlt.sargon.PerAssetTransfers
import com.radixdlt.sargon.PerAssetTransfersOfFungibleResource
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.perAssetTransfers
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import rdx.works.core.domain.DApp
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.resources.metadata.PublicKeyHash
import rdx.works.core.identifiedArrayListOf
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase

class SearchFeePayersUseCaseTest {

    private val profileUseCase = GetProfileUseCase(profileRepository = ProfileRepositoryFake)
    private val useCase = SearchFeePayersUseCase(
        profileUseCase = profileUseCase,
        stateRepository = StateRepositoryFake
    )

    @Test
    fun `when account with enough xrd exists, returns the selected fee payer`() =
        runTest {
            val manifestData = manifestDataWithAddress(account1)

            val result = useCase(manifestData, TransactionConfig.DEFAULT_LOCK_FEE.toDecimal192()).getOrThrow()

            assertEquals(
                TransactionFeePayers(
                    selectedAccountAddress = AccountAddress.init(account1.address),
                    candidates = listOf(
                        TransactionFeePayers.FeePayerCandidate(account1, 100.toDecimal192()),
                        TransactionFeePayers.FeePayerCandidate(account2, 0.toDecimal192())
                    )
                ),
                result
            )
        }

    @Test
    fun `when account with xrd does not exist, returns the null fee payer`() =
        runTest {
            val manifestData = manifestDataWithAddress(account1)

            val result = useCase(manifestData, 200.toDecimal192()).getOrThrow()

            assertEquals(
                TransactionFeePayers(
                    selectedAccountAddress = null,
                    candidates = listOf(
                        TransactionFeePayers.FeePayerCandidate(account1, 100.toDecimal192()),
                        TransactionFeePayers.FeePayerCandidate(account2, 0.toDecimal192())
                    )
                ),
                result
            )
        }

    companion object {
        private val account1 = account(name = "account1", address = AccountAddress.sampleMainnet.random())
        private val account2 = account(name = "account2", address = AccountAddress.sampleMainnet.random())

        private fun manifestDataWithAddress(
            account: Network.Account
        ) = TransactionManifestData.from(
            manifest = TransactionManifest.perAssetTransfers(
                transfers = PerAssetTransfers(
                    fromAccount = AccountAddress.init(account.address),
                    fungibleResources = listOf(
                        PerAssetTransfersOfFungibleResource(
                            resource = PerAssetFungibleResource(
                                resourceAddress = XrdResource.address(networkId = account.networkID),
                                divisibility = 18.toUByte()
                            ),
                            transfers = listOf(
                                PerAssetFungibleTransfer(
                                    useTryDepositOrAbort = true,
                                    amount = 10.toDecimal192(),
                                    recipient = AccountOrAddressOf.AddressOfExternalAccount(
                                        value = AccountAddress.sampleMainnet()
                                    )
                                )
                            )
                        )
                    ),
                    nonFungibleResources = emptyList()
                )
            )
        )

        private object ProfileRepositoryFake : ProfileRepository {
            private val profile = profile(accounts = identifiedArrayListOf(account1, account2))

            override val profileState: Flow<ProfileState> = flowOf(ProfileState.Restored(profile = profile))

            override val inMemoryProfileOrNull: Profile?
                get() = profile

            override suspend fun saveProfile(profile: Profile) {
                error("Not needed")
            }

            override suspend fun clearProfileDataOnly() {
                error("Not needed")
            }

            override suspend fun clearAllWalletData() {
                error("Not needed")
            }

            override fun deriveProfileState(content: String): ProfileState {
                error("Not needed")
            }
        }

        private object StateRepositoryFake : StateRepository {
            override fun observeAccountsOnLedger(accounts: List<Network.Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>> {
                error("Not needed")
            }

            override suspend fun getNextNFTsPage(
                account: Network.Account,
                resource: Resource.NonFungibleResource
            ): Result<Resource.NonFungibleResource> {
                error("Not needed")
            }

            override suspend fun updateLSUsInfo(
                account: Network.Account,
                validatorsWithStakes: List<ValidatorWithStakes>
            ): Result<List<ValidatorWithStakes>> {
                error("Not needed")
            }

            override suspend fun updateStakeClaims(account: Network.Account, claims: List<StakeClaim>): Result<List<StakeClaim>> {
                error("Not needed")
            }

            override suspend fun getResources(
                addresses: Set<ResourceAddress>,
                underAccountAddress: AccountAddress?,
                withDetails: Boolean
            ): Result<List<Resource>> {
                error("Not needed")
            }

            override suspend fun getPools(poolAddresses: Set<PoolAddress>): Result<List<Pool>> {
                error("Not needed")
            }

            override suspend fun getValidators(validatorAddresses: Set<ValidatorAddress>): Result<List<Validator>> {
                error("Not needed")
            }

            override suspend fun getNFTDetails(
                resourceAddress: ResourceAddress,
                localIds: Set<NonFungibleLocalId>
            ): Result<List<Resource.NonFungibleResource.Item>> {
                error("Not needed")
            }

            override suspend fun getOwnedXRD(accounts: List<Network.Account>): Result<Map<Network.Account, Decimal192>> {
                return Result.success(
                    mapOf(
                        account1 to 100.toDecimal192(),
                        account2 to 0.toDecimal192()
                    )
                )
            }

            override suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, List<PublicKeyHash>>> {
                error("Not needed")
            }

            override suspend fun getDAppsDetails(definitionAddresses: List<AccountAddress>, isRefreshing: Boolean): Result<List<DApp>> {
                error("Not needed")
            }

            override suspend fun getDAppDefinitions(componentAddresses: List<ComponentAddress>): Result<Map<ComponentAddress, AccountAddress?>> {
                error("Not needed")
            }

            override suspend fun cacheNewlyCreatedResources(newResources: List<Resource>): Result<Unit> {
                error("Not needed")
            }

            override suspend fun clearCachedState(): Result<Unit> {
                error("Not needed")
            }

        }
    }


}
