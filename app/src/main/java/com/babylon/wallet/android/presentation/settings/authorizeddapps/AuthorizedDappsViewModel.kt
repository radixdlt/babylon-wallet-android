package com.babylon.wallet.android.presentation.settings.authorizeddapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.DAppConnectionRepository
import javax.inject.Inject

@Suppress("MagicNumber")
@HiltViewModel
class AuthorizedDappsViewModel @Inject constructor(
    dAppConnectionRepository: DAppConnectionRepository
) : ViewModel() {

    val state =
        dAppConnectionRepository.getAuthorizedDapps().map {
            AuthorizedDappsUiState(it.toPersistentList())
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            AuthorizedDappsUiState()
        )
}

data class AuthorizedDappsUiState(
    val dapps: ImmutableList<Network.AuthorizedDapp> = persistentListOf()
)
