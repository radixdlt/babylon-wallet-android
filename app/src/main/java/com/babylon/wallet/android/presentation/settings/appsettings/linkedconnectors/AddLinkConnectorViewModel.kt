package com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors

import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.profile.domain.p2plink.AddP2PLinkUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddLinkConnectorViewModel @Inject constructor(
    private val peerdroidLink: PeerdroidLink,
    private val addP2PLinkUseCase: AddP2PLinkUseCase
) : StateViewModel<AddLinkConnectorUiState>() {

    private var currentConnectionPassword: String = ""

    override fun initialState() = AddLinkConnectorUiState.init

    fun onQrCodeScanned(connectionPassword: String) {
        currentConnectionPassword = connectionPassword
        _state.update {
            it.copy(showContent = AddLinkConnectorUiState.ShowContent.NameLinkConnector)
        }
    }

    fun onConnectorDisplayNameChanged(name: String) {
        _state.update {
            it.copy(
                connectorDisplayName = name,
                isContinueButtonEnabled = name.trim().isNotEmpty()
            )
        }
    }

    suspend fun onContinueClick() {
        _state.update {
            it.copy(isLoading = true)
        }
        val encryptionKey = parseEncryptionKeyFromConnectionPassword(connectionPassword = currentConnectionPassword)
        if (encryptionKey != null) {
            when (peerdroidLink.addConnection(encryptionKey)) {
                is rdx.works.peerdroid.helpers.Result.Success -> {
                    addP2PLinkUseCase(
                        displayName = state.value.connectorDisplayName,
                        connectionPassword = currentConnectionPassword
                    )
                }

                is rdx.works.peerdroid.helpers.Result.Error -> {
                    Timber.d("Failed to connect to remote peer.")
                }
            }
        }
        _state.value = AddLinkConnectorUiState.init
    }

    fun onCloseClick() {
        currentConnectionPassword = ""
        _state.value = AddLinkConnectorUiState.init
    }
}

data class AddLinkConnectorUiState(
    val isLoading: Boolean,
    val showContent: ShowContent,
    val isContinueButtonEnabled: Boolean,
    val connectorDisplayName: String
) : UiState {

    enum class ShowContent {
        ScanQrCode, NameLinkConnector
    }

    companion object {
        val init = AddLinkConnectorUiState(
            isLoading = false,
            showContent = ShowContent.ScanQrCode,
            isContinueButtonEnabled = false,
            connectorDisplayName = ""
        )
    }
}
