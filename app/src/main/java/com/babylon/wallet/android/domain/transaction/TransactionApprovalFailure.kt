package com.babylon.wallet.android.domain.transaction

import com.babylon.wallet.android.data.dapp.model.WalletErrorType

sealed interface TransactionApprovalFailure {
    object GetEpoch : TransactionApprovalFailure
    object RejectedByUser : TransactionApprovalFailure
    data class WrongNetwork(val currentNetworkId: Int, val requestNetworkId: Int) : TransactionApprovalFailure

    object ConvertManifest : TransactionApprovalFailure

    object BuildTransactionHeader : TransactionApprovalFailure

    object PrepareNotarizedTransaction : TransactionApprovalFailure

    object SubmitNotarizedTransaction : TransactionApprovalFailure

    data class InvalidTXDuplicate(val txId: String) : TransactionApprovalFailure

    data class FailedToPollTXStatus(val txId: String) : TransactionApprovalFailure

    data class GatewayRejected(val txId: String) : TransactionApprovalFailure

    data class GatewayCommittedFailure(val txId: String) : TransactionApprovalFailure

    fun toWalletErrorType(): WalletErrorType {
        return when (this) {
            BuildTransactionHeader -> WalletErrorType.FailedToPrepareTransaction
            ConvertManifest -> WalletErrorType.FailedToPrepareTransaction
            is FailedToPollTXStatus -> WalletErrorType.FailedToPollSubmittedTransaction
            is GatewayCommittedFailure -> WalletErrorType.SubmittedTransactionHasFailedTransactionStatus
            is GatewayRejected -> WalletErrorType.SubmittedTransactionHasRejectedTransactionStatus
            GetEpoch -> WalletErrorType.FailedToPrepareTransaction
            is InvalidTXDuplicate -> WalletErrorType.SubmittedTransactionWasDuplicate
            PrepareNotarizedTransaction -> WalletErrorType.FailedToSignTransaction
            RejectedByUser -> WalletErrorType.RejectedByUser
            SubmitNotarizedTransaction -> WalletErrorType.FailedToSubmitTransaction
            is WrongNetwork -> WalletErrorType.WrongNetwork
        }
    }

    fun getMessage(): String? {
        return when (this) {
            is FailedToPollTXStatus -> "TXID: $txId"
            is GatewayCommittedFailure -> "TXID: $txId"
            is GatewayRejected -> "TXID: $txId"
            is InvalidTXDuplicate -> "TXID: $txId"
            is WrongNetwork -> {
                "Wallet is using network ID: $currentNetworkId, request sent specified network ID: $requestNetworkId"
            }
            else -> null
        }
    }
}
