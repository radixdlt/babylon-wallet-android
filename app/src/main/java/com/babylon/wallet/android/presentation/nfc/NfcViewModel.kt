package com.babylon.wallet.android.presentation.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.nfc.common.NfcSessionIOHandler
import com.babylon.wallet.android.presentation.nfc.common.NfcSessionProxy
import com.radixdlt.sargon.NfcTagDriverPurpose
import com.radixdlt.sargon.extensions.toBagOfBytes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.toByteArray
import timber.log.Timber
import javax.inject.Inject

private const val NFC_READER_TIMEOUT = 60000

@HiltViewModel
class NfcViewModel @Inject constructor(
    private val nfcSessionIOHandler: NfcSessionIOHandler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : StateViewModel<NfcViewModel.State>(),
    OneOffEventHandler<NfcViewModel.Event> by OneOffEventHandlerImpl() {

    private var isoDep: IsoDep? = null
    private var transceiveRequestsJob: Job? = null

    override fun initialState(): State = State()

    init {
        initPurpose()
        observeNfcSessionEvents()
        observeTransceiveRequests()
    }

    fun enableNfcReaderMode(perform: suspend () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            perform()
        }
    }

    fun disableNfcReaderMode(perform: suspend () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            perform()
        }
    }

    fun onNfcDisabled() {
        _state.update { state -> state.copy(showNfcDisabled = true) }
    }

    fun onDismissErrorMessage() {
        _state.update { state -> state.copy(errorMessage = null) }
        onDismiss()
    }

    fun onNfcTagDiscovered(tag: Tag?) {
        if (tag == null) {
            onNfcReaderError(IllegalStateException("NFC tag is null"))
            return
        }

        viewModelScope.launch(ioDispatcher) {
            val dep = IsoDep.get(tag)
            dep.timeout = NFC_READER_TIMEOUT

            try {
                dep.connect()
                isoDep = dep
                nfcSessionIOHandler.onSessionReady()
            } catch (ex: Throwable) {
                Timber.d(ex)
                isoDep = null
                onNfcReaderError(ex)
            }
        }
    }

    fun onDismiss() {
        endSession()
    }

    private fun initPurpose() {
        _state.update { state ->
            state.copy(
                purpose = nfcSessionIOHandler.purpose
            )
        }
    }

    private fun observeNfcSessionEvents() {
        nfcSessionIOHandler.events
            .onEach { event ->
                when (event) {
                    is NfcSessionProxy.Event.SetMessage -> _state.update { state ->
                        state.copy(message = event.message)
                    }

                    is NfcSessionProxy.Event.EndSession -> endSession()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeTransceiveRequests() {
        transceiveRequestsJob?.cancel()
        transceiveRequestsJob = nfcSessionIOHandler.transceiveRequests
            .onEach { command ->
                val isoDep = isoDep

                if (isoDep == null || !isoDep.isConnected) {
                    nfcSessionIOHandler.onTransceiveResult(
                        Result.failure(IllegalStateException("Tag not connected"))
                    )
                    return@onEach
                }

                runCatching {
                    isoDep.transceive(command.toByteArray())
                }.onSuccess { response ->
                    val isBadStatusResponse = response.size < 2 ||
                        response[response.size - 2] != 0x90.toByte() ||
                        response[response.size - 1] != 0x00.toByte()

                    if (isBadStatusResponse) {
                        nfcSessionIOHandler.onTransceiveResult(
                            Result.failure(IllegalArgumentException("Transceive response bad status"))
                        )
                    } else {
                        nfcSessionIOHandler.onTransceiveResult(Result.success(response.toBagOfBytes()))
                    }
                }.onFailure { throwable ->
                    nfcSessionIOHandler.onTransceiveResult(Result.failure(throwable))

                    if (throwable is TagLostException) {
                        endSession()
                    }
                }
            }.flowOn(ioDispatcher).launchIn(viewModelScope)
    }

    private fun onNfcReaderError(error: Throwable) {
        _state.update { state ->
            state.copy(
                errorMessage = UiMessage.ErrorMessage(error)
            )
        }
        endSession()
    }

    private fun endSession() {
        viewModelScope.launch(ioDispatcher) {
            transceiveRequestsJob?.cancel()
            nfcSessionIOHandler.onSessionClosed()
            sendEvent(Event.Completed)
            closeIsoDep()
        }
    }

    private fun closeIsoDep() {
        try {
            isoDep?.close()
        } catch (ex: Throwable) {
            Timber.d(ex)
        }
        isoDep = null
    }

    data class State(
        val purpose: NfcTagDriverPurpose? = null,
        val showNfcDisabled: Boolean = false,
        val message: String? = null,
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {

        data object Completed : Event
    }
}
