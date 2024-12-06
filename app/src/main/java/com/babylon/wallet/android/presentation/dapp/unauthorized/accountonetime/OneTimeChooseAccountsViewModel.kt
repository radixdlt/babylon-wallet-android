package com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.WalletUnauthorizedRequest
import com.babylon.wallet.android.domain.model.signing.SignPurpose
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.hex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class OneTimeChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy
) : StateViewModel<OneTimeChooseAccountUiState>(),
    OneOffEventHandler<OneTimeChooseAccountsEvent> by OneOffEventHandlerImpl() {

    private val args = OneTimeChooseAccountsArgs(savedStateHandle)

    override fun initialState(): OneTimeChooseAccountUiState = OneTimeChooseAccountUiState(
        numberOfAccounts = args.numberOfAccounts,
        isExactAccountsCount = args.isExactAccountsCount,
        isSingleChoice = isSingleChoice()
    )

    init {
        viewModelScope.launch {
            getUnauthorizedRequest()

            getProfileUseCase.flow.map { it.activeAccountsOnCurrentNetwork }.collect { accounts ->
                // user can create a new account at the Choose Accounts screen,
                // therefore this part ensures that the selection state (if any account was selected)
                // remains once the user returns from the account creation flow
                val accountItems = accounts.map { account ->
                    val currentAccountItemState = _state.value.availableAccountItems.find { accountItemUiModel ->
                        accountItemUiModel.address == account.address
                    }
                    val defaultSelected = isSingleChoice() && accounts.size == 1
                    account.toUiModel(currentAccountItemState?.isSelected ?: defaultSelected)
                }

                _state.update {
                    it.copy(availableAccountItems = accountItems.toPersistentList())
                }
            }
        }
    }

    private suspend fun getUnauthorizedRequest() {
        val interactionId = args.unauthorizedRequestInteractionId
        interactionId.let {
            val requestToHandle = incomingRequestRepository.getRequest(interactionId) as? WalletUnauthorizedRequest
            if (requestToHandle == null) { // should never happen because
                // the validation first occurs in the initialization of the DAppUnauthorizedLoginViewModel
                sendEvent(OneTimeChooseAccountsEvent.TerminateFlow)
                return@let
            } else {
                _state.update { state ->
                    state.copy(walletUnauthorizedRequest = requestToHandle)
                }
            }
        }
    }

    fun onAccountSelected(index: Int) {
        if (_state.value.isExactAccountsCount && _state.value.numberOfAccounts == 1) {
            // Radio buttons selection unselects the previous one
            _state.update {
                it.copy(
                    availableAccountItems = it.availableAccountItems.mapIndexed { i, accountItem ->
                        if (index == i) {
                            accountItem.copy(isSelected = true)
                        } else {
                            accountItem.copy(isSelected = false)
                        }
                    }.toPersistentList()
                )
            }
        } else {
            _state.update {
                it.copy(
                    availableAccountItems = it.availableAccountItems.mapIndexed { i, accountItem ->
                        if (index == i) {
                            accountItem.copy(isSelected = !accountItem.isSelected)
                        } else {
                            accountItem
                        }
                    }.toPersistentList()
                )
            }
        }
    }

    private fun isSingleChoice(): Boolean {
        return args.numberOfAccounts == 1 && args.isExactAccountsCount
    }

    fun onContinueClick() {
        state.value.walletUnauthorizedRequest?.let { request ->
            setSigningInProgress(true)

            viewModelScope.launch {
                val selectedAccountEntities = state.value.selectedAccounts().mapNotNull {
                    getProfileUseCase().activeAccountOnCurrentNetwork(it.address)
                }.map { it.asProfileEntity() }

                // check if signature is required
                if (request.oneTimeAccountsRequestItem?.challenge != null) {
                    collectSignatures(
                        challenge = request.oneTimeAccountsRequestItem.challenge,
                        selectedAccountEntities = selectedAccountEntities,
                        metadata = request.metadata
                    )
                } else { // otherwise return the collected accounts without signatures
                    sendEvent(
                        OneTimeChooseAccountsEvent.AccountsCollected(
                            accountsWithSignatures = selectedAccountEntities.associateWith { null }
                        )
                    )
                    setSigningInProgress(false)
                }
            }
        }
    }

    private suspend fun collectSignatures(
        challenge: Exactly32Bytes,
        selectedAccountEntities: List<ProfileEntity.AccountEntity>,
        metadata: DappToWalletInteraction.RequestMetadata
    ) {
        val signRequest = SignRequest.RolaSignRequest(
            challengeHex = challenge.hex,
            origin = metadata.origin,
            dAppDefinitionAddress = metadata.dAppDefinitionAddress
        )

        accessFactorSourcesProxy.getSignatures(
            accessFactorSourcesInput = AccessFactorSourcesInput.ToGetSignatures(
                signPurpose = SignPurpose.SignAuth,
                signRequest = signRequest,
                signers = selectedAccountEntities.map { it.address }
            )
        ).onSuccess { result ->
            sendEvent(
                OneTimeChooseAccountsEvent.AccountsCollected(
                    accountsWithSignatures = result.signersWithSignatures
                        .filterKeys {
                            it is ProfileEntity.AccountEntity
                        }.mapKeys {
                            it.key as ProfileEntity.AccountEntity
                        }
                )
            )
            setSigningInProgress(false)
        }.onFailure {
            sendEvent(
                OneTimeChooseAccountsEvent.AuthorizationFailed(
                    throwable = RadixWalletException.DappRequestException.FailedToSignAuthChallenge(it)
                )
            )
            setSigningInProgress(false)
        }
    }

    private fun setSigningInProgress(isEnabled: Boolean) = _state.update { it.copy(isSigningInProgress = isEnabled) }
}

sealed interface OneTimeChooseAccountsEvent : OneOffEvent {

    data object TerminateFlow : OneTimeChooseAccountsEvent

    data class AccountsCollected(
        val accountsWithSignatures: Map<ProfileEntity.AccountEntity, SignatureWithPublicKey?>
    ) : OneTimeChooseAccountsEvent

    data class AuthorizationFailed(val throwable: RadixWalletException) : OneTimeChooseAccountsEvent
}

data class OneTimeChooseAccountUiState(
    val walletUnauthorizedRequest: WalletUnauthorizedRequest? = null,
    val availableAccountItems: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val numberOfAccounts: Int,
    val isExactAccountsCount: Boolean,
    val isSingleChoice: Boolean = false,
    val isSigningInProgress: Boolean = false
) : UiState {

    val isContinueButtonEnabled: Boolean
        get() = if (isExactAccountsCount) {
            availableAccountItems
                .count { accountItem ->
                    accountItem.isSelected
                } == numberOfAccounts
        } else {
            availableAccountItems.isEmpty() || availableAccountItems
                .count { accountItem ->
                    accountItem.isSelected
                } >= numberOfAccounts
        }

    fun selectedAccounts(): List<AccountItemUiModel> {
        return availableAccountItems
            .filter { accountItem ->
                accountItem.isSelected
            }
    }
}
