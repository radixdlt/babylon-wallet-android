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

import com.babylon.wallet.android.data.gateway.generated.models.RoyaltyAmount

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param methodName 
 * @param royaltyAmount 
 */
@Serializable

data class BlueprintMethodRoyalty (

    @SerialName(value = "method_name")
    val methodName: kotlin.String,

    @SerialName(value = "royalty_amount")
    val royaltyAmount: RoyaltyAmount? = null

)

