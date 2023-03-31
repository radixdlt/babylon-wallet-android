package com.babylon.wallet.android.presentation.account

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.domain.usecases.GetAccountResourcesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.model.AssetUiModel
import com.babylon.wallet.android.presentation.model.NftCollectionUiModel
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.model.toNftUiModel
import com.babylon.wallet.android.presentation.model.toTokenUiModel
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_NAME
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.decodeUtf8
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getAccountResourcesUseCase: GetAccountResourcesUseCase,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String = savedStateHandle.get<String>(ARG_ACCOUNT_ID).orEmpty()
    private val accountName: String = savedStateHandle.get<String>(ARG_ACCOUNT_NAME).orEmpty()

    private val _accountUiState = MutableStateFlow(
        AccountUiState(
            accountAddressFull = accountId,
            accountName = accountName.decodeUtf8()
        )
    )
    val accountUiState = _accountUiState.asStateFlow()

    init {
        loadAccountData(isRefreshing = false)
        viewModelScope.launch {
            appEventBus.events.filter { event ->
                event is AppEvent.GotFreeXrd || event is AppEvent.ApprovedTransaction
            }.collect {
                refresh()
            }
        }
    }

    fun refresh() {
        _accountUiState.update { state ->
            state.copy(isRefreshing = true)
        }
        loadAccountData(isRefreshing = true)
    }

    private fun loadAccountData(isRefreshing: Boolean) {
        viewModelScope.launch {
            if (accountId.isNotEmpty()) {
                val result = getAccountResourcesUseCase.getAccount(accountId, isRefreshing)
                result.onError { e ->
                    _accountUiState.update { accountUiState ->
                        accountUiState.copy(uiMessage = UiMessage.ErrorMessage(error = e), isLoading = false)
                    }
                }
                result.onValue { accountResource ->
                    val xrdToken = accountResource.fungibleTokens.find {
                        it.token.metadata[MetadataConstants.KEY_SYMBOL] == MetadataConstants.SYMBOL_XRD
                    }

                    val fungibleTokens = accountResource.fungibleTokens.filter {
                        it.token.metadata[MetadataConstants.KEY_SYMBOL] != MetadataConstants.SYMBOL_XRD
                    }

                    _accountUiState.update { accountUiState ->
                        accountUiState.copy(
                            isRefreshing = false,
                            isLoading = false,
                            xrdToken = xrdToken?.toTokenUiModel(),
                            fungibleTokens = fungibleTokens.toTokenUiModel().toPersistentList(),
                            nonFungibleTokens = accountResource.nonFungibleTokens.toNftUiModel().toPersistentList(),
                            gradientIndex = accountResource.appearanceID
                        )
                    }
                }
            } else {
                Timber.d("arg account id is empty")
            }
        }
    }

    fun onFungibleTokenClick(token: TokenUiModel) {
        _accountUiState.update { accountUiState ->
            accountUiState.copy(assetDetails = token)
        }
    }

    fun onNonFungibleTokenClick(
        nftCollectionUiModel: NftCollectionUiModel,
        nftItemUiModel: NftCollectionUiModel.NftItemUiModel
    ) {
        _accountUiState.update { accountUiState ->
            accountUiState.copy(
                assetDetails = nftCollectionUiModel,
                selectedNft = nftItemUiModel
            )
        }
    }
}

data class AccountUiState(
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
)

enum class AssetTypeTab(@StringRes val stringId: Int) {
    TOKEN_TAB(R.string.account_asset_row_tab_tokens),
    NTF_TAB(R.string.account_asset_row_tab_nfts),
}
