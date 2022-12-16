package com.babylon.wallet.android.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

fun <T> NavController.setDataForRoute(route: String, key: String, value: T) {
    backQueue.lastOrNull { it.destination.route == route }?.savedStateHandle?.set(key, value)
}

@Composable
fun <T> NavBackStackEntry.ReceiveData(key: String, onResult: (T) -> Unit) {
    val state = savedStateHandle.getLiveData<T>(key).observeAsState()
    state.value?.let {
        onResult(it)
        savedStateHandle.remove<T>(key)
    }
}
