package com.babylon.wallet.android.domain.usecase.wallet

import com.babylon.wallet.android.data.gateway.toFungibleToken
import com.babylon.wallet.android.data.gateway.toNonFungibleToken
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken
import javax.inject.Inject

class RequestAccountResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository,
) {
    suspend fun getAccountResources(address: String): Result<AccountResources> {
        return when (val accountResourcesResult = entityRepository.getAccountResources(address)) {
            is Result.Error -> Result.Error(accountResourcesResult.exception)
            is Result.Success -> {
                return Result.Success(
                    data = accountResourcesResult.data.let { accountResources ->
                        val fungibleTokens = mutableListOf<OwnedFungibleToken>()
                        accountResources.simpleFungibleTokens.forEach { fungibleToken ->
                            entityRepository.entityDetails(fungibleToken.address).onValue { response ->
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
                        val nonFungibleTokens = mutableListOf<OwnedNonFungibleToken>()
                        accountResources.simpleNonFungibleTokens.forEach { nonFungibleToken ->
                            entityRepository.entityDetails(nonFungibleToken.tokenResourceAddress).onValue { response ->
                                nonFungibleTokens.add(
                                    OwnedNonFungibleToken(
                                        nonFungibleToken.owner,
                                        nonFungibleToken.amount,
                                        nonFungibleToken.tokenResourceAddress,
                                        response.toNonFungibleToken()
                                    )
                                )
                            }
                        }
                        AccountResources(
                            address,
                            fungibleTokens = fungibleTokens.toList(),
                            nonFungibleTokens = nonFungibleTokens.toList()
                        )
                    }
                )
            }
        }
    }
}
