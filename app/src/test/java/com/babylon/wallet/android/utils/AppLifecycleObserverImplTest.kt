package com.babylon.wallet.android.utils

import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLifecycleObserverImplTest {

    private val sut = AppLifecycleObserverImpl(scope = TestScope())

    @Test
    fun `the app is in front while one of its activities is the top resumed one`() {
        val main = Any()
        val lock = Any()
        assertNull(sut.inFront.value)

        sut.onTopResumedActivityChanged(main, isTop = true)
        assertEquals(true, sut.inFront.value)

        // Its own lock screen on top: still in front once it is the top one.
        sut.onTopResumedActivityChanged(lock, isTop = true)
        sut.onTopResumedActivityChanged(main, isTop = false)
        assertEquals(true, sut.inFront.value)

        // The recent apps screen, or another app.
        sut.onTopResumedActivityChanged(lock, isTop = false)
        assertEquals(false, sut.inFront.value)
    }
}
