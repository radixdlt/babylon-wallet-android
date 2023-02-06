package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import java.util.UUID

val accountsRequestExact = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
    requestId = UUID.randomUUID().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(1, "", ""),
    authRequest = MessageFromDataChannel.IncomingRequest.AuthRequest.LoginRequest(""),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        requiresProofOfOwnership = false,
        numberOfAccounts = 1,
        quantifier = MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier.Exactly
    )
)
val accountsTwoRequestExact = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
    requestId = UUID.randomUUID().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(1, "", ""),
    authRequest = MessageFromDataChannel.IncomingRequest.AuthRequest.LoginRequest(""),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        requiresProofOfOwnership = false,
        numberOfAccounts = 2,
        quantifier = MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.Exactly
    )
)

val accountsRequestAtLeast = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
    requestId = UUID.randomUUID().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(1, "", ""),
    authRequest = MessageFromDataChannel.IncomingRequest.AuthRequest.LoginRequest(""),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        requiresProofOfOwnership = false,
        numberOfAccounts = 2,
        quantifier = MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier.AtLeast
    )
)
