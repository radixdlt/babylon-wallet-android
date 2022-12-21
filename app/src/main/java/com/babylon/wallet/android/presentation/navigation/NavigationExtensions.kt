package com.babylon.wallet.android.presentation.navigation

import androidx.navigation.NavController

fun <T> NavController.passData(key: String, value: T) {
    currentBackStackEntry?.savedStateHandle?.set(key, value)
}