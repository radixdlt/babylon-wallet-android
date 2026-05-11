package com.babylon.wallet.android.presentation.settings.preferences.addressbook

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AddressBookEntry
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.validatedOnNetworkOrNull
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.sortedForDisplay
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.addressbook.AddAddressBookEntryUseCase
import rdx.works.profile.domain.addressbook.DeleteAddressBookEntryUseCase
import rdx.works.profile.domain.addressbook.GetAddressBookEntriesOnCurrentNetworkUseCase
import rdx.works.profile.domain.addressbook.UpdateAddressBookEntryUseCase
import javax.inject.Inject

@HiltViewModel
class AddressBookViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getAddressBookEntriesOnCurrentNetworkUseCase: GetAddressBookEntriesOnCurrentNetworkUseCase,
    private val addAddressBookEntryUseCase: AddAddressBookEntryUseCase,
    private val updateAddressBookEntryUseCase: UpdateAddressBookEntryUseCase,
    private val deleteAddressBookEntryUseCase: DeleteAddressBookEntryUseCase
) : StateViewModel<AddressBookViewModel.State>() {

    override fun initialState(): State = State()

    fun onAppear() {
        loadEntries()
    }

    fun onAddClick() {
        val currentNetworkId = state.value.currentNetworkId
        if (currentNetworkId != null) {
            showAddForm(currentNetworkId)
            return
        }

        viewModelScope.launch {
            runCatching {
                getProfileUseCase().currentGateway.network.id
            }.onFailure {
                onError(it)
            }.onSuccess { networkId ->
                _state.update { currentState ->
                    currentState.copy(currentNetworkId = networkId)
                }
                showAddForm(networkId)
            }
        }
    }

    fun onEditClick(entry: AddressBookEntry) {
        _state.update { currentState ->
            currentState.copy(
                formInput = State.FormInput(
                    mode = State.FormInput.Mode.Edit(entry),
                    address = entry.address.string,
                    name = entry.name.value,
                    note = entry.note.orEmpty()
                )
            )
        }
    }

    fun onDeleteClick(entry: AddressBookEntry) {
        _state.update { currentState ->
            currentState.copy(entryToDelete = entry)
        }
    }

    fun onDeleteConfirmationDismissed(confirmed: Boolean) {
        val entry = state.value.entryToDelete

        _state.update { currentState ->
            currentState.copy(entryToDelete = null)
        }

        if (!confirmed || entry == null) return

        viewModelScope.launch {
            runCatching {
                deleteAddressBookEntryUseCase(entry.address)
            }.onFailure {
                onError(it)
            }.onSuccess { deleted ->
                if (deleted) {
                    loadEntries()
                } else {
                    onError(Throwable("Failed to delete address book entry"))
                }
            }
        }
    }

    fun onDismissErrorMessage() {
        _state.update { currentState ->
            currentState.copy(errorMessage = null)
        }
    }

    fun onDismissOwnAccountAddressAlert() {
        _state.update { currentState ->
            currentState.copy(showOwnAccountAddressAlert = false)
        }
    }

    fun onFormDismissed() {
        _state.update { currentState ->
            currentState.copy(formInput = null)
        }
    }

    fun onFormAddressChanged(value: String) {
        updateFormInput { it.copy(address = value) }
    }

    fun onFormNameChanged(value: String) {
        updateFormInput { it.copy(name = value) }
    }

    fun onFormNoteChanged(value: String) {
        updateFormInput { it.copy(note = value) }
    }

    fun onFormSaveClick() {
        val input = state.value.formInput ?: return
        if (!input.isValid) return

        viewModelScope.launch {
            updateFormInput { it.copy(isSaving = true) }

            val address = input.addressToSave ?: return@launch
            val name = DisplayName(input.trimmedName)
            val note = input.trimmedNote

            if (input.mode is State.FormInput.Mode.Add) {
                val ownAddresses = getProfileUseCase().activeAccountsOnCurrentNetwork.map { it.address }
                if ((address as? Address.Account)?.v1 in ownAddresses) {
                    updateFormInput { it.copy(isSaving = false) }
                    _state.update { currentState ->
                        currentState.copy(showOwnAccountAddressAlert = true)
                    }
                    return@launch
                }
            }

            val saveResult = runCatching {
                when (input.mode) {
                    is State.FormInput.Mode.Add -> addAddressBookEntryUseCase(address, name, note)
                    is State.FormInput.Mode.Edit -> updateAddressBookEntryUseCase(address, name, note)
                }
            }

            saveResult.onFailure {
                onError(it)
                updateFormInput { formInput -> formInput.copy(isSaving = false) }
            }.onSuccess { saved ->
                if (saved) {
                    _state.update { currentState ->
                        currentState.copy(formInput = null)
                    }
                    loadEntries()
                } else {
                    onError(Throwable("Failed to save address book entry"))
                    updateFormInput { formInput -> formInput.copy(isSaving = false) }
                }
            }
        }
    }

    private fun loadEntries() {
        viewModelScope.launch {
            runCatching {
                val profile = getProfileUseCase()
                val entries = getAddressBookEntriesOnCurrentNetworkUseCase().sortedForDisplay()

                State(
                    currentNetworkId = profile.currentGateway.network.id,
                    entries = entries.toPersistentList(),
                    errorMessage = null,
                    formInput = state.value.formInput,
                    entryToDelete = state.value.entryToDelete,
                    showOwnAccountAddressAlert = state.value.showOwnAccountAddressAlert
                )
            }.onFailure {
                onError(it)
            }.onSuccess { updatedState ->
                _state.emit(updatedState)
            }
        }
    }

    private fun updateFormInput(update: (State.FormInput) -> State.FormInput) {
        _state.update { currentState ->
            currentState.copy(
                formInput = currentState.formInput?.let(update)
            )
        }
    }

    private fun onError(throwable: Throwable) {
        _state.update { currentState ->
            currentState.copy(
                errorMessage = UiMessage.ErrorMessage(throwable)
            )
        }
    }

    private fun showAddForm(networkId: NetworkId) {
        _state.update { currentState ->
            currentState.copy(
                formInput = State.FormInput(
                    mode = State.FormInput.Mode.Add(networkId),
                    address = "",
                    name = "",
                    note = ""
                )
            )
        }
    }

    data class State(
        val currentNetworkId: NetworkId? = null,
        val entries: ImmutableList<AddressBookEntry> = persistentListOf(),
        val errorMessage: UiMessage.ErrorMessage? = null,
        val formInput: FormInput? = null,
        val entryToDelete: AddressBookEntry? = null,
        val showOwnAccountAddressAlert: Boolean = false
    ) : UiState {

        data class FormInput(
            val mode: Mode,
            val address: String,
            val name: String,
            val note: String,
            val isSaving: Boolean = false
        ) {
            sealed interface Mode {
                data class Add(val networkId: NetworkId) : Mode
                data class Edit(val entry: AddressBookEntry) : Mode
            }

            val trimmedName: String
                get() = name.trim()

            val trimmedNote: String?
                get() = note.trim().takeIf { it.isNotEmpty() }

            val validatedAddress: Address?
                get() = when (mode) {
                    is Mode.Add -> Address.validatedOnNetworkOrNull(
                        validating = address.trim().lowercase(),
                        networkId = mode.networkId
                    )

                    is Mode.Edit -> mode.entry.address
                }

            val addressToSave: Address?
                get() = validatedAddress

            val isAddressEditable: Boolean
                get() = mode is Mode.Add

            val isValid: Boolean
                get() = addressToSave != null && trimmedName.isNotEmpty()

            val hasAddressError: Boolean
                get() = isAddressEditable && address.isNotBlank() && validatedAddress == null

            val isEditing: Boolean
                get() = mode is Mode.Edit
        }
    }
}
