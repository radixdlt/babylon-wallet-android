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
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.repository.AccountRepository
import java.math.BigDecimal
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
        profileAccounts: List<OnNetwork.Account>,
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
        val allResources = entityRepository.stateEntityDetails(
            addresses = allResourceAddresses,
            isRefreshing = isRefreshing
        ).value()?.items ?: emptyList()

        // For every Account stored in the profile, map it to AccountResources
        profileAccounts.mapNotNull { profileAccount ->
            val accountOnNetwork = accounts.find {
                it.address == profileAccount.address
            } ?: return@mapNotNull null

            val accountFungibleResourceAddresses = accountOnNetwork.fungibleResourceAddresses
            val accountNonFungibleResourceAddresses = accountOnNetwork.nonFungibleResourceAddresses

            val fungibleTokens = allResources.filter {
                it.address in accountFungibleResourceAddresses
            }.map {
                OwnedFungibleToken(
                    owner = AccountAddress(accountOnNetwork.address),
                    amount = it.fungibleResources?.items?.first()?.amountDecimal ?: BigDecimal.ZERO, // TODO 1181
                    address = it.address,
                    token = it.toFungibleToken()
                )
            }

            val nonFungibleTokens = allResources.filter {
                it.address in accountNonFungibleResourceAddresses
            }.map {
                OwnedNonFungibleToken(
                    owner = AccountAddress(accountOnNetwork.address),
                    amount = it.nonFungibleResources?.items?.first()?.amount ?: 0L, // TODO 1181
                    tokenResourceAddress = it.address, // TODO 1181
                    token = it.toNonFungibleToken()
                )
            }

            AccountResources(
                address = profileAccount.address,
                displayName = profileAccount.displayName,
                currencySymbol = "$",
                value = "100",
                appearanceID = profileAccount.appearanceID,
                fungibleTokens = fungibleTokens,
                nonFungibleTokens = nonFungibleTokens
            )
        }
    }
}

//    @Suppress("LongMethod")
//    private suspend fun getSingleAccountResources(
//        address: String,
//        accountDisplayName: String,
//        appearanceId: Int,
//        isRefreshing: Boolean
//    ) = coroutineScope {
//        when (val accountResources = entityRepository.getAccountResources(address, isRefreshing)) {
//            is Result.Error -> Result.Error(accountResources.exception)
//            is Result.Success -> {
//                val fungibleTokens = mutableListOf<OwnedFungibleToken>()
//                val nonFungibleTokens = mutableListOf<OwnedNonFungibleToken>()
//
//                accountResources.data.let { resources ->
//                    val fungibleTokensDeferred = resources.simpleFungibleTokens.map { fungibleToken ->
//                        async {
//                            entityRepository.entityDetails(
//                                address = fungibleToken.address,
//                                isRefreshing = isRefreshing
//                            )
//                        }
//                    }
//
//                    val nonFungibleTokensDeferred = mutableListOf<Deferred<Result<EntityDetailsResponse>>>()
//                    val nonFungibleTokensIdsDeferred = mutableListOf<Deferred<Result<NonFungibleTokenIdContainer>>>()
//
//                    resources.simpleNonFungibleTokens
//                        .map { nonFungibleToken ->
//                            nonFungibleTokensDeferred.add(
//                                async {
//                                    entityRepository.entityDetails(
//                                        address = nonFungibleToken.tokenResourceAddress,
//                                        isRefreshing = isRefreshing
//                                    )
//                                }
//                            )
//                            nonFungibleTokensIdsDeferred.add(
//                                async {
//                                    nonFungibleRepository.nonFungibleIds(
//                                        address = nonFungibleToken.tokenResourceAddress,
//                                        isRefreshing = isRefreshing
//                                    )
//                                }
//                            )
//                        }
//
//                    // Run all requests simultaneously
//                    val accountResourcesJobs = AccountResourcesJobs(
//                        fungibleTokens = fungibleTokensDeferred.awaitAll(),
//                        nonFungibleTokens = nonFungibleTokensDeferred.awaitAll(),
//                        nonFungibleTokensIds = nonFungibleTokensIdsDeferred.awaitAll()
//                    )
//
//                    accountResourcesJobs.fungibleTokens.forEachIndexed { index, result ->
//                        val fungibleToken = accountResources.data.simpleFungibleTokens[index]
//                        result.onValue { entityDetailsResponse ->
//                            fungibleTokens.add(
//                                OwnedFungibleToken(
//                                    fungibleToken.owner,
//                                    fungibleToken.amount,
//                                    fungibleToken.address,
//                                    entityDetailsResponse.toFungibleToken()
//                                )
//                            )
//                        }
//                    }
//
//                    accountResourcesJobs.nonFungibleTokens.forEachIndexed { index, result ->
//                        val nonFungibleToken = accountResources.data.simpleNonFungibleTokens[index]
//                        val nonFungibleId = accountResourcesJobs.nonFungibleTokensIds[index]
//
//                        result.onValue { entityDetailsResponse ->
//                            nonFungibleId.onValue { nonFungibleTokenIdContainer ->
//                                nonFungibleTokens.add(
//                                    OwnedNonFungibleToken(
//                                        nonFungibleToken.owner,
//                                        nonFungibleToken.amount,
//                                        nonFungibleToken.tokenResourceAddress,
//                                        entityDetailsResponse.toNonFungibleToken(nonFungibleTokenIdContainer)
//                                    )
//                                )
//                            }
//                        }
//                    }
//                }
//
//                Result.Success(
//                    data = AccountResources(
//                        address = address,
//                        displayName = accountDisplayName,
//                        currencySymbol = "$", // TODO replace when endpoint ready
//                        value = "100",
//                        fungibleTokens = fungibleTokens,
//                        nonFungibleTokens = nonFungibleTokens,
//                        appearanceID = appearanceId
//                    )
//                )
//            }
//        }
//    }
//}
//
//data class AccountResourcesJobs(
//    val fungibleTokens: List<Result<EntityDetailsResponse>>,
//    val nonFungibleTokens: List<Result<EntityDetailsResponse>>,
//    val nonFungibleTokensIds: List<Result<NonFungibleTokenIdContainer>>,
//)
