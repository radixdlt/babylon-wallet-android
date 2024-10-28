package com.babylon.wallet.android.utils

import com.radixdlt.sargon.Epoch

fun Epoch.toMinutes(windowDurationInMinutes: ULong = 5U): Epoch {
    return this * windowDurationInMinutes
}
