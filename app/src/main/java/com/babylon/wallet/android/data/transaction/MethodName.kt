package com.babylon.wallet.android.data.transaction

enum class MethodName(val stringValue: String) {
    LockFee("lock_fee"),
    Free("free"),
    TryDepositOrAbort("try_deposit_or_abort"),
    TryDepositBatchOrAbort("try_deposit_batch_or_abort"),
    LockContingentFee("lock_contingent_fee"),
    Withdraw("withdraw"),
    WithdrawNonFungibles("withdraw_non_fungibles"),
    WithdrawByAmount("withdraw_by_amount"),
    WithdrawByIds("withdraw_by_ids"),
    LockFeeAndWithdraw("lock_fee_and_withdraw"),
    LockFeeAndWithdrawByAmount("lock_fee_and_withdraw_by_amount"),
    LockFeeAndWithdrawByIds("lock_fee_and_withdraw_by_ids"),
    CreateProof("create_proof"),
    CreateProofByAmount("create_proof_by_amount"),
    CreateProofByIds("create_proof_by_ids");

    companion object {
        fun methodsThatRequireAuth(): List<MethodName> {
            return listOf(
                LockFee,
                LockContingentFee,
                Withdraw,
                WithdrawByAmount,
                WithdrawByIds,
                LockFeeAndWithdraw,
                LockFeeAndWithdrawByAmount,
                LockFeeAndWithdrawByIds,
                CreateProof,
                CreateProofByAmount,
                CreateProofByIds
            )
        }
    }
}
