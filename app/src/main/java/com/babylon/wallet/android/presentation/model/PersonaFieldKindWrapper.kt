package com.babylon.wallet.android.presentation.model

import rdx.works.profile.data.model.pernetwork.Network

data class PersonaFieldKindWrapper(
    val kind: Network.Persona.Field.Kind,
    val selected: Boolean = false,
    val value: String = "",
    val valid: Boolean? = null,
    val required: Boolean = false,
    val wasEdited: Boolean = false,
    val shouldDisplayValidationError: Boolean = false
) {
    fun isPhoneNumber(): Boolean {
        return kind == Network.Persona.Field.Kind.PhoneNumber
    }
}
