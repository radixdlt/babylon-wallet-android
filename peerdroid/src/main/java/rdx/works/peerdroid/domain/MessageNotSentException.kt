package rdx.works.peerdroid.domain

/**
 * The message was not sent: nothing of it was handed to the data channel (there is no open data channel for the
 * connector, the channel is closed or disposed, or it did not take the message's first package). The connector
 * extension cannot have received any of it, so the caller may send it again.
 *
 * Any other failure of a send means that part or all of the message may have reached the connector extension:
 * peerdroid does not send it again, and a caller should send it again only if a second copy is harmless.
 */
class MessageNotSentException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
