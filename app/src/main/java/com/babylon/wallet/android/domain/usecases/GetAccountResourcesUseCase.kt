package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.amount
import com.babylon.wallet.android.data.gateway.generated.models.amountDecimal
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.nonfungible.NonFungibleRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.AccountAddress
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.FungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleMetadataContainer
import com.babylon.wallet.android.domain.model.NonFungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

class GetAccountResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository,
    private val nonFungibleRepository: NonFungibleRepository,
    private val accountRepository: AccountRepository
) {

    /**
     * Retrieves all data related to the accounts saved in the profile.
     *
     * @param isRefreshing When need to override the cache.
     */
    suspend fun getAccountsFromProfile(isRefreshing: Boolean) = getAccounts(
        profileAccounts = accountRepository.getAccounts(),
        isRefreshing = isRefreshing
    )

    /**
     * Retrieves all data related to this certain address to a saved address in the Profile.
     *
     * @param isRefreshing When need to override the cache.
     */
    suspend fun getSingleAccount(address: String, isRefreshing: Boolean): Result<AccountResources> {
        val profileAccount = accountRepository.getAccountByAddress(address) ?: return Result.Error(
            IllegalArgumentException("No Account Found with address $address")
        )

        return getAccounts(
            profileAccounts = listOf(profileAccount),
            isRefreshing = isRefreshing
        ).map {
            it.first()
        }
    }

    private suspend fun getAccounts(
        profileAccounts: List<Network.Account>,
        isRefreshing: Boolean
    ): Result<List<AccountResources>> = entityRepository.stateEntityDetails(
        addresses = profileAccounts.map { it.address },
        isRefreshing = isRefreshing
    ).map { result ->
        val accountsOnGateway = result.items

        // Compile a list of all accounts' fungible and non fungible resources
        val allResourceAddresses = accountsOnGateway.map { accountItem ->
            accountItem.allResourceAddresses
        }.flatten()

        // Query those resources all at once
        val allResources = allResourceAddresses.getResourcesData(isRefreshing)

        // Query non fungible ids only for non fungible resources contained in all accounts
        // and associate them with the resource address
        val nonFungiblesWithIds = accountsOnGateway.associateWithNonFungibleIds(isRefreshing)

        // For every Account stored in the profile, map it to AccountResources
        profileAccounts.mapNotNull { profileAccount ->
            val accountOnGateWay = accountsOnGateway.find {
                it.address == profileAccount.address
            } ?: return@mapNotNull null

            val fungibleTokens = accountOnGateWay.resolveFungibleTokens(allResources)
            val nonFungibleTokens = accountOnGateWay.resolveNonFungibleTokens(
                allResources,
                nonFungiblesWithIds
            )

            AccountResources(
                address = profileAccount.address,
                displayName = profileAccount.displayName,
                currencySymbol = "$", // TODO replace when endpoint ready
                value = "100",
                appearanceID = profileAccount.appearanceID,
                fungibleTokens = fungibleTokens,
                nonFungibleTokens = nonFungibleTokens
            )
        }
    }

    private suspend fun List<StateEntityDetailsResponseItem>.associateWithNonFungibleIds(
        isRefreshing: Boolean
    ) = map { it.nonFungibleResourceAddresses }
        .flatten()
        .associateWith { address ->
            nonFungibleRepository.nonFungibleIds(
                address = address,
                isRefreshing = isRefreshing
            ).value()
        }

    private suspend fun List<String>.getResourcesData(
        isRefreshing: Boolean
    ) = if (isNotEmpty()) {
        entityRepository.stateEntityDetails(
            addresses = this,
            isRefreshing = isRefreshing
        ).value()?.items.orEmpty()
    } else {
        emptyList()
    }

    private fun StateEntityDetailsResponseItem.resolveFungibleTokens(
        allResources: List<StateEntityDetailsResponseItem>
    ) = fungibleResources?.items?.map {
        val tokenResource = allResources.find { resource ->
            resource.address == it.resourceAddress
        } ?: error("Resource ${it.resourceAddress} not found")

        OwnedFungibleToken(
            owner = AccountAddress(address),
            amount = it.amountDecimal,
            address = it.resourceAddress,
            token = FungibleToken(
                address = tokenResource.address,
                metadata = tokenResource.metadata.asMetadataStringMap()
            )
        )
    }.orEmpty()

    private fun StateEntityDetailsResponseItem.resolveNonFungibleTokens(
        allResources: List<StateEntityDetailsResponseItem>,
        nonFungiblesWithIds: Map<String, NonFungibleTokenIdContainer?>
    ) = nonFungibleResources?.items?.map {
        val tokenResource = allResources.find { resource ->
            resource.address == it.resourceAddress
        } ?: error("Resource ${it.resourceAddress} not found")

        OwnedNonFungibleToken(
            owner = AccountAddress(address),
            amount = it.amount,
            tokenResourceAddress = it.resourceAddress,
            token = NonFungibleToken(
                address = tokenResource.address,
                nonFungibleIdContainer = nonFungiblesWithIds[it.resourceAddress],
                metadataContainer = NonFungibleMetadataContainer(
                    metadata = tokenResource.metadata.asMetadataStringMap()
                )
            )
        )
    }.orEmpty()
}
