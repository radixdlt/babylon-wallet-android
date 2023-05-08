package com.babylon.wallet.android.utils

import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel
import rdx.works.profile.data.model.factorsources.FactorSource
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun FactorSource.addedOnTimestampFormatted(): String {
    val formatter = DateTimeFormatter.ofPattern(LAST_USED_PERSONA_DATE_FORMAT).withZone(ZoneId.systemDefault())
    return formatter.format(lastUsedOn)
}

fun FactorSource.getLedgerDeviceModel(): LedgerDeviceModel? {
    return when (description) {
        "nanoS" -> LedgerDeviceModel.NanoS
        "nanoS+" -> LedgerDeviceModel.NanoSPlus
        "nanoX" -> LedgerDeviceModel.NanoX
        else -> null
    }
}
