package rdx.works.core

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
        assert(identifiedArrayList.add(IdentifiedString("1")).not())
    }

    @Test
    fun `test adding duplicate elements`() {
        assert(identifiedArrayList.addAll(listOf(IdentifiedString("1"), IdentifiedString("2"))).not())
        assert(identifiedArrayList.addAll(listOf(IdentifiedString("6"))))
    }

    @Test
    fun `test adding duplicate elements with plus operator`() {
        assert((identifiedArrayList + listOf(IdentifiedString("1"), IdentifiedString("2"))).size == 5)
        assert((identifiedArrayList + listOf(IdentifiedString("6"))).size == 6)
    }

    @Test
    fun `test adding duplicate with toIdentifiedHashSet()`() {
        val listWithDuplicates = listOf(IdentifiedString("1"), IdentifiedString("2"), IdentifiedString("1"))
        val identifiedArrayList = listWithDuplicates.toIdentifiedArrayList()
        assert(identifiedArrayList.size == 2)
        assert(identifiedArrayList.add(IdentifiedString("2")).not())
        assert(identifiedArrayList.add(IdentifiedString("3")))
        assert(identifiedArrayList.size == 3)
    }

    @Test
    fun `test removing items`() {
        assert(identifiedArrayList.remove(IdentifiedString("1")))
        assert(identifiedArrayList.remove(IdentifiedString("1")).not())
        assert(identifiedArrayList.remove(IdentifiedString("2")))
        assert(identifiedArrayList.size == 3)
    }

    @Test
    fun `test removeAll`() {
        assert(identifiedArrayList.removeAll(listOf(IdentifiedString("1"), IdentifiedString("2"))))
        assert(identifiedArrayList.removeAll(listOf(IdentifiedString("1"), IdentifiedString("2"))).not())
        assert(identifiedArrayList.size == 3)
        assert(identifiedArrayList.remove(IdentifiedString("4")))
        assert(identifiedArrayList.size == 2)
    }

}