package com.babylon.wallet.android.data.transaction

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.dapp.model.WalletErrorType

@Suppress("CyclomaticComplexMethod")
sealed interface DappRequestFailure {

    object GetEpoch : DappRequestFailure
    object RejectedByUser : DappRequestFailure
    object InvalidRequest : DappRequestFailure
    object InvalidPersona : DappRequestFailure
    data class WrongNetwork(val currentNetworkId: Int, val requestNetworkId: Int) : DappRequestFailure

    sealed interface TransactionApprovalFailure : DappRequestFailure {
        object ConvertManifest : TransactionApprovalFailure
        object BuildTransactionHeader : TransactionApprovalFailure
        object FailedToFindAccountWithEnoughFundsToLockFee : TransactionApprovalFailure
        object CompileTransactionIntent : TransactionApprovalFailure
        object SignCompiledTransactionIntent : TransactionApprovalFailure
        object PrepareNotarizedTransaction : TransactionApprovalFailure
        object SubmitNotarizedTransaction : TransactionApprovalFailure
        data class InvalidTXDuplicate(val txId: String) : TransactionApprovalFailure
        data class FailedToPollTXStatus(val txId: String) : TransactionApprovalFailure
        data class GatewayRejected(val txId: String) : TransactionApprovalFailure
        data class GatewayCommittedFailure(val txId: String) : TransactionApprovalFailure
    }

    sealed interface DappVerificationFailure : DappRequestFailure {
        object WrongAccountType : DappRequestFailure
        object UnknownWebsite : DappRequestFailure
        object RadixJsonNotFound : DappRequestFailure
        object UnknownDefinitionAddress : DappRequestFailure
    }

    sealed interface LedgerCommunicationFailure : DappRequestFailure {
        object FailedToGetDeviceId : LedgerCommunicationFailure
        object FailedToDerivePublicKeys : LedgerCommunicationFailure
        object FailedToSignTransaction : LedgerCommunicationFailure
    }

    fun toWalletErrorType(): WalletErrorType {
        return when (this) {
            TransactionApprovalFailure.BuildTransactionHeader -> WalletErrorType.FailedToPrepareTransaction
            TransactionApprovalFailure.ConvertManifest -> WalletErrorType.FailedToPrepareTransaction
            is TransactionApprovalFailure.FailedToPollTXStatus -> WalletErrorType.FailedToPollSubmittedTransaction
            is TransactionApprovalFailure.GatewayCommittedFailure -> {
                WalletErrorType.SubmittedTransactionHasFailedTransactionStatus
            }
            is TransactionApprovalFailure.GatewayRejected -> {
                WalletErrorType.SubmittedTransactionHasRejectedTransactionStatus
            }
            GetEpoch -> WalletErrorType.FailedToPrepareTransaction
            is TransactionApprovalFailure.InvalidTXDuplicate -> WalletErrorType.SubmittedTransactionWasDuplicate
            TransactionApprovalFailure.PrepareNotarizedTransaction -> WalletErrorType.FailedToSignTransaction
            RejectedByUser -> WalletErrorType.RejectedByUser
            TransactionApprovalFailure.SubmitNotarizedTransaction -> WalletErrorType.FailedToSubmitTransaction
            is WrongNetwork -> WalletErrorType.WrongNetwork
            TransactionApprovalFailure.FailedToFindAccountWithEnoughFundsToLockFee -> {
                WalletErrorType.FailedToFindAccountWithEnoughFundsToLockFee
            }
            DappVerificationFailure.RadixJsonNotFound -> WalletErrorType.RadixJsonNotFound
            DappVerificationFailure.UnknownDefinitionAddress -> WalletErrorType.UnknownDefinitionAddress
            DappVerificationFailure.UnknownWebsite -> WalletErrorType.UnknownWebsite
            DappVerificationFailure.WrongAccountType -> WalletErrorType.WrongAccountType
            InvalidPersona -> WalletErrorType.InvalidPersona
            InvalidRequest -> WalletErrorType.InvalidRequest
            TransactionApprovalFailure.CompileTransactionIntent -> WalletErrorType.FailedToCompileTransaction
            TransactionApprovalFailure.SignCompiledTransactionIntent -> WalletErrorType.FailedToSignTransaction
            LedgerCommunicationFailure.FailedToDerivePublicKeys -> WalletErrorType.InvalidRequest
            LedgerCommunicationFailure.FailedToGetDeviceId -> WalletErrorType.InvalidRequest
            LedgerCommunicationFailure.FailedToSignTransaction -> WalletErrorType.InvalidRequest
        }
    }

    @StringRes
    fun toDescriptionRes(): Int {
        return when (this) {
            TransactionApprovalFailure.BuildTransactionHeader -> R.string.error_transactionFailure_header
            TransactionApprovalFailure.ConvertManifest -> R.string.error_transactionFailure_manifest
            is TransactionApprovalFailure.FailedToPollTXStatus -> R.string.error_transactionFailure_pollStatus
            is TransactionApprovalFailure.GatewayCommittedFailure -> R.string.error_transactionFailure_commit
            is TransactionApprovalFailure.GatewayRejected -> R.string.error_transactionFailure_rejected
            GetEpoch -> R.string.error_transactionFailure_epoch
            is TransactionApprovalFailure.InvalidTXDuplicate -> R.string.error_transactionFailure_duplicate
            TransactionApprovalFailure.PrepareNotarizedTransaction -> R.string.error_transactionFailure_prepare
            RejectedByUser -> R.string.error_transactionFailure_rejectedByUser
            TransactionApprovalFailure.SubmitNotarizedTransaction -> R.string.error_transactionFailure_submit
            is WrongNetwork -> R.string.error_transactionFailure_network
            TransactionApprovalFailure.FailedToFindAccountWithEnoughFundsToLockFee -> R.string.error_transactionFailure_noFundsToApproveTransaction
            DappVerificationFailure.RadixJsonNotFound -> R.string.dAppRequest_validationOutcome_shortExplanationBadContent
            DappVerificationFailure.UnknownDefinitionAddress -> R.string.dAppRequest_validationOutcome_devExplanationInvalidDappDefinitionAddress
            DappVerificationFailure.UnknownWebsite -> R.string.dAppRequest_validationOutcome_devExplanationInvalidOrigin
            DappVerificationFailure.WrongAccountType -> R.string.dAppRequest_validationOutcome_devExplanationInvalidDappDefinitionAddress
            InvalidPersona -> R.string.error_dappRequest_invalidPersonaId
            InvalidRequest -> R.string.error_dappRequest_invalidRequest
            TransactionApprovalFailure.CompileTransactionIntent -> R.string.error_transactionFailure_prepare
            TransactionApprovalFailure.SignCompiledTransactionIntent -> R.string.error_transactionFailure_prepare
            LedgerCommunicationFailure.FailedToDerivePublicKeys -> R.string.common_somethingWentWrong // TODO consider different copy
            LedgerCommunicationFailure.FailedToGetDeviceId -> R.string.common_somethingWentWrong // TODO consider different copy
            LedgerCommunicationFailure.FailedToSignTransaction -> R.string.common_somethingWentWrong // TODO consider different copy
        }
    }

    fun getDappMessage(): String? {
        return when (this) {
            is TransactionApprovalFailure.FailedToPollTXStatus -> "TXID: $txId"
            is TransactionApprovalFailure.GatewayCommittedFailure -> "TXID: $txId"
            is TransactionApprovalFailure.GatewayRejected -> "TXID: $txId"
            is TransactionApprovalFailure.InvalidTXDuplicate -> "TXID: $txId"
            is WrongNetwork -> {
                "Wallet is using network ID: $currentNetworkId, request sent specified network ID: $requestNetworkId"
            }
            else -> null
        }
    }
}
