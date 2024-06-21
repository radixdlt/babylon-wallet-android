package com.babylon.wallet.android.presentation.status.address

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.VerifyAddressOnLedgerUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetPoolsUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetValidatorsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.resources.Resource
import rdx.works.core.mapWhen
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.core.sargon.isLedgerAccount
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddressDetailsDialogViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val verifyAddressOnLedgerUseCase: VerifyAddressOnLedgerUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getPoolsUseCase: GetPoolsUseCase,
    private val getValidatorsUseCase: GetValidatorsUseCase
) : StateViewModel<AddressDetailsDialogViewModel.State>(),
    OneOffEventHandler<AddressDetailsDialogViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(actionableAddress = AddressDetailsArgs(savedStateHandle).actionableAddress)

    init {
        resolveCommonSections()
        resolveLedgerSection()
        resolveTitle()
    }

    private fun resolveCommonSections() {
        viewModelScope.launch {
            val actionableAddress = _state.value.actionableAddress

            val sections = mutableSetOf<State.Section>()

            if (actionableAddress is ActionableAddress.Address && actionableAddress.address is Address.Account) {
                sections.add(State.Section.AccountAddressQRCode(accountAddress = actionableAddress.address.v1))
            }

            sections.add(
                State.Section.FullAddress(
                    rawAddress = actionableAddress.rawAddress(),
                    truncatedPart = actionableAddress.truncatedPart()
                )
            )

            if (actionableAddress.isVisitableInDashboard) {
                actionableAddress.dashboardUrl()?.let {
                    sections.add(State.Section.VisitDashboard(url = it))
                }
            }

            _state.update { state -> state.copy(sections = sections.sortedBy { it.order }) }
        }
    }

    private fun resolveLedgerSection() {
        viewModelScope.launch {
            val actionableAddress = _state.value.actionableAddress

            if (actionableAddress is ActionableAddress.Address && actionableAddress.address is Address.Account) {
                val account = getProfileUseCase().activeAccountsOnCurrentNetwork.find { it.address == actionableAddress.address.v1 }

                if (account?.isLedgerAccount == true) {
                    _state.update { state ->
                        state.copy(
                            sections = state.sections.toMutableList().apply {
                                add(State.Section.VerifyAddressOnLedger(accountAddress = actionableAddress.address.v1))
                            }.sortedBy { it.order }
                        )
                    }
                }
            }
        }
    }

    private fun resolveTitle() = viewModelScope.launch {
        val title = when (val actionableAddress = _state.value.actionableAddress) {
            is ActionableAddress.Address -> resolveAddressTitle(actionableAddress)
            is ActionableAddress.GlobalId -> getResourcesUseCase(addresses = setOf(actionableAddress.address.resourceAddress))
                .getOrNull()?.let { resources ->
                    val resource = resources.firstOrNull()
                    if (resource is Resource.FungibleResource) {
                        resource.addressDialogTitle
                    } else {
                        resource?.name
                    }?.takeIf { it.isNotBlank() }
                }

            is ActionableAddress.TransactionId -> {
                null
            }
        }

        _state.update { it.copy(title = title) }
    }

    private suspend fun AddressDetailsDialogViewModel.resolveAddressTitle(
        actionableAddress: ActionableAddress.Address,
    ) = when (val address = actionableAddress.address) {
        is Address.Account -> {
            val accountOnProfile = getProfileUseCase().activeAccountsOnCurrentNetwork.find {
                it.address == address.v1
            }

            accountOnProfile?.displayName?.value
        }

        is Address.Identity -> {
            val personaOnProfile = getProfileUseCase().activePersonasOnCurrentNetwork.find {
                it.address == address.v1
            }

            personaOnProfile?.displayName?.value
        }

        is Address.Resource -> {
            getResourcesUseCase(addresses = setOf(address.v1))
                .getOrNull()?.let { resources ->
                    val resource = resources.firstOrNull()
                    if (resource is Resource.FungibleResource) {
                        resource.addressDialogTitle
                    } else {
                        resource?.name
                    }?.takeIf { it.isNotBlank() }
                }
        }

        is Address.Pool -> {
            getPoolsUseCase(poolAddresses = setOf(address.v1))
                .getOrNull()?.let { pools ->
                    pools.firstOrNull()?.name?.takeIf { it.isNotBlank() }
                }
        }

        is Address.Validator -> {
            getValidatorsUseCase(validatorAddresses = setOf(address.v1))
                .getOrNull()?.let { validators ->
                    validators.firstOrNull()?.name?.takeIf { it.isNotBlank() }
                }
        }

        else -> {
            null
        }
    }

    fun onCopyClick() {
        viewModelScope.launch {
            sendEvent(Event.PerformCopy(valueToCopy = _state.value.actionableAddress.rawAddress()))
        }
    }

    fun onEnlargeClick() {
        viewModelScope.launch {
            val address = _state.value.actionableAddress.rawAddress()
            val ranges = mutableListOf<OpenEndRange<Int>>()
            var latestNumberRange: OpenEndRange<Int>? = null
            address.forEachIndexed { index, char ->
                if (!char.isDigit()) {
                    latestNumberRange?.let {
                        ranges.add(it)
                        latestNumberRange = null
                    }
                } else {
                    latestNumberRange = latestNumberRange?.let {
                        it.start until index + 1
                    } ?: run {
                        index until index + 1
                    }
                }
            }
            latestNumberRange?.let {
                ranges.add(it)
            }

            sendEvent(
                Event.PerformEnlarge(
                    value = address,
                    numberRanges = ranges
                )
            )
        }
    }

    fun onHideEnlargeClick() {
        viewModelScope.launch {
            sendEvent(Event.CloseEnlarged)
        }
    }

    fun onShareClick() {
        viewModelScope.launch {
            sendEvent(
                Event.PerformShare(
                    shareTitle = _state.value.title,
                    shareValue = _state.value.actionableAddress.rawAddress()
                )
            )
        }
    }

    fun onVisitDashboardClick() {
        viewModelScope.launch {
            val dashboardUrl = _state.value.actionableAddress.dashboardUrl()
            if (dashboardUrl != null) {
                sendEvent(Event.PerformVisitDashBoard(url = dashboardUrl))
            }
        }
    }

    fun onVerifyOnLedgerDeviceClick() {
        viewModelScope.launch {
            val accountAddress = (_state.value.actionableAddress as? ActionableAddress.Address)?.let {
                it.address as? Address.Account
            }?.v1

            if (accountAddress != null) {
                val ledgerSection = state.value.sections.find {
                    it is State.Section.VerifyAddressOnLedger
                } as? State.Section.VerifyAddressOnLedger ?: return@launch

                _state.update { state ->
                    state.copy(sections = state.sections.mapWhen(
                        predicate = { it is State.Section.VerifyAddressOnLedger },
                        mutation = {
                            ledgerSection.copy(isVerifying = true)
                        }
                    ))
                }
                val isVerified = verifyAddressOnLedgerUseCase(address = accountAddress).fold(
                    onSuccess = { true },
                    onFailure = {
                        Timber.w(it)
                        false
                    }
                )

                _state.update { state ->
                    state.copy(sections = state.sections.mapWhen(
                        predicate = { it is State.Section.VerifyAddressOnLedger },
                        mutation = {
                            ledgerSection.copy(isVerifying = false)
                        }
                    ))
                }
                sendEvent(Event.ShowLedgerVerificationResult(isVerified = isVerified))
            }
        }
    }

    private val Resource.FungibleResource.addressDialogTitle: String
        get() {
            val symbol = symbol.takeIf { it.isNotBlank() }
            val name = name.takeIf { it.isNotBlank() }

            return if (symbol != null && name != null) {
                "$name ($symbol)"
            } else {
                symbol ?: name.orEmpty()
            }
        }

    data class State(
        val actionableAddress: ActionableAddress,
        val title: String? = null,
        val sections: List<Section> = emptyList()
    ) : UiState {

        sealed interface Section {
            val order: Short

            data class AccountAddressQRCode(
                override val order: Short = 0,
                val accountAddress: AccountAddress
            ) : Section

            data class FullAddress(
                override val order: Short = 1,
                val rawAddress: String,
                val truncatedPart: String
            ) : Section {

                val boldRanges: ImmutableList<OpenEndRange<Int>> = run {
                    val visibleCharsWhenTruncated = rawAddress.split(truncatedPart)

                    if (visibleCharsWhenTruncated.size != 2) return@run persistentListOf()

                    val startRange = 0 until visibleCharsWhenTruncated[0].length
                    val endRange = rawAddress.length - visibleCharsWhenTruncated[1].length until rawAddress.length

                    persistentListOf(
                        startRange,
                        endRange
                    )
                }
            }

            data class VisitDashboard(
                override val order: Short = 2,
                val url: String
            ) : Section

            data class VerifyAddressOnLedger(
                override val order: Short = 3,
                val accountAddress: AccountAddress,
                val isVerifying: Boolean = false
            ) : Section
        }
    }

    sealed interface Event : OneOffEvent {
        data class PerformCopy(val valueToCopy: String) : Event
        data class PerformEnlarge(
            val value: String,
            val numberRanges: List<OpenEndRange<Int>>
        ) : Event

        data object CloseEnlarged : Event
        data class PerformShare(
            val shareTitle: String?,
            val shareValue: String,
        ) : Event

        data class PerformVisitDashBoard(val url: String) : Event
        data class ShowLedgerVerificationResult(val isVerified: Boolean) : Event
    }
}
