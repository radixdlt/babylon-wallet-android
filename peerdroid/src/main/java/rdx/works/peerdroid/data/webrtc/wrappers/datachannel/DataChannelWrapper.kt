package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import android.util.Log
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
import java.nio.ByteBuffer

@JvmInline
value class DataChannelWrapper(
    private val webRtcDataChannel: DataChannel
) {

    val state: DataChannelEvent.StateChanged.State
        get() = when (webRtcDataChannel.state()) {
            DataChannel.State.CONNECTING -> DataChannelEvent.StateChanged.State.CONNECTING
            DataChannel.State.OPEN -> DataChannelEvent.StateChanged.State.OPEN
            DataChannel.State.CLOSING -> DataChannelEvent.StateChanged.State.CLOSING
            DataChannel.State.CLOSED -> DataChannelEvent.StateChanged.State.CLOSE
            else -> DataChannelEvent.StateChanged.State.UNKNOWN
        }

    suspend fun sendMessage(message: String): Result<Unit> {
        return try {
            val listOfPackages = splitMessage(messageInByteArray = message.toByteArray())
            listOfPackages.forEachIndexed { index, basePackage ->
                if (index == 0) {
                    val metadata = basePackage as BasePackage.MetadataPackage
                    val metadataDto = metadata.toPackageMessageDto()
                    send(metadataDto)
                } else {
                    val chunk = basePackage as BasePackage.ChunkPackage
                    val chunkDto = chunk.toPackageMessageDto()
                    send(chunkDto)
                }
            }
            Result.Success(Unit)
        } catch (iobe: IndexOutOfBoundsException) {
            Log.e("DATA_CHANNEL", "failed to wrap byte array to byte buffer: ${iobe.localizedMessage}")
            Result.Error("failed to wrap byte array to byte buffer: ${iobe.localizedMessage}")
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Log.e("DATA_CHANNEL", "failed to convert and send the message: ${exception.localizedMessage}")
            Result.Error("failed to send message with exception: ${exception.localizedMessage}")
        }
    }

    val dataChannelEvents: Flow<DataChannelEvent>
        get() = webRtcDataChannel
            .eventFlow()
            .map { dataChannelEvent ->
                if (dataChannelEvent is DataChannelEvent.IncomingMessage.Package) {
                    val result = assembleAndVerifyMessageFromPackageList(
                        packageList = dataChannelEvent.messageInListOfPackages
                    )
                    when (result) {
                        is Result.Success -> {
                            val incomingMessageByteArray = result.data
                            DataChannelEvent.IncomingMessage.Message(
                                decodedMessage = incomingMessageByteArray.decodeToString()
                            )
                        }
                        is Result.Error -> {
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
                    sendReceiveMessageError(messageId) // inform extension
                }
                Result.Error("data channel failed to initialize")
            }
            is Result.Success -> {
                sendReceiveMessageConfirmation(result.data) // inform extension
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
        send(confirmationDto)
    }

    // once the incoming message is assembled but not verified send an error to the extension
    private suspend fun sendReceiveMessageError(messageId: String) {
        val errorDto = PackageMessageDto(
            messageId = messageId,
            packageType = PackageMessageDto.PackageType.MESSAGE_ERROR.type,
            error = "messageHashesMismatch"
        )
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
}
