package com.babylon.wallet.android.presentation.model

import rdx.works.profile.data.model.pernetwork.Network

data class PersonaFieldKindWrapper(
    val id: Network.Persona.Field.ID,
    val selected: Boolean = false,
    val value: String = "",
    val valid: Boolean? = null,
    val required: Boolean = false,
    val wasEdited: Boolean = false,
    val shouldDisplayValidationError: Boolean = false
) {
    fun isPhoneNumber(): Boolean {
        return id == Network.Persona.Field.ID.PhoneNumber
    }
}
