package com.babylon.wallet.android.utils

import android.content.Context
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.asRadixWalletException
import com.babylon.wallet.android.domain.toUserFriendlyMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ExceptionMessageProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun throwableMessage(throwable: Throwable): String {
        return throwable.getMessage(context)
    }
}

private fun Throwable.getMessage(context: Context): String {
    return asRadixWalletException()?.toUserFriendlyMessage(context) ?: message
        ?: context.getString(R.string.common_somethingWentWrong)
}
