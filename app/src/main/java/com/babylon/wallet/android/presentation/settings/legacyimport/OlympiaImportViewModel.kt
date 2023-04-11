package com.babylon.wallet.android.presentation.settings.legacyimport

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.OlympiaAccountDetails
import com.babylon.wallet.android.domain.OlympiaAccountType
import com.babylon.wallet.android.domain.OlympiaWalletData
import com.babylon.wallet.android.domain.getOlympiaTestAccounts
import com.babylon.wallet.android.domain.olympiaTestSeedPhrase
import com.babylon.wallet.android.domain.validatePublicKeysOf
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.domain.AddOlympiaFactorSourceUseCase
import javax.inject.Inject

@HiltViewModel
class OlympiaImportViewModel @Inject constructor(
    private val addOlympiaFactorSourceUseCase: AddOlympiaFactorSourceUseCase
) : StateViewModel<OlympiaImportUiState>(),
    OneOffEventHandler<OlympiaImportEvent> by OneOffEventHandlerImpl() {

    private val scannedData = mutableSetOf<String>()
    private var olympiaWalletData: OlympiaWalletData? = null

    fun onQrCodeScanned(qrData: String) {
        scannedData.add(qrData)
//        val olympiaWalletData = testPayloadChunks.parseOlympiaWalletAccountData()
//        olympiaWalletData?.let { data ->
//            this.olympiaWalletData = data
        viewModelScope.launch {
            _state.update {
                it.copy(
                    currentPage = ImportPage.AccountList,
                    olympiaAccounts = getOlympiaTestAccounts().map { Selectable(it) }.toPersistentList()
                )
            }
            sendEvent(OlympiaImportEvent.NextPage(ImportPage.AccountList))
        }
//        }
    }

    fun onBackClick() {
        previousPage()
    }

    private fun nextPage() {
        viewModelScope.launch {
            delay(300)
            val currentPage = _state.value.currentPage
            _state.value.pages.nextPage(currentPage)?.let { nextPage ->
                _state.update { it.copy(currentPage = nextPage) }
                sendEvent(OlympiaImportEvent.NextPage(nextPage))
            }
        }
    }

    private fun previousPage() {
        viewModelScope.launch {
            val newPage = _state.value.pages.previousPage(_state.value.currentPage)
            newPage?.let { page -> _state.update { it.copy(currentPage = page) } }
            sendEvent(OlympiaImportEvent.PreviousPage(newPage))
        }
    }

    private fun buildPages(olympiaAccounts: List<OlympiaAccountDetails>): PersistentList<ImportPage> {
        val hasSoftwareAccounts = olympiaAccounts.any { it.type == OlympiaAccountType.Software }
        val hasHardwareAccounts = olympiaAccounts.any { it.type == OlympiaAccountType.Hardware }
        var pages = _state.value.pages.toList()
        when {
            hasSoftwareAccounts -> {
                pages = pages + listOf(ImportPage.MnemonicInput)
                if (hasHardwareAccounts) {
                    pages = pages + listOf(ImportPage.HardwareAccount)
                }
            }
            hasHardwareAccounts -> {
                pages = pages + listOf(ImportPage.HardwareAccount)
            }
            else -> {}
        }
        return (pages + listOf(ImportPage.ImportComplete)).toPersistentList()
    }

    fun onAccountSelected(item: Selectable<OlympiaAccountDetails>) {
        val updatedAccounts = _state.value.olympiaAccounts.mapWhen(predicate = {
            it.data == item.data
        }) {
            it.copy(selected = !it.selected)
        }.toPersistentList()
        _state.update { state ->
            state.copy(olympiaAccounts = updatedAccounts, importButtonEnabled = updatedAccounts.any { it.selected })
        }
    }

    fun onSeedPhraseChanged(value: String) {
        _state.update { state ->
            state.copy(seedPhrase = value)
        }
        validateMnemonic()
    }

    fun onPassphraseChanged(value: String) {
        _state.update { state ->
            state.copy(bip39Passphrase = value)
        }
        validateMnemonic()
    }

    private fun validateMnemonic() {
        _state.update { state ->
            val seedPhrase = _state.value.seedPhrase
            val words = seedPhrase.split(" ")
            state.copy(importSoftwareAccountsEnabled = words.size > 10)
        }
    }

    fun onImportAccounts() {
        val selectedAccounts = _state.value.olympiaAccounts.filter { it.selected }.map { it.data }
        val pages = buildPages(selectedAccounts)
        _state.update { state ->
            state.copy(pages = pages)
        }
        nextPage()
    }

    fun onImportSoftwareAccounts() {
        val selectedSoftwareAccounts = _state.value.olympiaAccounts.filter {
            it.selected && it.data.type == OlympiaAccountType.Software
        }.map { it.data }
        val mnemonicWithPassphrase = MnemonicWithPassphrase(_state.value.seedPhrase, _state.value.bip39Passphrase)
        val accountsValid = mnemonicWithPassphrase.validatePublicKeysOf(selectedSoftwareAccounts)
    }

    override fun initialState() = OlympiaImportUiState(seedPhrase = olympiaTestSeedPhrase)

}

enum class ImportPage {
    ScanQr, AccountList, MnemonicInput, HardwareAccount, ImportComplete
}

fun List<ImportPage>.nextPage(currentPage: ImportPage): ImportPage? {
    val currentIndex = indexOf(currentPage)
    if (currentIndex == -1 || currentIndex == size - 1) return null
    return this[currentIndex + 1]
}

fun List<ImportPage>.previousPage(currentPage: ImportPage): ImportPage? {
    val currentIndex = indexOf(currentPage)
    if (currentIndex == -1 || currentIndex == 0) return null
    return this[currentIndex - 1]
}

sealed interface OlympiaImportEvent : OneOffEvent {
    data class NextPage(val page: ImportPage) : OlympiaImportEvent
    data class PreviousPage(val page: ImportPage?) : OlympiaImportEvent
}

data class OlympiaImportUiState(
    val isLoading: Boolean = false,
    val pages: ImmutableList<ImportPage> = persistentListOf(ImportPage.ScanQr, ImportPage.AccountList),
    val currentPage: ImportPage = ImportPage.ScanQr,
    val importButtonEnabled: Boolean = false,
    val importSoftwareAccountsEnabled: Boolean = false,
    val triggerCameraPermissionPrompt: Boolean = false,
    val olympiaAccounts: ImmutableList<Selectable<OlympiaAccountDetails>> = persistentListOf(),
    val seedPhrase: String = "",
    val bip39Passphrase: String = ""
) : UiState
