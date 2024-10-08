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


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 
 *
 * Values: FungibleResource,NonFungibleResource,FungibleVault,NonFungibleVault,Package,Component
 */
@Serializable
enum class StateEntityDetailsResponseItemDetailsType(val value: kotlin.String) {

    @SerialName(value = "FungibleResource")
    FungibleResource("FungibleResource"),

    @SerialName(value = "NonFungibleResource")
    NonFungibleResource("NonFungibleResource"),

    @SerialName(value = "FungibleVault")
    FungibleVault("FungibleVault"),

    @SerialName(value = "NonFungibleVault")
    NonFungibleVault("NonFungibleVault"),

    @SerialName(value = "Package")
    Package("Package"),

    @SerialName(value = "Component")
    Component("Component");

    /**
     * Override [toString()] to avoid using the enum variable name as the value, and instead use
     * the actual value defined in the API spec file.
     *
     * This solves a problem when the variable name and its value are different, and ensures that
     * the client sends the correct enum values to the server always.
     */
    override fun toString(): kotlin.String = value

    companion object {
        /**
         * Converts the provided [data] to a [String] on success, null otherwise.
         */
        fun encode(data: kotlin.Any?): kotlin.String? = if (data is StateEntityDetailsResponseItemDetailsType) "$data" else null

        /**
         * Returns a valid [StateEntityDetailsResponseItemDetailsType] for [data], null otherwise.
         */
        fun decode(data: kotlin.Any?): StateEntityDetailsResponseItemDetailsType? = data?.let {
          val normalizedData = "$it".lowercase()
          values().firstOrNull { value ->
            it == value || normalizedData == "$value".lowercase()
          }
        }
    }
}

