package com.babylon.wallet.android.domain

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.ProfileException

sealed class RadixWalletException(cause: Throwable? = null) : Throwable(cause = cause) {
    data object DappMetadataEmpty : RadixWalletException()
    data object SignatureCancelled : RadixWalletException()
    data object FailedToCollectLedgerSignature : RadixWalletException()
    data object FailedToCollectSigners : RadixWalletException()

    sealed class IncomingMessageException(cause: Throwable? = null) : RadixWalletException(cause = cause) {
        data class MessageParse(override val cause: Throwable? = null) : IncomingMessageException(cause)
        data class LedgerResponseParse(override val cause: Throwable? = null) : IncomingMessageException(cause)
        data class Unknown(override val cause: Throwable? = null) : IncomingMessageException(cause)
    }

    sealed class DappRequestException(cause: Throwable? = null) :
        RadixWalletException(cause = cause),
        ConnectorExtensionThrowable {

        data object GetEpoch : DappRequestException()
        data object RejectedByUser : DappRequestException()
        data object InvalidRequest : DappRequestException()
        data object UnacceptableManifest : DappRequestException()
        data object InvalidPersona : DappRequestException()
        data object InvalidRequestChallenge : DappRequestException()
        data object NotPossibleToAuthenticateAutomatically : DappRequestException()
        data class FailedToSignAuthChallenge(override val cause: Throwable? = null) :
            DappRequestException(cause = cause)

        data class WrongNetwork(
            val currentNetworkId: Int,
            val requestNetworkId: Int
        ) : DappRequestException() {
            val currentNetworkName: String
                get() = runCatching {
                    Radix.Network.fromId(currentNetworkId)
                }.getOrNull()?.name.orEmpty().replaceFirstChar(Char::titlecase)
            val requestNetworkName: String
                get() = runCatching {
                    Radix.Network.fromId(requestNetworkId)
                }.getOrNull()?.name.orEmpty().replaceFirstChar(Char::titlecase)
        }

        override val ceError: ConnectorExtensionError
            get() = when (this) {
                GetEpoch -> WalletErrorType.FailedToPrepareTransaction
                RejectedByUser -> WalletErrorType.RejectedByUser
                is WrongNetwork -> WalletErrorType.WrongNetwork
                InvalidPersona -> WalletErrorType.InvalidPersona
                InvalidRequest -> WalletErrorType.InvalidRequest
                is FailedToSignAuthChallenge -> WalletErrorType.FailedToSignAuthChallenge
                UnacceptableManifest -> WalletErrorType.FailedToPrepareTransaction
                is InvalidRequestChallenge -> WalletErrorType.InvalidRequest
                NotPossibleToAuthenticateAutomatically -> WalletErrorType.InvalidRequest
            }
    }

    sealed class PrepareTransactionException(cause: Throwable? = null) :
        RadixWalletException(cause = cause),
        ConnectorExtensionThrowable {
        data object ConvertManifest : PrepareTransactionException()
        data class BuildTransactionHeader(override val cause: Throwable) : PrepareTransactionException(cause)
        data object FailedToFindAccountWithEnoughFundsToLockFee : PrepareTransactionException()
        data object CompileTransactionIntent : PrepareTransactionException()
        data class SignCompiledTransactionIntent(override val cause: Throwable? = null) :
            PrepareTransactionException(cause)

        data class PrepareNotarizedTransaction(override val cause: Throwable? = null) :
            PrepareTransactionException(cause)

        data class SubmitNotarizedTransaction(override val cause: Throwable? = null) : PrepareTransactionException()
        data object ReceivingAccountDoesNotAllowDeposits : PrepareTransactionException()

        override val ceError: ConnectorExtensionError
            get() = when (this) {
                is BuildTransactionHeader -> WalletErrorType.FailedToPrepareTransaction
                ConvertManifest -> WalletErrorType.FailedToPrepareTransaction
                is PrepareNotarizedTransaction -> WalletErrorType.FailedToSignTransaction
                is SubmitNotarizedTransaction -> WalletErrorType.FailedToSubmitTransaction
                FailedToFindAccountWithEnoughFundsToLockFee -> {
                    WalletErrorType.FailedToFindAccountWithEnoughFundsToLockFee
                }

                CompileTransactionIntent -> WalletErrorType.FailedToCompileTransaction
                is SignCompiledTransactionIntent -> WalletErrorType.FailedToSignTransaction
                ReceivingAccountDoesNotAllowDeposits -> WalletErrorType.FailedToPrepareTransaction
            }
    }

    sealed class TransactionSubmitException : RadixWalletException(), ConnectorExtensionThrowable {
        data class InvalidTXDuplicate(val txId: String) : TransactionSubmitException()
        data class FailedToPollTXStatus(val txId: String) : TransactionSubmitException()
        data class GatewayRejected(val txId: String) : TransactionSubmitException()
        data class GatewayCommittedException(val txId: String) : TransactionSubmitException()

        sealed class TransactionRejected : TransactionSubmitException() {
            data class Permanently(val txId: String) : TransactionRejected()
            data class Temporary(val txId: String, val txProcessingTime: String) : TransactionRejected()
        }

        sealed class TransactionCommitted : TransactionSubmitException() {
            data class Failure(val txId: String) : TransactionCommitted()
        }

        fun getDappMessage(): String? {
            return when (this) {
                is FailedToPollTXStatus -> "TXID: $txId"
                is InvalidTXDuplicate -> "TXID: $txId"
                is TransactionCommitted.Failure -> "TXID: $txId"
                else -> null
            }
        }

        override val ceError: ConnectorExtensionError
            get() = when (this) {
                is FailedToPollTXStatus -> WalletErrorType.FailedToPollSubmittedTransaction
                is GatewayCommittedException -> {
                    WalletErrorType.SubmittedTransactionHasFailedTransactionStatus
                }

                is GatewayRejected -> {
                    WalletErrorType.SubmittedTransactionHasRejectedTransactionStatus
                }

                is InvalidTXDuplicate -> WalletErrorType.SubmittedTransactionWasDuplicate
                is TransactionCommitted.Failure -> WalletErrorType.SubmittedTransactionHasFailedTransactionStatus
                is TransactionRejected.Permanently -> WalletErrorType.SubmittedTransactionHasPermanentlyRejectedTransactionStatus
                is TransactionRejected.Temporary -> WalletErrorType.SubmittedTransactionHasTemporarilyRejectedTransactionStatus
            }
    }

    sealed class DappVerificationException : RadixWalletException(), ConnectorExtensionThrowable {
        data object WrongAccountType : DappVerificationException()
        data object UnknownWebsite : DappVerificationException()
        data object RadixJsonNotFound : DappVerificationException()
        data object UnknownDefinitionAddress : DappVerificationException()
        data object ClaimedEntityAddressNotPresent : DappVerificationException()

        override val ceError: ConnectorExtensionError
            get() = when (this) {
                RadixJsonNotFound -> WalletErrorType.RadixJsonNotFound
                UnknownDefinitionAddress -> WalletErrorType.UnknownDefinitionAddress
                UnknownWebsite -> WalletErrorType.UnknownWebsite
                WrongAccountType -> WalletErrorType.WrongAccountType
                ClaimedEntityAddressNotPresent -> WalletErrorType.WrongAccountType
            }
    }

    sealed class LedgerCommunicationException : RadixWalletException(), ConnectorExtensionThrowable {
        data object FailedToGetDeviceId : LedgerCommunicationException()
        data object FailedToDerivePublicKeys : LedgerCommunicationException()
        data object FailedToDeriveAndDisplayAddress : LedgerCommunicationException()
        data object FailedToSignAuthChallenge : LedgerCommunicationException()
        data class FailedToSignTransaction(val reason: LedgerErrorCode) : LedgerCommunicationException()

        override val ceError: ConnectorExtensionError
            get() = when (this) {
                is FailedToDerivePublicKeys -> WalletErrorType.InvalidRequest
                FailedToGetDeviceId -> WalletErrorType.InvalidRequest
                is FailedToSignTransaction -> WalletErrorType.InvalidRequest
                is FailedToDeriveAndDisplayAddress -> WalletErrorType.InvalidRequest
                FailedToSignAuthChallenge -> WalletErrorType.InvalidRequest
            }
    }
}

typealias ConnectorExtensionError = WalletErrorType

interface ConnectorExtensionThrowable {
    val ceError: ConnectorExtensionError
}

fun RadixWalletException.LedgerCommunicationException.toUserFriendlyMessage(context: Context): String {
    return context.getString(
        when (this) {
            RadixWalletException.LedgerCommunicationException.FailedToDerivePublicKeys -> {
                R.string.ledgerHardwareDevices_verification_requestFailed
            }

            RadixWalletException.LedgerCommunicationException.FailedToGetDeviceId -> {
                R.string.common_somethingWentWrong
            } // TODO consider different copy
            is RadixWalletException.LedgerCommunicationException.FailedToSignTransaction -> when (this.reason) {
                LedgerErrorCode.Generic -> R.string.common_somethingWentWrong
                LedgerErrorCode.BlindSigningNotEnabledButRequired -> R.string.error_transactionFailure_blindSigningNotEnabledButRequired
                LedgerErrorCode.UserRejectedSigningOfTransaction -> R.string.error_transactionFailure_rejected
            }

            is RadixWalletException.LedgerCommunicationException.FailedToDeriveAndDisplayAddress -> R.string.common_somethingWentWrong
            RadixWalletException.LedgerCommunicationException.FailedToSignAuthChallenge -> {
                R.string.ledgerHardwareDevices_verification_requestFailed
            }
        }
    )
}

fun RadixWalletException.DappVerificationException.toUserFriendlyMessage(context: Context): String {
    return context.getString(
        when (this) {
            RadixWalletException.DappVerificationException.RadixJsonNotFound -> {
                R.string.dAppRequest_validationOutcome_shortExplanationBadContent
            }

            RadixWalletException.DappVerificationException.UnknownDefinitionAddress -> {
                R.string.dAppRequest_validationOutcome_devExplanationInvalidDappDefinitionAddress
            }

            RadixWalletException.DappVerificationException.UnknownWebsite -> {
                R.string.dAppRequest_validationOutcome_devExplanationInvalidOrigin
            }

            RadixWalletException.DappVerificationException.WrongAccountType -> {
                R.string.dAppRequest_validationOutcome_devExplanationInvalidDappDefinitionAddress
            }

            RadixWalletException.DappVerificationException.ClaimedEntityAddressNotPresent -> {
                R.string.common_somethingWentWrong
            } // TODO consider different copy
        }
    )
}

fun RadixWalletException.DappRequestException.toUserFriendlyMessage(context: Context): String {
    return when (this) {
        RadixWalletException.DappRequestException.InvalidPersona -> context.getString(R.string.error_dappRequest_invalidPersonaId)
        RadixWalletException.DappRequestException.InvalidRequest -> context.getString(
            R.string.dAppRequest_validationOutcome_invalidRequestMessage
        )

        RadixWalletException.DappRequestException.UnacceptableManifest -> context.getString(
            R.string.transactionReview_unacceptableManifest_rejected
        )

        is RadixWalletException.DappRequestException.InvalidRequestChallenge -> context.getString(
            R.string.dAppRequest_requestMalformedAlert_message
        )

        is RadixWalletException.DappRequestException.WrongNetwork -> {
            context.getString(
                R.string.dAppRequest_requestWrongNetworkAlert_message,
                currentNetworkName,
                requestNetworkName
            )
        }

        is RadixWalletException.DappRequestException.FailedToSignAuthChallenge -> context.getString(
            R.string.common_somethingWentWrong
        ) // TODO consider different copy
        RadixWalletException.DappRequestException.GetEpoch -> context.getString(R.string.error_transactionFailure_epoch)
        RadixWalletException.DappRequestException.RejectedByUser -> context.getString(R.string.error_transactionFailure_rejectedByUser)
        RadixWalletException.DappRequestException.NotPossibleToAuthenticateAutomatically -> context.getString(
            R.string.common_somethingWentWrong
        ) // TODO consider different copy
    }
}

fun RadixWalletException.TransactionSubmitException.toUserFriendlyMessage(context: Context): String {
    return when (this) {
        is RadixWalletException.TransactionSubmitException.FailedToPollTXStatus -> {
            context.getString(R.string.error_transactionFailure_pollStatus)
        }

        is RadixWalletException.TransactionSubmitException.GatewayCommittedException -> {
            context.getString(R.string.error_transactionFailure_commit)
        }

        is RadixWalletException.TransactionSubmitException.GatewayRejected -> {
            context.getString(R.string.error_transactionFailure_rejected)
        }

        is RadixWalletException.TransactionSubmitException.InvalidTXDuplicate -> {
            context.getString(R.string.error_transactionFailure_duplicate)
        }

        is RadixWalletException.TransactionSubmitException.TransactionCommitted.Failure -> {
            context.getString(R.string.transactionStatus_failed_text)
        }

        is RadixWalletException.TransactionSubmitException.TransactionRejected.Permanently -> {
            context.getString(R.string.transactionStatus_rejected_text)
        }

        is RadixWalletException.TransactionSubmitException.TransactionRejected.Temporary -> {
            context.getString(
                R.string.transactionStatus_error_text,
                txProcessingTime
            )
        }
    }
}

fun RadixWalletException.PrepareTransactionException.toUserFriendlyMessage(context: Context): String {
    // Consists of two strings
    if (this is RadixWalletException.PrepareTransactionException.ReceivingAccountDoesNotAllowDeposits) {
        return "${context.getString(R.string.error_transactionFailure_reviewFailure)}\n\n" +
            context.getString(R.string.error_transactionFailure_doesNotAllowThirdPartyDeposits)
    }
    return context.getString(
        when (this) {
            is RadixWalletException.PrepareTransactionException.BuildTransactionHeader -> R.string.error_transactionFailure_header
            RadixWalletException.PrepareTransactionException.ConvertManifest -> R.string.error_transactionFailure_manifest
            is RadixWalletException.PrepareTransactionException.PrepareNotarizedTransaction -> R.string.error_transactionFailure_prepare
            is RadixWalletException.PrepareTransactionException.SubmitNotarizedTransaction -> R.string.error_transactionFailure_submit
            RadixWalletException.PrepareTransactionException.FailedToFindAccountWithEnoughFundsToLockFee ->
                R.string.error_transactionFailure_noFundsToApproveTransaction

            RadixWalletException.PrepareTransactionException.CompileTransactionIntent -> R.string.error_transactionFailure_prepare
            is RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent ->
                if (this.cause is ProfileException.NoMnemonic) {
                    R.string.transactionReview_noMnemonicError_text
                } else {
                    R.string.error_transactionFailure_prepare
                }

            RadixWalletException.PrepareTransactionException.ReceivingAccountDoesNotAllowDeposits ->
                R.string.error_transactionFailure_doesNotAllowThirdPartyDeposits
        }
    )
}

fun RadixWalletException.toUserFriendlyMessage(context: Context): String {
    return when (this) {
        RadixWalletException.FailedToCollectLedgerSignature -> context.getString(
            R.string.common_somethingWentWrong
        ) // TODO consider different copy
        RadixWalletException.FailedToCollectSigners -> context.getString(
            R.string.common_somethingWentWrong
        ) // TODO consider different copy
        RadixWalletException.DappMetadataEmpty -> context.getString(R.string.common_somethingWentWrong)
        RadixWalletException.SignatureCancelled -> context.getString(R.string.common_somethingWentWrong)

        is RadixWalletException.IncomingMessageException -> context.getString(
            R.string.common_somethingWentWrong
        ) // TODO consider different copy
        is RadixWalletException.DappRequestException -> toUserFriendlyMessage(context)
        is RadixWalletException.DappVerificationException -> toUserFriendlyMessage(context)
        is RadixWalletException.LedgerCommunicationException -> toUserFriendlyMessage(context)
        is RadixWalletException.PrepareTransactionException -> toUserFriendlyMessage(context)
        is RadixWalletException.TransactionSubmitException -> toUserFriendlyMessage(context)
    }
}

@Composable
fun RadixWalletException.userFriendlyMessage(): String {
    return toUserFriendlyMessage(LocalContext.current)
}

fun RadixWalletException.getDappMessage(): String? {
    return when (this) {
        is RadixWalletException.TransactionSubmitException -> getDappMessage()
        is RadixWalletException.DappRequestException.WrongNetwork -> {
            "Wallet is using network ID: $currentNetworkId, request sent specified network ID: $requestNetworkId"
        }

        else -> null
    }
}

fun RadixWalletException.toConnectorExtensionError(): ConnectorExtensionError? {
    return (this as? ConnectorExtensionThrowable?)?.ceError
}

fun Throwable.asRadixWalletException(): RadixWalletException? {
    return this as? RadixWalletException
}
