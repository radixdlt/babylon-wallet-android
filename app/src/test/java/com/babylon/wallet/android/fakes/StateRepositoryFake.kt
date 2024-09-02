package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.toDecimal192
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import rdx.works.core.domain.DApp
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.metadata.PublicKeyHash

open class StateRepositoryFake : StateRepository {
    override fun observeAccountsOnLedger(accounts: List<Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>> = flowOf()

    override suspend fun getNextNFTsPage(
        account: Account,
        resource: Resource.NonFungibleResource
    ): Result<Resource.NonFungibleResource> = Result.failure(RuntimeException("Not implemented"))

    override suspend fun updateLSUsInfo(
        account: Account,
        validatorsWithStakes: List<ValidatorWithStakes>
    ): Result<List<ValidatorWithStakes>> = Result.failure(RuntimeException("Not implemented"))

    override suspend fun updateStakeClaims(account: Account, claims: List<StakeClaim>): Result<List<StakeClaim>> =
        Result.success(claims)

    override suspend fun getResources(
        addresses: Set<ResourceAddress>,
        underAccountAddress: AccountAddress?,
        withDetails: Boolean,
        withAllMetadata: Boolean
    ): Result<List<Resource>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getPools(poolAddresses: Set<PoolAddress>): Result<List<Pool>> = Result.failure(RuntimeException("Not implemented"))

    override suspend fun getValidators(validatorAddresses: Set<ValidatorAddress>): Result<List<Validator>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getNFTDetails(
        resourceAddress: ResourceAddress,
        localIds: Set<NonFungibleLocalId>
    ): Result<List<Resource.NonFungibleResource.Item>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getOwnedXRD(accounts: List<Account>): Result<Map<Account, Decimal192>> {
        return Result.success(accounts.associateWith {
            if (it == accounts.first()) {
                100.toDecimal192()
            } else {
                0.toDecimal192()
            }
        })
    }

    override suspend fun getEntityOwnerKeys(entities: List<ProfileEntity>): Result<Map<ProfileEntity, List<PublicKeyHash>>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun getDAppsDetails(definitionAddresses: List<AccountAddress>, isRefreshing: Boolean): Result<List<DApp>> {
        return Result.success(
            definitionAddresses.mapIndexed { index, accountAddress ->
                DApp(
                    dAppAddress = accountAddress,
                    metadata = listOf(
                        Metadata.Primitive(ExplicitMetadataKey.NAME.key, "dApp $index", MetadataType.String)
                    )
                )
            }
        )
    }

    override suspend fun getDAppDefinitions(componentAddresses: List<ComponentAddress>): Result<Map<ComponentAddress, AccountAddress?>> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun cacheNewlyCreatedResources(newResources: List<Resource>): Result<Unit> =
        Result.failure(RuntimeException("Not implemented"))

    override suspend fun clearCachedState(): Result<Unit> = Result.success(Unit)

    override suspend fun cacheNewlyCreatedNFTItems(newItems: List<Resource.NonFungibleResource.Item>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun clearCachedNewlyCreatedNFTItems(items: List<Resource.NonFungibleResource.Item>): Result<Unit> {
        return Result.success(Unit)
    }

}