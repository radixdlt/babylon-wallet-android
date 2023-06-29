package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import io.ktor.util.moveToByteArray
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import org.webrtc.DataChannel
import rdx.works.peerdroid.data.PackageDto
import timber.log.Timber

// This is a callbackFlow wrapper of the native DataChannel.Observer callback and it does two things:
// 1. returns the state of the data channel - when this is changed
// 2. returns the type of PackageDto - when a message is received
internal fun DataChannel.eventFlow(): Flow<DataChannelMessage> = callbackFlow {
    val callback = object : DataChannel.Observer {

        override fun onBufferedAmountChange(p0: Long) {
            Timber.d("📯 onBufferedAmountChange")
        }

        override fun onStateChange() {
            val state = this@eventFlow.state()
            trySend(DataChannelMessage.StateChanged(state = state))
        }

        override fun onMessage(p0: DataChannel.Buffer?) {
            if (p0?.data == null) {
                trySend(DataChannelMessage.UnknownError(message = "received null data channel buffer"))
            } else {
                try {
                    val jsonString = p0.data.moveToByteArray().decodeToString()
                    val message = decodeAndParsePackageFromJson(jsonString)
                    trySend(message)
                } catch (exception: Exception) {
                    Timber.e("📯 an error occurred while decoding the package 📦: ${exception.localizedMessage} ⬅")
                    trySend(DataChannelMessage.UnknownError(message = "error occurred while decoding the package"))
                }
            }
        }

        private fun decodeAndParsePackageFromJson(packageJsonString: String): DataChannelMessage {
            return when (val packageMessageDto = Json.decodeFromString<PackageDto>(packageJsonString)) {
                is PackageDto.MetaData -> {
                    DataChannelMessage.Message.MetaData(packageMessageDto)
                }
                is PackageDto.Chunk -> {
                    DataChannelMessage.Message.Chunk(packageMessageDto)
                }
                is PackageDto.ReceiveMessageConfirmation -> {
                    DataChannelMessage.RemoteClientReceivedMessage.Confirmation(messageId = packageMessageDto.messageId)
                }
                is PackageDto.ReceiveMessageError -> {
                    DataChannelMessage.RemoteClientReceivedMessage.Error(messageId = packageMessageDto.messageId)
                }
            }
        }
    }

    registerObserver(callback)

    Timber.d("📯 event flow for remote client ${this@eventFlow.label()} is registered")

    awaitClose {
        Timber.d("📯 event flow for remote client ${this@eventFlow.label()} awaitClose ⭕️")
        unregisterObserver()
        Timber.d("📯 for remote client ${this@eventFlow.label()}: unregister observer and check the state ${this@eventFlow.state()}")
    }
}
