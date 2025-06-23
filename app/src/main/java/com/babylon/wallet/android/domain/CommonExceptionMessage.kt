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
fun CommonException.toMessage(includeSupportMessage: Boolean = true): String =
    with(LocalContext.current) {
        toMessage(
            context = this,
            includeSupportMessage = includeSupportMessage
        )
    }

fun CommonException.toMessage(
    context: Context,
    includeSupportMessage: Boolean = true
): String {
    val messageBuilder = StringBuilder()

    messageBuilder.appendLine(publicMessage(context))

    if (includeSupportMessage) {
        messageBuilder.appendLine()
        messageBuilder.appendLine(
            context.getString(
                R.string.error_emailSupportMessage,
                errorCodeFromError(error = this).toString()
            )
        )
    }

    if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
        messageBuilder.appendLine()
        messageBuilder.appendLine("Debug message:")
        messageBuilder.appendLine(errorMessageFromError(error = this))
    }

    return messageBuilder.toString()
}

fun CommonException.mailReportMessage(): String {
    val messageBuilder = StringBuilder()

    messageBuilder.appendLine("Code: ${errorCodeFromError(error = this)}")
    messageBuilder.appendLine()
    messageBuilder.appendLine("Error Message:")
    messageBuilder.appendLine(errorMessageFromError(error = this))
    messageBuilder.appendLine()
    messageBuilder.appendLine("================")
    messageBuilder.appendLine()
    messageBuilder.appendLine(stackTraceToString())

    return messageBuilder.toString()
}

private fun CommonException.publicMessage(context: Context) = when (this) {
    is CommonException.UnableToLoadMnemonicFromSecureStorage -> "Please restore your Seed Phrase and try again"
    is CommonException.SecureStorageAccessException ->
        "There was an issue trying to access you Seed Phrase from secure storage. ${this.errorMessage}"

    is CommonException.SecureStorageReadException -> context.getString(R.string.homePage_secureFolder_warning)
    is CommonException.MissingNftDataField,
    is CommonException.UnexpectedNftDataFormat,
    is CommonException.RnsInvalidDomain,
    is CommonException.RnsUnauthenticDomain,
    is CommonException.RnsInvalidDomainConfiguration,
    is CommonException.RnsInvalidRecordContext,
    is CommonException.GwMissingResponseItem -> context.getString(R.string.error_rns_unknownDomain)

    is CommonException.RnsUnsupportedNetwork -> context.getString(R.string.error_transactionFailure_network)
    else -> context.getString(R.string.common_somethingWentWrong)
}
