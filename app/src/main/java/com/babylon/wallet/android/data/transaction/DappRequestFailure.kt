package com.babylon.wallet.android.data.transaction

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.dapp.model.WalletErrorType

@Suppress("CyclomaticComplexMethod")
sealed class DappRequestFailure(msg: String? = null) : Exception(msg.orEmpty()) {

    object GetEpoch : DappRequestFailure()
    object RejectedByUser : DappRequestFailure()
    object InvalidRequest : DappRequestFailure()
    object InvalidPersona : DappRequestFailure()
    data class FailedToSignAuthChallenge(val msg: String = "") : DappRequestFailure(msg)
    data class WrongNetwork(val currentNetworkId: Int, val requestNetworkId: Int) : DappRequestFailure()

    sealed class TransactionApprovalFailure : DappRequestFailure() {
        object ConvertManifest : TransactionApprovalFailure()
        object BuildTransactionHeader : TransactionApprovalFailure()
        object FailedToFindAccountWithEnoughFundsToLockFee : TransactionApprovalFailure()
        object CompileTransactionIntent : TransactionApprovalFailure()
        object SignCompiledTransactionIntent : TransactionApprovalFailure()
        object PrepareNotarizedTransaction : TransactionApprovalFailure()
        object SubmitNotarizedTransaction : TransactionApprovalFailure()
        data class InvalidTXDuplicate(val txId: String) : TransactionApprovalFailure()
        data class FailedToPollTXStatus(val txId: String) : TransactionApprovalFailure()
        data class GatewayRejected(val txId: String) : TransactionApprovalFailure()
        data class GatewayCommittedFailure(val txId: String) : TransactionApprovalFailure()
    }

    sealed class DappVerificationFailure : DappRequestFailure() {
        object WrongAccountType : DappVerificationFailure()
        object UnknownWebsite : DappVerificationFailure()
        object RadixJsonNotFound : DappVerificationFailure()
        object UnknownDefinitionAddress : DappVerificationFailure()
    }

    sealed class LedgerCommunicationFailure : DappRequestFailure() {
        object FailedToGetDeviceId : LedgerCommunicationFailure()
        object FailedToDerivePublicKeys : LedgerCommunicationFailure()
        object FailedToSignTransaction : LedgerCommunicationFailure()
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
            LedgerCommunicationFailure.FailedToDerivePublicKeys -> WalletErrorType.InvalidRequest
            LedgerCommunicationFailure.FailedToGetDeviceId -> WalletErrorType.InvalidRequest
            LedgerCommunicationFailure.FailedToSignTransaction -> WalletErrorType.InvalidRequest
        }
    }

    @StringRes
    fun toDescriptionRes(): Int {
        return when (this) {
            TransactionApprovalFailure.BuildTransactionHeader -> R.string.tx_fail_header
            TransactionApprovalFailure.ConvertManifest -> R.string.tx_fail_manifest
            is TransactionApprovalFailure.FailedToPollTXStatus -> R.string.tx_fail_poll_status
            is TransactionApprovalFailure.GatewayCommittedFailure -> R.string.tx_fail_commit
            is TransactionApprovalFailure.GatewayRejected -> R.string.tx_fail_rejected
            GetEpoch -> R.string.tx_fail_epoch
            is TransactionApprovalFailure.InvalidTXDuplicate -> R.string.tx_fail_duplicate
            TransactionApprovalFailure.PrepareNotarizedTransaction -> R.string.tx_fail_prepare
            RejectedByUser -> R.string.tx_fail_rejected_by_user
            TransactionApprovalFailure.SubmitNotarizedTransaction -> R.string.tx_fail_submit
            is WrongNetwork -> R.string.tx_fail_network
            TransactionApprovalFailure.FailedToFindAccountWithEnoughFundsToLockFee -> R.string.no_funds_to_approve_transaction
            DappVerificationFailure.RadixJsonNotFound -> R.string.radix_json_file_is_missing
            DappVerificationFailure.UnknownDefinitionAddress -> R.string.definition_address_does_not_match
            DappVerificationFailure.UnknownWebsite -> R.string.origin_does_not_match
            DappVerificationFailure.WrongAccountType -> R.string.expected_to_find_dapp_account_type
            InvalidPersona -> R.string.invalid_persona_id_specified_by_dapp
            InvalidRequest -> R.string.invalid_request
            TransactionApprovalFailure.CompileTransactionIntent -> R.string.tx_fail_prepare
            TransactionApprovalFailure.SignCompiledTransactionIntent -> R.string.tx_fail_sign
            LedgerCommunicationFailure.FailedToDerivePublicKeys -> R.string.ledger_failure
            LedgerCommunicationFailure.FailedToGetDeviceId -> R.string.ledger_failure
            LedgerCommunicationFailure.FailedToSignTransaction -> R.string.ledger_failure
            is FailedToSignAuthChallenge -> R.string.failed_to_sign_auth_challenge
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
