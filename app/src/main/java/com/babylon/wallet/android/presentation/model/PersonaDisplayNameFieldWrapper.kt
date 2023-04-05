package com.babylon.wallet.android.presentation.model

data class PersonaDisplayNameFieldWrapper(
    val value: String = "",
    val valid: Boolean? = null,
    val required: Boolean = false,
    val wasEdited: Boolean = false,
    val shouldDisplayValidationError: Boolean = false
)
