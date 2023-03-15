package rdx.works.core

fun <T> List<T>.mapWhen(predicate: (T) -> Boolean, mutation: (T) -> T): List<T> = map { value ->
    if (predicate(value)) {
        mutation(value)
    } else {
        value
    }
}
