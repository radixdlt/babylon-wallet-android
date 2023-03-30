package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.webrtc.DataChannel
import rdx.works.peerdroid.data.PackageMessageDto
import rdx.works.peerdroid.domain.BasePackage
import rdx.works.peerdroid.domain.BasePackage.ChunkPackage.Companion.toPackageMessageDto
import rdx.works.peerdroid.domain.BasePackage.MetadataPackage.Companion.toPackageMessageDto
import rdx.works.peerdroid.helpers.Result
import rdx.works.peerdroid.messagechunking.assembleChunks
import rdx.works.peerdroid.messagechunking.splitMessage
import rdx.works.peerdroid.messagechunking.verifyAssembledMessage
import timber.log.Timber
import java.nio.ByteBuffer

@Suppress("InjectDispatcher")
data class DataChannelWrapper(
    private val remoteClientId: String,
    private val webRtcDataChannel: DataChannel
) {

    val state: DataChannelEvent.StateChanged
        get() = when (webRtcDataChannel.state()) {
            DataChannel.State.CONNECTING -> DataChannelEvent.StateChanged.CONNECTING
            DataChannel.State.OPEN -> DataChannelEvent.StateChanged.OPEN
            DataChannel.State.CLOSING -> DataChannelEvent.StateChanged.CLOSING
            DataChannel.State.CLOSED -> DataChannelEvent.StateChanged.CLOSE
            else -> DataChannelEvent.StateChanged.UNKNOWN
        }

    suspend fun sendMessage(message: String): Result<Unit> {
        return try {
            Timber.d("📯 send message 📦 to remote client: $remoteClientId")
            val listOfPackages = splitMessage(messageInByteArray = message.toByteArray())
            listOfPackages.forEachIndexed { index, basePackage ->
                if (index == 0) {
                    val metadata = basePackage as BasePackage.MetadataPackage
                    val metadataDto = metadata.toPackageMessageDto()
                    send(packageMessageDto = metadataDto)
                } else {
                    val chunk = basePackage as BasePackage.ChunkPackage
                    val chunkDto = chunk.toPackageMessageDto()
                    send(packageMessageDto = chunkDto)
                }
            }
            Result.Success(Unit)
        } catch (iobe: IndexOutOfBoundsException) {
            Timber.e("📯 failed to wrap byte array to byte buffer: ${iobe.localizedMessage}")
            Result.Error("failed to wrap byte array to byte buffer: ${iobe.localizedMessage}")
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Timber.e("📯 failed to convert and send the message: ${exception.localizedMessage}")
            Result.Error("failed to send message with exception: ${exception.localizedMessage}")
        }
    }

    val dataChannelEvents: Flow<DataChannelEvent>
        get() = webRtcDataChannel
            .eventFlow()
            .map { dataChannelEvent ->
                // if the data channel event is type of IncomingMessage.Package then
                // 1. assemble it, 2. verify it, 3. convert it to type of IncomingMessage.DecodedMessage
                // so the PeerdroidConnector can deliver a a json string of the message to the wallet
                if (dataChannelEvent is DataChannelEvent.IncomingMessage.Package) {
                    val result = assembleAndVerifyMessageFromPackageList(
                        packageList = dataChannelEvent.messageInListOfPackages
                    )
                    when (result) {
                        is Result.Success -> {
                            val incomingMessageByteArray = result.data
                            DataChannelEvent.IncomingMessage.DecodedMessage(
                                remoteClientId = remoteClientId,
                                messageInJsonString = incomingMessageByteArray.decodeToString()
                            )
                        }
                        is Result.Error -> {
                            Timber.e("📯 failed to assemble and verify incoming message from remote client $remoteClientId")
                            DataChannelEvent.IncomingMessage.MessageHashMismatch
                        }
                    }
                } else {
                    dataChannelEvent
                }
            }
            .flowOn(Dispatchers.IO)

    private suspend fun assembleAndVerifyMessageFromPackageList(
        packageList: List<BasePackage>
    ): Result<ByteArray> {
        val assembledMessageInByteArray = packageList.assembleChunks()
        val result = verifyAssembledMessage(
            assembledMessage = assembledMessageInByteArray,
            chunks = packageList
        )

        return when (result) {
            is Result.Error -> {
                result.data?.let { messageId ->
                    sendReceiveMessageError(messageId = messageId) // inform extension
                }
                Result.Error("failed to assemble and verify incoming message")
            }
            is Result.Success -> {
                sendReceiveMessageConfirmation(messageId = result.data) // inform extension
                Result.Success(assembledMessageInByteArray)
            }
        }
    }

    // once the incoming message is assembled and verified send a confirmation to the extension
    private suspend fun sendReceiveMessageConfirmation(messageId: String) {
        val confirmationDto = PackageMessageDto(
            messageId = messageId,
            packageType = PackageMessageDto.PackageType.MESSAGE_CONFIRMATION.type
        )
        Timber.d("📯 send ReceiveMessageConfirmation ❕ to remote client $remoteClientId")
        send(packageMessageDto = confirmationDto)
    }

    // once the incoming message is assembled but not verified send an error to the extension
    private suspend fun sendReceiveMessageError(messageId: String) {
        val errorDto = PackageMessageDto(
            messageId = messageId,
            packageType = PackageMessageDto.PackageType.MESSAGE_ERROR.type,
            error = "messageHashesMismatch"
        )
        Timber.d("📯 send sendReceiveMessageError ❗️ to remote client $remoteClientId")
        send(errorDto)
    }

    private suspend fun send(packageMessageDto: PackageMessageDto) {
        withContext(Dispatchers.IO) {
            val metadataJson = Json.encodeToString(packageMessageDto)
            val messageByteBuffer = ByteBuffer.wrap(metadataJson.encodeToByteArray())
            val messageWebRtcBuffer = DataChannel.Buffer(messageByteBuffer, false)
            webRtcDataChannel.send(messageWebRtcBuffer)
        }
    }

    fun close() {
        Timber.d("📯 ${this.webRtcDataChannel} close for remote client: $remoteClientId")
        webRtcDataChannel.close()
        Timber.d("📯 ${this.webRtcDataChannel} state is ${this.state} for remote client: $remoteClientId")
    }
}
