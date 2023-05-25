package com.babylon.wallet.android.presentation.transfer.assets

import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.pernetwork.Network

class AssetsChooserDelegate(
    private val state: MutableStateFlow<TransferViewModel.State>,
    private val viewModelScope: CoroutineScope,
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase
) {

    /**
     * Starts the assets chooser flow
     *
     * [fromAccount] is used to fetch the resources of that account
     * [targetAccount] is the account which we target to attach assets
     * [restOfTargetAccounts] is used to potentially filter out non fungibles that have been already used
     */
    fun onChooseAssets(
        fromAccount: Network.Account,
        targetAccount: TargetAccount,
        restOfTargetAccounts: List<TargetAccount>
    ) {
        state.update {
            it.copy(sheet = Sheet.ChooseAssets(targetAccount = targetAccount))
        }

        viewModelScope.launch {
            getAccountsWithResourcesUseCase(accounts = listOf(fromAccount), isRefreshing = false)
                .onValue { accountWithResources ->
                    val resources = accountWithResources.firstOrNull()?.resources

                    if (resources == null) {
                        updateSheetState { it.copy(resources = null) }
                    } else {
                        // Compile a list of all used nft items
                        val usedNftItemIds = restOfTargetAccounts.map { target ->
                            target.assets.filterIsInstance<SpendingAsset.NFT>().map { it.address }
                        }.flatten()

                        val nftCollections = resources.nonFungibleResources.map { collection ->
                            // Remove any "already used" nft items
                            collection.copy(
                                items = collection.items.filterNot { it.globalAddress in usedNftItemIds }
                            )
                        }.filterNot { it.items.isEmpty() } // And also remove any collection that ended up with no items

                        updateSheetState {
                            it.copy(
                                resources = resources.copy(nonFungibleResources = nftCollections)
                            )
                        }
                    }
                }.onError { error ->
                    updateSheetState {
                        it.copy(
                            resources = Resources.EMPTY,
                            uiMessage = UiMessage.ErrorMessage(error)
                        )
                    }
                }
        }
    }

    fun onTabSelected(tab: Sheet.ChooseAssets.Tab) {
        updateSheetState { it.copy(selectedTab = tab) }
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
        state.update { state ->
            val chooseAssetState = (state.sheet as? Sheet.ChooseAssets) ?: return@update state

            state.copy(
                targetAccounts = state.targetAccounts.mapWhen(
                    predicate = {
                        it.id == chooseAssetState.targetAccount.id
                    },
                    mutation = {
                        chooseAssetState.targetAccount
                    }
                ),
                sheet = Sheet.None
            )
        }
    }

    private fun updateSheetState(
        onUpdate: (Sheet.ChooseAssets) -> Sheet.ChooseAssets
    ) {
        state.update { state ->
            if (state.sheet is Sheet.ChooseAssets) {
                state.copy(sheet = onUpdate(state.sheet))
            } else {
                state
            }
        }
    }
}
