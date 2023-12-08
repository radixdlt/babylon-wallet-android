package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.webrtc.DataChannel
import rdx.works.peerdroid.data.PackageDto
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
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
            "📯 init DataChannelWrapper for remote connector: ${connectionIdHolder.id} " +
                "with label the remote client id ${this.webRtcDataChannel.label()}",
        )
    }

    suspend fun sendMessage(message: String): Result<Unit> {
        return try {
            val listOfPackages = splitMessage(messageInByteArray = message.toByteArray())
            listOfPackages.forEachIndexed { index, packageDto ->
                if (index == 0) {
                    val metadata = packageDto as PackageDto.MetaData
                    Timber.d("📯 send message 📦 with messageId = ${metadata.messageId} to remote connector: ${connectionIdHolder.id}")
                    send(packageDto = metadata)
                } else {
                    val chunk = packageDto as PackageDto.Chunk
                    send(packageDto = chunk)
                }
            }
            Result.success(Unit)
        } catch (iobe: IndexOutOfBoundsException) {
            Timber.e("📯 failed to wrap byte array to byte buffer: ${iobe.localizedMessage}❗")
            Result.failure(Throwable("failed to wrap byte array to byte buffer: ${iobe.localizedMessage}"))
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Timber.e("📯 failed to convert and send the message: ${exception.localizedMessage}❗")
            Result.failure(Throwable("failed to send message with exception: ${exception.localizedMessage}"))
        }
    }

    private val currentListOfChunks = mutableListOf<PackageDto.Chunk>()
    private var currentMessageId = ""
    private var currentHashOfMessage = ""
    private var currentSizeOfMessageInChunks = -1

    val dataChannelEvents: Flow<DataChannelWrapperEvent>
        get() = webRtcDataChannel
            .eventFlow()
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
                        Timber.d("📯 remote connector ${connectionIdHolder.id} received the message❕ ⬅️️")
                        emit(DataChannelEvent.ReceiveMessage.Confirmation(messageId = dataChannelMessage.messageId))
                    }
                    is DataChannelMessage.RemoteConnectorReceivedMessage.Error -> {
                        Timber.d("📯 remote connector ${connectionIdHolder.id} failed to receive the message❗️ ⬅️️")
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
            .conflate()
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
                                        "from remote connector ${connectionIdHolder.id} to the wallet ✅"
                                )
                            }
                            .onFailure {
                                emit(DataChannelWrapperEvent.Error(connectionIdHolder))
                            }
                    }
                    is DataChannelEvent.StateChanged -> {
                        Timber.d("📯 state for remote connector: ${connectionIdHolder.id} changed: ${dataChannelEvent.state} 📶️")
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
        Timber.d("📯 send ReceiveMessageConfirmation ❕ for messageId = $messageId to remote connector ${connectionIdHolder.id} ➡️")
        return send(packageDto = confirmationDto)
    }

    // once the incoming message is assembled but not verified send an error to the extension
    private suspend fun sendReceiveMessageError(messageId: String): Boolean {
        val errorDto = PackageDto.ReceiveMessageError(
            messageId = messageId
        )
        Timber.d("📯 send sendReceiveMessageError ❗️ or messageId = $messageId to remote connector ${connectionIdHolder.id} ➡️")
        return send(errorDto)
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
                "${connectionIdHolder.id} 🔻 and with remote client id ${this.webRtcDataChannel.label()}"
        )
        webRtcDataChannel.unregisterObserver()
        webRtcDataChannel.close()
        Timber.d(
            "📯 ${this.webRtcDataChannel} state: ${this.webRtcDataChannel.state()}, for remote connector: " +
                "${connectionIdHolder.id} and with remote client id ${this.webRtcDataChannel.label()}"
        )
    }
}
