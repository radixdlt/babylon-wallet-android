package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.data.transaction.model.TransactionFeePayers
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AccountOrAddressOf
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PerAssetFungibleResource
import com.radixdlt.sargon.PerAssetFungibleTransfer
import com.radixdlt.sargon.PerAssetTransfers
import com.radixdlt.sargon.PerAssetTransfersOfFungibleResource
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.perAssetTransfers
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase

class SearchFeePayersUseCaseTest {

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities()
    private val account1 = profile.activeAccountsOnCurrentNetwork[0]
    private val account2 = profile.activeAccountsOnCurrentNetwork[1]
    private val profileUseCase = GetProfileUseCase(profileRepository = FakeProfileRepository(profile))
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
                    selectedAccountAddress = account1.address,
                    candidates = listOf(
                        TransactionFeePayers.FeePayerCandidate(account1, 100.toDecimal192())
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
                        TransactionFeePayers.FeePayerCandidate(account1, 100.toDecimal192())
                    )
                ),
                result
            )
        }

    companion object {
        private fun manifestDataWithAddress(
            account: Account
        ) = TransactionManifestData.from(
            manifest = TransactionManifest.perAssetTransfers(
                transfers = PerAssetTransfers(
                    fromAccount = account.address,
                    fungibleResources = listOf(
                        PerAssetTransfersOfFungibleResource(
                            resource = PerAssetFungibleResource(
                                resourceAddress = XrdResource.address(networkId = account.networkId),
                                divisibility = 18.toUByte()
                            ),
                            transfers = listOf(
                                PerAssetFungibleTransfer(
                                    useTryDepositOrAbort = true,
                                    amount = 10.toDecimal192(),
                                    recipient = AccountOrAddressOf.AddressOfExternalAccount(
                                        value = AccountAddress.sampleMainnet.random()
                                    )
                                )
                            )
                        )
                    ),
                    nonFungibleResources = emptyList()
                )
            )
        )

        private object StateRepositoryFake : StateRepository {
            override fun observeAccountsOnLedger(accounts: List<Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>> {
                TODO("Not yet implemented")
            }

            override suspend fun getNextNFTsPage(
                account: Account,
                resource: Resource.NonFungibleResource
            ): Result<Resource.NonFungibleResource> {
                TODO("Not yet implemented")
            }

            override suspend fun updateLSUsInfo(
                account: Account,
                validatorsWithStakes: List<ValidatorWithStakes>
            ): Result<List<ValidatorWithStakes>> {
                TODO("Not yet implemented")
            }

            override suspend fun updateStakeClaims(account: Account, claims: List<StakeClaim>): Result<List<StakeClaim>> {
                TODO("Not yet implemented")
            }

            override suspend fun getResources(
                addresses: Set<ResourceAddress>,
                underAccountAddress: AccountAddress?,
                withDetails: Boolean,
                withAllMetadata: Boolean
            ): Result<List<Resource>> {
                TODO("Not yet implemented")
            }

            override suspend fun getPools(poolAddresses: Set<PoolAddress>): Result<List<Pool>> {
                TODO("Not yet implemented")
            }

            override suspend fun getValidators(validatorAddresses: Set<ValidatorAddress>): Result<List<Validator>> {
                TODO("Not yet implemented")
            }

            override suspend fun getNFTDetails(
                resourceAddress: ResourceAddress,
                localIds: Set<NonFungibleLocalId>
            ): Result<List<Resource.NonFungibleResource.Item>> {
                TODO("Not yet implemented")
            }

            override suspend fun getOwnedXRD(accounts: List<Account>): Result<Map<Account, Decimal192>> {
                return Result.success(accounts.associateWith {
                    if (it == accounts.first()) {
                        100.toDecimal192()
                    } else {
                        0.toDecimal192()
                    }
                })
            }

            override suspend fun getEntityOwnerKeys(entities: List<ProfileEntity>): Result<Map<ProfileEntity, List<PublicKeyHash>>> {
                TODO("Not yet implemented")
            }

            override suspend fun getDAppsDetails(definitionAddresses: List<AccountAddress>, isRefreshing: Boolean): Result<List<DApp>> {
                TODO("Not yet implemented")
            }

            override suspend fun getDAppDefinitions(componentAddresses: List<ComponentAddress>): Result<Map<ComponentAddress, AccountAddress?>> {
                TODO("Not yet implemented")
            }

            override suspend fun cacheNewlyCreatedResources(newResources: List<Resource>): Result<Unit> {
                TODO("Not yet implemented")
            }

            override suspend fun clearCachedState(): Result<Unit> {
                TODO("Not yet implemented")
            }
        }
    }


}
