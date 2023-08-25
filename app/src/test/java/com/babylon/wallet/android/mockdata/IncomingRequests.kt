package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import rdx.works.core.UUIDGenerator

val accountsRequestExact = MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
    remoteConnectorId = "remoteConnectorId",
    interactionId = UUIDGenerator.uuid().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(1, "", "", false),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        numberOfValues = MessageFromDataChannel.IncomingRequest.NumberOfValues(
            1,
            MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
        ),
        challenge = null
    )
)
val accountsTwoRequestExact = MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
    remoteConnectorId = "remoteConnectorId",
    interactionId = UUIDGenerator.uuid().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(1, "", "", false),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        numberOfValues = MessageFromDataChannel.IncomingRequest.NumberOfValues(
            2,
            MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
        ),
        challenge = null
    )
)

val accountsRequestAtLeast = MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
    remoteConnectorId = "remoteConnectorId",
    interactionId = UUIDGenerator.uuid().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(1, "", "", false),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        numberOfValues = MessageFromDataChannel.IncomingRequest.NumberOfValues(
            2,
            MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
        ),
        challenge = null
    )
)
