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
 * @param releaseVersion The release that is currently deployed to the Gateway API.
 * @param openApiSchemaVersion The Open API Schema version that was used to generate the API models.
 * @param imageTag Image tag that is currently deployed to the Gateway API.
 */
@Serializable

data class GatewayInfoResponseReleaseInfo (

    /* The release that is currently deployed to the Gateway API. */
    @SerialName(value = "release_version")
    val releaseVersion: kotlin.String,

    /* The Open API Schema version that was used to generate the API models. */
    @SerialName(value = "open_api_schema_version")
    val openApiSchemaVersion: kotlin.String,

    /* Image tag that is currently deployed to the Gateway API. */
    @SerialName(value = "image_tag")
    val imageTag: kotlin.String

)

