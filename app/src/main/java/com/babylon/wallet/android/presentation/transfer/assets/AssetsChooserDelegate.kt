package com.babylon.wallet.android.presentation.transfer.assets

import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetNextNFTsPageUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.domain.usecases.assets.UpdateLSUsInfo
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet
import com.radixdlt.sargon.Account
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.Resource
import javax.inject.Inject

class AssetsChooserDelegate @Inject constructor(
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    private val getFiatValueUseCase: GetFiatValueUseCase,
    private val getNextNFTsPageUseCase: GetNextNFTsPageUseCase,
    private val updateLSUsInfo: UpdateLSUsInfo,
    private val getNetworkInfoUseCase: GetNetworkInfoUseCase,
) : ViewModelDelegate<TransferViewModel.State>() {

    /**
     * Starts the assets chooser flow
     *
     * [fromAccount] is used to fetch the resources of that account
     * [targetAccount] is the account which we target to attach assets
     */
    fun onChooseAssets(
        fromAccount: Account,
        targetAccount: TargetAccount
    ) = viewModelScope.launch {
        _state.update {
            it.copy(sheet = Sheet.ChooseAssets.init(forTargetAccount = targetAccount))
        }

        getWalletAssetsUseCase(accounts = listOf(fromAccount), isRefreshing = false)
            .catch { error ->
                updateSheetState {
                    it.copy(
                        assets = Assets(),
                        uiMessage = UiMessage.ErrorMessage(error)
                    )
                }
            }
            .collect { accountsWithAssets ->
                val accountWithAssets = accountsWithAssets.firstOrNull()
                val assets = accountWithAssets?.assets

                accountWithAssets?.let {
                    getFiatValueUseCase.forAccount(accountWithAssets = it, isRefreshing = true)
                        .onSuccess { assetsPrices ->
                            updateSheetState { state ->
                                state.copy(assetsWithAssetsPrices = assetsPrices.associateBy { assetPrice -> assetPrice.asset })
                            }
                        }
                        .onFailure { exception ->
                            if (exception is FiatPriceRepository.PricesNotSupportedInNetwork) {
                                disableFiatPrices()
                            }
                            updateSheetState { state ->
                                state.copy(assetsWithAssetsPrices = emptyMap())
                            }
                        }
                }

                updateSheetState { it.copy(assets = assets) }

                onLatestEpochRequest()
            }
    }

    fun onTabSelected(tab: AssetsTab) {
        updateSheetState { it.copy(assetsViewState = it.assetsViewState.copy(selectedTab = tab)) }
    }

    fun onCollectionToggle(collectionId: String) {
        updateSheetState { it.copy(assetsViewState = it.assetsViewState.onCollectionToggle(collectionId)) }
    }

    fun onAssetSelectionChanged(asset: SpendingAsset, isChecked: Boolean) {
        updateSheetState { state ->
            if (isChecked) {
                state.copy(targetAccount = state.targetAccount.addAsset(asset))
            } else {
                state.copy(targetAccount = state.targetAccount.removeAsset(asset))
            }
        }
    }

    fun onUiMessageShown() {
        updateSheetState { it.copy(uiMessage = null) }
    }

    fun onChooseAssetsSubmitted() {
        _state.update { state ->
            val chooseAssetState = (state.sheet as? Sheet.ChooseAssets) ?: return@update state

            state
                .replace(chooseAssetState.targetAccount)
                .copy(sheet = Sheet.None)
        }
    }

    fun onNextNFTsPageRequest(resource: Resource.NonFungibleResource) {
        val sheet = _state.value.sheet as? Sheet.ChooseAssets ?: return
        val account = _state.value.fromAccount ?: return
        if (resource.address !in sheet.nonFungiblesWithPendingNFTs) {
            updateSheetState { state -> state.onNFTsLoading(resource) }
            viewModelScope.launch {
                getNextNFTsPageUseCase(account, resource)
                    .onSuccess { resourceWithUpdatedNFTs ->
                        updateSheetState { state -> state.onNFTsReceived(resourceWithUpdatedNFTs) }
                    }.onFailure { error ->
                        updateSheetState { state -> state.onNFTsError(resource, error) }
                    }
            }
        }
    }

    fun onStakesRequest() {
        val sheet = _state.value.sheet as? Sheet.ChooseAssets ?: return
        val account = _state.value.fromAccount ?: return
        val lsus = sheet.assets?.ownedLiquidStakeUnits ?: return
        val validatorsWithStakes = ValidatorWithStakes.from(lsus, sheet.assets.ownedStakeClaims)
        val unknownLSUs = validatorsWithStakes.any { !it.isDetailsAvailable }

        if (!sheet.pendingStakeUnits && unknownLSUs) {
            updateSheetState { state -> state.copy(pendingStakeUnits = true) }
            viewModelScope.launch {
                updateLSUsInfo(account, validatorsWithStakes).onSuccess {
                    updateSheetState { state -> state.onValidatorsReceived(it) }
                }.onFailure { error ->
                    updateSheetState { state -> state.copy(pendingStakeUnits = false, uiMessage = UiMessage.ErrorMessage(error)) }
                }
            }
        }
    }

    private fun onLatestEpochRequest() = viewModelScope.launch {
        getNetworkInfoUseCase().onSuccess { info ->
            updateSheetState { state -> state.copy(epoch = info.epoch) }
        }
    }

    private fun disableFiatPrices() {
        updateSheetState { state ->
            state.copy(isFiatBalancesEnabled = false)
        }
    }

    private fun updateSheetState(
        onUpdate: (Sheet.ChooseAssets) -> Sheet.ChooseAssets
    ) {
        _state.update { state ->
            if (state.sheet is Sheet.ChooseAssets) {
                state.copy(sheet = onUpdate(state.sheet))
            } else {
                state
            }
        }
    }
}
