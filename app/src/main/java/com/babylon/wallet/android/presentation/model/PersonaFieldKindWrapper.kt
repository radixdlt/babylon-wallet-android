package com.babylon.wallet.android.presentation.model

import rdx.works.profile.data.model.pernetwork.PersonaDataEntryID

data class PersonaFieldKindWrapper(
    val id: PersonaDataEntryID,
    val selected: Boolean = false,
    val value: String = "",
    val valid: Boolean? = null,
    val required: Boolean = false,
    val wasEdited: Boolean = false,
    val shouldDisplayValidationError: Boolean = false
) {
    fun isPhoneNumber(): Boolean {
        //TODO persona data
        return false
    }
}
