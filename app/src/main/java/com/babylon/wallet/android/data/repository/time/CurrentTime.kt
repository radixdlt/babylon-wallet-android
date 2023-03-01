package com.babylon.wallet.android.data.repository.time

import java.time.Instant

interface CurrentTime {

    fun now(): Instant
}

class CurrentTimeImpl : CurrentTime {

    override fun now(): Instant = Instant.now()
}
