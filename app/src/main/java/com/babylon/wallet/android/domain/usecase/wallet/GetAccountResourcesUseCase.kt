package com.babylon.wallet.android.domain.usecase.wallet

import com.babylon.wallet.android.data.gateway.toFungibleToken
import com.babylon.wallet.android.data.gateway.toNonFungibleToken
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.nonfungible.NonFungibleRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class GetAccountResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository,
    private val nonFungibleRepository: NonFungibleRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(address: String): Result<AccountResources> {
        var accountDisplayName = ""
        profileRepository.readProfileSnapshot()?.let { profileSnapshot ->
            accountDisplayName = profileSnapshot.toProfile().getAccountByAddress(address)?.displayName.orEmpty()
        }
        return when (val accountResourcesResult = entityRepository.getAccountResources(address)) {
            is Result.Error -> Result.Error(accountResourcesResult.exception)
            is Result.Success -> {
                return Result.Success(
                    data = accountResourcesResult.data.let { accountResources ->
                        // TODO make api calls to run simultaneously, not one by one to increase efficiency
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
                            val nonFungibleIds =
                                nonFungibleRepository.nonFungibleIds(nonFungibleToken.tokenResourceAddress).value()
                            entityRepository.entityDetails(nonFungibleToken.tokenResourceAddress).onValue { response ->
                                nonFungibleTokens.add(
                                    OwnedNonFungibleToken(
                                        nonFungibleToken.owner,
                                        nonFungibleToken.amount,
                                        nonFungibleToken.tokenResourceAddress,
                                        response.toNonFungibleToken(nonFungibleIds)
                                    )
                                )
                            }
                        }
                        AccountResources(
                            address = address,
                            displayName = accountDisplayName,
                            currencySymbol = "$", // TODO replace when endpoint ready
                            value = "100",
                            fungibleTokens = fungibleTokens.toList(),
                            nonFungibleTokens = nonFungibleTokens.toList()
                        )
                    }
                )
            }
        }
    }
}
