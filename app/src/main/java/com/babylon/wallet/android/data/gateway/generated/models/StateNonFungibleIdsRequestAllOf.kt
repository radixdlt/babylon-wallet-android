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


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param resourceAddress Bech32m-encoded human readable version of the address.
 */
@Serializable

data class StateNonFungibleIdsRequestAllOf (

    /* Bech32m-encoded human readable version of the address. */
    @SerialName(value = "resource_address")
    val resourceAddress: kotlin.String

)

