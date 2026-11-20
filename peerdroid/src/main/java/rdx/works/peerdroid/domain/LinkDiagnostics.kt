package rdx.works.peerdroid.domain

/**
 * The short form of a connection id used in logs and reports (it is not a secret, but the full id is noise).
 */
val ConnectionIdHolder.shortId: String
    get() = id.take(SHORT_ID_LENGTH)

private const val SHORT_ID_LENGTH = 8
