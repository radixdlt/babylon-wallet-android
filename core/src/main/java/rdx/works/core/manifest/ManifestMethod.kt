package rdx.works.core.manifest

enum class ManifestMethod(val value: String) {
    LockFee("lock_fee"),
    Free("free"),
    TryDepositBatchOrAbort("try_deposit_batch_or_abort"),
    TryDepositOrAbort("try_deposit_or_abort"),
    Withdraw("withdraw"),
    WithdrawNonFungibles("withdraw_non_fungibles"),
}
