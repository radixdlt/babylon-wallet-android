package com.babylon.wallet.android.data

import com.babylon.wallet.android.data.AssetDto.Companion.toTokenUiModel
import com.babylon.wallet.android.data.AssetDto.NftClassDto.Companion.toNftClassUiModel
import com.babylon.wallet.android.data.profile.model.Account
import com.babylon.wallet.android.data.profile.model.Address
import com.babylon.wallet.android.domain.AssetType
import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.model.TokenUi
import java.text.NumberFormat
import java.util.Locale

// WARNING: this is not the actual data structure of the data transfer object and
// might change completely in the future, since we don't have any knowledge of the backend.
//
data class AccountDto(
    val id: String,
    val name: String,
    val hash: String,
    val value: Float,
    val currency: String,
    val assets: List<AssetDto>
) {

    companion object {
        fun AccountDto.toUiModel() = AccountUi(
            id = id,
            name = name,
            hash = hash,
            amount = amountToUiFormat(value, currency),
            currencySymbol = currency,
            tokens = sortTokens(
                assets.filter { assetDto ->
                    assetDto.type == AssetType.TOKEN.typeId
                }
            ),
            nfts = assets.filter { assetDto ->
                assetDto.type == AssetType.NTF.typeId
            }.flatMap { assetDto ->
                assetDto.nftClasses?.map { nftClassDto ->
                    nftClassDto.toNftClassUiModel()
                }.orEmpty()
            }
        )
        fun AccountDto.toDAppUiModel() = Account(
            name = name,
            address = Address(hash),
            value = value.toString(),
            currency = currency
        )
    }

    // 1. The Radix token (XRD), if the account holds any, is always shown at top of the list
    // 2. Those that have a market price (= tokenValue) available are all displayed above those that do not have a one
    // 3. Within the tokens that have a market price, sort them by the total value the user holds of
    //    that token (not the price of 1 unit of the token), highest to lowest
    //
    // We might move this later to the domain layer and keep there the business logic!
    // AccountDto => Account, Token, Nft + sorting => TokenUi, NftUi, ...
    private fun sortTokens(tokensList: List<AssetDto>): List<TokenUi> {
        return tokensList.toMutableList().sortedWith(
            compareBy(
                { it.symbol == "XRD" && it.name == "Radix" },
                { it.marketPrice?.times(it.tokenQuantity) }
            )
        ).map {
            it.toTokenUiModel()
        }.reversed()
    }

    // TODO the format api returns the symbol alongside the amount,
    //  so we can drop later the "currencySymbol" from the AccountUi model
    private fun amountToUiFormat(amount: Float, currencySymbol: String): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        var formattedAmount = currencyFormat.format(amount)
        formattedAmount = formattedAmount.removePrefix(currencySymbol)
        return formattedAmount
    }
}
