package com.babylon.wallet.android.data.transaction

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
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

    @StringRes
    fun toDescriptionRes(): Int {
        return when (this) {
            BuildTransactionHeader -> R.string.tx_fail_header
            ConvertManifest -> R.string.tx_fail_manifest
            is FailedToPollTXStatus -> R.string.tx_fail_poll_status
            is GatewayCommittedFailure -> R.string.tx_fail_commit
            is GatewayRejected -> R.string.tx_fail_rejected
            GetEpoch -> R.string.tx_fail_epoch
            is InvalidTXDuplicate -> R.string.tx_fail_duplicate
            PrepareNotarizedTransaction -> R.string.tx_fail_prepare
            RejectedByUser -> R.string.tx_fail_rejected_by_user
            SubmitNotarizedTransaction -> R.string.tx_fail_submit
            is WrongNetwork -> R.string.tx_fail_network
        }
    }

    fun getDappMessage(): String? {
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
