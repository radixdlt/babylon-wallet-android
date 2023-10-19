package rdx.works.core

import org.junit.Before
import org.junit.Test

class DistinctListImplTest {

    internal class DistinctString(val value: String) : Distinct {

        override val identifier: String
            get() = value
    }

    private val distinctListImpl = DistinctListImpl<DistinctString>()

    @Before
    fun setUp() {
        distinctListImpl.add(DistinctString("1"))
        distinctListImpl.add(DistinctString("2"))
        distinctListImpl.add(DistinctString("3"))
        distinctListImpl.addAll(listOf(DistinctString("4"), DistinctString("5")))
    }

    @Test(expected = DuplicateElementException::class)
    fun `test adding duplicate element`() {
        distinctListImpl.add(DistinctString("1"))
    }

    @Test(expected = DuplicateElementException::class)
    fun `test adding duplicate element at index 0`() {
        distinctListImpl.add(0, DistinctString("1"))
    }

    @Test(expected = DuplicateElementException::class)
    fun `test adding duplicate elements`() {
        distinctListImpl.addAll(listOf(DistinctString("1"), DistinctString("2")))
    }

    @Test(expected = DuplicateElementException::class)
    fun `test adding duplicate elements at index 0`() {
        distinctListImpl.addAll(0, listOf(DistinctString("1"), DistinctString("2")))
    }

    @Test(expected = DuplicateElementException::class)
    fun `test adding duplicate elements with plus operator`() {
        distinctListImpl + listOf(DistinctString("1"), DistinctString("2"))
    }

    @Test(expected = DuplicateElementException::class)
    fun `test adding duplicate with toDistinctList() `() {
        val listWithDuplicates = listOf(DistinctString("1"), DistinctString("2"), DistinctString("1"))
        listWithDuplicates.toDistinctList()
    }

}