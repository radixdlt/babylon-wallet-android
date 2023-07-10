package com.babylon.wallet.android.presentation.settings.account.specificassets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject
import kotlin.random.Random

@Suppress("LongParameterList")
@HiltViewModel
class SpecificAssetsDepositsViewModel @Inject constructor(
    private val deviceSecurityHelper: DeviceSecurityHelper,
    private val getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<SpecificAssetsDepositsUiState>() {

    private val args = SpecificAssetsArgs(savedStateHandle)

    override fun initialState(): SpecificAssetsDepositsUiState = SpecificAssetsDepositsUiState(accountAddress = args.address)

    init {
        loadAccount()
    }

    fun onAddAsset() {
        // TODO not implemented
    }

    private fun loadAccount() {
        viewModelScope.launch {
            getProfileUseCase.accountsOnCurrentNetwork.map { accounts -> accounts.first { it.address == args.address } }
                .collect { account ->
                    _state.update {
                        it.copy(
                            account = account,
                            isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
                        )
                    }
                }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }
}

data class Asset(val iconUrl: String, val address: String, val name: String) {
    companion object {
        fun sampleAsset(): Asset {
            return Asset("", SampleDataProvider().randomAddress(), "name${Random.nextInt()}")
        }
    }
}

data class SpecificAssetsDepositsUiState(
    val account: Network.Account? = null,
    val accountAddress: String,
    val isLoading: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val error: UiMessage? = null,
    val allowedAssets: ImmutableList<Asset> = persistentListOf(Asset.sampleAsset(), Asset.sampleAsset(), Asset.sampleAsset()),
    val deniedAssets: ImmutableList<Asset> = persistentListOf(Asset.sampleAsset(), Asset.sampleAsset())
) : UiState
