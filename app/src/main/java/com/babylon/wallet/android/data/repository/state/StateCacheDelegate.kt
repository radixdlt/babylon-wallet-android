package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.AccountOnLedger
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.model.pernetwork.Network

class StateCacheDelegate(
    private val stateDao: StateDao
) {

    fun observeAllResources(accounts: List<Network.Account>): Flow<Map<Network.Account, CachedDetails>> {
        val accountAddresses = accounts.map { it.address }
        return stateDao.observeAccountsPortfolio(accountAddresses)
            .distinctUntilChanged()
            .map { detailsWithResources ->
                val result = mutableMapOf<Network.Account, CachedDetails>()

                detailsWithResources.forEach { accountDetailsAndResources ->
                    val account = accounts.find { it.address == accountDetailsAndResources.address } ?: return@forEach

                    // Parse details for this account
                    val cachedDetails = CachedDetails(
                        stateVersion = accountDetailsAndResources.accountStateVersion,
                        accountDetails = AccountDetails(
                            typeMetadataItem = accountDetailsAndResources.accountType?.let { AccountTypeMetadataItem(it) }
                        )
                    )

                    // Compile all resources owned by this account
                    if (accountDetailsAndResources.resource != null && accountDetailsAndResources.amount != null) {
                        when (val resource = accountDetailsAndResources.resource.toResource(accountDetailsAndResources.amount)) {
                            is Resource.FungibleResource -> {
                                result[account] = result.getOrDefault(account, cachedDetails).also {
                                    it.fungibles.add(resource)
                                }
                            }

                            is Resource.NonFungibleResource -> {
                                result[account] = result.getOrDefault(account, cachedDetails).also {
                                    it.nonFungibles.add(resource)
                                }
                            }
                        }
                    } else {
                        result[account] = cachedDetails
                    }

                    // Compile all pools
                    val poolAddresses = result[account]?.poolAddresses().orEmpty()
                    stateDao.getPoolDetails(poolAddresses, accountDetailsAndResources.accountStateVersion).onEach { poolWithResource ->
                        if (poolWithResource.resource != null && poolWithResource.amount != null) {
                            result[account]?.pools?.getOrDefault(poolWithResource.address, mutableListOf())?.also {
                                it.add(poolWithResource.resource.toResource(poolWithResource.amount) as Resource.FungibleResource)
                            }
                        }
                    }
                    val remainingPools = poolAddresses - result[account]?.pools?.keys
                    if (remainingPools.isNotEmpty()) {
                        result.remove(account)
                    }

                    // Compile all validators
                    val validatorsAddresses = result[account]?.validatorAddresses().orEmpty()
                    val validators = stateDao.getValidators(validatorsAddresses, accountDetailsAndResources.accountStateVersion)
                    if (validators.size != validatorsAddresses.size) {
                        result.remove(account)
                    }
                    result[account]?.validators?.addAll(validators.map { it.asValidatorDetail() })
                }

                result
            }.distinctUntilChanged()
    }

    data class CachedDetails(
        val stateVersion: Long,
        val accountDetails: AccountDetails,
        val fungibles: MutableList<Resource.FungibleResource> = mutableListOf(),
        val nonFungibles: MutableList<Resource.NonFungibleResource> = mutableListOf(),
        val pools: MutableMap<String, MutableList<Resource.FungibleResource>> = mutableMapOf(),
        val validators: MutableList<ValidatorDetail> = mutableListOf()
    ) {

        fun poolAddresses() = fungibles.mapNotNull { it.poolAddress }.toSet()

        fun validatorAddresses() = (fungibles.mapNotNull { it.validatorAddress } + nonFungibles.mapNotNull { it.validatorAddress }).toSet()

        fun toAccountDetails() = AccountOnLedger(
            accountDetails,
            fungibles,
            nonFungibles,
            validators,
            pools
        )

    }
}
