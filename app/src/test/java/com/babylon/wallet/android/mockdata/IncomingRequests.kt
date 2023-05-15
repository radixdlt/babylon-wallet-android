package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.data.dapp.model.WalletInteraction
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import rdx.works.core.UUIDGenerator

val accountsRequestExact = MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
    dappId = "dappId",
    requestId = UUIDGenerator.uuid().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(WalletInteraction.Metadata.VERSION, 1, "", ""),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        requiresProofOfOwnership = false,
        numberOfAccounts = 1,
        quantifier = MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.Exactly
    )
)
val accountsTwoRequestExact = MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
    dappId = "dappId",
    requestId = UUIDGenerator.uuid().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(WalletInteraction.Metadata.VERSION, 1, "", ""),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        requiresProofOfOwnership = false,
        numberOfAccounts = 2,
        quantifier = MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.Exactly
    )
)

val accountsRequestAtLeast = MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
    dappId = "dappId",
    requestId = UUIDGenerator.uuid().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(WalletInteraction.Metadata.VERSION, 1, "", ""),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        requiresProofOfOwnership = false,
        numberOfAccounts = 2,
        quantifier = MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast
    )
)
