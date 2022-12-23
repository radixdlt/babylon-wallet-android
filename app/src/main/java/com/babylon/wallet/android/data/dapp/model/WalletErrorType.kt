package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName

enum class WalletErrorType {
    @SerialName("rejectedByUser")
    RejectedByUser,

    @SerialName("wrongNetwork")
    WrongNetwork,

    @SerialName("failedToPrepareTransaction")
    FailedToPrepareTransaction,

    @SerialName("failedToCompileTransaction")
    FailedToCompileTransaction,

    @SerialName("failedToSignTransaction")
    FailedToSignTransaction,

    @SerialName("failedToSubmitTransaction")
    FailedToSubmitTransaction,

    @SerialName("failedToPollSubmittedTransaction")
    FailedToPollSubmittedTransaction,

    @SerialName("submittedTransactionWasDuplicate")
    SubmittedTransactionWasDuplicate,

    @SerialName("submittedTransactionHasFailedTransactionStatus")
    SubmittedTransactionHasFailedTransactionStatus,

    @SerialName("submittedTransactionHasRejectedTransactionStatus")
    SubmittedTransactionHasRejectedTransactionStatus;
}
