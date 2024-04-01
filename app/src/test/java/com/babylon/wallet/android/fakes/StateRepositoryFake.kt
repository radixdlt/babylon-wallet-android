package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.repository.state.StateRepository
import rdx.works.core.domain.DApp
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.PublicKeyHash
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import java.lang.RuntimeException
import java.math.BigDecimal

open class StateRepositoryFake : StateRepository {
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

    override suspend fun getResources(addresses: Set<ResourceAddress>, underAccountAddress: String?, withDetails: Boolean): Result<List<Resource>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getPools(poolAddresses: Set<PoolAddress>): Result<List<Pool>> = Result.failure(RuntimeException("Not implemented"))

    override suspend fun getValidators(validatorAddresses: Set<ValidatorAddress>): Result<List<Validator>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getNFTDetails(resourceAddress: ResourceAddress, localIds: Set<NonFungibleLocalId>): Result<List<Resource.NonFungibleResource.Item>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getOwnedXRD(accounts: List<Network.Account>): Result<Map<Network.Account, BigDecimal>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, List<PublicKeyHash>>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getDAppsDetails(definitionAddresses: List<AccountAddress>, isRefreshing: Boolean): Result<List<DApp>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getDAppDefinitions(componentAddresses: List<ComponentAddress>): Result<Map<ComponentAddress, AccountAddress?>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun cacheNewlyCreatedResources(newResources: List<Resource>): Result<Unit> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun clearCachedState(): Result<Unit> = Result.success(Unit)

}