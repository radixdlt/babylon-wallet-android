package com.babylon.wallet.android.presentation.account

import android.content.ClipData
import android.content.ClipboardManager
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.usecase.wallet.GetAccountResourcesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.model.AssetUiModel
import com.babylon.wallet.android.presentation.model.NftCollectionUiModel
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.model.toNftUiModel
import com.babylon.wallet.android.presentation.model.toTokenUiModel
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_GRADIENT_INDEX
import com.babylon.wallet.android.utils.truncatedHash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getAccountResourcesUseCase: GetAccountResourcesUseCase,
    private val clipboardManager: ClipboardManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String = savedStateHandle.get<String>(ARG_ACCOUNT_ID).orEmpty()
    private val gradientIndex = savedStateHandle[ARG_GRADIENT_INDEX] ?: 0

    private val _accountUiState = MutableStateFlow(AccountUiState(gradientIndex = gradientIndex))
    val accountUiState = _accountUiState.asStateFlow()

    init {
        loadAccountData()
    }

    fun refresh() {
        _accountUiState.update { state ->
            state.copy(isRefreshing = true)
        }
        loadAccountData()
    }

    private fun loadAccountData() {
        viewModelScope.launch {
            if (accountId.isNotEmpty()) {
                // TODO how to handle the case when the gateway doesn't return the account?
                // TODO this should probably change to flow later
                val result = getAccountResourcesUseCase(accountId)
                result.onError { e ->
                    _accountUiState.update { accountUiState ->
                        accountUiState.copy(uiMessage = UiMessage(error = e))
                    }
                }
                result.onValue { accountResource ->
                    val xrdToken = if (accountResource.hasXrdToken()) {
                        accountResource.fungibleTokens[INDEX_OF_XRD]
                    } else {
                        null
                    }
                    val fungibleTokens = if (accountResource.hasXrdToken()) {
                        accountResource.fungibleTokens.subList(1, accountResource.fungibleTokens.size)
                    } else {
                        accountResource.fungibleTokens
                    }
                    _accountUiState.update { accountUiState ->
                        accountUiState.copy(
                            isRefreshing = false,
                            isLoading = false,
                            accountAddressShortened = accountResource.address.truncatedHash(),
                            xrdToken = xrdToken?.toTokenUiModel(),
                            fungibleTokens = fungibleTokens.toTokenUiModel().toPersistentList(),
                            nonFungibleTokens = accountResource.nonFungibleTokens.toNftUiModel().toPersistentList()
                        )
                    }
                }
            } else {
                Timber.d("arg account id is empty")
            }
        }
    }

    fun onCopyAccountAddress(hash: String) {
        val clipData = ClipData.newPlainText("accountHash", hash)
        clipboardManager.setPrimaryClip(clipData)
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

    companion object {
        private const val INDEX_OF_XRD = 0
    }
}

data class AccountUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val gradientIndex: Int = 0,
    val accountAddressShortened: String = "",
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
