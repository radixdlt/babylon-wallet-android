package com.babylon.wallet.android.domain.model

data class Selectable<T>(
    val data: T,
    val selected: Boolean = false
)
