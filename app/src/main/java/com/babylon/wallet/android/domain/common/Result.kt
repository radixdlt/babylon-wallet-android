package com.babylon.wallet.android.domain.common

suspend fun <T, R> Result<T>.map(action: suspend (T) -> R): Result<R> = fold(
    onSuccess = { Result.success(action(it)) },
    onFailure = { Result.failure(it) }
)

inline fun <T, R> Result<T>.switchMap(action: (T) -> Result<R>): Result<R> = fold(
    onSuccess = { action(it) },
    onFailure = { Result.failure(it) }
)
