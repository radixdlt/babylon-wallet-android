package com.babylon.wallet.android.domain.usecase.wallet

import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsResponse
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
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

@Suppress("LongMethod")
class GetAccountResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository,
    private val nonFungibleRepository: NonFungibleRepository,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        address: String
    ): Result<AccountResources> = coroutineScope {
        var accountDisplayName = ""
        var appearanceId = -1
        profileRepository.readProfileSnapshot()?.let { profileSnapshot ->
            val account = profileSnapshot.toProfile().getAccountByAddress(address)
            accountDisplayName = account?.displayName.orEmpty()
            appearanceId = account?.appearanceID ?: -1
        }

        when (val accountResourcesResult = entityRepository.getAccountResources(address)) {
            is Result.Error -> Result.Error(accountResourcesResult.exception)
            is Result.Success -> {
                val fungibleTokens = mutableListOf<OwnedFungibleToken>()
                val nonFungibleTokens = mutableListOf<OwnedNonFungibleToken>()

                accountResourcesResult.data.let { accountResources ->
                    val fungibleTokensDeferred = accountResources.simpleFungibleTokens.map { fungibleToken ->
                        async {
                            entityRepository.entityDetails(fungibleToken.address)
                        }
                    }

                    val nonFungibleTokensDeferred = mutableListOf<Deferred<Result<EntityDetailsResponse>>>()
                    val nonFungibleIdsDeferred = mutableListOf<Deferred<Result<NonFungibleTokenIdContainer>>>()

                    accountResources.simpleNonFungibleTokens
                        .map { nonFungibleToken ->
                            nonFungibleTokensDeferred.add(
                                async {
                                    entityRepository.entityDetails(nonFungibleToken.tokenResourceAddress)
                                }
                            )
                            nonFungibleIdsDeferred.add(
                                async {
                                    nonFungibleRepository.nonFungibleIds(nonFungibleToken.tokenResourceAddress)
                                }
                            )
                        }

                    // Run all requests simultaneously
                    val accountResourcesJobs = AccountResourcesJobs(
                        fungibleTokensDeferred.awaitAll(),
                        nonFungibleTokensDeferred.awaitAll(),
                        nonFungibleIdsDeferred.awaitAll()
                    )

                    accountResourcesJobs.fungibleResults.forEachIndexed { index, result ->
                        val fungibleToken = accountResourcesResult.data.simpleFungibleTokens[index]
                        result.onValue { response ->
                            fungibleTokens.add(
                                OwnedFungibleToken(
                                    fungibleToken.owner,
                                    fungibleToken.amount,
                                    fungibleToken.address,
                                    response.toFungibleToken()
                                )
                            )
                        }
                    }

                    accountResourcesJobs.nonFungibleResults.forEachIndexed { index, result ->
                        val nonFungibleToken = accountResourcesResult.data.simpleNonFungibleTokens[index]
                        val nonFungibleId = accountResourcesJobs.nonFungibleIdResults[index]

                        result.onValue { response ->
                            nonFungibleId.onValue { nonFungibleIdResponse ->
                                nonFungibleTokens.add(
                                    OwnedNonFungibleToken(
                                        nonFungibleToken.owner,
                                        nonFungibleToken.amount,
                                        nonFungibleToken.tokenResourceAddress,
                                        response.toNonFungibleToken(nonFungibleIdResponse)
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
    val fungibleResults: List<Result<EntityDetailsResponse>>,
    val nonFungibleResults: List<Result<EntityDetailsResponse>>,
    val nonFungibleIdResults: List<Result<NonFungibleTokenIdContainer>>
)
