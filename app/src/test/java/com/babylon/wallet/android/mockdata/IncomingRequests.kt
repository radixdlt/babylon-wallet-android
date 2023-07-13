package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import rdx.works.core.UUIDGenerator

val accountsRequestExact = MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
    dappId = "dappId",
    requestId = UUIDGenerator.uuid().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(1, "", "", false),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        numberOfAccounts = 1,
        numberOfValues = MessageFromDataChannel.IncomingRequest.NumberOfValues.Exactly,
        challenge = null
    )
)
val accountsTwoRequestExact = MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
    dappId = "dappId",
    requestId = UUIDGenerator.uuid().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(1, "", "", false),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        numberOfAccounts = 2,
        numberOfValues = MessageFromDataChannel.IncomingRequest.NumberOfValues.Exactly,
        challenge = null
    )
)

val accountsRequestAtLeast = MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
    dappId = "dappId",
    requestId = UUIDGenerator.uuid().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(1, "", "", false),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        numberOfAccounts = 2,
        numberOfValues = MessageFromDataChannel.IncomingRequest.NumberOfValues.AtLeast,
        challenge = null
    )
)
