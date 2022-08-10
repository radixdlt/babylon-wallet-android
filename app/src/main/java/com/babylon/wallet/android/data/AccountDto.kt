package com.babylon.wallet.android.data

import com.babylon.wallet.android.data.AssetDto.Companion.toTokenUiModel
import com.babylon.wallet.android.data.AssetDto.NftClassDto.Companion.toNftClassUiModel
import com.babylon.wallet.android.domain.AssetType
import com.babylon.wallet.android.presentation.model.AccountUi
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
            tokens = assets.filter { assetDto ->
                assetDto.type == AssetType.TOKEN.typeId
            }.map { assetDto ->
                assetDto.toTokenUiModel()
            },
            nfts = assets.filter { assetDto ->
                assetDto.type == AssetType.NTF.typeId
            }.flatMap { assetDto ->
                assetDto.nftClasses?.map { nftClassDto ->
                    nftClassDto.toNftClassUiModel()
                }.orEmpty()
            }
        )
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
