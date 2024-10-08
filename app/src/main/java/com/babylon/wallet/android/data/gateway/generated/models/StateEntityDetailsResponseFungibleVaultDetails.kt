/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package com.babylon.wallet.android.data.gateway.generated.models

import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregatedVaultItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param type 
 * @param resourceAddress Bech32m-encoded human readable version of the address.
 * @param balance 
 */
@Serializable

data class StateEntityDetailsResponseFungibleVaultDetails (

    @Contextual @SerialName(value = "type")
    override val type: StateEntityDetailsResponseItemDetailsType,

    /* Bech32m-encoded human readable version of the address. */
    @SerialName(value = "resource_address")
    val resourceAddress: kotlin.String,

    @SerialName(value = "balance")
    val balance: FungibleResourcesCollectionItemVaultAggregatedVaultItem

) : StateEntityDetailsResponseItemDetails()

