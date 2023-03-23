package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.domain.model.FungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleMetadataContainer
import com.babylon.wallet.android.domain.model.NonFungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer

fun StateEntityDetailsResponseItem.toFungibleToken() = FungibleToken(
    address = address,
    metadata = metadata.asMetadataStringMap()
)

fun StateEntityDetailsResponseItem.toNonFungibleToken(idContainer: NonFungibleTokenIdContainer?) = NonFungibleToken(
    address = address,
    nonFungibleIdContainer = idContainer,
    metadataContainer = NonFungibleMetadataContainer(
        metadata = metadata.asMetadataStringMap()
    )
)
