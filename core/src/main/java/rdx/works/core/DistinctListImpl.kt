package rdx.works.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// see this issue for explanation https://github.com/Kotlin/kotlinx.serialization/issues/685
typealias DistinctList<T> =
    @Serializable(with = DistinctListSerializer::class)
    DistinctListImpl<T>

class DistinctListImpl<T : Distinct> : ArrayList<T>() {

    override fun add(element: T): Boolean {
        ensureElementsNotPresentInTheList(listOf(element))
        return super.add(element)
    }

    override fun add(index: Int, element: T) {
        ensureElementsNotPresentInTheList(listOf(element))
        super.add(index, element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        ensureElementsNotPresentInTheList(elements)
        return super.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        ensureElementsNotPresentInTheList(elements)
        return super.addAll(index, elements)
    }

    internal fun ensureElementsNotPresentInTheList(elements: Iterable<T>) {
        val duplicate = elements.find { element -> any { it.identifier == element.identifier } }
        if (duplicate != null) {
            throw DuplicateElementException(duplicate.identifier)
        }
    }
}

internal fun <T : Distinct> ensureOnlyDistinctElementsPresentInTheList(elements: Iterable<T>) {
    if (elements.distinctBy { it.identifier }.size != elements.count()) {
        throw DuplicateElementException()
    }
}

operator fun <T : Distinct> DistinctList<T>.plus(elements: Iterable<T>): DistinctList<T> {
    ensureElementsNotPresentInTheList(elements)
    return DistinctListImpl<T>().apply {
        addAll(this@plus)
        addAll(elements)
    }
}

fun <T : Distinct> Iterable<T>.toDistinctList(): DistinctList<T> {
    ensureOnlyDistinctElementsPresentInTheList(this)
    return this as? DistinctList
        ?: (DistinctList<T>() + this)
}

fun <T : Distinct> emptyDistinctList(): DistinctList<T> = DistinctListImpl()

class DistinctListSerializer<T : Distinct>(elementSerializer: KSerializer<T>) : KSerializer<DistinctListImpl<T>> {

    private val delegateSerializer = ListSerializer(elementSerializer)

    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: DistinctListImpl<T>) {
        delegateSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): DistinctListImpl<T> {
        return DistinctListImpl<T>().apply {
            addAll(delegateSerializer.deserialize(decoder))
        }
    }
}

interface Distinct {
    val identifier: String
}

data class DuplicateElementException(val identifier: String? = null) : RuntimeException(
    if (identifier == null) "Duplicate elements found" else "Duplicate element with id $identifier"
)
