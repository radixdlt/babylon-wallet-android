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
 * @param address Bech32m-encoded human readable version of the address.
 */
@Serializable

data class StateEntityMetadataPageRequestAllOf (

    /* Bech32m-encoded human readable version of the address. */
    @SerialName(value = "address")
    val address: kotlin.String

)

