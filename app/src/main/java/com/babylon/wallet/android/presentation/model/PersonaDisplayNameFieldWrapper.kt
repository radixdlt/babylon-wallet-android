package com.babylon.wallet.android.presentation.model

import com.radixdlt.sargon.extensions.SharedConstants

data class PersonaDisplayNameFieldWrapper(
    val value: String = "",
    val required: Boolean = false,
    val wasEdited: Boolean = false
) {
    val validationState: ValidationState
        get() {
            val valueTrimmed = value.trim()
            return when {
                valueTrimmed.isEmpty() -> ValidationState.Empty
                valueTrimmed.length > SharedConstants.entityNameMaxLength -> ValidationState.TooLong
                else -> ValidationState.Valid
            }
        }

    val isValid: Boolean
        get() = validationState == ValidationState.Valid

    enum class ValidationState {
        Valid,
        Empty,
        TooLong
    }
}
