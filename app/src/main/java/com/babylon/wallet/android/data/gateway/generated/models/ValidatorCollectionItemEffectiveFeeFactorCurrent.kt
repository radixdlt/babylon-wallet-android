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
 * @param feeFactor String-encoded decimal representing the amount of a related fungible resource.
 */
@Serializable

data class ValidatorCollectionItemEffectiveFeeFactorCurrent (

    /* String-encoded decimal representing the amount of a related fungible resource. */
    @SerialName(value = "fee_factor")
    val feeFactor: kotlin.String

)
