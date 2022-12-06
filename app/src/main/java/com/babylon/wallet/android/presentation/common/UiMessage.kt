package com.babylon.wallet.android.presentation.common

data class UiMessage(val error: Throwable?, val timestamp: Long = System.currentTimeMillis())
