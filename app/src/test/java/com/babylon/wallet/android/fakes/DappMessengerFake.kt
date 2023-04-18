package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.ce.dapp.DappMessenger
import com.babylon.wallet.android.data.ce.dapp.model.WalletErrorType
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import rdx.works.profile.data.model.pernetwork.Network

class DappMessengerFake : DappMessenger {

    override suspend fun sendWalletInteractionUnauthorizedSuccessResponse(
        dappId: String,
        requestId: String,
        onetimeAccounts: List<AccountItemUiModel>,
        onetimeDataFields: List<Network.Persona.Field>
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

    override suspend fun sendWalletInteractionAuthorizedSuccessResponse(
        dappId: String,
        interactionId: String,
        persona: Network.Persona,
        usePersona: Boolean,
        oneTimeAccounts: List<AccountItemUiModel>,
        ongoingAccounts: List<AccountItemUiModel>,
        ongoingDataFields: List<Network.Persona.Field>,
        onetimeDataFields: List<Network.Persona.Field>
    ): Result<Unit> {
        return Result.Success(Unit)
    }

}
