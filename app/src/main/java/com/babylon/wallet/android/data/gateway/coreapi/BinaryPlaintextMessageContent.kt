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

package com.babylon.wallet.android.data.gateway.coreapi

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param type
 * @param valueHex The hex-encoded value of a message that the author decided to provide as raw bytes.
 */
@Serializable
data class BinaryPlaintextMessageContent(

    @Contextual @SerialName(value = "type")
    override val type: PlaintextMessageContentType,

    /* The hex-encoded value of a message that the author decided to provide as raw bytes. */
    @SerialName(value = "value_hex")
    val valueHex: kotlin.String

) : PlaintextMessageContent()