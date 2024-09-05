package com.babylon.wallet.android.presentation.settings.approveddapps

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.utils.AccountLockersObserver
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.profile.data.repository.DAppConnectionRepository
import javax.inject.Inject

@Suppress("MagicNumber")
@HiltViewModel
class ApprovedDappsViewModel @Inject constructor(
    private val getDAppsUseCase: GetDAppsUseCase,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val accountLockersObserver: AccountLockersObserver
) : StateViewModel<AuthorizedDappsUiState>() {

    private var accountLockerDepositsJob: Job? = null

    override fun initialState(): AuthorizedDappsUiState = AuthorizedDappsUiState()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            dAppConnectionRepository.getAuthorizedDApps().collect { approvedDapps ->
                val addresses = approvedDapps.map { it.dappDefinitionAddress }.toSet()
                getDAppsUseCase(
                    definitionAddresses = addresses,
                    needMostRecentData = false
                ).onSuccess { dApps ->
                    val result = approvedDapps.mapNotNull { authorisedDApp ->
                        dApps.find { it.dAppAddress == authorisedDApp.dappDefinitionAddress }
                            ?.let { dApp ->
                                AuthorizedDappsUiState.DAppUiItem(
                                    dApp = dApp,
                                    hasDeposits = false
                                )
                            }
                    }

                    _state.update { it.copy(dApps = result.toImmutableList(), isLoading = false) }
                    observeAccountLockerDeposits(dApps)
                }.onFailure { error ->
                    _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error), isLoading = false) }
                }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    private fun observeAccountLockerDeposits(dApps: List<DApp>) {
        accountLockerDepositsJob?.cancel()
        accountLockerDepositsJob = viewModelScope.launch {
            accountLockersObserver.depositsByAccount()
                .map { it.values.flatten() }
                .collect { deposits ->
                    _state.update {
                        it.copy(
                            dApps = dApps.map { dApp ->
                                AuthorizedDappsUiState.DAppUiItem(
                                    dApp = dApp,
                                    hasDeposits = deposits.any { deposit -> deposit.lockerAddress == dApp.lockerAddress }
                                )
                            }.toImmutableList()
                        )
                    }
                }
        }
    }
}

data class AuthorizedDappsUiState(
    val dApps: ImmutableList<DAppUiItem> = persistentListOf(),
    val isLoading: Boolean = false,
    val uiMessage: UiMessage? = null
) : UiState {

    data class DAppUiItem(
        val dApp: DApp,
        val hasDeposits: Boolean
    )
}
