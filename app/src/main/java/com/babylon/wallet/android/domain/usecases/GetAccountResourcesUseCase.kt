package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.gateway.generated.models.amount
import com.babylon.wallet.android.data.gateway.generated.models.amountDecimal
import com.babylon.wallet.android.data.gateway.toFungibleToken
import com.babylon.wallet.android.data.gateway.toNonFungibleToken
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.nonfungible.NonFungibleRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.AccountAddress
import com.babylon.wallet.android.domain.model.AccountResources
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

    suspend fun getAccountsFromProfile(isRefreshing: Boolean) = getAccounts(
        profileAccounts = accountRepository.getAccounts(),
        isRefreshing = isRefreshing
    )

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
        val accounts = result.items

        // Compile a list of all accounts' fungible and non fungible resources
        val allResourceAddresses = accounts.map { accountItem ->
            accountItem.allResourceAddresses
        }.flatten()

        // Query those resources all at once
        val allResources = if (allResourceAddresses.isNotEmpty()) {
            entityRepository.stateEntityDetails(
                addresses = allResourceAddresses,
                isRefreshing = isRefreshing
            ).value()?.items ?: emptyList()
        } else {
            emptyList()
        }

        // Query non fungible ids only for non fungible resources contained in all accounts
        // and associate them with the resource address
        val nonFungiblesWithIds = accounts
            .map { it.nonFungibleResourceAddresses }
            .flatten()
            .associateWith { address ->
                nonFungibleRepository.nonFungibleIds(
                    address = address,
                    isRefreshing = isRefreshing
                ).value()
            }

        // For every Account stored in the profile, map it to AccountResources
        profileAccounts.mapNotNull { profileAccount ->
            val accountOnGateWay = accounts.find {
                it.address == profileAccount.address
            } ?: return@mapNotNull null

            val fungibleTokens = accountOnGateWay.fungibleResources?.items?.map {
                val tokenResource = allResources.find { resource ->
                    resource.address == it.resourceAddress
                }?.toFungibleToken() ?: error("Resource ${it.resourceAddress} not found")

                OwnedFungibleToken(
                    owner = AccountAddress(accountOnGateWay.address),
                    amount = it.amountDecimal,
                    address = it.resourceAddress,
                    token = tokenResource
                )
            } ?: emptyList()

            val nonFungibleTokens = accountOnGateWay.nonFungibleResources?.items?.map {
                val tokenResource = allResources.find { resource ->
                    resource.address == it.resourceAddress
                }?.toNonFungibleToken(nonFungiblesWithIds[it.resourceAddress]) ?: error("Resource ${it.resourceAddress} not found")

                OwnedNonFungibleToken(
                    owner = AccountAddress(accountOnGateWay.address),
                    amount = it.amount,
                    tokenResourceAddress = it.resourceAddress,
                    token = tokenResource
                )
            } ?: emptyList()

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
}
