package com.babylon.wallet.android.presentation.settings.legacyimport

data class Selectable<T>(
    val data: T,
    val selected: Boolean = false
)