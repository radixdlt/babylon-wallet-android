package com.babylon.wallet.core.domain

import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import rdx.works.core.domain.ThemeSelection

class ThemeSelectionTest {

    @Test
    fun testToLiteral() {
        assertEquals(
            "light",
            ThemeSelection.LIGHT.toLiteral()
        )
        assertEquals(
            "dark",
            ThemeSelection.DARK.toLiteral()
        )
        assertEquals(
            "system",
            ThemeSelection.SYSTEM.toLiteral()
        )
    }

    @Test
    fun testFromLiteral() {
        assertEquals(
            ThemeSelection.LIGHT,
            ThemeSelection.fromLiteral("light")
        )
        assertEquals(
            ThemeSelection.DARK,
            ThemeSelection.fromLiteral("dark")
        )
        assertEquals(
            ThemeSelection.SYSTEM,
            ThemeSelection.fromLiteral("system")
        )
        assertThrows(IllegalStateException::class.java) {
            ThemeSelection.fromLiteral("any")
        }

    }
}