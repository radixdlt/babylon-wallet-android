package rdx.works.peerdroid.domain

/**
 * The message was not sent: nothing of it was handed to the transport (e.g. there is no open data channel or iroh
 * session for the connector, or the data channel did not take the message's first package). The connector extension
 * cannot have received any of it, so it may be sent again, over the same transport or another one.
 *
 * Any other failure of a send means that part or all of the message may have reached the connector extension: no
 * transport sends it again, over itself or another one, and a caller should send it again only if a second copy is
 * harmless.
 */
class MessageNotSentException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
