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

import com.babylon.wallet.android.data.gateway.generated.models.MetadataTypedValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataValueType

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param type 
 * @param propertyValues 
 */
@Serializable

data class MetadataU64ArrayValue (

    @Contextual @SerialName(value = "type")
    override val type: MetadataValueType,

    @SerialName(value = "values")
    val propertyValues: kotlin.collections.List<kotlin.String>

) : MetadataTypedValue()

