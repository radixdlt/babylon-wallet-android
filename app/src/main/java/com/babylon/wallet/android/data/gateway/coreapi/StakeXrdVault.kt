package com.babylon.wallet.android.data.gateway.coreapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StakeXrdVault(

    @SerialName(value = "is_global")
    val isGlobal: Boolean? = null,

    @SerialName(value = "entity_type")
    val entityType: String? = null,

    @SerialName(value = "entity_address")
    val entityAddress: String? = null

)