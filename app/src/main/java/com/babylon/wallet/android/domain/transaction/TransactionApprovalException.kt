package com.babylon.wallet.android.domain.transaction

class TransactionApprovalException(
    failureCause: TransactionApprovalFailureCause,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)

enum class TransactionApprovalFailureCause {
    CompileTxIntent,
    GenerateTXId,
    GetEpoch,
    CompileSignedTXIntent,
    SigningIntentWithAccountSigners,
    SignSignedCompiledIntentWithNotarySigner,
    ConvertAccountSignature,
    ConvertNotarySignature,
    CompileNotarizedTXIntent,
    SubmitNotarizedTransaction,
    InvalidTXDuplicate,
    FailedToPollTXStatus
}