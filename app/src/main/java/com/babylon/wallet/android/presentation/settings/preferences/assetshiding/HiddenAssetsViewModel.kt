package com.babylon.wallet.android.presentation.settings.preferences.assetshiding

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AssetAddress
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.extensions.hiddenAssets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.ChangeAssetVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HiddenAssetsViewModel @Inject constructor(
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val changeAssetVisibilityUseCase: ChangeAssetVisibilityUseCase,
) : StateViewModel<HiddenAssetsViewModel.State>(),
    OneOffEventHandler<HiddenAssetsViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(isLoading = true)

    init {
        observeHiddenAssets()
    }

    fun onUnhideClick(assetAddress: AssetAddress) {
        _state.update { it.copy(unhideAsset = assetAddress) }
    }

    fun unhide(assetAddress: AssetAddress) {
        viewModelScope.launch {
            changeAssetVisibilityUseCase.unhide(assetAddress)
        }
    }

    fun cancelUnhide() {
        _state.update { it.copy(unhideAsset = null) }
    }

    private fun observeHiddenAssets() {
        viewModelScope.launch {
            getWalletAssetsUseCase(getProfileUseCase().activeAccountsOnCurrentNetwork, false)
                .flatMapLatest { accountsWithAssets ->
                    getProfileUseCase.flow.map { profile ->
                        val hiddenAssets = profile.appPreferences.assets.asIdentifiable().hiddenAssets
                        val hiddenTokenAddresses = hiddenAssets.mapNotNull { it as? AssetAddress.Fungible }
                        val hiddenNonFungibleAddresses = hiddenAssets.mapNotNull { it as? AssetAddress.NonFungible }
                        val hiddenPoolUnitAddresses = hiddenAssets.mapNotNull { it as? AssetAddress.PoolUnit }

                        val allTokens = accountsWithAssets.mapNotNull { it.assets?.tokens }.flatten()
                        val allNonFungibles = accountsWithAssets.mapNotNull { it.assets?.nonFungibles }.flatten()
                        val allPoolUnits = accountsWithAssets.mapNotNull { it.assets?.poolUnits }.flatten()

                        State(
                            isLoading = false,
                            tokens = hiddenTokenAddresses.mapNotNull { hiddenTokenAddress ->
                                val token = allTokens.firstOrNull { it.resource.address == hiddenTokenAddress.v1 }
                                    ?: return@mapNotNull null

                                State.Asset(
                                    address = hiddenTokenAddress,
                                    icon = token.resource.iconUrl,
                                    name = token.resource.symbol.ifBlank { token.resource.name },
                                    description = null
                                )
                            },
                            nfts = hiddenNonFungibleAddresses.mapNotNull { hiddenNonFungibleAddress ->
                                val collection = allNonFungibles.firstOrNull {
                                    it.resource.address == hiddenNonFungibleAddress.v1.resourceAddress
                                }?.collection ?: return@mapNotNull null
                                val item = collection.items.firstOrNull { it.localId == hiddenNonFungibleAddress.v1.nonFungibleLocalId }

                                State.Asset(
                                    address = hiddenNonFungibleAddress,
                                    icon = item?.imageUrl ?: collection.iconUrl,
                                    name = collection.name,
                                    description = item?.name
                                )
                            },
                            poolUnits = hiddenPoolUnitAddresses.mapNotNull { hiddenPoolUnitAddress ->
                                val poolUnit = allPoolUnits.firstOrNull { it.pool?.address == hiddenPoolUnitAddress.v1 }
                                    ?: return@mapNotNull null

                                State.Asset(
                                    address = AssetAddress.PoolUnit(poolUnit.pool?.address ?: return@mapNotNull null),
                                    icon = poolUnit.stake.iconUrl,
                                    name = poolUnit.stake.name.ifBlank { poolUnit.stake.symbol }
                                        .takeIf { it.isNotBlank() },
                                    description = poolUnit.pool?.associatedDApp?.name
                                )
                            }
                        )
                    }
                }.collect {
                    _state.emit(it)
                }
        }
    }

    sealed interface Event : OneOffEvent {
        data object Close : Event
    }

    data class State(
        val isLoading: Boolean = false,
        val tokens: List<Asset> = emptyList(),
        val nfts: List<Asset> = emptyList(),
        val poolUnits: List<Asset> = emptyList(),
        val unhideAsset: AssetAddress? = null
    ) : UiState {

        data class Asset(
            val address: AssetAddress,
            val icon: Uri?,
            val name: String?,
            val description: String?
        )
    }
}
