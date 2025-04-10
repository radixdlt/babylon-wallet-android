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

import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValue
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueKind
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueMapEntry

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param kind 
 * @param keyKind 
 * @param valueKind 
 * @param propertyEntries 
 * @param typeName The name of the type of this value. This is only output when a schema is present and the type has a name. This property is ignored when the value is used as an input to the API. 
 * @param fieldName The name of the field which hosts this value. This property is only included if this value is a child of a `Tuple` or `Enum` with named fields. This property is ignored when the value is used as an input to the API. 
 * @param keyTypeName 
 * @param valueTypeName 
 */
@Serializable

data class ProgrammaticScryptoSborValueMap (

    @Contextual @SerialName(value = "kind")
    override val kind: ProgrammaticScryptoSborValueKind,

    @Contextual @SerialName(value = "key_kind")
    val keyKind: ProgrammaticScryptoSborValueKind,

    @Contextual @SerialName(value = "value_kind")
    val valueKind: ProgrammaticScryptoSborValueKind,

    @SerialName(value = "entries")
    val propertyEntries: kotlin.collections.List<ProgrammaticScryptoSborValueMapEntry>,

    /* The name of the type of this value. This is only output when a schema is present and the type has a name. This property is ignored when the value is used as an input to the API.  */
    @SerialName(value = "type_name")
    override val typeName: kotlin.String? = null,

    /* The name of the field which hosts this value. This property is only included if this value is a child of a `Tuple` or `Enum` with named fields. This property is ignored when the value is used as an input to the API.  */
    @SerialName(value = "field_name")
    override val fieldName: kotlin.String? = null,

    @SerialName(value = "key_type_name")
    val keyTypeName: kotlin.String? = null,

    @SerialName(value = "value_type_name")
    val valueTypeName: kotlin.String? = null

) : ProgrammaticScryptoSborValue()

