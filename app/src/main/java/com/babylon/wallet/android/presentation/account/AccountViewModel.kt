@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.domain.usecases.assets.GetNextNFTsPageUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.domain.usecases.assets.UpdateLSUsInfo
import com.babylon.wallet.android.domain.usecases.transaction.SendClaimRequestUseCase
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.apppreferences.Radix.dashboardUrl
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.activeAccountsOnCurrentNetwork
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getNetworkInfoUseCase: GetNetworkInfoUseCase,
    private val getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase,
    private val getNextNFTsPageUseCase: GetNextNFTsPageUseCase,
    private val updateLSUsInfo: UpdateLSUsInfo,
    private val appEventBus: AppEventBus,
    private val sendClaimRequestUseCase: SendClaimRequestUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<AccountUiState>(), OneOffEventHandler<AccountEvent> by OneOffEventHandlerImpl() {

    private val accountAddress: String = savedStateHandle.get<String>(ARG_ACCOUNT_ADDRESS).orEmpty()

    override fun initialState(): AccountUiState = AccountUiState(accountWithAssets = null)

    private val refreshFlow = MutableSharedFlow<Unit>()
    private val accountFlow = combine(
        getProfileUseCase.activeAccountsOnCurrentNetwork.mapNotNull { accountsInProfile ->
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
                                it.copy(isRefreshing = false, uiMessage = UiMessage.ErrorMessage(error = error))
                            }
                        }
                        .mapNotNull { it.firstOrNull() }
                }
                .collectLatest { accountWithAssets ->
                    // Update assets of the account each time they are updated
                    _state.update { state ->
                        state.copy(
                            accountWithAssets = state.accountWithAssets?.copy(assets = accountWithAssets.assets),
                            isRefreshing = false
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
            getEntitiesWithSecurityPromptUseCase().collect { accounts ->
                val securityPrompt = accounts.find { it.entity.address == accountAddress }?.prompt

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
        val account = _state.value.accountWithAssets?.account ?: return

        viewModelScope.launch {
            sendEvent(AccountEvent.OnFungibleClick(resource, account))
        }
    }

    fun onNonFungibleResourceClicked(
        nonFungibleResource: Resource.NonFungibleResource,
        item: Resource.NonFungibleResource.Item
    ) {
        val account = _state.value.accountWithAssets?.account ?: return

        viewModelScope.launch {
            sendEvent(
                AccountEvent.OnNonFungibleClick(
                    resource = nonFungibleResource,
                    item = item,
                    account = account
                )
            )
        }
    }

    fun onLSUUnitClicked(liquidStakeUnit: LiquidStakeUnit) {
        val account = _state.value.accountWithAssets?.account ?: return

        viewModelScope.launch {
            sendEvent(AccountEvent.OnLSUClick(liquidStakeUnit = liquidStakeUnit, account = account))
        }
    }

    fun onPoolUnitClicked(poolUnit: PoolUnit) {
        val account = _state.value.accountWithAssets?.account ?: return

        viewModelScope.launch {
            sendEvent(AccountEvent.OnPoolUnitClick(poolUnit, account))
        }
    }

    fun onApplySecuritySettings(securityPromptType: SecurityPromptType) {
        viewModelScope.launch {
            val factorSourceId = _state.value.accountWithAssets?.account
                ?.factorSourceId as? FactorSourceID.FromHash ?: return@launch

            when (securityPromptType) {
                SecurityPromptType.NEEDS_BACKUP -> sendEvent(NavigateToMnemonicBackup(factorSourceId))
                SecurityPromptType.NEEDS_RESTORE -> sendEvent(NavigateToMnemonicRestore(factorSourceId))
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onNextNftPageRequest(resource: Resource.NonFungibleResource) {
        val account = state.value.accountWithAssets?.account ?: return
        if (!state.value.isRefreshing && resource.resourceAddress !in state.value.nonFungiblesWithPendingNFTs) {
            _state.update { state -> state.onNFTsLoading(resource) }
            viewModelScope.launch {
                getNextNFTsPageUseCase(account, resource)
                    .onSuccess { resourceWithUpdatedNFTs ->
                        _state.update { state ->
                            state.onNFTsReceived(resourceWithUpdatedNFTs)
                        }
                    }.onFailure { error ->
                        _state.update { state ->
                            state.onNFTsError(resource, error)
                        }
                    }
            }
        }
    }

    fun onStakesRequest() {
        val account = state.value.accountWithAssets?.account ?: return
        val stakes = state.value.accountWithAssets?.assets?.ownedValidatorsWithStakes ?: return
        val unknownLSUs = stakes.any { !it.isDetailsAvailable }
        onLatestEpochRequest()
        if (!state.value.isRefreshing && !state.value.pendingStakeUnits && unknownLSUs) {
            _state.update { state -> state.copy(pendingStakeUnits = true) }
            viewModelScope.launch {
                updateLSUsInfo(account, stakes).onSuccess {
                    _state.update { state -> state.onValidatorsReceived(it) }
                }.onFailure { error ->
                    _state.update { state -> state.copy(pendingStakeUnits = false, uiMessage = UiMessage.ErrorMessage(error)) }
                }
            }
        }
    }

    fun onClaimClick(stakeClaims: List<StakeClaim>) {
        val account = state.value.accountWithAssets?.account ?: return
        val epoch = state.value.epoch ?: return
        viewModelScope.launch {
            sendClaimRequestUseCase(
                account = account,
                claims = stakeClaims,
                epoch = epoch
            )
        }
    }

    private fun onLatestEpochRequest() = viewModelScope.launch {
        getNetworkInfoUseCase().onSuccess { info ->
            _state.update { it.copy(epoch = info.epoch) }
        }
    }

    private fun loadAccountDetails(withRefresh: Boolean) {
        _state.update { it.copy(isRefreshing = withRefresh) }
        viewModelScope.launch { refreshFlow.emit(Unit) }
        onLatestEpochRequest()
    }
}

internal sealed interface AccountEvent : OneOffEvent {
    data class NavigateToMnemonicBackup(val factorSourceId: FactorSourceID.FromHash) : AccountEvent
    data class NavigateToMnemonicRestore(val factorSourceId: FactorSourceID.FromHash) : AccountEvent
    data class OnFungibleClick(val resource: Resource.FungibleResource, val account: Network.Account) : AccountEvent
    data class OnNonFungibleClick(
        val resource: Resource.NonFungibleResource,
        val item: Resource.NonFungibleResource.Item,
        val account: Network.Account
    ) : AccountEvent
    data class OnPoolUnitClick(val poolUnit: PoolUnit, val account: Network.Account) : AccountEvent
    data class OnLSUClick(val liquidStakeUnit: LiquidStakeUnit, val account: Network.Account) : AccountEvent
}

data class AccountUiState(
    val accountWithAssets: AccountWithAssets? = null,
    val nonFungiblesWithPendingNFTs: Set<String> = setOf(),
    val pendingStakeUnits: Boolean = false,
    private val securityPromptType: SecurityPromptType? = null,
    val epoch: Long? = null,
    val isRefreshing: Boolean = false,
    val uiMessage: UiMessage? = null
) : UiState {

    val visiblePrompt: SecurityPromptType?
        get() = when (securityPromptType) {
            SecurityPromptType.NEEDS_RESTORE -> securityPromptType
            SecurityPromptType.NEEDS_BACKUP -> if (accountWithAssets?.assets?.hasXrd() == true) {
                SecurityPromptType.NEEDS_BACKUP
            } else {
                null
            }
            else -> null
        }

    val historyDashboardUrl: String?
        get() = accountWithAssets?.account?.let { account ->
            val dashboardUrl = NetworkId.from(account.networkID).dashboardUrl()
            return "$dashboardUrl/account/${account.address}/recent-transactions"
        }

    val isTransferEnabled: Boolean
        get() = accountWithAssets?.assets != null

    fun onNFTsLoading(forResource: Resource.NonFungibleResource): AccountUiState {
        return copy(nonFungiblesWithPendingNFTs = nonFungiblesWithPendingNFTs + forResource.resourceAddress)
    }

    fun onNFTsReceived(forResource: Resource.NonFungibleResource): AccountUiState {
        if (accountWithAssets?.assets?.nonFungibles == null) return this
        return copy(
            accountWithAssets = accountWithAssets.copy(
                assets = accountWithAssets.assets.copy(
                    nonFungibles = accountWithAssets.assets.nonFungibles.mapWhen(
                        predicate = {
                            it.resourceAddress == forResource.resourceAddress && it.items.size < forResource.items.size
                        },
                        mutation = { forResource }
                    )
                )
            ),
            nonFungiblesWithPendingNFTs = nonFungiblesWithPendingNFTs - forResource.resourceAddress
        )
    }

    fun onNFTsError(forResource: Resource.NonFungibleResource, error: Throwable): AccountUiState {
        if (accountWithAssets?.assets?.nonFungibles == null) return this
        return copy(
            nonFungiblesWithPendingNFTs = nonFungiblesWithPendingNFTs - forResource.resourceAddress,
            uiMessage = UiMessage.ErrorMessage(error = error)
        )
    }

    fun onValidatorsReceived(validatorsWithStakes: List<ValidatorWithStakes>): AccountUiState = copy(
        accountWithAssets = accountWithAssets?.copy(
            assets = accountWithAssets.assets?.copy(
                validatorsWithStakes = validatorsWithStakes
            )
        ),
        pendingStakeUnits = false
    )
}
