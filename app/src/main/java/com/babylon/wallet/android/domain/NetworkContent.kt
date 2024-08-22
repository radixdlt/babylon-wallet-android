package com.babylon.wallet.android.domain

sealed class NetworkContent<out T> {
    data object None : NetworkContent<Nothing>()
    data object Empty : NetworkContent<Nothing>()
    data object Loading : NetworkContent<Nothing>()
    data class Loaded<T>(override val data: T) : NetworkContent<T>()

    open val data: T?
        get() = (this as? Loaded)?.data
}
