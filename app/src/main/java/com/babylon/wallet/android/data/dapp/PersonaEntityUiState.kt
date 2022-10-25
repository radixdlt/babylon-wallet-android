package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.profile.PersonaEntity

data class PersonaEntityUiState(
    val personaEntity: PersonaEntity,
    val selected: Boolean = false
)