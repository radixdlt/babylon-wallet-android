package com.babylon.wallet.android.domain.model

import rdx.works.profile.data.model.pernetwork.Network

data class PersonaFieldKindWrapper(
    val kind: Network.Persona.Field.Kind,
    val selected: Boolean = false,
    val value: String = "",
    val valid: Boolean? = null,
    val required: Boolean = false
)
