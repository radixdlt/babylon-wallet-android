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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.resources.Resource
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

    private fun resolveTitle() {
        viewModelScope.launch {
            when (val actionableAddress = _state.value.actionableAddress) {
                is ActionableAddress.Address -> when (actionableAddress.address) {
                    is Address.Account -> {
                        val accountOnProfile = getProfileUseCase().activeAccountsOnCurrentNetwork.find {
                            it.address == actionableAddress.address.v1
                        }

                        if (accountOnProfile != null) {
                            _state.update { it.copy(title = accountOnProfile.displayName.value) }
                        }
                    }
                    is Address.Identity -> {
                        val personaOnProfile = getProfileUseCase().activePersonasOnCurrentNetwork.find {
                            it.address == actionableAddress.address.v1
                        }

                        if (personaOnProfile != null) {
                            _state.update { it.copy(title = personaOnProfile.displayName.value) }
                        }
                    }
                    is Address.Resource -> {
                        getResourcesUseCase(addresses = setOf(actionableAddress.address.v1))
                            .onSuccess { resources ->
                                val resource = resources.firstOrNull()
                                if (resource != null) {
                                    val name = if (resource is Resource.FungibleResource) {
                                        resource.addressDialogTitle
                                    } else {
                                        resource.name
                                    }

                                    _state.update { state -> state.copy(title = name.takeIf { it.isNotBlank() }) }
                                }
                            }
                    }
                    is Address.Pool -> {
                        getPoolsUseCase(poolAddresses = setOf(actionableAddress.address.v1))
                            .onSuccess { pools ->
                                val pool = pools.firstOrNull()
                                if (pool != null) {
                                    _state.update { state -> state.copy(title = pool.name.takeIf { it.isNotBlank() }) }
                                }
                            }
                    }
                    is Address.Validator -> {
                        getValidatorsUseCase(validatorAddresses = setOf(actionableAddress.address.v1))
                            .onSuccess { validators ->
                                val validator = validators.firstOrNull()
                                if (validator != null) {
                                    _state.update { state -> state.copy(title = validator.name.takeIf { it.isNotBlank() }) }
                                }
                            }
                    }
                    else -> {}
                }
                is ActionableAddress.GlobalId -> {
                    getResourcesUseCase(addresses = setOf(actionableAddress.address.resourceAddress))
                        .onSuccess { resources ->
                            val resource = resources.firstOrNull()
                            if (resource != null) {
                                val name = if (resource is Resource.FungibleResource) {
                                    resource.addressDialogTitle
                                } else {
                                    resource.name
                                }

                                _state.update { state -> state.copy(title = name.takeIf { it.isNotBlank() }) }
                            }
                        }
                }
                is ActionableAddress.TransactionId -> {}
            }
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
                val isVerified = verifyAddressOnLedgerUseCase(address = accountAddress).fold(
                    onSuccess = { true },
                    onFailure = {
                        Timber.w(it)
                        false
                    }
                )
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
            } else symbol ?: name.orEmpty()
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

                val boldRanges: List<OpenEndRange<Int>> = run {
                    val visibleCharsWhenTruncated = rawAddress.split(truncatedPart)

                    if (visibleCharsWhenTruncated.size != 2) return@run emptyList()

                    val startRange = 0 until visibleCharsWhenTruncated[0].length
                    val endRange = rawAddress.length - visibleCharsWhenTruncated[1].length until rawAddress.length

                    listOf(
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
                val accountAddress: AccountAddress
            ) : Section
        }

    }

    sealed interface Event: OneOffEvent {
        data class PerformCopy(val valueToCopy: String): Event
        data class PerformEnlarge(
            val value: String,
            val numberRanges: List<OpenEndRange<Int>>
        ): Event
        data object CloseEnlarged: Event
        data class PerformShare(
            val shareTitle: String?,
            val shareValue: String,
        ): Event
        data class PerformVisitDashBoard(val url: String): Event
        data class ShowLedgerVerificationResult(val isVerified: Boolean): Event
    }
}