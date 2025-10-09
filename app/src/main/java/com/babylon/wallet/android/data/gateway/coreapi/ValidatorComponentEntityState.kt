package com.babylon.wallet.android.data.gateway.coreapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidatorComponentEntityState(

    @SerialName(value = "stake_xrd_vault")
    val stakeXrdVault: StakeXrdVault? = null,

    @SerialName(value = "stake_unit_resource_address")
    val stakeUnitResourceAddress: String? = null,

    @SerialName(value = "claim_token_resource_address")
    val claimTokenResourceAddress: String? = null,

    @SerialName(value = "default_deposit_rule")
    val defaultDepositRule: DefaultDepositRule? = null

)