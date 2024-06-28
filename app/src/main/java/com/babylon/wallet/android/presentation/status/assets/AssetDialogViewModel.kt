package com.babylon.wallet.android.presentation.status.assets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SendClaimRequestUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Decimal192
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AssetDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val sendClaimRequestUseCase: SendClaimRequestUseCase,
    private val getNetworkInfoUseCase: GetNetworkInfoUseCase,
    private val getFiatValueUseCase: GetFiatValueUseCase
) : StateViewModel<AssetDialogViewModel.State>() {

    private val args = AssetDialogArgs.from(savedStateHandle)

    override fun initialState(): State = State(args = args)

    init {
        viewModelScope.launch {
            resolveAssetsFromAddressUseCase(
                fungibleAddresses = when (args) {
                    is AssetDialogArgs.Fungible -> setOf(args.resourceAddress)
                    is AssetDialogArgs.NFT -> setOf()
                },
                nonFungibleIds = when (args) {
                    is AssetDialogArgs.Fungible -> mapOf()
                    is AssetDialogArgs.NFT -> mapOf(
                        args.resourceAddress to args.localId?.let { setOf(it) }.orEmpty()
                    )
                }
            ).mapCatching { assets ->
                when (val asset = assets.first()) {
                    // In case we receive a fungible asset, let's copy the custom amount
                    is Asset.Fungible -> {
                        val fungibleArgs = (args as? AssetDialogArgs.Fungible) ?: return@mapCatching asset

                        val resourceWithAmount = asset.resource.copy(
                            ownedAmount = fungibleArgs.fungibleAmountOf(asset.resource.address)
                        )
                        when (asset) {
                            is LiquidStakeUnit -> asset.copy(fungibleResource = resourceWithAmount)
                            is PoolUnit -> asset.copy(stake = resourceWithAmount)
                            is Token -> asset.copy(resource = resourceWithAmount)
                        }
                    }

                    is Asset.NonFungible -> {
                        asset
                    }
                }
            }.mapCatching { asset ->
                _state.update { it.copy(asset = asset) }

                args.underAccountAddress?.let { accountAddress ->
                    val account = getProfileUseCase().activeAccountOnCurrentNetwork(accountAddress)
                    _state.update { it.copy(accountContext = account) }

                    if (account != null) {
                        getFiatValueUseCase.forAsset(
                            asset = asset,
                            account = account,
                            isRefreshing = true
                        )
                            .onSuccess { assetPrice ->
                                _state.update { state ->
                                    state.copy(assetPrice = assetPrice)
                                }
                            }
                            .onFailure {
                                if (it is FiatPriceRepository.PricesNotSupportedInNetwork) {
                                    disableFiatPrices()
                                }
                                _state.update { state ->
                                    state.copy(assetPrice = null)
                                }
                            }
                    }
                }

                asset
            }.onSuccess { asset ->
                if (asset is StakeClaim) {
                    // Need to get the current epoch so as to resolve the state of the claim
                    getNetworkInfoUseCase().onSuccess { info ->
                        _state.update { it.copy(epoch = info.epoch) }
                    }.onFailure { error ->
                        _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
                    }
                }
            }.onFailure { error ->
                Timber.w(error)
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    @Suppress("ComplexCondition")
    fun onClaimClick() {
        val state = _state.value
        val claim = state.asset as? StakeClaim
        val nft = claim?.resource?.items?.firstOrNull()
        val account = state.accountContext
        if (nft != null && state.epoch != null && account != null && nft.isReadyToClaim(state.epoch)) {
            viewModelScope.launch {
                sendClaimRequestUseCase(
                    account = account,
                    claim = claim,
                    nft = nft,
                    epoch = state.epoch
                )
            }
        }
    }

    private fun disableFiatPrices() {
        _state.update { state ->
            state.copy(isFiatBalancesEnabled = false)
        }
    }

    data class State(
        val args: AssetDialogArgs,
        val asset: Asset? = null,
        val epoch: Long? = null,
        val isFiatBalancesEnabled: Boolean = true,
        val assetPrice: AssetPrice? = null,
        val uiMessage: UiMessage? = null,
        val accountContext: Account? = null,
    ) : UiState {

        val isLoadingBalance = args.underAccountAddress != null && assetPrice == null

        val claimState: ClaimState?
            get() {
                val item = (asset as? Asset.NonFungible)?.resource?.items?.firstOrNull()
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
            abstract val amount: Decimal192

            data class Unstaking(
                override val amount: Decimal192,
                private val current: Long,
                private val claim: Long
            ) : ClaimState() {
                val approximateClaimMinutes: Long
                    get() = (claim - current) * EPOCH_TIME_MINUTES
            }

            data class ReadyToClaim(
                override val amount: Decimal192
            ) : ClaimState()

            companion object {
                private const val EPOCH_TIME_MINUTES = 5
            }
        }
    }
}
