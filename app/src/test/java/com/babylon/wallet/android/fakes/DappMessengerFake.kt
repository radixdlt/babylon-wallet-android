package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.domain.common.Result

class DappMessengerFake : DappMessenger {

    override suspend fun sendWalletInteractionResponseFailure(
        dappId: String,
        requestId: String,
        error: WalletErrorType,
        message: String?
    ): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun sendTransactionWriteResponseSuccess(
        dappId: String,
        requestId: String,
        txId: String
    ): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun sendWalletInteractionSuccessResponse(dappId: String, response: WalletInteractionResponse): Result<Unit> {
        return Result.Success(Unit)
    }

}
