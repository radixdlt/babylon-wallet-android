package com.babylon.wallet.android.presentation.dapp.authorized.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.usecases.signing.SignAuthUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asProfileEntity
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
import java.util.Collections.emptyList
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val signAuthUseCase: SignAuthUseCase
) : StateViewModel<ChooseAccountUiState>(), OneOffEventHandler<ChooseAccountsEvent> by OneOffEventHandlerImpl() {

    private val args = ChooseAccountsArgs(savedStateHandle)

    override fun initialState(): ChooseAccountUiState = ChooseAccountUiState(
        numberOfAccounts = args.numberOfAccounts,
        isExactAccountsCount = args.isExactAccountsCount,
        isOneTimeRequest = args.isOneTimeRequest,
        showBackButton = args.showBack
    )

    init {
        viewModelScope.launch {
            getAuthorizedRequest()

            getProfileUseCase.flow.map { it.activeAccountsOnCurrentNetwork }.collect { accounts ->
                // Check if single or multiple choice (radio or checkbox)
                val isSingleChoice = args.numberOfAccounts == 1 && args.isExactAccountsCount
                _state.update { it.copy(isSingleChoice = isSingleChoice) }

                // user can create a new account at the Choose Accounts screen,
                // therefore this part ensures that the selection state (if any account was selected)
                // remains once the user returns from the account creation flow
                val accountItems = accounts.map { account ->
                    val currentAccountItemState =
                        _state.value.availableAccountItems.find { accountItemUiModel ->
                            accountItemUiModel.address == account.address
                        }
                    account.toUiModel(currentAccountItemState?.isSelected ?: false)
                }

                _state.update {
                    it.copy(
                        availableAccountItems = accountItems.toPersistentList(),
                        showProgress = false,
                        isContinueButtonEnabled = !it.isExactAccountsCount && it.numberOfAccounts == 0
                    )
                }
            }
        }
    }

    private suspend fun getAuthorizedRequest() {
        val interactionId = args.authorizedRequestInteractionId
        interactionId.let {
            val requestToHandle = incomingRequestRepository.getRequest(interactionId) as? WalletAuthorizedRequest
            if (requestToHandle == null) { // should never happen because
                // the validation first occurs in the initialization of the DAppAuthorizedLoginViewModel
                sendEvent(ChooseAccountsEvent.TerminateFlow)
                return@let
            } else {
                _state.update { state ->
                    state.copy(walletAuthorizedRequest = requestToHandle)
                }
            }
        }
    }

    fun onAccountSelected(index: Int) {
        // update the isSelected property of the AccountItemUiModel based on index
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
        val isContinueButtonEnabled = if (_state.value.isExactAccountsCount) {
            _state.value
                .availableAccountItems
                .count { accountItem ->
                    accountItem.isSelected
                } == args.numberOfAccounts
        } else {
            _state.value
                .availableAccountItems
                .count { accountItem ->
                    accountItem.isSelected
                } >= args.numberOfAccounts
        }

        _state.update {
            it.copy(
                isContinueButtonEnabled = isContinueButtonEnabled,
                selectedAccounts = it.availableAccountItems.filter { accountItem -> accountItem.isSelected }
            )
        }
    }

    fun onContinueClick() {
        state.value.walletAuthorizedRequest?.let { request ->
            setSigningInProgress(true)

            viewModelScope.launch {
                val selectedAccountEntities = state.value.selectedAccounts().mapNotNull {
                    getProfileUseCase().activeAccountOnCurrentNetwork(it.address)
                }.map { it.asProfileEntity() }

                val challenge = if (state.value.isOneTimeRequest) {
                    request.oneTimeAccountsRequestItem?.challenge
                } else {
                    request.ongoingAccountsRequestItem?.challenge
                }

                // check if signature is required
                if (challenge != null) {
                    collectSignatures(
                        challenge = challenge,
                        selectedAccountEntities = selectedAccountEntities,
                        metadata = request.metadata
                    )
                } else { // otherwise return the collected accounts without signatures
                    sendEvent(
                        ChooseAccountsEvent.AccountsCollected(
                            accountsWithSignatures = selectedAccountEntities.associateWith { null },
                            isOneTimeRequest = state.value.isOneTimeRequest
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
        signAuthUseCase(
            challenge = challenge,
            entities = selectedAccountEntities,
            metadata = metadata
        ).onSuccess { signersWithSignatures ->
            sendEvent(
                ChooseAccountsEvent.AccountsCollected(
                    accountsWithSignatures = signersWithSignatures.map {
                        it.key as ProfileEntity.AccountEntity to it.value
                    }.associate { it.first to it.second },
                    isOneTimeRequest = state.value.isOneTimeRequest
                )
            )
            setSigningInProgress(false)
        }.onFailure {
            sendEvent(
                ChooseAccountsEvent.AuthorizationFailed(
                    throwable = RadixWalletException.DappRequestException.FailedToSignAuthChallenge
                )
            )
            setSigningInProgress(false)
        }
    }

    private fun setSigningInProgress(isEnabled: Boolean) = _state.update { it.copy(isSigningInProgress = isEnabled) }
}

sealed interface ChooseAccountsEvent : OneOffEvent {

    data object TerminateFlow : ChooseAccountsEvent

    data class AccountsCollected(
        val accountsWithSignatures: Map<ProfileEntity.AccountEntity, SignatureWithPublicKey?>,
        val isOneTimeRequest: Boolean = false
    ) : ChooseAccountsEvent

    data class AuthorizationFailed(val throwable: RadixWalletException) : ChooseAccountsEvent
}

data class ChooseAccountUiState(
    val walletAuthorizedRequest: WalletAuthorizedRequest? = null,
    val numberOfAccounts: Int,
    val isExactAccountsCount: Boolean,
    val availableAccountItems: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val isContinueButtonEnabled: Boolean = false,
    val isOneTimeRequest: Boolean = false,
    val isSingleChoice: Boolean = false,
    val showProgress: Boolean = true,
    val showBackButton: Boolean = false,
    val selectedAccounts: List<AccountItemUiModel> = emptyList(),
    val isSigningInProgress: Boolean = false
) : UiState {

    fun selectedAccounts(): List<AccountItemUiModel> {
        return availableAccountItems
            .filter { accountItem ->
                accountItem.isSelected
            }
    }
}
