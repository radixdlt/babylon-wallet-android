package com.babylon.wallet.android.presentation.account

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.model.AssetUiModel
import com.babylon.wallet.android.presentation.model.NftCollectionUiModel
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.model.toNftUiModel
import com.babylon.wallet.android.presentation.model.toTokenUiModel
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.utils.unsecuredFactorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : StateViewModel<AccountUiState>(), OneOffEventHandler<AccountEvent> by OneOffEventHandlerImpl() {

    private val accountId: String = savedStateHandle.get<String>(ARG_ACCOUNT_ID).orEmpty()

    override fun initialState(): AccountUiState = AccountUiState(
        accountAddressFull = accountId
    )

    init {
        viewModelScope.launch {
            getProfileUseCase.accountOnCurrentNetwork(accountId)?.let { account ->
                _state.update { state ->
                    state.copy(accountName = account.displayName)
                }
            }
        }
        loadAccountData(isRefreshing = false)

        viewModelScope.launch {
            appEventBus.events.filter { event ->
                event is AppEvent.GotFreeXrd || event is AppEvent.ApprovedTransaction
            }.collect {
                refresh()
            }
        }
        viewModelScope.launch {
            appEventBus.events.filter { event ->
                event is AppEvent.RestoredMnemonic
            }.collect {
                loadAccountData(isRefreshing = false)
            }
        }

        observeBackedUpMnemonics()
    }

    private fun observeBackedUpMnemonics() {
        viewModelScope.launch {
            preferencesManager.getBackedUpFactorSourceIds().distinctUntilChanged().collect {
                loadAccountData(isRefreshing = false)
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
        viewModelScope.launch {
            if (accountId.isNotEmpty()) {
                val result = getAccountsWithResourcesUseCase.getAccount(accountId, isRefreshing)
                result.onError { e ->
                    _state.update { accountUiState ->
                        accountUiState.copy(uiMessage = UiMessage.ErrorMessage(error = e), isLoading = false)
                    }
                }
                result.onValue { accountWithResources ->
                    val xrdToken = accountWithResources.fungibleResources.find {
                        it.symbol == MetadataConstants.SYMBOL_XRD
                    }

                    val fungibleTokens = accountWithResources.fungibleResources.filter {
                        it.symbol != MetadataConstants.SYMBOL_XRD
                    }

                    _state.update { accountUiState ->
                        accountUiState.copy(
                            showSecurityPrompt = accountWithResources.needMnemonicBackup(),
                            needMnemonicRecovery = accountWithResources.needMnemonicRecovery(),
                            isRefreshing = false,
                            isLoading = false,
                            xrdToken = xrdToken?.toTokenUiModel(),
                            fungibleTokens = fungibleTokens.toTokenUiModel().toPersistentList(),
                            nonFungibleTokens = accountWithResources.nonFungibleResources.toNftUiModel().toPersistentList(),
                            gradientIndex = accountWithResources.account.appearanceID % AccountGradientList.size
                        )
                    }
                }
            } else {
                Timber.d("arg account id is empty")
            }
        }
    }

    fun onFungibleTokenClick(token: TokenUiModel) {
        _state.update { accountUiState ->
            accountUiState.copy(assetDetails = token)
        }
    }

    fun onNonFungibleTokenClick(
        nftCollectionUiModel: NftCollectionUiModel,
        nftItemUiModel: NftCollectionUiModel.NftItemUiModel
    ) {
        _state.update { accountUiState ->
            accountUiState.copy(
                assetDetails = nftCollectionUiModel,
                selectedNft = nftItemUiModel
            )
        }
    }

    fun onApplySecuritySettings() {
        viewModelScope.launch {
            getProfileUseCase.accountOnCurrentNetwork(state.value.accountAddressFull)?.unsecuredFactorSourceId()?.let {
                sendEvent(AccountEvent.NavigateToMnemonicBackup(it))
            }
        }
    }
}

internal sealed interface AccountEvent : OneOffEvent {
    data class NavigateToMnemonicBackup(val factorSourceId: FactorSource.ID) : AccountEvent
}

data class AccountUiState(
    val showSecurityPrompt: Boolean = false,
    val needMnemonicRecovery: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val gradientIndex: Int = 0,
    val accountName: String = "",
    val accountAddressFull: String = "",
    val walletFiatBalance: String? = null,
    val xrdToken: TokenUiModel? = null,
    val assetDetails: AssetUiModel? = null,
    val selectedNft: NftCollectionUiModel.NftItemUiModel? = null,
    val fungibleTokens: ImmutableList<TokenUiModel> = persistentListOf(),
    val nonFungibleTokens: ImmutableList<NftCollectionUiModel> = persistentListOf(),
    val uiMessage: UiMessage? = null
) : UiState

enum class AssetTypeTab(@StringRes val stringId: Int) {
    TOKEN_TAB(R.string.account_asset_row_tab_tokens),
    NTF_TAB(R.string.account_asset_row_tab_nfts),
}
