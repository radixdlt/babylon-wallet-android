package com.babylon.wallet.android.presentation.settings.connecteddapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.repository.DAppConnectionRepository
import javax.inject.Inject

@HiltViewModel
class ConnectedDappsViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository
) : ViewModel() {

    private val _state: MutableStateFlow<ConnectedDappsUiState> =
        MutableStateFlow(ConnectedDappsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            dAppConnectionRepository.getConnectedDapps().collect { dapps ->
                _state.update {
                    it.copy(dapps = dapps.toPersistentList())
                }
            }
        }
    }
}

data class ConnectedDappsUiState(
    val dapps: ImmutableList<OnNetwork.ConnectedDapp> = persistentListOf()
)
