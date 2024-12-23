package com.babylon.wallet.android.domain

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.errorCodeFromError
import com.radixdlt.sargon.errorMessageFromError

@Composable
fun CommonException.toMessage(): String = with(LocalContext.current) {
    toMessage(context = this)
}

fun CommonException.toMessage(context: Context): String {
    val messageBuilder = StringBuilder()

    messageBuilder.appendLine(publicMessage(context))

    if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
        messageBuilder.appendLine()
        messageBuilder.appendLine("[${errorCodeFromError(error = this)}]")
        messageBuilder.appendLine(errorMessageFromError(error = this))
    }

    return messageBuilder.toString()
}

private fun CommonException.publicMessage(context: Context) = when (this) {
    is CommonException.UnableToLoadMnemonicFromSecureStorage -> "Please restore your Seed Phrase and try again"
    is CommonException.SecureStorageAccessException ->
        "There was an issue trying to access you Seed Phrase from secure storage. ${this.errorMessage}"
    is CommonException.SecureStorageReadException -> context.getString(R.string.homePage_secureFolder_warning)
    else -> context.getString(R.string.common_somethingWentWrong)
}
