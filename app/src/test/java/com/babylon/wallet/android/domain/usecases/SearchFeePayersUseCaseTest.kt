package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.OwnerKeyHashesMetadataItem
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.TransactionManifest
import com.radixdlt.ret.knownAddresses
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import rdx.works.core.ret.BabylonManifestBuilder
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase
import java.math.BigDecimal

class SearchFeePayersUseCaseTest {

    private val profileUseCase = GetProfileUseCase(profileRepository = ProfileRepositoryFake)
    private val useCase = SearchFeePayersUseCase(
        profileUseCase = profileUseCase,
        stateRepository = StateRepositoryFake
    )

    @Test
    fun `when account with enough xrd exists, returns the selected fee payer`() =
        runTest {
            val manifest = manifestWithAddress(account1)

            val result = useCase(manifest, TransactionConfig.DEFAULT_LOCK_FEE.toBigDecimal()).getOrThrow()

            assertEquals(
                FeePayerSearchResult(
                    feePayerAddress = account1.address,
                    candidates = listOf(
                        FeePayerSearchResult.FeePayerCandidate(account1, BigDecimal(100)),
                        FeePayerSearchResult.FeePayerCandidate(account2, BigDecimal.ZERO)
                    )
                ),
                result
            )
        }

    @Test
    fun `when account with xrd does not exist, returns the null fee payer`() =
        runTest {
            val manifest = manifestWithAddress(account1)

            val result = useCase(manifest, BigDecimal(200)).getOrThrow()

            assertEquals(
                FeePayerSearchResult(
                    feePayerAddress = null,
                    candidates = listOf(
                        FeePayerSearchResult.FeePayerCandidate(account1, BigDecimal(100)),
                        FeePayerSearchResult.FeePayerCandidate(account2, BigDecimal.ZERO)
                    )
                ),
                result
            )
        }

    companion object {
        private val account1 = account(name = "account1", address = "account_rdx12x20vgu94d96g3demdumxl6yjpvm0jy8dhrr03g75299ghxrwq76uh")
        private val account2 = account(name = "account2", address = "account_rdx12x20vgu94d96g3demdumxl6yjpvm0jy8dhrr03g75299ghxrwq73uh")

        private fun manifestWithAddress(
            account: Network.Account,
            networkId: Int = Radix.Gateway.default.network.id
        ): TransactionManifest = BabylonManifestBuilder()
            .withdrawFromAccount(
                fromAddress = Address(account.address),
                fungible = knownAddresses(networkId = networkId.toUByte()).resourceAddresses.xrd,
                amount = Decimal("10")
            ).build(networkId)

        private object ProfileRepositoryFake : ProfileRepository {
            private val profile = profile(accounts = listOf(account1, account2))

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

        private object StateRepositoryFake : StateRepository {
            override fun observeAccountsOnLedger(accounts: List<Network.Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>> {
                error("Not needed")
            }

            override suspend fun getMoreNFTs(
                account: Network.Account,
                resource: Resource.NonFungibleResource
            ): Result<Resource.NonFungibleResource> {
                error("Not needed")
            }

            override suspend fun updateLSUsInfo(account: Network.Account, validatorsWithStakes: List<ValidatorWithStakes>): Result<Unit> {
                error("Not needed")
            }

            override suspend fun getResources(
                addresses: Set<String>,
                underAccountAddress: String?,
                withDetails: Boolean
            ): Result<List<Resource>> {
                error("Not needed")
            }

            override suspend fun getPool(poolAddress: String): Result<Pool> {
                error("Not needed")
            }

            override suspend fun getValidator(validatorAddress: String): Result<ValidatorDetail> {
                error("Not needed")
            }

            override suspend fun getNFTDetails(resourceAddress: String, localId: String): Result<Resource.NonFungibleResource.Item> {
                error("Not needed")
            }

            override suspend fun getOwnedXRD(accounts: List<Network.Account>): Result<Map<Network.Account, BigDecimal>> {
                return Result.success(
                    mapOf(
                        account1 to BigDecimal(100),
                        account2 to BigDecimal.ZERO
                    )
                )
            }

            override suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, OwnerKeyHashesMetadataItem>> {
                error("Not needed")
            }

            override suspend fun getDAppsDetails(definitionAddresses: List<String>): Result<List<DApp>> {
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
