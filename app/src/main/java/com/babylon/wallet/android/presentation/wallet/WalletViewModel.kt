package com.babylon.wallet.android.presentation.wallet

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.usecase.wallet.GetAccountResourcesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val mainViewRepository: MainViewRepository,
    private val clipboardManager: ClipboardManager,
    private val getAccountsUseCase: GetAccountResourcesUseCase,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _walletUiState: MutableStateFlow<WalletUiState> = MutableStateFlow(WalletUiState())
    val walletUiState = _walletUiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadResourceData()
        }
    }

    private suspend fun loadResourceData() {

        val wallet = mainViewRepository.getWallet()

        profileRepository.readProfileSnapshot()?.let { profileSnapshot ->
            val profile = profileSnapshot.toProfile()
            val accountsResourcesList = mutableListOf<AccountResources>()
            val accounts = profile.getAccounts()
            val results = accounts.map { account ->
                viewModelScope.async {
                    getAccountsUseCase(account.entityAddress.address)
                }
            }.awaitAll()

            results.forEachIndexed { index, accountResourcesList ->
                accountResourcesList.onError { error ->
                    _walletUiState.update { it.copy(error = UiMessage(error), isLoading = false) }
                }
                accountResourcesList.onValue { accountResources ->
                    // TODO this is temporary because we use only one account address therefore data which we fetch
                    // would be the same. Therefore we fetch display name from the profile instead
                    val accountName = accounts[index].displayName.orEmpty()
                    accountsResourcesList.add(
                        AccountResources(
                            address = accountResources.address,
                            displayName = accountName,
                            currencySymbol = accountResources.currencySymbol,
                            value = accountResources.value,
                            fungibleTokens = accountResources.fungibleTokens,
                            nonFungibleTokens = accountResources.nonFungibleTokens
                        )
                    )
                }
            }

            _walletUiState.update { state ->
                state.copy(wallet = wallet, resources = accountsResourcesList.toPersistentList(), isLoading = false)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _walletUiState.update { it.copy(isRefreshing = true) }
            loadResourceData()
            _walletUiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onCopyAccountAddress(hashValue: String) {
        val clipData = ClipData.newPlainText("accountHash", hashValue)
        clipboardManager.setPrimaryClip(clipData)
    }
}

data class WalletUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val wallet: WalletData? = null,
    val resources: ImmutableList<AccountResources> = persistentListOf(),
    val error: UiMessage? = null
)

data class WalletData(
    val currency: String,
    val amount: String
)
