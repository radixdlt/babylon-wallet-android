package com.babylon.wallet.android.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

fun <T> NavController.passDataBack(route: String, key: String, value: T) {
    backQueue.firstOrNull { it.destination.route == route }?.let {
        it.savedStateHandle[key] = value
    }
}

@Composable
fun <T> NavBackStackEntry.receiveDataOnce(key: String): T? {
    val result = savedStateHandle.get<T>(key)
    if (result != null) {
        savedStateHandle.remove<Boolean>(key)
    }
    return result
}
