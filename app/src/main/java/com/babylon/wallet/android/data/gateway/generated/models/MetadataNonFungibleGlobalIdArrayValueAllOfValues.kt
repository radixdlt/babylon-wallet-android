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
 * @param nonFungibleId String-encoded non-fungible ID.
 */
@Serializable

data class MetadataNonFungibleGlobalIdArrayValueAllOfValues (

    /* Bech32m-encoded human readable version of the address. */
    @SerialName(value = "resource_address")
    val resourceAddress: kotlin.String,

    /* String-encoded non-fungible ID. */
    @SerialName(value = "non_fungible_id")
    val nonFungibleId: kotlin.String

)
