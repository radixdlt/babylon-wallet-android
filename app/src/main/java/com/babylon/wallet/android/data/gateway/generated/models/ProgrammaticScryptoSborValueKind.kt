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
 * These are the Scrypto SBOR `ValueKind`s, but with `Bytes` added as an alias for `Vec`, to display such values as hex-encoded strings. 
 *
 * Values: Bool,I8,I16,I32,I64,I128,U8,U16,U32,U64,U128,String,Enum,Array,Bytes,Map,Tuple,Reference,Own,Decimal,PreciseDecimal,NonFungibleLocalId
 */
@Serializable
enum class ProgrammaticScryptoSborValueKind(val value: kotlin.String) {

    @SerialName(value = "Bool")
    Bool("Bool"),

    @SerialName(value = "I8")
    I8("I8"),

    @SerialName(value = "I16")
    I16("I16"),

    @SerialName(value = "I32")
    I32("I32"),

    @SerialName(value = "I64")
    I64("I64"),

    @SerialName(value = "I128")
    I128("I128"),

    @SerialName(value = "U8")
    U8("U8"),

    @SerialName(value = "U16")
    U16("U16"),

    @SerialName(value = "U32")
    U32("U32"),

    @SerialName(value = "U64")
    U64("U64"),

    @SerialName(value = "U128")
    U128("U128"),

    @SerialName(value = "String")
    String("String"),

    @SerialName(value = "Enum")
    Enum("Enum"),

    @SerialName(value = "Array")
    Array("Array"),

    @SerialName(value = "Bytes")
    Bytes("Bytes"),

    @SerialName(value = "Map")
    Map("Map"),

    @SerialName(value = "Tuple")
    Tuple("Tuple"),

    @SerialName(value = "Reference")
    Reference("Reference"),

    @SerialName(value = "Own")
    Own("Own"),

    @SerialName(value = "Decimal")
    Decimal("Decimal"),

    @SerialName(value = "PreciseDecimal")
    PreciseDecimal("PreciseDecimal"),

    @SerialName(value = "NonFungibleLocalId")
    NonFungibleLocalId("NonFungibleLocalId");

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
        fun encode(data: kotlin.Any?): kotlin.String? = if (data is ProgrammaticScryptoSborValueKind) "$data" else null

        /**
         * Returns a valid [ProgrammaticScryptoSborValueKind] for [data], null otherwise.
         */
        fun decode(data: kotlin.Any?): ProgrammaticScryptoSborValueKind? = data?.let {
          val normalizedData = "$it".lowercase()
          values().firstOrNull { value ->
            it == value || normalizedData == "$value".lowercase()
          }
        }
    }
}

