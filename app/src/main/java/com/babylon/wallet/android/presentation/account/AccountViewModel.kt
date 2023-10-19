package com.babylon.wallet.android.presentation.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.usecases.GetAccountsForSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.GetAccountsWithAssetsUseCase
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.utils.factorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetworkWithAddress
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getAccountsWithAssetsUseCase: GetAccountsWithAssetsUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getAccountsForSecurityPromptUseCase: GetAccountsForSecurityPromptUseCase,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : StateViewModel<AccountUiState>(), OneOffEventHandler<AccountEvent> by OneOffEventHandlerImpl() {

    private val accountAddress: String = savedStateHandle.get<String>(ARG_ACCOUNT_ADDRESS).orEmpty()

    override fun initialState(): AccountUiState = AccountUiState(accountWithAssets = null)

    init {
        viewModelScope.launch {
            getProfileUseCase.accountOnCurrentNetworkWithAddress(accountAddress).collectLatest { account ->
                account?.let {
                    _state.update { state ->
                        state.copy(accountWithAssets = AccountWithAssets(account = account, assets = null))
                    }
                    loadAccountData(isRefreshing = false)
                }
            }
        }

        viewModelScope.launch {
            appEventBus.events.filter { event ->
                event is AppEvent.RefreshResourcesNeeded || event is AppEvent.RestoredMnemonic
            }.collect {
                refresh(fetchNewData = it !is AppEvent.RestoredMnemonic)
            }
        }

        observeSecurityPrompt()
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

    fun refresh(fetchNewData: Boolean = true) {
        _state.update { state ->
            state.copy(refreshing = fetchNewData)
        }
        loadAccountData(isRefreshing = fetchNewData)
    }

    private fun loadAccountData(isRefreshing: Boolean) {
        val account = _state.value.accountWithAssets?.account ?: return
        viewModelScope.launch {
            val result = getAccountsWithAssetsUseCase(
                accounts = listOf(account),
                isRefreshing = isRefreshing
            )
            result.onError { e ->
                Timber.w(e)
                _state.update { accountUiState ->
                    accountUiState.copy(
                        uiMessage = UiMessage.ErrorMessage.from(error = e),
                        isLoading = false,
                        refreshing = false
                    )
                }
            }
            result.onValue { accountsWithResources ->
                _state.update { accountUiState ->
                    accountUiState.copy(
                        accountWithAssets = accountsWithResources.first(),
                        isLoading = false,
                        refreshing = false
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
