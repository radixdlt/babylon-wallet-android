package com.babylon.wallet.android.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.radixdlt.sargon.TimePeriod
import com.radixdlt.sargon.TimePeriodUnit

@Composable
fun TimePeriod.title(): String {
    val value = value.toInt()
    val isSingular = value == 1
    return when (unit) {
        TimePeriodUnit.DAYS -> if (isSingular) {
            stringResource(id = R.string.shieldWizardRecovery_fallback_day_period)
        } else {
            stringResource(id = R.string.shieldWizardRecovery_fallback_days_period, value)
        }
        TimePeriodUnit.WEEKS -> if (isSingular) {
            stringResource(id = R.string.shieldWizardRecovery_fallback_week_period)
        } else {
            stringResource(id = R.string.shieldWizardRecovery_fallback_weeks_period, value)
        }

        TimePeriodUnit.MINUTES -> if (isSingular) "1 minute" else "$value minutes"
    }
}

@Composable
fun TimePeriodUnit.displayName(): String = when (this) {
    TimePeriodUnit.DAYS -> stringResource(id = R.string.shieldWizardRecovery_fallback_days_label)
    TimePeriodUnit.WEEKS -> stringResource(id = R.string.shieldWizardRecovery_fallback_weeks_label)
    TimePeriodUnit.MINUTES -> "Minutes"
}
