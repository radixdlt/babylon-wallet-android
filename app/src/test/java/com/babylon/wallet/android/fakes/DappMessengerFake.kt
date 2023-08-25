package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.domain.common.Result

class DappMessengerFake : DappMessenger {

    override suspend fun sendWalletInteractionResponseFailure(
        remoteConnectorId: String,
        requestId: String,
        error: WalletErrorType,
        message: String?
    ): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun sendTransactionWriteResponseSuccess(
        remoteConnectorId: String,
        requestId: String,
        txId: String
    ): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun sendWalletInteractionSuccessResponse(
        remoteConnectorId: String,
        response: WalletInteractionResponse
    ): Result<Unit> {
        return Result.Success(Unit)
    }

}
