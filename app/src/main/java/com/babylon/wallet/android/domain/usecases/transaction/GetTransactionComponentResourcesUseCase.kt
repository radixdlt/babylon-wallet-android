package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.MetadataConstants.KEY_ICON
import com.babylon.wallet.android.domain.model.MetadataConstants.KEY_SYMBOL
import com.babylon.wallet.android.presentation.transaction.TransactionAccountItemUiModel
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

class GetTransactionComponentResourcesUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val entityRepository: EntityRepository
) {
    @Suppress("LongMethod")
    suspend fun invoke(
        componentAddresses: List<String>,
        amount: String
    ): Result<TransactionAccountItemUiModel> {
        val entityDetailsResponse = entityRepository.stateEntityDetails(
            addresses = componentAddresses,
            isRefreshing = false
        )
        var account: Result<TransactionAccountItemUiModel> = Result.Error()
        entityDetailsResponse.onError {
            // TODO This is a hack but will exists until KET will return appropriate flag
            val accountAddressesOnly = componentAddresses.filter { address ->
                address.startsWith("account_")
            }
            val accountsDetailsResponse = entityRepository.stateEntityDetails(
                addresses = accountAddressesOnly,
                isRefreshing = false
            )
            accountsDetailsResponse.onValue { stateAccountDetailsResponse ->
                val accountItem = stateAccountDetailsResponse.items.filter {
                    it.details?.type == StateEntityDetailsResponseItemDetailsType.component
                }.first()

                val accountDisplayName = accountRepository
                    .getAccountByAddress(accountItem.address)?.displayName.orEmpty()

                val accountAppearanceId = accountRepository
                    .getAccountByAddress(accountItem.address)?.appearanceID ?: 1

                account = Result.Success(
                    TransactionAccountItemUiModel(
                        address = accountItem.address,
                        displayName = accountDisplayName,
                        tokenSymbol = "Unknown",
                        tokenQuantity = amount,
                        fiatAmount = "$1234.12",
                        appearanceID = accountAppearanceId,
                        iconUrl = ""
                    )
                )
            }
        }

        entityDetailsResponse.onValue { stateEntityDetailsResponse ->
            val resourceItem = stateEntityDetailsResponse.items.first {
                it.details?.type == StateEntityDetailsResponseItemDetailsType.fungibleResource
            }
            val accountItem = stateEntityDetailsResponse.items.first {
                it.details?.type == StateEntityDetailsResponseItemDetailsType.component
            }

            var tokenSymbol = ""
            var iconUrl = ""
            if (resourceItem.metadata.asMetadataStringMap().containsKey(KEY_SYMBOL)) {
                tokenSymbol = resourceItem.metadata.asMetadataStringMap().getValue(KEY_SYMBOL)
            }
            if (resourceItem.metadata.asMetadataStringMap().containsKey(KEY_ICON)) {
                iconUrl = resourceItem.metadata.asMetadataStringMap().getValue(KEY_ICON)
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
