package com.babylon.wallet.android.domain

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import rdx.works.profile.data.model.apppreferences.Radix

sealed class RadixWalletException(msg: String? = null, cause: Throwable? = null) : Throwable(message = msg.orEmpty(), cause = cause) {

    data object DappMetadataEmpty : RadixWalletException()
    data object SignatureCancelledException : RadixWalletException()
    data object FailedToCollectLedgerSignature : RadixWalletException()
    data object FailedToCollectSigners : RadixWalletException("Failed to find signers for the transaction")

    sealed class IncomingMessageException(cause: Throwable? = null) : RadixWalletException(cause = cause) {
        data class MessageParseException(override val cause: Throwable? = null) : IncomingMessageException(cause)
        data class LedgerResponseParseException(override val cause: Throwable? = null) : IncomingMessageException(cause)
        data class UnknownException(override val cause: Throwable? = null) : IncomingMessageException(cause)
    }

    sealed class DappRequestException(cause: Throwable? = null) : RadixWalletException(cause = cause) {

        data object GetEpoch : DappRequestException()
        data object RejectedByUser : DappRequestException()
        data object InvalidRequest : DappRequestException()
        data object UnacceptableManifest : DappRequestException()
        data object InvalidPersona : DappRequestException()
        data object InvalidRequestChallenge : DappRequestException()
        data object NotPossibleToAuthenticateAutomatically : DappRequestException()
        data class FailedToSignAuthChallenge(override val cause: Throwable? = null) : DappRequestException(cause = cause)
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

        fun toWalletErrorType(): WalletErrorType {
            return when (this) {
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
    }

    sealed class PrepareTransactionException(cause: Throwable? = null) : RadixWalletException(cause = cause) {
        data object ConvertManifest : PrepareTransactionException()
        data class BuildTransactionHeader(override val cause: Throwable) : PrepareTransactionException(cause)
        data object FailedToFindAccountWithEnoughFundsToLockFee : PrepareTransactionException()
        data object CompileTransactionIntent : PrepareTransactionException()
        data class SignCompiledTransactionIntent(override val cause: Throwable? = null) : PrepareTransactionException(cause)
        data class PrepareNotarizedTransaction(override val cause: Throwable? = null) : PrepareTransactionException(cause)
        data class SubmitNotarizedTransaction(override val cause: Throwable? = null) : PrepareTransactionException()

        fun toWalletErrorType(): WalletErrorType {
            return when (this) {
                is BuildTransactionHeader -> WalletErrorType.FailedToPrepareTransaction
                ConvertManifest -> WalletErrorType.FailedToPrepareTransaction
                is PrepareNotarizedTransaction -> WalletErrorType.FailedToSignTransaction
                is SubmitNotarizedTransaction -> WalletErrorType.FailedToSubmitTransaction
                FailedToFindAccountWithEnoughFundsToLockFee -> {
                    WalletErrorType.FailedToFindAccountWithEnoughFundsToLockFee
                }

                CompileTransactionIntent -> WalletErrorType.FailedToCompileTransaction
                is SignCompiledTransactionIntent -> WalletErrorType.FailedToSignTransaction
            }
        }
    }

    sealed class TransactionSubmitException : RadixWalletException() {
        data class InvalidTXDuplicate(val txId: String) : TransactionSubmitException()
        data class FailedToPollTXStatus(val txId: String) : TransactionSubmitException()
        data class GatewayRejected(val txId: String) : TransactionSubmitException()
        data class GatewayCommittedException(val txId: String) : TransactionSubmitException()

        fun toWalletErrorType(): WalletErrorType {
            return when (this) {
                is FailedToPollTXStatus -> WalletErrorType.FailedToPollSubmittedTransaction
                is GatewayCommittedException -> {
                    WalletErrorType.SubmittedTransactionHasFailedTransactionStatus
                }

                is GatewayRejected -> {
                    WalletErrorType.SubmittedTransactionHasRejectedTransactionStatus
                }

                is InvalidTXDuplicate -> WalletErrorType.SubmittedTransactionWasDuplicate
            }
        }
    }

    sealed class DappVerificationException : RadixWalletException() {
        data object WrongAccountType : DappVerificationException()
        data object UnknownWebsite : DappVerificationException()
        data object RadixJsonNotFound : DappVerificationException()
        data object UnknownDefinitionAddress : DappVerificationException()
        data object ClaimedEntityAddressNotPresent : DappVerificationException()

        fun toWalletErrorType(): WalletErrorType {
            return when (this) {
                RadixJsonNotFound -> WalletErrorType.RadixJsonNotFound
                UnknownDefinitionAddress -> WalletErrorType.UnknownDefinitionAddress
                UnknownWebsite -> WalletErrorType.UnknownWebsite
                WrongAccountType -> WalletErrorType.WrongAccountType
                ClaimedEntityAddressNotPresent -> WalletErrorType.WrongAccountType
            }
        }
    }

    sealed class LedgerCommunicationFailure : RadixWalletException() {
        data object FailedToGetDeviceId : LedgerCommunicationFailure()
        data object FailedToDerivePublicKeys : LedgerCommunicationFailure()
        data object FailedToDeriveAndDisplayAddress : LedgerCommunicationFailure()
        data class FailedToSignTransaction(val reason: LedgerErrorCode) : LedgerCommunicationFailure()

        fun toWalletErrorType(): WalletErrorType {
            return when (this) {
                is FailedToDerivePublicKeys -> WalletErrorType.InvalidRequest
                FailedToGetDeviceId -> WalletErrorType.InvalidRequest
                is FailedToSignTransaction -> WalletErrorType.InvalidRequest
                is FailedToDeriveAndDisplayAddress -> WalletErrorType.InvalidRequest
            }
        }
    }
}

@Composable
fun RadixWalletException.LedgerCommunicationFailure.toUserFriendlyMessage(): String {
    return stringResource(
        id = when (this) {
            RadixWalletException.LedgerCommunicationFailure.FailedToDerivePublicKeys -> {
                R.string.common_somethingWentWrong
            } // TODO consider different copy
            RadixWalletException.LedgerCommunicationFailure.FailedToGetDeviceId -> {
                R.string.common_somethingWentWrong
            } // TODO consider different copy
            is RadixWalletException.LedgerCommunicationFailure.FailedToSignTransaction -> when (this.reason) {
                LedgerErrorCode.Generic -> R.string.common_somethingWentWrong
                LedgerErrorCode.BlindSigningNotEnabledButRequired -> R.string.error_transactionFailure_blindSigningNotEnabledButRequired
                LedgerErrorCode.UserRejectedSigningOfTransaction -> R.string.error_transactionFailure_rejected
            }

            is RadixWalletException.LedgerCommunicationFailure.FailedToDeriveAndDisplayAddress -> R.string.common_somethingWentWrong
        }
    )
}

@Composable
fun RadixWalletException.DappVerificationException.toUserFriendlyMessage(): String {
    return stringResource(
        id = when (this) {
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

@Composable
fun RadixWalletException.DappRequestException.toUserFriendlyMessage(): String {
    return when (this) {
        RadixWalletException.DappRequestException.InvalidPersona -> stringResource(R.string.error_dappRequest_invalidPersonaId)
        RadixWalletException.DappRequestException.InvalidRequest -> stringResource(
            R.string.dAppRequest_validationOutcome_invalidRequestMessage
        )
        RadixWalletException.DappRequestException.UnacceptableManifest -> stringResource(
            R.string.transactionReview_unacceptableManifest_rejected
        )
        is RadixWalletException.DappRequestException.InvalidRequestChallenge -> stringResource(
            R.string.dAppRequest_requestMalformedAlert_message
        )
        is RadixWalletException.DappRequestException.WrongNetwork -> {
            stringResource(R.string.dAppRequest_requestWrongNetworkAlert_message, currentNetworkName, requestNetworkName)
        }

        is RadixWalletException.DappRequestException.FailedToSignAuthChallenge -> stringResource(
            R.string.common_somethingWentWrong
        ) // TODO consider different copy
        RadixWalletException.DappRequestException.GetEpoch -> stringResource(R.string.error_transactionFailure_epoch)
        RadixWalletException.DappRequestException.RejectedByUser -> stringResource(R.string.error_transactionFailure_rejectedByUser)
        RadixWalletException.DappRequestException.NotPossibleToAuthenticateAutomatically -> stringResource(
            R.string.common_somethingWentWrong
        ) // TODO consider different copy
    }
}

@Composable
fun RadixWalletException.TransactionSubmitException.toUserFriendlyMessage(): String {
    return stringResource(
        id = when (this) {
            is RadixWalletException.TransactionSubmitException.FailedToPollTXStatus -> R.string.error_transactionFailure_pollStatus
            is RadixWalletException.TransactionSubmitException.GatewayCommittedException -> R.string.error_transactionFailure_commit
            is RadixWalletException.TransactionSubmitException.GatewayRejected -> R.string.error_transactionFailure_rejected
            is RadixWalletException.TransactionSubmitException.InvalidTXDuplicate -> R.string.error_transactionFailure_duplicate
        }
    )
}

@Composable
fun RadixWalletException.PrepareTransactionException.toUserFriendlyMessage(): String {
    return stringResource(
        id = when (this) {
            is RadixWalletException.PrepareTransactionException.BuildTransactionHeader -> R.string.error_transactionFailure_header
            RadixWalletException.PrepareTransactionException.ConvertManifest -> R.string.error_transactionFailure_manifest
            is RadixWalletException.PrepareTransactionException.PrepareNotarizedTransaction -> R.string.error_transactionFailure_prepare
            is RadixWalletException.PrepareTransactionException.SubmitNotarizedTransaction -> R.string.error_transactionFailure_submit
            RadixWalletException.PrepareTransactionException.FailedToFindAccountWithEnoughFundsToLockFee ->
                R.string.error_transactionFailure_noFundsToApproveTransaction

            RadixWalletException.PrepareTransactionException.CompileTransactionIntent -> R.string.error_transactionFailure_prepare
            is RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent -> R.string.error_transactionFailure_prepare
        }
    )
}

@Composable
fun RadixWalletException.toUserFriendlyMessage(): String {
    return when (this) {
        RadixWalletException.FailedToCollectLedgerSignature -> stringResource(
            id = R.string.common_somethingWentWrong
        ) // TODO consider different copy
        RadixWalletException.FailedToCollectSigners -> stringResource(
            id = R.string.common_somethingWentWrong
        ) // TODO consider different copy
        RadixWalletException.DappMetadataEmpty -> stringResource(id = R.string.common_somethingWentWrong)
        RadixWalletException.SignatureCancelledException -> stringResource(id = R.string.common_somethingWentWrong)

        is RadixWalletException.IncomingMessageException -> stringResource(
            id = R.string.common_somethingWentWrong
        ) // TODO consider different copy
        is RadixWalletException.DappRequestException -> toUserFriendlyMessage()
        is RadixWalletException.DappVerificationException -> toUserFriendlyMessage()
        is RadixWalletException.LedgerCommunicationFailure -> toUserFriendlyMessage()
        is RadixWalletException.PrepareTransactionException -> toUserFriendlyMessage()
        is RadixWalletException.TransactionSubmitException -> toUserFriendlyMessage()
    }
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

fun RadixWalletException.toWalletErrorType(): WalletErrorType? {
    return when (this) {
        is RadixWalletException.DappRequestException -> toWalletErrorType()
        is RadixWalletException.DappVerificationException -> toWalletErrorType()
        is RadixWalletException.LedgerCommunicationFailure -> toWalletErrorType()
        is RadixWalletException.PrepareTransactionException -> toWalletErrorType()
        is RadixWalletException.TransactionSubmitException -> toWalletErrorType()
        else -> null
    }
}

fun Throwable.asRadixWalletException(): RadixWalletException? {
    return this as? RadixWalletException
}
