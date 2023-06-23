package com.babylon.wallet.android.utils

import com.babylon.wallet.android.data.dapp.model.AccountsRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.Network

fun LedgerHardwareWalletFactorSource.getLedgerDeviceModel(): LedgerDeviceModel? {
    return when (this.hint.model) {
        "nanoS" -> LedgerDeviceModel.NanoS
        "nanoS+" -> LedgerDeviceModel.NanoSPlus
        "nanoX" -> LedgerDeviceModel.NanoX
        else -> null
    }
}

fun List<Network.Account>.toDataModel(): AccountsRequestResponseItem? {
    if (this.isEmpty()) {
        return null
    }

    val accounts = map { accountItemUiModel ->
        AccountsRequestResponseItem.Account(
            address = accountItemUiModel.address,
            label = accountItemUiModel.displayName,
            appearanceId = accountItemUiModel.appearanceID
        )
    }

    return AccountsRequestResponseItem(
        accounts = accounts,
        challenge = null,
        proofs = null
    )
}
