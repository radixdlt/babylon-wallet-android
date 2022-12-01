package com.babylon.wallet.android.domain.common

data class UiMessage(val error: Throwable?, val timestamp: Long = System.currentTimeMillis())
