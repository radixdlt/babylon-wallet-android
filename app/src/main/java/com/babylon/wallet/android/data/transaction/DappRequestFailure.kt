package com.babylon.wallet.android.data.transaction

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.data.dapp.model.WalletErrorType

@Suppress("CyclomaticComplexMethod")
sealed class DappRequestFailure(msg: String? = null) : Exception(msg.orEmpty()) {

    data object GetEpoch : DappRequestFailure()
    data object RejectedByUser : DappRequestFailure()
    data object InvalidRequest : DappRequestFailure()
    data object UnacceptableManifest : DappRequestFailure()
    data object InvalidPersona : DappRequestFailure()
    data class FailedToSignAuthChallenge(val msg: String = "") : DappRequestFailure(msg)
    data class WrongNetwork(val currentNetworkId: Int, val requestNetworkId: Int) : DappRequestFailure()

    sealed class TransactionApprovalFailure : DappRequestFailure() {
        data object ConvertManifest : TransactionApprovalFailure()
        data object BuildTransactionHeader : TransactionApprovalFailure()
        data object FailedToFindAccountWithEnoughFundsToLockFee : TransactionApprovalFailure()
        data object CompileTransactionIntent : TransactionApprovalFailure()
        data object SignCompiledTransactionIntent : TransactionApprovalFailure()
        data object PrepareNotarizedTransaction : TransactionApprovalFailure()
        data object SubmitNotarizedTransaction : TransactionApprovalFailure()
        data class InvalidTXDuplicate(val txId: String) : TransactionApprovalFailure()
        data class FailedToPollTXStatus(val txId: String) : TransactionApprovalFailure()
        data class GatewayRejected(val txId: String) : TransactionApprovalFailure()
        data class GatewayCommittedFailure(val txId: String) : TransactionApprovalFailure()
    }

    sealed class DappVerificationFailure : DappRequestFailure() {
        data object WrongAccountType : DappVerificationFailure()
        data object UnknownWebsite : DappVerificationFailure()
        data object RadixJsonNotFound : DappVerificationFailure()
        data object UnknownDefinitionAddress : DappVerificationFailure()
        data object ClaimedEntityAddressNotPresent : DappVerificationFailure()
    }

    sealed class LedgerCommunicationFailure : DappRequestFailure() {
        data object FailedToGetDeviceId : LedgerCommunicationFailure()
        data object FailedToDerivePublicKeys : LedgerCommunicationFailure()
        data class FailedToSignTransaction(val reason: LedgerErrorCode) : LedgerCommunicationFailure()
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
            is FailedToSignAuthChallenge -> WalletErrorType.FailedToSignAuthChallenge
            is LedgerCommunicationFailure.FailedToDerivePublicKeys -> WalletErrorType.InvalidRequest
            LedgerCommunicationFailure.FailedToGetDeviceId -> WalletErrorType.InvalidRequest
            is LedgerCommunicationFailure.FailedToSignTransaction -> WalletErrorType.InvalidRequest
            DappVerificationFailure.ClaimedEntityAddressNotPresent -> WalletErrorType.WrongAccountType
            UnacceptableManifest -> WalletErrorType.FailedToPrepareTransaction
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
            TransactionApprovalFailure.FailedToFindAccountWithEnoughFundsToLockFee ->
                R.string.error_transactionFailure_noFundsToApproveTransaction
            DappVerificationFailure.RadixJsonNotFound -> R.string.dAppRequest_validationOutcome_shortExplanationBadContent
            DappVerificationFailure.UnknownDefinitionAddress ->
                R.string.dAppRequest_validationOutcome_devExplanationInvalidDappDefinitionAddress
            DappVerificationFailure.UnknownWebsite -> R.string.dAppRequest_validationOutcome_devExplanationInvalidOrigin
            DappVerificationFailure.WrongAccountType -> R.string.dAppRequest_validationOutcome_devExplanationInvalidDappDefinitionAddress
            InvalidPersona -> R.string.error_dappRequest_invalidPersonaId
            InvalidRequest -> R.string.error_dappRequest_invalidRequest
            TransactionApprovalFailure.CompileTransactionIntent -> R.string.error_transactionFailure_prepare
            TransactionApprovalFailure.SignCompiledTransactionIntent -> R.string.error_transactionFailure_prepare
            LedgerCommunicationFailure.FailedToDerivePublicKeys -> R.string.common_somethingWentWrong // TODO consider different copy
            LedgerCommunicationFailure.FailedToGetDeviceId -> R.string.common_somethingWentWrong // TODO consider different copy
            is LedgerCommunicationFailure.FailedToSignTransaction -> when (this.reason) {
                LedgerErrorCode.Generic -> R.string.common_somethingWentWrong
                LedgerErrorCode.BlindSigningNotEnabledButRequired -> R.string.error_transactionFailure_blindSigningNotEnabledButRequired
                LedgerErrorCode.UserRejectedSigningOfTransaction -> R.string.error_transactionFailure_rejected
            }
            is FailedToSignAuthChallenge -> R.string.common_somethingWentWrong // TODO consider different copy
            DappVerificationFailure.ClaimedEntityAddressNotPresent -> R.string.common_somethingWentWrong // TODO consider different copy
            UnacceptableManifest -> R.string.common_somethingWentWrong // TODO consider different copy
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
