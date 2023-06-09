package com.babylon.wallet.android.presentation.account

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.usecases.GetAccountsForSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.utils.unsecuredFactorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getAccountsForSecurityPromptUseCase: GetAccountsForSecurityPromptUseCase,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : StateViewModel<AccountUiState>(), OneOffEventHandler<AccountEvent> by OneOffEventHandlerImpl() {

    private val accountId: String = savedStateHandle.get<String>(ARG_ACCOUNT_ID).orEmpty()

    override fun initialState(): AccountUiState = AccountUiState(accountWithResources = null)

    init {
        viewModelScope.launch {
            getProfileUseCase.accountOnCurrentNetwork(accountId)?.let { account ->
                _state.update { state ->
                    state.copy(accountWithResources = AccountWithResources(account = account, resources = null))
                }
                loadAccountData(isRefreshing = false)
            }
        }

        viewModelScope.launch {
            appEventBus.events.filter { event ->
                event is AppEvent.GotFreeXrd || event is AppEvent.TransactionEvent.Successful
            }.collect {
                refresh()
            }
        }

        observeBackedUpMnemonics()
    }

    private fun observeBackedUpMnemonics() {
        viewModelScope.launch {
            getAccountsForSecurityPromptUseCase().collect { accounts ->
                _state.update { state ->
                    state.copy(usesNotBackedUpMnemonic = accounts.any { it.address == accountId })
                }
            }
        }
    }

    fun refresh() {
        _state.update { state ->
            state.copy(isRefreshing = true)
        }
        loadAccountData(isRefreshing = true)
    }

    private fun loadAccountData(isRefreshing: Boolean) {
        val account = _state.value.accountWithResources?.account ?: return
        viewModelScope.launch {
            val result = getAccountsWithResourcesUseCase(listOf(account), isRefreshing)
            result.onError { e ->
                _state.update { accountUiState ->
                    accountUiState.copy(uiMessage = UiMessage.ErrorMessage(error = e), isLoading = false)
                }
            }
            result.onValue { accountsWithResources ->
                _state.update { accountUiState ->
                    accountUiState.copy(
                        accountWithResources = accountsWithResources.first(),
                        isRefreshing = false,
                        isLoading = false
                    )
                }
            }
        }
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
            accountUiState.copy(selectedResource = SelectedResource.SelectedNonFungibleResource(nonFungibleResource, item))
        }
    }

    fun onApplySecuritySettings() {
        viewModelScope.launch {
            _state.value.accountWithResources?.account?.unsecuredFactorSourceId()?.let {
                sendEvent(AccountEvent.NavigateToMnemonicBackup(it))
            }
        }
    }
}

internal sealed interface AccountEvent : OneOffEvent {
    data class NavigateToMnemonicBackup(val factorSourceId: FactorSource.ID) : AccountEvent
}

data class AccountUiState(
    val accountWithResources: AccountWithResources? = null,
    private val usesNotBackedUpMnemonic: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedResource: SelectedResource? = null,
    val uiMessage: UiMessage? = null
) : UiState {

    val isSecurityPromptVisible: Boolean
        get() {
            val resources = accountWithResources?.resources ?: return false

            return usesNotBackedUpMnemonic && resources.hasXrd()
        }
}

enum class AssetTypeTab(@StringRes val stringId: Int) {
    TOKEN_TAB(R.string.account_tokens),
    NTF_TAB(R.string.account_nfts),
}

sealed interface SelectedResource {
    data class SelectedFungibleResource(val fungible: Resource.FungibleResource) : SelectedResource
    data class SelectedNonFungibleResource(
        val nonFungible: Resource.NonFungibleResource,
        val item: Resource.NonFungibleResource.Item
    ) : SelectedResource
}
