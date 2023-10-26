package rdx.works.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test


class IdentifiedArrayListImplTest {

    internal class IdentifiedString(val value: String) : Identified {

        override val identifier: String
            get() = value

    }

    private val identifiedArrayList = IdentifiedArrayList<IdentifiedString>()

    @Before
    fun setUp() {
        identifiedArrayList.add(IdentifiedString("1"))
        identifiedArrayList.add(IdentifiedString("2"))
        identifiedArrayList.add(IdentifiedString("3"))
        identifiedArrayList.addAll(listOf(IdentifiedString("4"), IdentifiedString("5"), IdentifiedString("5")))
    }

    @Test
    fun `test adding duplicate element`() {
        assertTrue(!identifiedArrayList.add(IdentifiedString("1")))
    }

    @Test
    fun `test adding duplicate elements`() {
        assertFalse(identifiedArrayList.addAll(listOf(IdentifiedString("1"), IdentifiedString("2"))))
        assertTrue(identifiedArrayList.addAll(listOf(IdentifiedString("6"))))
    }

    @Test
    fun `test adding duplicate elements with plus operator`() {
        assertTrue((identifiedArrayList + listOf(IdentifiedString("1"), IdentifiedString("2"))).size == 5)
        assertTrue((identifiedArrayList + listOf(IdentifiedString("6"))).size == 6)
    }

    @Test
    fun `test adding duplicate with toIdentifiedHashSet()`() {
        val listWithDuplicates = listOf(IdentifiedString("1"), IdentifiedString("2"), IdentifiedString("1"))
        val identifiedArrayList = listWithDuplicates.toIdentifiedArrayList()
        assertTrue(identifiedArrayList.size == 2)
        assertFalse(identifiedArrayList.add(IdentifiedString("2")))
        assertTrue(identifiedArrayList.add(IdentifiedString("3")))
        assertTrue(identifiedArrayList.size == 3)
    }

    @Test
    fun `test removing items`() {
        assertTrue(identifiedArrayList.remove(IdentifiedString("1")))
        assertFalse(identifiedArrayList.remove(IdentifiedString("1")))
        assertTrue(identifiedArrayList.remove(IdentifiedString("2")))
        assertTrue(identifiedArrayList.size == 3)
    }

    @Test
    fun `test removeAll`() {
        assertTrue(identifiedArrayList.removeAll(listOf(IdentifiedString("1"), IdentifiedString("2"))))
        assertFalse(identifiedArrayList.removeAll(listOf(IdentifiedString("1"), IdentifiedString("2"))))
        assertTrue(identifiedArrayList.size == 3)
        assertTrue(identifiedArrayList.remove(IdentifiedString("4")))
        assertTrue(identifiedArrayList.size == 2)
    }

}