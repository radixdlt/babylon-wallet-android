package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.presentation.transaction.TransactionAccountItemUiModel
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

class GetTransactionComponentResourcesUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val entityRepository: EntityRepository
) {
    suspend fun invoke(
        accountAddresses: List<String>,
        amount: String
    ): Result<TransactionAccountItemUiModel> {
        val entityDetailsResponse = entityRepository.stateEntityDetails(
            addresses = accountAddresses
        )
        var account: Result<TransactionAccountItemUiModel> = Result.Error()
        entityDetailsResponse.onValue { stateEntityDetailsResponse ->
            val resourceItem = stateEntityDetailsResponse.items.first {
                it.details?.type == StateEntityDetailsResponseItemDetailsType.fungibleResource
            }
            val accountItem = stateEntityDetailsResponse.items.first {
                it.details?.type == StateEntityDetailsResponseItemDetailsType.component
            }

            var tokenSymbol = ""
            var iconUrl = ""
            if (resourceItem.metadata.asMetadataStringMap().containsKey("symbol")) {
                tokenSymbol = resourceItem.metadata.asMetadataStringMap().getValue("symbol")
            }
            if (resourceItem.metadata.asMetadataStringMap().containsKey("icon")) {
                iconUrl = resourceItem.metadata.asMetadataStringMap().getValue("icon")
            }

            val accountDisplayName = accountRepository
                .getAccountByAddress(accountItem.address)?.displayName.orEmpty()

            val accountAppearanceId = accountRepository
                .getAccountByAddress(accountItem.address)?.appearanceID ?: 1

            account = Result.Success(
                TransactionAccountItemUiModel(
                    address = accountItem.address,
                    displayName = accountDisplayName,
                    tokenSymbol = tokenSymbol,
                    tokenQuantity = amount,
                    fiatAmount = "$1234.12",
                    appearanceID = accountAppearanceId,
                    iconUrl = iconUrl
                )
            )
        }
        return account
    }
}
