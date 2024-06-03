package com.babylon.wallet.android.data.dapp.model

class IncompatibleRequestVersionException(
    val requestId: String,
    val requestVersion: Long?
) : IllegalStateException()
