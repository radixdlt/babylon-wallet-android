package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName

enum class WalletErrorType {
    @SerialName("incompatibleVersion")
    IncompatibleVersion,

    @SerialName("rejectedByUser")
    RejectedByUser,

    @SerialName("wrongNetwork")
    WrongNetwork,

    @SerialName("failedToPrepareTransaction")
    FailedToPrepareTransaction,

    @SerialName("failedToCompileTransaction")
    FailedToCompileTransaction,

    @SerialName("failedToFindAccountWithEnoughFundsToLockFee")
    FailedToFindAccountWithEnoughFundsToLockFee,

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
    SubmittedTransactionHasRejectedTransactionStatus,

    @SerialName("submittedTransactionHasPermanentlyRejectedTransactionStatus")
    SubmittedTransactionHasPermanentlyRejectedTransactionStatus,

    @SerialName("submittedTransactionHasTemporarilyRejectedTransactionStatus")
    SubmittedTransactionHasTemporarilyRejectedTransactionStatus,

    @SerialName("unknownWebsite")
    UnknownWebsite,

    @SerialName("radixJsonNotFound")
    RadixJsonNotFound,

    @SerialName("wrongAccountType")
    WrongAccountType,

    @SerialName("unknownDefinitionAddress")
    UnknownDefinitionAddress,

    @SerialName("invalidPersona")
    InvalidPersona,

    @SerialName("invalidRequest")
    InvalidRequest,

    @SerialName("failedToSignAuthChallenge")
    FailedToSignAuthChallenge,
}
