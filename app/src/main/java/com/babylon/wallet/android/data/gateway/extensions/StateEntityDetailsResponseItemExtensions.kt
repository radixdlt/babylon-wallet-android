package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem

val StateEntityDetailsResponseItem.fungibleResourceAddresses: List<String>
    get() = fungibleResources?.items?.map { it.resourceAddress }.orEmpty()

val StateEntityDetailsResponseItem.nonFungibleResourceAddresses: List<String>
    get() = nonFungibleResources?.items?.map { it.resourceAddress }.orEmpty()

val StateEntityDetailsResponseItem.allResourceAddresses: List<String>
    get() = fungibleResourceAddresses + nonFungibleResourceAddresses
