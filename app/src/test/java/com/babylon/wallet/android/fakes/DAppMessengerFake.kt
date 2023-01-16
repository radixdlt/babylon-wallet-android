package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.presentation.dapp.account.AccountItemUiModel

class DAppMessengerFake : DAppMessenger {

    override suspend fun sendAccountsResponse(
        requestId: String,
        accounts: List<AccountItemUiModel>
    ): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun sendTransactionWriteResponseFailure(
        requestId: String,
        error: WalletErrorType,
        message: String?
    ): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun sendTransactionWriteResponseSuccess(
        requestId: String,
        txId: String
    ): Result<Unit> {
        return Result.Success(Unit)
    }
}
