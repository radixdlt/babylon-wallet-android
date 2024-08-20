package com.babylon.wallet.android.presentation.settings.preferences.assetshiding

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AssetAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.keyImageUrl
import rdx.works.core.sargon.hidden
import rdx.works.core.sargon.pools
import rdx.works.profile.domain.ChangeAssetVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class HiddenAssetsViewModel @Inject constructor(
    private val stateRepository: StateRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val changeAssetVisibilityUseCase: ChangeAssetVisibilityUseCase,
    @DefaultDispatcher private val coroutineDispatcher: CoroutineDispatcher
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
            getProfileUseCase.flow
                .map { profile ->
                    val hiddenAssetAddresses = profile.appPreferences.assets.hidden()
                    val resources = buildResources(hiddenAssetAddresses)

                    State(
                        isLoading = false,
                        tokens = resources.fungibles.map { resource ->
                            State.Asset(
                                address = AssetAddress.Fungible(resource.address),
                                icon = resource.iconUrl,
                                name = resource.symbol.ifBlank { resource.name },
                                description = null
                            )
                        },
                        nonFungibles = hiddenAssetAddresses.mapNotNull {
                            val assetAddress = it as? AssetAddress.NonFungible ?: return@mapNotNull null
                            val resource = resources.nonFungibles.firstOrNull { resource ->
                                resource.address == assetAddress.v1.resourceAddress
                            } ?: return@mapNotNull null
                            val item = resource.items.firstOrNull { item ->
                                item.localId == assetAddress.v1.nonFungibleLocalId
                            }

                            State.Asset(
                                address = assetAddress,
                                icon = item?.imageUrl ?: resource.iconUrl,
                                name = resource.name,
                                description = item?.name
                            )
                        },
                        poolUnits = resources.pools.map { pool ->
                            State.Asset(
                                address = AssetAddress.PoolUnit(pool.address),
                                icon = pool.metadata.keyImageUrl(),
                                name = pool.name.takeIf { it.isNotBlank() },
                                description = pool.associatedDApp?.name?.takeIf { it.isNotBlank() }
                            )
                        }
                    )
                }
                .flowOn(coroutineDispatcher)
                .collect {
                    _state.emit(it)
                }
        }
    }

    private suspend fun buildResources(
        addresses: List<AssetAddress>
    ): Resources {
        val resourceAddresses = addresses.mapNotNull { address ->
            when (address) {
                is AssetAddress.Fungible -> address.v1
                is AssetAddress.NonFungible -> address.v1.resourceAddress
                else -> null
            }
        }

        val resources = stateRepository.getResources(
            addresses = resourceAddresses.toSet(),
            underAccountAddress = null,
            withDetails = true,
            withAllMetadata = false
        ).getOrThrow()

        val nfts = resourceAddresses.filterIsInstance<AssetAddress.NonFungible>()
            .groupBy { it.v1.resourceAddress }
            .mapValues { entry ->
                stateRepository.getNFTDetails(
                    resourceAddress = entry.key,
                    localIds = entry.value.map { it.v1.nonFungibleLocalId }.toSet()
                ).getOrThrow()
            }

        return Resources(
            fungibles = resources.filterIsInstance<Resource.FungibleResource>(),
            nonFungibles = resources.filterIsInstance<Resource.NonFungibleResource>().map { collection ->
                collection.copy(items = nfts[collection.address].orEmpty())
            },
            pools = stateRepository.getPools(
                poolAddresses = addresses.pools().toSet()
            ).getOrThrow()
        )
    }

    private data class Resources(
        val fungibles: List<Resource.FungibleResource>,
        val nonFungibles: List<Resource.NonFungibleResource>,
        val pools: List<Pool>
    )

    sealed interface Event : OneOffEvent {
        data object Close : Event
    }

    data class State(
        val isLoading: Boolean = false,
        val tokens: List<Asset> = emptyList(),
        val nonFungibles: List<Asset> = emptyList(),
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
