package com.babylon.wallet.android.presentation.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.usecases.GetAccountsForSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.presentation.account.AccountEvent.NavigateToMnemonicBackup
import com.babylon.wallet.android.presentation.account.AccountEvent.NavigateToMnemonicRestore
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ADDRESS
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getAccountsForSecurityPromptUseCase: GetAccountsForSecurityPromptUseCase,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : StateViewModel<AccountUiState>(), OneOffEventHandler<AccountEvent> by OneOffEventHandlerImpl() {

    private val accountAddress: String = savedStateHandle.get<String>(ARG_ACCOUNT_ADDRESS).orEmpty()

    override fun initialState(): AccountUiState = AccountUiState(accountWithAssets = null)

    private val refreshFlow = MutableSharedFlow<Unit>()
    private val accountFlow = combine(
        getProfileUseCase.accountsOnCurrentNetwork.mapNotNull { accountsInProfile ->
            accountsInProfile.find { it.address == accountAddress }
        },
        refreshFlow
    ) { account, _ -> account }

    init {
        viewModelScope.launch {
            accountFlow
                .onEach { account ->
                    // Update details of profile account each time it is updated
                    _state.update { state ->
                        val accountWithAssets = state.accountWithAssets?.copy(account = account) ?: AccountWithAssets(account = account)
                        state.copy(accountWithAssets = accountWithAssets)
                    }
                }
                .flatMapLatest { account ->
                    getWalletAssetsUseCase(listOf(account), state.value.isRefreshing)
                        .catch { error ->
                            _state.update {
                                it.copy(isLoading = false, refreshing = false, uiMessage = UiMessage.ErrorMessage(error = error))
                            }
                        }
                        .mapNotNull { it.firstOrNull() }
                }
                .collectLatest { accountWithAssets ->
                    // Update assets of the account each time they are updated
                    _state.update { state ->
                        state.copy(
                            accountWithAssets = state.accountWithAssets?.copy(assets = accountWithAssets.assets),
                            isLoading = false,
                            refreshing = false
                        )
                    }
                }
        }

        viewModelScope.launch {
            appEventBus.events.filter { event ->
                event is AppEvent.RefreshResourcesNeeded || event is AppEvent.RestoredMnemonic
            }.collect {
                loadAccountDetails(withRefresh = it !is AppEvent.RestoredMnemonic)
            }
        }

        observeSecurityPrompt()
        loadAccountDetails(withRefresh = false)
    }

    private fun observeSecurityPrompt() {
        viewModelScope.launch {
            getAccountsForSecurityPromptUseCase().collect { accounts ->
                val securityPrompt = accounts.find { it.account.address == accountAddress }?.prompt

                _state.update { state ->
                    state.copy(securityPromptType = securityPrompt)
                }
            }
        }
    }

    fun refresh() {
        loadAccountDetails(withRefresh = true)
    }

    fun onFungibleResourceClicked(resource: Resource.FungibleResource) {
        _state.update { accountUiState ->
            accountUiState.copy(selectedResource = SelectedResource.SelectedFungibleResource(resource))
        }
    }

    fun onNonFungibleResourceClicked(
        nonFungibleResource: Resource.NonFungibleResource,
        item: Resource.NonFungibleResource.Item
    ) {
        _state.update { accountUiState ->
            accountUiState.copy(
                selectedResource = SelectedResource.SelectedNonFungibleResource(
                    nonFungible = nonFungibleResource,
                    item = item
                )
            )
        }
    }

    fun onLSUUnitClicked(resource: LiquidStakeUnit, validatorDetail: ValidatorDetail) {
        _state.update { accountUiState ->
            accountUiState.copy(selectedResource = SelectedResource.SelectedLSUUnit(resource, validatorDetail))
        }
    }

    fun onPoolUnitClicked(resource: PoolUnit) {
        _state.update { accountUiState ->
            accountUiState.copy(selectedResource = SelectedResource.SelectedPoolUnit(resource))
        }
    }

    fun onApplySecuritySettings(securityPromptType: SecurityPromptType) {
        viewModelScope.launch {
            val factorSourceId = _state.value.accountWithAssets?.account
                ?.factorSourceId() as? FactorSourceID.FromHash ?: return@launch

            when (securityPromptType) {
                SecurityPromptType.NEEDS_BACKUP -> sendEvent(NavigateToMnemonicBackup(factorSourceId))
                SecurityPromptType.NEEDS_RESTORE -> sendEvent(NavigateToMnemonicRestore(factorSourceId))
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    private fun loadAccountDetails(withRefresh: Boolean) {
        _state.update { it.copy(refreshing = withRefresh) }
        viewModelScope.launch { refreshFlow.emit(Unit) }
    }
}

internal sealed interface AccountEvent : OneOffEvent {
    data class NavigateToMnemonicBackup(val factorSourceId: FactorSourceID.FromHash) : AccountEvent
    data class NavigateToMnemonicRestore(val factorSourceId: FactorSourceID.FromHash) : AccountEvent
}

data class AccountUiState(
    val accountWithAssets: AccountWithAssets? = null,
    private val securityPromptType: SecurityPromptType? = null,
    val isLoading: Boolean = true,
    private val refreshing: Boolean = false,
    val selectedResource: SelectedResource? = null,
    val uiMessage: UiMessage? = null,
) : UiState {

    val visiblePrompt: SecurityPromptType?
        get() = when (securityPromptType) {
            SecurityPromptType.NEEDS_RESTORE -> securityPromptType
            SecurityPromptType.NEEDS_BACKUP -> if (accountWithAssets?.assets?.hasXrd() == true && !isLoading) {
                SecurityPromptType.NEEDS_BACKUP
            } else {
                null
            }

            else -> null
        }

    val isRefreshing: Boolean
        get() = !isLoading && refreshing

    val isTransferEnabled: Boolean
        get() = !isLoading

    val isHistoryEnabled: Boolean = false
}

sealed interface SelectedResource {
    data class SelectedFungibleResource(val fungible: Resource.FungibleResource) : SelectedResource
    data class SelectedNonFungibleResource(
        val nonFungible: Resource.NonFungibleResource,
        val item: Resource.NonFungibleResource.Item
    ) : SelectedResource

    data class SelectedLSUUnit(val lsuUnit: LiquidStakeUnit, val validatorDetail: ValidatorDetail) : SelectedResource
    data class SelectedPoolUnit(val poolUnit: PoolUnit) : SelectedResource
}
