package com.babylon.wallet.android.presentation.settings.preferences.assetshiding

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetPoolsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.ResourceIdentifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.keyImageUrl
import rdx.works.core.sargon.hidden
import rdx.works.core.sargon.pools
import rdx.works.profile.domain.ChangeResourceVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class HiddenAssetsViewModel @Inject constructor(
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getPoolsUseCase: GetPoolsUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val changeResourceVisibilityUseCase: ChangeResourceVisibilityUseCase,
    private val appEventBus: AppEventBus,
    @DefaultDispatcher private val coroutineDispatcher: CoroutineDispatcher
) : StateViewModel<HiddenAssetsViewModel.State>(),
    OneOffEventHandler<HiddenAssetsViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(isLoading = true)

    init {
        observeHiddenAssets()
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    fun onUnhideClick(assetAddress: ResourceIdentifier) {
        _state.update { it.copy(unhideResource = assetAddress) }
    }

    fun unhide(assetAddress: ResourceIdentifier) {
        viewModelScope.launch {
            changeResourceVisibilityUseCase.unhide(assetAddress)
            appEventBus.sendEvent(AppEvent.RefreshAssetsNeeded)
        }
    }

    fun cancelUnhide() {
        _state.update { it.copy(unhideResource = null) }
    }

    private fun observeHiddenAssets() {
        viewModelScope.launch {
            getProfileUseCase.flow
                .map { profile ->
                    val hiddenResourceAddresses = profile.appPreferences.resources.hidden()
                    val resources = buildResources(hiddenResourceAddresses)

                    State(
                        isLoading = false,
                        tokens = resources.fungibles.map { resource ->
                            State.Resource(
                                address = Address.Resource(resource.address),
                                identifier = ResourceIdentifier.Fungible(resource.address),
                                icon = resource.iconUrl,
                                name = resource.symbol.ifBlank { resource.name }
                            )
                        },
                        nonFungibles = resources.nonFungibles.map { resource ->
                            State.Resource(
                                address = Address.Resource(resource.address),
                                identifier = ResourceIdentifier.NonFungible(resource.address),
                                icon = resource.iconUrl,
                                name = resource.name
                            )
                        },
                        poolUnits = resources.pools.map { pool ->
                            State.Resource(
                                address = Address.Pool(pool.address),
                                identifier = ResourceIdentifier.PoolUnit(pool.address),
                                icon = pool.metadata.keyImageUrl(),
                                name = pool.name.takeIf { it.isNotBlank() }
                            )
                        }
                    )
                }
                .catch { throwable ->
                    _state.update { it.copy(message = UiMessage.ErrorMessage(throwable)) }
                }
                .flowOn(coroutineDispatcher)
                .collect {
                    _state.emit(it)
                }
        }
    }

    private suspend fun buildResources(
        addresses: List<ResourceIdentifier>
    ): Resources {
        val resourceAddresses = addresses.mapNotNull { address ->
            when (address) {
                is ResourceIdentifier.Fungible -> address.v1
                is ResourceIdentifier.NonFungible -> address.v1
                else -> null
            }
        }

        val resources = getResourcesUseCase(
            addresses = resourceAddresses.toSet(),
            withDetails = true
        ).getOrThrow()

        return Resources(
            fungibles = resources.filterIsInstance<Resource.FungibleResource>(),
            nonFungibles = resources.filterIsInstance<Resource.NonFungibleResource>(),
            pools = getPoolsUseCase(
                poolAddresses = addresses.pools().toSet()
            ).getOrNull().orEmpty()
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
        val tokens: List<Resource> = emptyList(),
        val nonFungibles: List<Resource> = emptyList(),
        val poolUnits: List<Resource> = emptyList(),
        val unhideResource: ResourceIdentifier? = null,
        val message: UiMessage? = null
    ) : UiState {

        data class Resource(
            val address: Address,
            val identifier: ResourceIdentifier,
            val icon: Uri?,
            val name: String?
        )
    }
}
