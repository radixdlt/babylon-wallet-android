package com.babylon.wallet.android.presentation.settings.securitycenter.mfafactorinstance

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.selectfactorsource.SelectFactorSourceInput
import com.babylon.wallet.android.presentation.selectfactorsource.SelectFactorSourceProxy
import com.radixdlt.sargon.Account
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.nonFungibleGlobalId
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.accountAddressOrNull
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.addressbook.GetAddressBookEntriesOnCurrentNetworkUseCase
import javax.inject.Inject

@HiltViewModel
class MfaFactorInstanceViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val selectFactorSourceProxy: SelectFactorSourceProxy,
    private val getProfileUseCase: GetProfileUseCase,
    private val getAddressBookEntriesOnCurrentNetworkUseCase: GetAddressBookEntriesOnCurrentNetworkUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<MfaFactorInstanceViewModel.State>(),
    OneOffEventHandler<MfaFactorInstanceViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(isLoadingCurrentUsage = true)

    init {
        loadCurrentUsage()
    }

    fun onGetNewInstanceClick() {
        viewModelScope.launch {
            val factorSource = selectFactorSourceProxy.selectFactorSource(
                context = SelectFactorSourceInput.Context.MfaFactorInstance
            )?.let { selectedFactorSourceId ->
                getProfileUseCase().factorSourceById(selectedFactorSourceId.value)
            } ?: return@launch

            sargonOsManager.callSafely(defaultDispatcher) {
                val mfaFactorInstance = getNewMfaFactorInstance(factorSource)
                mfaFactorInstance.nonFungibleGlobalId()
            }.onSuccess { signatureResourceGlobalId ->
                sendEvent(
                    Event.ShowAddressDetails(
                        ActionableAddress.GlobalId(
                            address = signatureResourceGlobalId,
                            isVisitableInDashboard = true,
                            isOnlyLocalIdVisible = false
                        )
                    )
                )
            }.onFailure { error ->
                _state.update { state ->
                    state.copy(uiMessage = UiMessage.ErrorMessage(error))
                }
            }
        }
    }

    fun onFactorSourceClick(factorSourceId: FactorSourceId) {
        viewModelScope.launch {
            sendEvent(Event.ShowFactorSourceDetails(factorSourceId))
        }
    }

    fun onMessageShown() {
        _state.update { state ->
            state.copy(uiMessage = null)
        }
    }

    private fun loadCurrentUsage() {
        viewModelScope.launch {
            runCatching {
                val usedResources = sargonOsManager.callSafely(defaultDispatcher) {
                    usedMfaSignatureResourcesWithAccountsCurrentNetwork()
                }.getOrThrow()
                val profile = getProfileUseCase()
                val activeAccountsByAddress = profile.activeAccountsOnCurrentNetwork.associateBy { it.address }
                val factorSourcesById = profile.factorSources.associateBy { it.id }
                val addressBookNamesByAddress = getAddressBookEntriesOnCurrentNetworkUseCase()
                    .mapNotNull { entry -> entry.accountAddressOrNull?.let { it to entry.name.value } }
                    .toMap()

                usedResources.map { usedResource ->
                    val factorSourceId = usedResource.mfaFactorInstance.factorInstance.factorSourceId
                    State.ActiveUsage(
                        signatureResource = usedResource.nonFungibleGlobalId,
                        accounts = usedResource.accountAddresses.map { address ->
                            State.UsedByAccount(
                                address = address,
                                profileAccount = activeAccountsByAddress[address],
                                addressBookName = addressBookNamesByAddress[address]
                            )
                        },
                        factorSource = factorSourcesById[factorSourceId]
                    )
                }
            }.onSuccess { activeUsages ->
                _state.update { state ->
                    state.copy(
                        activeUsages = activeUsages,
                        isLoadingCurrentUsage = false
                    )
                }
            }.onFailure { error ->
                _state.update { state ->
                    state.copy(
                        activeUsages = emptyList(),
                        isLoadingCurrentUsage = false,
                        uiMessage = UiMessage.ErrorMessage(error)
                    )
                }
            }
        }
    }

    data class State(
        val activeUsages: List<ActiveUsage> = emptyList(),
        val isLoadingCurrentUsage: Boolean,
        val uiMessage: UiMessage? = null
    ) : UiState {
        data class UsedByAccount(
            val address: AccountAddress,
            val profileAccount: Account?,
            val addressBookName: String?
        )

        data class ActiveUsage(
            val signatureResource: NonFungibleGlobalId,
            val accounts: List<UsedByAccount>,
            val factorSource: FactorSource?
        )
    }

    sealed interface Event : OneOffEvent {
        data class ShowAddressDetails(
            val actionableAddress: ActionableAddress
        ) : Event

        data class ShowFactorSourceDetails(
            val factorSourceId: FactorSourceId
        ) : Event
    }
}
