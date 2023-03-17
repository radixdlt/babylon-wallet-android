package rdx.works.core

/**
 * Used when in need to map a certain value of the list to the new value
 * provided by the [mutation] lambda.
 *
 * Be aware that since this method only changes one element of the list, the type of the mutated
 * item cannot change, as the original [map] function does, which can alter the type of the list.
 */
inline fun <T> Iterable<T>.mapWhen(
    predicate: (T) -> Boolean,
    mutation: (T) -> T
): List<T> = map { value ->
    if (predicate(value)) {
        mutation(value)
    } else {
        value
    }
}
