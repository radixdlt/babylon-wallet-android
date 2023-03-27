package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import io.ktor.util.moveToByteArray
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.webrtc.DataChannel
import rdx.works.peerdroid.data.PackageMessageDto
import rdx.works.peerdroid.data.PackageMessageDto.Companion.toChunk
import rdx.works.peerdroid.data.PackageMessageDto.Companion.toMetadata
import rdx.works.peerdroid.domain.BasePackage
import timber.log.Timber

// Once the WebRTC flow is complete & data channel is open
// then this will be used to observe the incoming messages & the state changes.
internal fun DataChannel.eventFlow(): Flow<DataChannelEvent> = callbackFlow {

    val callback = object : DataChannel.Observer {

        override fun onBufferedAmountChange(p0: Long) {
            Timber.d("onBufferedAmountChange")
        }

        override fun onStateChange() {
            val state = when (this@eventFlow.state()) {
                DataChannel.State.CONNECTING -> DataChannelEvent.StateChanged.CONNECTING
                DataChannel.State.OPEN -> DataChannelEvent.StateChanged.OPEN
                DataChannel.State.CLOSING -> DataChannelEvent.StateChanged.CLOSING
                DataChannel.State.CLOSED -> DataChannelEvent.StateChanged.CLOSE
                else -> DataChannelEvent.StateChanged.UNKNOWN
            }
            trySend(state)
        }

        override fun onMessage(p0: DataChannel.Buffer?) {
            if (p0?.data == null) {
                trySend(DataChannelEvent.UnknownError(message = "received null data channel buffer"))
            } else {
                try {
                    val jsonString = p0.data.moveToByteArray().decodeToString()
                    Timber.d("package message json is: $jsonString")
                    // parse json string to a PackageMessageDto object
                    val packageMessageDto = Json.decodeFromString<PackageMessageDto>(jsonString)
                    parsePackageDto(packageMessageDto = packageMessageDto)
                } catch (exception: Exception) {
                    Timber.e("an error occurred while decoding the message: ${exception.localizedMessage}")
                }
            }
        }

        private var currentMessageId = "" // used to build the list with all the chunks for this messageId
        private var currentChunkCount = -1

        // This list holds all chunks for an incoming message.
        // The first one in the list is always the metadata [MetadataPackage],
        // and the rest the [ChunkPackage]s.
        // Once the list is complete with all the chunks of the incoming message,
        // then we return it.
        private val messageInListOfPackages = mutableListOf<BasePackage>()

        private fun parsePackageDto(packageMessageDto: PackageMessageDto) {
            when (PackageMessageDto.PackageType.from(packageMessageDto.packageType)) {
                PackageMessageDto.PackageType.METADATA -> {
                    keepMetaDataInMemory(packageMessageDto.toMetadata())
                }
                PackageMessageDto.PackageType.CHUNK -> {
                    try {
                        parseChunkAndSendEventIfListIsComplete(packageMessageDto.toChunk())
                    } catch (exception: Exception) {
                        Timber.e("exception occurred while parsing chunk packages: ${exception.localizedMessage}")
                        trySend(
                            DataChannelEvent.UnknownError(
                                message = "exception occurred while parsing chunk packages"
                            )
                        )
                    }
                }
                PackageMessageDto.PackageType.MESSAGE_CONFIRMATION -> {
                    trySend(
                        DataChannelEvent.IncomingMessage.ConfirmationNotification(
                            messageId = packageMessageDto.messageId
                        )
                    )
                }
                PackageMessageDto.PackageType.MESSAGE_ERROR -> {
                    trySend(
                        DataChannelEvent.IncomingMessage.ErrorNotification(
                            messageId = packageMessageDto.messageId
                        )
                    )
                }
            }
        }

        private fun keepMetaDataInMemory(currentMetadata: BasePackage.MetadataPackage) {
            messageInListOfPackages.clear()
            currentMessageId = currentMetadata.messageId
            currentChunkCount = currentMetadata.chunkCount
            messageInListOfPackages.add(0, currentMetadata)
        }

        private fun parseChunkAndSendEventIfListIsComplete(currentChunk: BasePackage.ChunkPackage) {
            // Check that the chunk has
            // 1. the same message id with the metadata
            // 2. index is less the amount of chunks
            val validChunkMessageId = currentMessageId == currentChunk.messageId
            val currentChunkIndexLessThanChunksCount = currentChunk.chunkIndex < currentChunkCount
            if (validChunkMessageId && currentChunkIndexLessThanChunksCount) {
                when (val lastItem = messageInListOfPackages.last()) {
                    is BasePackage.MetadataPackage -> { // this should be the first element in messageInListOfPackages
                        if (currentChunk.chunkIndex == 0) {
                            messageInListOfPackages.add(currentChunk)
                        }
                    }
                    is BasePackage.ChunkPackage -> {
                        // Every new chunk index should be incremented compared to the last chunk index
                        val currentIndexGreaterThanLastIndex = lastItem.chunkIndex == currentChunk.chunkIndex - 1
                        if (currentIndexGreaterThanLastIndex) {
                            messageInListOfPackages.add(currentChunk)
                        }
                    }
                }

                val lastChunk = messageInListOfPackages.size == currentChunkCount + 1
                if (lastChunk) {
                    // clear the values
                    currentMessageId = ""
                    currentChunkCount = -1
                    trySend(
                        DataChannelEvent.IncomingMessage.Package(
                            messageInListOfPackages = messageInListOfPackages
                        )
                    )
                }
            } else {
                trySend(
                    DataChannelEvent.UnknownError("unable to parse chunk and build list of chunks")
                )
            }
        }
    }

    registerObserver(callback)

    trySend(this@eventFlow.currentState())

    awaitClose {
        Timber.d("$this@eventFlow: awaitClose")
        unregisterObserver()
        Timber.d("$this@eventFlow: unregister observer and state is: ${this@eventFlow.currentState()}")
    }
}

private fun DataChannel.currentState(): DataChannelEvent.StateChanged {
    return when (this.state()) {
        DataChannel.State.CONNECTING -> DataChannelEvent.StateChanged.CONNECTING
        DataChannel.State.OPEN -> DataChannelEvent.StateChanged.OPEN
        DataChannel.State.CLOSING -> DataChannelEvent.StateChanged.OPEN
        DataChannel.State.CLOSED -> DataChannelEvent.StateChanged.CLOSING
        else -> DataChannelEvent.StateChanged.UNKNOWN
    }
}
