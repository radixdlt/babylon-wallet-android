package com.babylon.wallet.android.presentation.status.assets.nonfungible

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetNFTDetailsUseCase
import com.babylon.wallet.android.domain.usecases.assets.ObserveResourceUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SendClaimRequestUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class NonFungibleAssetDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeResourceUseCase: ObserveResourceUseCase,
    getNFTDetailsUseCase: GetNFTDetailsUseCase,
    getNetworkInfoUseCase: GetNetworkInfoUseCase,
    getProfileUseCase: GetProfileUseCase,
    private val sendClaimRequestUseCase: SendClaimRequestUseCase
) : StateViewModel<NonFungibleAssetDialogViewModel.State>() {

    private val args = NonFungibleAssetDialogArgs(savedStateHandle)
    override fun initialState(): State = State(
        resourceAddress = args.resourceAddress,
        localId = args.localId,
        isNewlyCreated = args.isNewlyCreated
    )

    init {
        if (args.localId != null) {
            viewModelScope.launch {
                getNFTDetailsUseCase(
                    resourceAddress = _state.value.resourceAddress,
                    localId = args.localId
                ).onSuccess { item ->
                    _state.update { it.copy(item = item) }

                    if (item.claimAmountXrd != null) {
                        // Need to get the current epoch so as to resolve the state of the claim
                        getNetworkInfoUseCase().onSuccess { info ->
                            _state.update { it.copy(epoch = info.epoch) }
                        }.onFailure { error ->
                            _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
                        }
                    }
                }.onFailure { error ->
                    _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
                }
            }
        }

        if (args.accountAddress != null) {
            viewModelScope.launch {
                val account = getProfileUseCase.accountOnCurrentNetwork(withAddress = args.accountAddress)
                _state.update { it.copy(accountContext = account) }
            }
        }

        observeResourceUseCase(
            resourceAddress = args.resourceAddress,
            withDetails = !args.isNewlyCreated
        ).filterIsInstance<Resource.NonFungibleResource>()
            .onEach { resource ->
                _state.update { it.copy(resource = resource) }
            }
            .catch { error ->
                Timber.w(error)
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
            }
            .launchIn(viewModelScope)
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onClaimClick() {
        val resource = _state.value.resource ?: return
        val item = _state.value.item ?: return
        val epoch = _state.value.epoch ?: return
        val account = _state.value.accountContext ?: return

        if (item.isReadyToClaim(epoch)) {
            viewModelScope.launch {
                sendClaimRequestUseCase(
                    account = account,
                    claim = StakeClaim(resource),
                    nft = item,
                    epoch = epoch
                )
            }
        }
    }

    data class State(
        val resourceAddress: String,
        val localId: String?,
        val resource: Resource.NonFungibleResource? = null,
        val item: Resource.NonFungibleResource.Item? = null,
        val accountContext : Network.Account? = null,
        val epoch: Long? = null,
        val isNewlyCreated: Boolean,
        val uiMessage: UiMessage? = null
    ) : UiState {

        val claimState: ClaimState?
            get() {
                val claimAmount = item?.claimAmountXrd ?: return null
                val claimEpoch = item.claimEpoch ?: return null
                val currentEpoch = epoch ?: return null

                return if (claimEpoch <= currentEpoch) {
                    ClaimState.ReadyToClaim(
                        amount = claimAmount
                    )
                } else {
                    ClaimState.Unstaking(
                        amount = claimAmount,
                        current = currentEpoch,
                        claim = claimEpoch
                    )
                }
            }

        sealed class ClaimState {
            abstract val amount: BigDecimal

            data class Unstaking(
                override val amount: BigDecimal,
                private val current: Long,
                private val claim: Long
            ) : ClaimState() {
                val approximateClaimMinutes: Long
                    get() = (claim - current) * EPOCH_TIME_MINUTES
            }

            data class ReadyToClaim(
                override val amount: BigDecimal
            ) : ClaimState()

            companion object {
                private const val EPOCH_TIME_MINUTES = 5
            }
        }
    }
}
