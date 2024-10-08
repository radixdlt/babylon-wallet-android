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
 * A set of flags to configure the response of the transaction preview.
 *
 * @param radixEngineToolkitReceipt This flag controls whether the preview response will include a Radix Engine Toolkit serializable receipt or not.
 * If not provided, this defaults to `false` and no toolkit receipt is provided in the response.
 */
@Serializable

data class TransactionPreviewOptIns (

    /* This flag controls whether the preview response will include a Radix Engine Toolkit serializable receipt or not.
    If not provided, this defaults to `false` and no toolkit receipt is provided in the response.  */
    @SerialName(value = "radix_engine_toolkit_receipt")
    val radixEngineToolkitReceipt: kotlin.Boolean? = false

)
