package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.presentation.dapp.account.AccountItemUiModel
import rdx.works.profile.data.model.pernetwork.Network

class DappMessengerFake : DappMessenger {

    override suspend fun sendAccountsResponse(
        dappId: String,
        requestId: String,
        accounts: List<AccountItemUiModel>
    ): Result<Unit> {
        return Result.Success(Unit)
    }

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

    override suspend fun sendWalletInteractionSuccessResponse(
        dappId: String,
        interactionId: String,
        persona: Network.Persona,
        usePersona: Boolean,
        oneTimeAccounts: List<AccountItemUiModel>,
        ongoingAccounts: List<AccountItemUiModel>
    ): Result<Unit> {
        return Result.Success(Unit)
    }
}
