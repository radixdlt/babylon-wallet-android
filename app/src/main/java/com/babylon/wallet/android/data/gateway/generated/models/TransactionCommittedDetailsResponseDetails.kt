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
 * @param receipt 
 * @param referencedGlobalEntities 
 * @param rawHex Hex-encoded binary blob.
 * @param messageHex Hex-encoded binary blob.
 */
@Serializable

data class TransactionCommittedDetailsResponseDetails (

    @SerialName(value = "receipt")
    val receipt: TransactionReceipt,

    @SerialName(value = "referenced_global_entities")
    val referencedGlobalEntities: kotlin.collections.List<kotlin.String>,

    /* Hex-encoded binary blob. */
    @SerialName(value = "raw_hex")
    val rawHex: kotlin.String? = null,

    /* Hex-encoded binary blob. */
    @SerialName(value = "message_hex")
    val messageHex: kotlin.String? = null

)

