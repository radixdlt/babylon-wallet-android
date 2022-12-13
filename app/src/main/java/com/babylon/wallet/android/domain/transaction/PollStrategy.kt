package com.babylon.wallet.android.domain.transaction

data class PollStrategy(
    val maxTries: Int = 20,
    val delayBetweenTriesMs: Long = 2000L,
    val maxConsecutiveErrors: Int = 3
)
