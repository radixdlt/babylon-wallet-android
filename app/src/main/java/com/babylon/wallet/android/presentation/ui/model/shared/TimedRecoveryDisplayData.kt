package com.babylon.wallet.android.presentation.ui.model.shared

import com.babylon.wallet.android.utils.TimeFormatter
import com.radixdlt.sargon.AddressOfAccountOrPersona
import kotlin.time.Duration

data class TimedRecoveryDisplayData(
    val remainingTime: Duration?,
    val entityAddress: AddressOfAccountOrPersona
) {

    val formattedTime = remainingTime?.let { TimeFormatter.formatShortTruncatedToHours(it) }
}
