package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.gateway.generated.models.EntityDetailsResponse
import com.babylon.wallet.android.data.gateway.toFungibleToken
import com.babylon.wallet.android.data.gateway.toNonFungibleToken
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.nonfungible.NonFungibleRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

class GetAccountResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository,
    private val nonFungibleRepository: NonFungibleRepository,
    private val accountRepository: AccountRepository
) {

    suspend operator fun invoke(isRefreshing: Boolean, failOnAnyError: Boolean = true): Result<List<AccountResources>> = coroutineScope {
        val accountResourceList = mutableListOf<AccountResources>()
        val results = accountRepository.getAccounts().map { account ->
            async {
                getSingleAccountResources(
                    account.address,
                    account.displayName,
                    account.appearanceID,
                    isRefreshing
                )
            }
        }.awaitAll()

        results.forEach { result ->
            if (failOnAnyError && result is Result.Error) return@coroutineScope Result.Error(result.exception)
            result.onValue {
                accountResourceList.add(it)
            }
        }
        if (accountResourceList.isNotEmpty()) {
            Result.Success(accountResourceList.toList())
        } else {
            Result.Error()
        }
    }

    suspend operator fun invoke(address: String, isRefreshing: Boolean): Result<AccountResources> {
        val account = accountRepository.getAccountByAddress(address)
        requireNotNull(account) {
            "account is null"
        }
        return getSingleAccountResources(
            account.address,
            account.displayName,
            account.appearanceID,
            isRefreshing
        )
    }

    @Suppress("LongMethod")
    private suspend fun getSingleAccountResources(
        address: String,
        accountDisplayName: String,
        appearanceId: Int,
        isRefreshing: Boolean
    ) = coroutineScope {
        when (val accountResources = entityRepository.getAccountResources(address, isRefreshing)) {
            is Result.Error -> Result.Error(accountResources.exception)
            is Result.Success -> {
                val fungibleTokens = mutableListOf<OwnedFungibleToken>()
                val nonFungibleTokens = mutableListOf<OwnedNonFungibleToken>()

                accountResources.data.let { resources ->
                    val fungibleTokensDeferred = resources.simpleFungibleTokens.map { fungibleToken ->
                        async {
                            entityRepository.entityDetails(
                                address = fungibleToken.address,
                                isRefreshing = isRefreshing
                            )
                        }
                    }

                    val nonFungibleTokensDeferred = mutableListOf<Deferred<Result<EntityDetailsResponse>>>()
                    val nonFungibleTokensIdsDeferred = mutableListOf<Deferred<Result<NonFungibleTokenIdContainer>>>()

                    resources.simpleNonFungibleTokens
                        .map { nonFungibleToken ->
                            nonFungibleTokensDeferred.add(
                                async {
                                    entityRepository.entityDetails(
                                        address = nonFungibleToken.tokenResourceAddress,
                                        isRefreshing = isRefreshing
                                    )
                                }
                            )
                            nonFungibleTokensIdsDeferred.add(
                                async {
                                    nonFungibleRepository.nonFungibleIds(
                                        address = nonFungibleToken.tokenResourceAddress,
                                        isRefreshing = isRefreshing
                                    )
                                }
                            )
                        }

                    // Run all requests simultaneously
                    val accountResourcesJobs = AccountResourcesJobs(
                        fungibleTokens = fungibleTokensDeferred.awaitAll(),
                        nonFungibleTokens = nonFungibleTokensDeferred.awaitAll(),
                        nonFungibleTokensIds = nonFungibleTokensIdsDeferred.awaitAll()
                    )

                    accountResourcesJobs.fungibleTokens.forEachIndexed { index, result ->
                        val fungibleToken = accountResources.data.simpleFungibleTokens[index]
                        result.onValue { entityDetailsResponse ->
                            fungibleTokens.add(
                                OwnedFungibleToken(
                                    fungibleToken.owner,
                                    fungibleToken.amount,
                                    fungibleToken.address,
                                    entityDetailsResponse.toFungibleToken()
                                )
                            )
                        }
                    }

                    accountResourcesJobs.nonFungibleTokens.forEachIndexed { index, result ->
                        val nonFungibleToken = accountResources.data.simpleNonFungibleTokens[index]
                        val nonFungibleId = accountResourcesJobs.nonFungibleTokensIds[index]

                        result.onValue { entityDetailsResponse ->
                            nonFungibleId.onValue { nonFungibleTokenIdContainer ->
                                nonFungibleTokens.add(
                                    OwnedNonFungibleToken(
                                        nonFungibleToken.owner,
                                        nonFungibleToken.amount,
                                        nonFungibleToken.tokenResourceAddress,
                                        entityDetailsResponse.toNonFungibleToken(nonFungibleTokenIdContainer)
                                    )
                                )
                            }
                        }
                    }
                }

                Result.Success(
                    data = AccountResources(
                        address = address,
                        displayName = accountDisplayName,
                        currencySymbol = "$", // TODO replace when endpoint ready
                        value = "100",
                        fungibleTokens = fungibleTokens,
                        nonFungibleTokens = nonFungibleTokens,
                        appearanceID = appearanceId
                    )
                )
            }
        }
    }
}

data class AccountResourcesJobs(
    val fungibleTokens: List<Result<EntityDetailsResponse>>,
    val nonFungibleTokens: List<Result<EntityDetailsResponse>>,
    val nonFungibleTokensIds: List<Result<NonFungibleTokenIdContainer>>,
)
