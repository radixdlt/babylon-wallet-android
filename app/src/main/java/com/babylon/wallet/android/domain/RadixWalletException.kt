@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.domain

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.utils.replaceDoublePercent
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.extensions.string
import rdx.works.profile.cloudbackup.model.BackupServiceException
import rdx.works.profile.domain.ProfileException

sealed class RadixWalletException(cause: Throwable? = null) : Throwable(cause = cause) {

    sealed class GatewayException(cause: Throwable? = null) : RadixWalletException(cause) {

        data class ClientError(override val cause: Throwable? = null) : GatewayException(cause)
        data class HttpError(
            val code: Int?,
            override val message: String
        ) : GatewayException(null) {

            companion object {
                const val RATE_LIMIT_REACHED = 429
            }
        }

        data class NetworkError(override val cause: Throwable? = null) : GatewayException(cause)
    }

    sealed class DappRequestException(cause: Throwable? = null) :
        RadixWalletException(cause = cause),
        DappWalletInteractionThrowable {

        data object GetEpoch : DappRequestException()
        data object RejectedByUser : DappRequestException()
        data object InvalidRequest : DappRequestException()
        data object UnacceptableManifest : DappRequestException()
        data object InvalidPersona : DappRequestException()
        data object InvalidRequestChallenge : DappRequestException()
        data object NotPossibleToAuthenticateAutomatically : DappRequestException()
        data class FailedToSignAuthChallenge(override val cause: Throwable? = null) :
            DappRequestException(cause = cause)

        data class PreviewError(override val cause: Throwable?) : DappRequestException()
        data object InvalidPreAuthorizationExpirationTooClose : DappRequestException()
        data object InvalidPreAuthorizationExpired : DappRequestException()

        data class WrongNetwork(
            val currentNetworkId: NetworkId,
            val requestNetworkId: NetworkId
        ) : DappRequestException() {
            val currentNetworkName: String
                get() = currentNetworkId.string.replaceFirstChar(Char::titlecase)
            val requestNetworkName: String
                get() = requestNetworkId.string.replaceFirstChar(Char::titlecase)
        }

        override val dappWalletInteractionErrorType: DappWalletInteractionErrorType
            get() = when (this) {
                is FailedToSignAuthChallenge -> DappWalletInteractionErrorType.FAILED_TO_SIGN_AUTH_CHALLENGE
                GetEpoch -> DappWalletInteractionErrorType.INVALID_REQUEST
                InvalidPersona -> DappWalletInteractionErrorType.INVALID_PERSONA
                InvalidRequest -> DappWalletInteractionErrorType.INVALID_REQUEST
                InvalidRequestChallenge -> DappWalletInteractionErrorType.FAILED_TO_SIGN_AUTH_CHALLENGE
                NotPossibleToAuthenticateAutomatically -> DappWalletInteractionErrorType.INVALID_REQUEST
                RejectedByUser -> DappWalletInteractionErrorType.REJECTED_BY_USER
                UnacceptableManifest -> DappWalletInteractionErrorType.INVALID_REQUEST
                is WrongNetwork -> DappWalletInteractionErrorType.WRONG_NETWORK
                is PreviewError -> DappWalletInteractionErrorType.FAILED_TO_PREPARE_TRANSACTION
                is InvalidPreAuthorizationExpirationTooClose -> DappWalletInteractionErrorType.SUBINTENT_EXPIRATION_TOO_CLOSE
                is InvalidPreAuthorizationExpired -> DappWalletInteractionErrorType.EXPIRED_SUBINTENT
            }
    }

    sealed class PrepareTransactionException(cause: Throwable? = null) :
        RadixWalletException(
            cause = cause
        ),
        DappWalletInteractionThrowable {
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

        override val dappWalletInteractionErrorType: DappWalletInteractionErrorType
            get() = when (this) {
                is BuildTransactionHeader -> DappWalletInteractionErrorType.FAILED_TO_PREPARE_TRANSACTION
                is ConvertManifest -> DappWalletInteractionErrorType.FAILED_TO_PREPARE_TRANSACTION
                is PrepareNotarizedTransaction -> DappWalletInteractionErrorType.FAILED_TO_SIGN_TRANSACTION
                is SubmitNotarizedTransaction -> DappWalletInteractionErrorType.FAILED_TO_SUBMIT_TRANSACTION
                is FailedToFindAccountWithEnoughFundsToLockFee -> {
                    DappWalletInteractionErrorType.FAILED_TO_FIND_ACCOUNT_WITH_ENOUGH_FUNDS_TO_LOCK_FEE
                }

                is CompileTransactionIntent -> DappWalletInteractionErrorType.FAILED_TO_COMPILE_TRANSACTION
                is SignCompiledTransactionIntent -> DappWalletInteractionErrorType.FAILED_TO_SIGN_TRANSACTION
                is ReceivingAccountDoesNotAllowDeposits -> DappWalletInteractionErrorType.FAILED_TO_PREPARE_TRANSACTION
            }
    }

    sealed class TransactionSubmitException : RadixWalletException(), DappWalletInteractionThrowable {
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
            data class AssertionFailed(val txId: String) : TransactionCommitted()
        }

        fun getDappMessage(): String? {
            return when (this) {
                is FailedToPollTXStatus -> "TXID: $txId"
                is InvalidTXDuplicate -> "TXID: $txId"
                is TransactionCommitted.Failure -> "TXID: $txId"
                else -> null
            }
        }

        override val dappWalletInteractionErrorType: DappWalletInteractionErrorType
            get() = when (this) {
                is FailedToPollTXStatus -> DappWalletInteractionErrorType.FAILED_TO_POLL_SUBMITTED_TRANSACTION
                is GatewayCommittedException -> {
                    DappWalletInteractionErrorType.SUBMITTED_TRANSACTION_HAS_FAILED_TRANSACTION_STATUS
                }

                is GatewayRejected -> {
                    DappWalletInteractionErrorType.SUBMITTED_TRANSACTION_HAS_REJECTED_TRANSACTION_STATUS
                }

                is InvalidTXDuplicate -> DappWalletInteractionErrorType.SUBMITTED_TRANSACTION_WAS_DUPLICATE
                is TransactionCommitted.Failure -> DappWalletInteractionErrorType.SUBMITTED_TRANSACTION_HAS_FAILED_TRANSACTION_STATUS
                is TransactionRejected.Permanently -> DappWalletInteractionErrorType.SUBMITTED_TRANSACTION_HAS_REJECTED_TRANSACTION_STATUS
                is TransactionRejected.Temporary -> DappWalletInteractionErrorType.SUBMITTED_TRANSACTION_HAS_REJECTED_TRANSACTION_STATUS
                is TransactionCommitted.AssertionFailed -> {
                    DappWalletInteractionErrorType.SUBMITTED_TRANSACTION_HAS_FAILED_TRANSACTION_STATUS
                }
            }
    }

    sealed class DappVerificationException : RadixWalletException(), DappWalletInteractionThrowable {
        data object WrongAccountType : DappVerificationException()
        data object UnknownWebsite : DappVerificationException()
        data object RadixJsonNotFound : DappVerificationException()
        data object UnknownDefinitionAddress : DappVerificationException()

        override val dappWalletInteractionErrorType: DappWalletInteractionErrorType
            get() = when (this) {
                RadixJsonNotFound -> DappWalletInteractionErrorType.RADIX_JSON_NOT_FOUND
                UnknownDefinitionAddress -> DappWalletInteractionErrorType.UNKNOWN_DAPP_DEFINITION_ADDRESS
                UnknownWebsite -> DappWalletInteractionErrorType.UNKNOWN_WEBSITE
                WrongAccountType -> DappWalletInteractionErrorType.WRONG_ACCOUNT_TYPE
            }
    }

    sealed class LedgerCommunicationException : RadixWalletException(), DappWalletInteractionThrowable {
        data object FailedToConnect : LedgerCommunicationException()
        data object FailedToGetDeviceId : LedgerCommunicationException()
        data object FailedToDerivePublicKeys : LedgerCommunicationException()
        data object FailedToDeriveAndDisplayAddress : LedgerCommunicationException()
        data object FailedToSignAuthChallenge : LedgerCommunicationException()
        data class FailedToSignTransaction(val reason: LedgerErrorCode) : LedgerCommunicationException()

        override val dappWalletInteractionErrorType: DappWalletInteractionErrorType
            get() = when (this) {
                is FailedToDerivePublicKeys -> DappWalletInteractionErrorType.INVALID_REQUEST
                FailedToGetDeviceId -> DappWalletInteractionErrorType.INVALID_REQUEST
                is FailedToSignTransaction -> DappWalletInteractionErrorType.INVALID_REQUEST
                is FailedToDeriveAndDisplayAddress -> DappWalletInteractionErrorType.INVALID_REQUEST
                FailedToSignAuthChallenge -> DappWalletInteractionErrorType.INVALID_REQUEST
                FailedToConnect -> DappWalletInteractionErrorType.INVALID_REQUEST
            }
    }

    sealed class LinkConnectionException : RadixWalletException() {

        data object OldQRVersion : LinkConnectionException()

        data object InvalidQR : LinkConnectionException()

        data object InvalidSignature : LinkConnectionException()

        data object UnknownPurpose : LinkConnectionException()

        data object PurposeChangeNotSupported : LinkConnectionException()
    }

    data class CloudBackupException(
        val error: BackupServiceException
    ) : RadixWalletException()

    data class ResourceCouldNotBeResolvedInTransaction(
        val address: ResourceOrNonFungible
    ) : RadixWalletException() {

        constructor(resourceAddress: ResourceAddress) : this(ResourceOrNonFungible.Resource(value = resourceAddress))

        constructor(resourceAddress: ResourceAddress, localId: NonFungibleLocalId) : this(
            ResourceOrNonFungible.NonFungible(
                NonFungibleGlobalId(
                    resourceAddress = resourceAddress,
                    nonFungibleLocalId = localId
                )
            )
        )
    }
}

interface DappWalletInteractionThrowable {
    val dappWalletInteractionErrorType: DappWalletInteractionErrorType
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
                LedgerErrorCode.BlindSigningNotEnabledButRequired -> R.string.ledgerHardwareDevices_couldNotSign_message
                LedgerErrorCode.UserRejectedSigningOfTransaction -> R.string.error_transactionFailure_rejected
            }

            is RadixWalletException.LedgerCommunicationException.FailedToDeriveAndDisplayAddress -> R.string.common_somethingWentWrong
            RadixWalletException.LedgerCommunicationException.FailedToSignAuthChallenge -> {
                R.string.ledgerHardwareDevices_verification_requestFailed
            }

            RadixWalletException.LedgerCommunicationException.FailedToConnect -> R.string.common_somethingWentWrong
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
                requestNetworkName,
                currentNetworkName
            )
        }

        is RadixWalletException.DappRequestException.FailedToSignAuthChallenge -> context.getString(
            R.string.common_somethingWentWrong
        ) // TODO consider different copy
        RadixWalletException.DappRequestException.GetEpoch -> context.getString(R.string.error_transactionFailure_epoch)
        RadixWalletException.DappRequestException.RejectedByUser -> context.getString(R.string.error_transactionFailure_rejectedByUser)
        RadixWalletException.DappRequestException.NotPossibleToAuthenticateAutomatically -> context.getString(
            R.string.common_somethingWentWrong
        )
        is RadixWalletException.DappRequestException.PreviewError -> context.getString(R.string.error_transactionFailure_reviewFailure)
        is RadixWalletException.DappRequestException.InvalidPreAuthorizationExpirationTooClose -> context.getString(
            R.string.dAppRequest_validationOutcome_preAuthorizationExpirationTooClose
        )
        is RadixWalletException.DappRequestException.InvalidPreAuthorizationExpired -> context.getString(
            R.string.dAppRequest_validationOutcome_preAuthorizationExpired
        )
    }
}

fun RadixWalletException.GatewayException.toUserFriendlyMessage(context: Context): String = when (this) {
    is RadixWalletException.GatewayException.ClientError -> cause?.message.orEmpty()
    is RadixWalletException.GatewayException.NetworkError -> "The Internet connection appears to be offline."
    is RadixWalletException.GatewayException.HttpError -> when (code) {
        RadixWalletException.GatewayException.HttpError.RATE_LIMIT_REACHED -> context.getString(R.string.common_rateLimitReached)
        else -> message
    }
}

fun RadixWalletException.LinkConnectionException.toUserFriendlyMessage(context: Context): String = when (this) {
    RadixWalletException.LinkConnectionException.OldQRVersion -> {
        context.getString(R.string.linkedConnectors_oldQRErrorMessage)
    }

    RadixWalletException.LinkConnectionException.InvalidQR,
    RadixWalletException.LinkConnectionException.InvalidSignature -> {
        context.getString(R.string.linkedConnectors_incorrectQrMessage)
    }

    RadixWalletException.LinkConnectionException.PurposeChangeNotSupported -> {
        context.getString(R.string.linkedConnectors_changingPurposeNotSupportedErrorMessage)
    }

    RadixWalletException.LinkConnectionException.UnknownPurpose -> {
        context.getString(R.string.linkedConnectors_unknownPurposeErrorMessage)
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

        is RadixWalletException.TransactionSubmitException.TransactionCommitted.AssertionFailed -> {
            context.getString(R.string.transactionStatus_assertionFailure_text).replaceDoublePercent()
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
            is RadixWalletException.PrepareTransactionException.ConvertManifest -> R.string.error_transactionFailure_manifest
            is RadixWalletException.PrepareTransactionException.PrepareNotarizedTransaction -> R.string.error_transactionFailure_prepare
            is RadixWalletException.PrepareTransactionException.SubmitNotarizedTransaction -> R.string.error_transactionFailure_submit
            is RadixWalletException.PrepareTransactionException.FailedToFindAccountWithEnoughFundsToLockFee ->
                R.string.error_transactionFailure_noFundsToApproveTransaction

            is RadixWalletException.PrepareTransactionException.CompileTransactionIntent -> R.string.error_transactionFailure_prepare
            is RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent ->
                when (val cause = this.cause) {
                    is ProfileException.NoMnemonic -> {
                        R.string.common_noMnemonicAlert_text
                    }

                    is RadixWalletException.LedgerCommunicationException -> {
                        return cause.toUserFriendlyMessage(context)
                    }

                    else -> {
                        R.string.error_transactionFailure_prepare
                    }
                }

            RadixWalletException.PrepareTransactionException.ReceivingAccountDoesNotAllowDeposits ->
                R.string.error_transactionFailure_doesNotAllowThirdPartyDeposits
        }
    )
}

fun RadixWalletException.CloudBackupException.toUserFriendlyMessage(): String = when (error) {
    is BackupServiceException.ClaimedByAnotherDevice -> "Profile claimed by another device"
    is BackupServiceException.ServiceException -> "${error.statusCode} - ${error.message}"
    is BackupServiceException.UnauthorizedException -> "Access to Google Drive denied."
    is BackupServiceException.RecoverableUnauthorizedException -> "Access to Google Drive denied."
    is BackupServiceException.Unknown -> "Unknown error occurred cause: ${cause?.message}"
}

fun RadixWalletException.toUserFriendlyMessage(context: Context): String {
    return when (this) {
        is RadixWalletException.ResourceCouldNotBeResolvedInTransaction -> context.getString(
            R.string.common_somethingWentWrong
        )

        is RadixWalletException.DappRequestException -> toUserFriendlyMessage(context)
        is RadixWalletException.DappVerificationException -> toUserFriendlyMessage(context)
        is RadixWalletException.LedgerCommunicationException -> toUserFriendlyMessage(context)
        is RadixWalletException.PrepareTransactionException -> toUserFriendlyMessage(context)
        is RadixWalletException.TransactionSubmitException -> toUserFriendlyMessage(context)
        is RadixWalletException.GatewayException -> toUserFriendlyMessage(context)
        is RadixWalletException.LinkConnectionException -> toUserFriendlyMessage(context)
        is RadixWalletException.CloudBackupException -> toUserFriendlyMessage()
    }
}

@Composable
fun RadixWalletException.userFriendlyMessage(): String {
    return toUserFriendlyMessage(LocalContext.current)
}

fun Throwable.getDappMessage(): String? {
    return when (this) {
        is RadixWalletException.TransactionSubmitException -> getDappMessage()
        is RadixWalletException.DappRequestException.WrongNetwork -> {
            "Wallet is using network ID: $currentNetworkId, request sent specified network ID: $requestNetworkId"
        }

        else -> message
    }
}

fun Throwable.toDappWalletInteractionErrorType(): DappWalletInteractionErrorType? {
    return (this as? DappWalletInteractionThrowable?)?.dappWalletInteractionErrorType
}

fun Throwable.asRadixWalletException(): RadixWalletException? {
    return this as? RadixWalletException
}
