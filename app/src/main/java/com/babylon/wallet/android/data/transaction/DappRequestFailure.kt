package com.babylon.wallet.android.data.transaction

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.dapp.model.WalletErrorType

@Suppress("CyclomaticComplexMethod")
sealed interface DappRequestFailure {
    object GetEpoch : DappRequestFailure
    object RejectedByUser : DappRequestFailure
    data class WrongNetwork(val currentNetworkId: Int, val requestNetworkId: Int) : DappRequestFailure

    object ConvertManifest : DappRequestFailure

    object BuildTransactionHeader : DappRequestFailure
    object FailedToFindAccountWithEnoughFundsToLockFee : DappRequestFailure

    object PrepareNotarizedTransaction : DappRequestFailure

    object SubmitNotarizedTransaction : DappRequestFailure

    data class InvalidTXDuplicate(val txId: String) : DappRequestFailure

    data class FailedToPollTXStatus(val txId: String) : DappRequestFailure

    data class GatewayRejected(val txId: String) : DappRequestFailure

    data class GatewayCommittedFailure(val txId: String) : DappRequestFailure
    object WrongAccountType : DappRequestFailure
    object UnknownWebsite : DappRequestFailure
    object RadixJsonNotFound : DappRequestFailure
    object UnknownDefinitionAddress : DappRequestFailure

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
            FailedToFindAccountWithEnoughFundsToLockFee -> WalletErrorType.FailedToFindAccountWithEnoughFundsToLockFee
            RadixJsonNotFound -> WalletErrorType.RadixJsonNotFound
            UnknownDefinitionAddress -> WalletErrorType.UnknownDefinitionAddress
            UnknownWebsite -> WalletErrorType.UnknownWebsite
            WrongAccountType -> WalletErrorType.WrongAccountType
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
            FailedToFindAccountWithEnoughFundsToLockFee -> R.string.no_funds_to_approve_transaction
            RadixJsonNotFound -> R.string.radix_json_file_is_missing
            UnknownDefinitionAddress -> R.string.definition_address_does_not_match
            UnknownWebsite -> R.string.origin_does_not_match
            WrongAccountType -> R.string.expected_to_find_dapp_account_type
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
