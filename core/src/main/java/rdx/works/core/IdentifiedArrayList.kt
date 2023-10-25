package rdx.works.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// see this issue for explanation https://github.com/Kotlin/kotlinx.serialization/issues/685
typealias IdentifiedArrayList<T> =
    @Serializable(with = IdentifiedSetSerializer::class)
    IdentifiedArrayListImpl<T>

class IdentifiedArrayListImpl<T : Identified> : ArrayList<T>() {

    private val elementIds = mutableSetOf<String>()

    override fun add(element: T): Boolean {
        if (elementIds.contains(element.identifier)) {
            return false
        }
        elementIds.add(element.identifier)
        return super.add(element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val newElements = elements.distinctBy { it.identifier }.filter { !elementIds.contains(it.identifier) }
        if (newElements.isEmpty()) {
            return false
        }
        elementIds.addAll(newElements.map { it.identifier })
        return super.addAll(newElements)
    }

    override fun clear() {
        elementIds.clear()
        super.clear()
    }

    override fun remove(element: T): Boolean {
        if (!elementIds.contains(element.identifier)) {
            return false
        }
        elementIds.remove(element.identifier)
        return removeIf { it.identifier == element.identifier }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val idsToRemove = elements.filter { elementIds.contains(it.identifier) }.map { it.identifier }
        if (idsToRemove.isEmpty()) {
            return false
        }
        elementIds.removeAll(idsToRemove)
        return removeIf { idsToRemove.contains(it.identifier) }
    }

    override fun contains(element: T): Boolean {
        return elementIds.contains(element.identifier)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return elementIds.containsAll(elements.map { it.identifier })
    }
}

operator fun <T : Identified> IdentifiedArrayList<T>.plus(elements: Collection<T>): IdentifiedArrayList<T> {
    val existingIds = map { it.identifier }
    val newElements = elements.filter { !existingIds.contains(it.identifier) }
    return IdentifiedArrayList<T>().apply {
        addAll(this@plus)
        addAll(newElements)
    }
}

fun <T : Identified> Collection<T>.toIdentifiedArrayList(): IdentifiedArrayList<T> {
    return this as? IdentifiedArrayList<T>
        ?: (IdentifiedArrayList<T>().apply { addAll(this@toIdentifiedArrayList.distinctBy { it.identifier }) })
}

fun <T : Identified> emptyIdentifiedArrayList(): IdentifiedArrayList<T> = IdentifiedArrayList()

fun <T : Identified> identifiedArrayListOf(vararg elements: T): IdentifiedArrayList<T> =
    if (elements.isNotEmpty()) elements.toList().toIdentifiedArrayList() else emptyIdentifiedArrayList()

class IdentifiedSetSerializer<T : Identified>(elementSerializer: KSerializer<T>) : KSerializer<IdentifiedArrayList<T>> {

    private val delegateSerializer = ListSerializer(elementSerializer)

    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: IdentifiedArrayList<T>) {
        delegateSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): IdentifiedArrayList<T> {
        return IdentifiedArrayList<T>().apply {
            addAll(delegateSerializer.deserialize(decoder))
        }
    }
}

interface Identified {
    val identifier: String
}
