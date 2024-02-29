package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.repository.state.StateRepository
import rdx.works.core.domain.DApp
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import rdx.works.core.domain.assets.ValidatorDetail
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.PublicKeyHash
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import java.lang.RuntimeException
import java.math.BigDecimal

open class StateRepositoryFake: StateRepository {
    override fun observeAccountsOnLedger(accounts: List<Network.Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>> = flowOf()

    override suspend fun getNextNFTsPage(
        account: Network.Account,
        resource: Resource.NonFungibleResource
    ): Result<Resource.NonFungibleResource> = Result.failure(RuntimeException("Not implemented"))

    override suspend fun updateLSUsInfo(
        account: Network.Account,
        validatorsWithStakes: List<ValidatorWithStakes>
    ): Result<List<ValidatorWithStakes>> = Result.failure(RuntimeException("Not implemented"))

    override suspend fun updateStakeClaims(account: Network.Account, claims: List<StakeClaim>): Result<List<StakeClaim>> =
        Result.success(claims)

    override suspend fun getResources(addresses: Set<String>, underAccountAddress: String?, withDetails: Boolean): Result<List<Resource>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getPools(poolAddresses: Set<String>): Result<List<Pool>> = Result.failure(RuntimeException("Not implemented"))

    override suspend fun getValidators(validatorAddresses: Set<String>): Result<List<ValidatorDetail>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getNFTDetails(resourceAddress: String, localIds: Set<String>): Result<List<Resource.NonFungibleResource.Item>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getOwnedXRD(accounts: List<Network.Account>): Result<Map<Network.Account, BigDecimal>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, List<PublicKeyHash>>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getDAppsDetails(definitionAddresses: List<String>, isRefreshing: Boolean): Result<List<DApp>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun cacheNewlyCreatedResources(newResources: List<Resource>): Result<Unit> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun clearCachedState(): Result<Unit> = Result.success(Unit)

}