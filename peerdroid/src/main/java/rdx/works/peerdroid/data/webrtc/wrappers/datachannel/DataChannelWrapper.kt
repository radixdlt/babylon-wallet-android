package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.webrtc.DataChannel
import rdx.works.peerdroid.data.PackageDto
import rdx.works.peerdroid.data.describe
import rdx.works.peerdroid.data.logId
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.MessageNotSentException
import rdx.works.peerdroid.messagechunking.assembleChunks
import rdx.works.peerdroid.messagechunking.splitMessage
import rdx.works.peerdroid.messagechunking.verifyAssembledMessage
import timber.log.Timber
import java.nio.ByteBuffer
import kotlin.collections.ArrayList

@Suppress("InjectDispatcher")
data class DataChannelWrapper(
    private val connectionIdHolder: ConnectionIdHolder,
    private val webRtcDataChannel: DataChannel
) {

    init {
        Timber.d(
            "📯 init DataChannelWrapper for ${connectionIdHolder.logId} " +
                "with label the remote client id ${this.webRtcDataChannel.label()}",
        )
    }

    // One message at a time on the channel, all its packages together: the connector extension reassembles one
    // message at a time, so the packages of two messages sent at once interleave and one of them is lost, and a
    // message left half sent (its sender cancelled after the first package) keeps it from reading later ones.
    private val sending = Mutex()

    /**
     * Sends [message] in chunks. Fails with [MessageNotSentException] when the data channel did not take its first
     * package (closed, or its send buffer is full), since the connector extension then cannot have received anything.
     *
     * The link is reported open as soon as its peer connection is, which can be a few milliseconds before the data
     * channel itself: a message sent then (the account list, as the link opens) waits for the channel to open.
     */
    suspend fun sendMessage(message: String): Result<Unit> = sending.withLock {
        // A caller that gives up while the channel opens leaves; once the first package goes, the message is finished.
        awaitOpenChannel()
        withContext(NonCancellable) { sendChunked(message) }
    }

    private suspend fun awaitOpenChannel() {
        var waitedMs = 0L
        // A disposed channel throws on state(): it is not opening, and the send then fails as before.
        while (runCatching { webRtcDataChannel.state() }.getOrNull() == DataChannel.State.CONNECTING &&
            waitedMs < OPEN_TIMEOUT_MS
        ) {
            delay(OPEN_POLL_MS)
            waitedMs += OPEN_POLL_MS
        }
    }

    private suspend fun sendChunked(message: String): Result<Unit> {
        var packagesSent = 0
        return try {
            val listOfPackages = splitMessage(messageInByteArray = message.toByteArray())
            listOfPackages.forEachIndexed { index, packageDto ->
                if (index == 0) {
                    val metadata = packageDto as PackageDto.MetaData
                    Timber.d(
                        "📯 send message 📦 with messageId = ${metadata.messageId} (${listOfPackages.size - 1} chunks) " +
                            "to ${connectionIdHolder.logId}"
                    )
                    if (!send(packageDto = metadata)) {
                        return Result.failure(MessageNotSentException(rejectedPackageReason()))
                    }
                    packagesSent++
                } else {
                    val chunk = packageDto as PackageDto.Chunk
                    if (!send(packageDto = chunk)) {
                        // Part of the message is out: it must not be sent again over another transport.
                        return Result.failure(IllegalStateException("${rejectedPackageReason()} after $index packages"))
                    }
                    packagesSent++
                }
            }
            Result.success(Unit)
        } catch (iobe: IndexOutOfBoundsException) {
            Timber.e("📯 failed to wrap byte array to byte buffer: ${iobe.describe()}❗")
            Result.failure(sendFailure(packagesSent, "failed to wrap byte array to byte buffer: ${iobe.localizedMessage}", iobe))
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Timber.e("📯 failed to convert and send the message: ${exception.describe()}❗")
            Result.failure(sendFailure(packagesSent, "failed to send message with exception: ${exception.localizedMessage}", exception))
        }
    }

    // Before the first package was taken (e.g. the channel is disposed), nothing of the message went out.
    private fun sendFailure(packagesSent: Int, reason: String, cause: Throwable): Exception = if (packagesSent == 0) {
        MessageNotSentException(reason, cause)
    } else {
        IllegalStateException("$reason after $packagesSent packages", cause)
    }

    private fun rejectedPackageReason(): String {
        val channelState = runCatching { "state ${webRtcDataChannel.state()}, ${webRtcDataChannel.bufferedAmount()} bytes buffered" }
            .getOrElse { "disposed" }
        return "the data channel did not accept the message ($channelState)"
    }

    private val currentListOfChunks = mutableListOf<PackageDto.Chunk>()
    private var currentMessageId = ""
    private var currentHashOfMessage = ""
    private var currentSizeOfMessageInChunks = -1

    val dataChannelEvents: Flow<DataChannelWrapperEvent>
        get() = webRtcDataChannel
            .eventFlow()
            // Every package is kept, in order, while a message is processed: the receipt confirmation can wait for the
            // wallet's own message to be sent, and a full buffer would drop packages (the observer uses trySend).
            .buffer(Channel.UNLIMITED)
            .transform { dataChannelMessage ->
                when (dataChannelMessage) {
                    is DataChannelMessage.Message.MetaData -> {
                        // This is the most important part of this flow:
                        // if the currentMessageId and the currentHashOfMessage are not empty
                        // then the assembling and verification of the previous message is STILL IN PROGRESS
                        if (currentMessageId.isEmpty() && currentHashOfMessage.isEmpty()) {
                            keepMetaDataInMemory(dataChannelMessage.metadataDto)
                        }
                    }
                    is DataChannelMessage.Message.Chunk -> {
                        val chunk = dataChannelMessage.chunkDto
                        if (currentMessageId != chunk.messageId) {
                            emit(DataChannelEvent.Error)
                        }

                        currentListOfChunks.add(index = chunk.chunkIndex, chunk)

                        if (currentListOfChunks.count() == currentSizeOfMessageInChunks) {
                            emit(
                                DataChannelEvent.CompleteMessage(
                                    messageId = currentMessageId,
                                    listOfChunks = ArrayList(currentListOfChunks)
                                )
                            )
                        }
                    }
                    is DataChannelMessage.RemoteConnectorReceivedMessage.Confirmation -> {
                        Timber.d("📯 ${connectionIdHolder.logId} received the message❕ ⬅️️")
                        emit(DataChannelEvent.ReceiveMessage.Confirmation(messageId = dataChannelMessage.messageId))
                    }
                    is DataChannelMessage.RemoteConnectorReceivedMessage.Error -> {
                        Timber.d("📯 ${connectionIdHolder.logId} failed to receive the message❗️ ⬅️️")
                        emit(DataChannelEvent.ReceiveMessage.Error(messageId = dataChannelMessage.messageId))
                    }
                    is DataChannelMessage.StateChanged -> {
                        emit(dataChannelMessage.state.toDomainModel())
                    }
                    is DataChannelMessage.UnknownError -> {
                        emit(DataChannelEvent.Error)
                    }
                }
            }
            .transform { dataChannelEvent ->
                when (dataChannelEvent) {
                    is DataChannelEvent.CompleteMessage -> {
                        val completeChunkList = dataChannelEvent.listOfChunks
                        assembleAndVerifyMessageFromPackageList(
                            packageList = ArrayList(completeChunkList)
                        )
                            .onSuccess { incomingMessageByteArray ->
                                emit(
                                    DataChannelWrapperEvent.MessageFromRemoteConnectionId(
                                        connectionIdHolder = connectionIdHolder,
                                        messageInJsonString = incomingMessageByteArray.decodeToString()
                                    )
                                )
                                Timber.d(
                                    "📯 forward the complete message with messageId = ${dataChannelEvent.messageId} " +
                                        "from ${connectionIdHolder.logId} to the wallet ✅"
                                )
                            }
                            .onFailure {
                                emit(DataChannelWrapperEvent.Error(connectionIdHolder))
                            }
                    }
                    is DataChannelEvent.StateChanged -> {
                        Timber.d("📯 state for ${connectionIdHolder.logId} changed: ${dataChannelEvent.state} 📶️")
                        emit(
                            DataChannelWrapperEvent.StateChangedForRemoteConnector(
                                connectionIdHolder = connectionIdHolder,
                                state = dataChannelEvent.state
                            )
                        )
                    }
                    is DataChannelEvent.Error -> {
                        // TODO handle this on the wallet by showing a snackbar message error?
                        emit(DataChannelWrapperEvent.Error(connectionIdHolder = connectionIdHolder))
                    }
                    else -> {
                        // it's the ReceiveMessage - do nothing at the moment
                    }
                }
            }
            .flowOn(Dispatchers.IO)

    private fun keepMetaDataInMemory(metaData: PackageDto.MetaData) {
        currentSizeOfMessageInChunks = metaData.chunkCount
        currentMessageId = metaData.messageId
        currentHashOfMessage = metaData.hashOfMessage
        currentListOfChunks.clear()
        Timber.d(
            "📯 🔗 build the list of packages with chunkCount $currentSizeOfMessageInChunks " +
                "of messageId = $currentMessageId and hash = $currentHashOfMessage ⬅️"
        )
    }

    private suspend fun assembleAndVerifyMessageFromPackageList(packageList: List<PackageDto.Chunk>): Result<ByteArray> {
        Timber.d("📯 assemble and verify complete message from packageList with size: ${packageList.size}")
        val assembledMessageInByteArray = assembleChunks(currentMessageId, packageList)

        val result = verifyAssembledMessage(
            assembledMessage = assembledMessageInByteArray,
            expectedHashOfMessage = currentHashOfMessage
        )
        return result.getOrNull()?.let {
            sendReceiveMessageConfirmation(messageId = currentMessageId) // inform remote connector
            clearMetaDataFromMemory()
            Result.success(assembledMessageInByteArray)
        } ?: run {
            sendReceiveMessageError(messageId = currentMessageId) // inform remote connector
            clearMetaDataFromMemory()
            Result.failure(Throwable("failed to assemble and verify incoming message"))
        }
    }

    private fun clearMetaDataFromMemory() {
        currentSizeOfMessageInChunks = -1
        currentHashOfMessage = ""
        currentMessageId = ""
        Timber.d("📯 metadata cleared from memory and waiting for next message")
    }

    // once the incoming message is assembled and verified send a confirmation to the extension
    private suspend fun sendReceiveMessageConfirmation(messageId: String): Boolean {
        val confirmationDto = PackageDto.ReceiveMessageConfirmation(
            messageId = messageId
        )
        Timber.d("📯 send ReceiveMessageConfirmation ❕ for messageId = $messageId to ${connectionIdHolder.logId} ➡️")
        return sending.withLock { send(packageDto = confirmationDto) }
    }

    // once the incoming message is assembled but not verified send an error to the extension
    private suspend fun sendReceiveMessageError(messageId: String): Boolean {
        val errorDto = PackageDto.ReceiveMessageError(
            messageId = messageId
        )
        Timber.d("📯 send sendReceiveMessageError ❗️ or messageId = $messageId to ${connectionIdHolder.logId} ➡️")
        return sending.withLock { send(errorDto) }
    }

    private suspend fun send(packageDto: PackageDto): Boolean {
        return withContext(Dispatchers.IO) { // TODO not sure dispatcher is needed
            val packageInJson = Json.encodeToString(packageDto)
            val messageByteBuffer = ByteBuffer.wrap(packageInJson.encodeToByteArray())
            val messageWebRtcBuffer = DataChannel.Buffer(messageByteBuffer, false)
            webRtcDataChannel.send(messageWebRtcBuffer)
        }
    }

    fun close() {
        Timber.d(
            "📯 DataChannelWrapper of ${this.webRtcDataChannel} close for remote connector: " +
                "${connectionIdHolder.logId} 🔻 and with remote client id ${this.webRtcDataChannel.label()}"
        )
        // The observer is left to the channel's event flow, which unregisters it when it ends (the channel closing
        // ends it): unregistering it here first kept that flow, and the reader collecting it, alive for good.
        webRtcDataChannel.close()
        Timber.d(
            "📯 ${this.webRtcDataChannel} state: ${this.webRtcDataChannel.state()}, for remote connector: " +
                "${connectionIdHolder.logId} and with remote client id ${this.webRtcDataChannel.label()}"
        )
    }

    private companion object {
        const val OPEN_POLL_MS = 20L
        const val OPEN_TIMEOUT_MS = 5_000L
    }
}
