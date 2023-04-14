package com.babylon.wallet.android.presentation.settings.legacyimport

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.InfoMessageType
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.utils.DeviceSecurityHelper
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
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.domain.AddOlympiaFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.CheckIfFactorSourceForAccountsExistUseCase
import rdx.works.profile.domain.account.MigrateOlympiaAccountsUseCase
import rdx.works.profile.domain.currentNetworkAccountHashes
import rdx.works.profile.olympiaimport.ChunkInfo
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import rdx.works.profile.olympiaimport.OlympiaAccountType
import rdx.works.profile.olympiaimport.OlympiaWalletData
import rdx.works.profile.olympiaimport.chunkInfo
import rdx.works.profile.olympiaimport.isProperQrPayload
import rdx.works.profile.olympiaimport.olympiaTestSeedPhrase
import rdx.works.profile.olympiaimport.parseOlympiaWalletAccountData
import rdx.works.profile.olympiaimport.validatePublicKeysOf
import rdx.works.profile.olympiaimport.verifyPayload
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class OlympiaImportViewModel @Inject constructor(
    private val addOlympiaFactorSourceUseCase: AddOlympiaFactorSourceUseCase,
    private val migrateOlympiaAccountsUseCase: MigrateOlympiaAccountsUseCase,
    private val checkIfFactorSourceForAccountsExistUseCase: CheckIfFactorSourceForAccountsExistUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val deviceSecurityHelper: DeviceSecurityHelper
) : StateViewModel<OlympiaImportUiState>(),
    OneOffEventHandler<OlympiaImportEvent> by OneOffEventHandlerImpl() {

    private var mnemonicWithPassphrase: MnemonicWithPassphrase? = null
    private val scannedData = mutableSetOf<String>()
    private var olympiaWalletData: OlympiaWalletData? = null
    private val initialPages = listOf(ImportPage.ScanQr, ImportPage.AccountList)
    private var existingFactorSourceId: FactorSource.ID? = null

    fun onQrCodeScanned(qrData: String) {
        if (!qrData.isProperQrPayload()) {
            _state.update {
                it.copy(uiMessage = UiMessage.InfoMessage(InfoMessageType.InvalidPayload))
            }
            return
        }
        scannedData.add(qrData)
        if (!scannedData.verifyPayload()) {
            _state.update {
                it.copy(qrChunkInfo = scannedData.chunkInfo())
            }
            return
        }

        viewModelScope.launch {
            val olympiaWalletData = scannedData.parseOlympiaWalletAccountData(
                getProfileUseCase.currentNetworkAccountHashes()
            )
            olympiaWalletData?.let { data ->
                this@OlympiaImportViewModel.olympiaWalletData = data
                _state.update { state ->
                    state.copy(
                        currentPage = ImportPage.AccountList,
                        olympiaAccounts = data.accountData.map { Selectable(it) }.toPersistentList()
                    )
                }
                sendEvent(OlympiaImportEvent.NextPage(ImportPage.AccountList))
            }
        }
    }

    fun onBackClick() {
        previousPage()
    }

    @Suppress("MagicNumber")
    private fun nextPage() {
        viewModelScope.launch {
            val currentPage = _state.value.currentPage
            _state.value.pages.nextPage(currentPage)?.let { nextPage ->
                _state.update { it.copy(currentPage = nextPage, hideBack = nextPage == ImportPage.ImportComplete) }
                delay(300)
                sendEvent(OlympiaImportEvent.NextPage(nextPage))
            }
        }
    }

    private fun previousPage() {
        viewModelScope.launch {
            val newPage = _state.value.pages.previousPage(_state.value.currentPage)
            if (newPage == ImportPage.ScanQr) {
                scannedData.clear()
                _state.update { it.copy(qrChunkInfo = null) }
            }
            newPage?.let { page -> _state.update { it.copy(currentPage = page) } }
            sendEvent(OlympiaImportEvent.PreviousPage(newPage))
        }
    }

    private fun buildPages(olympiaAccounts: List<OlympiaAccountDetails>): PersistentList<ImportPage> {
        val hasSoftwareAccounts = olympiaAccounts.any { it.type == OlympiaAccountType.Software }
        val hasHardwareAccounts = olympiaAccounts.any { it.type == OlympiaAccountType.Hardware }
        var pages = initialPages
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
            state.copy(importSoftwareAccountsEnabled = olympiaWalletData?.mnemonicWordCount == words.size)
        }
    }

    fun onImportAccounts() {
        val selectedAccounts = _state.value.olympiaAccounts.filter {
            !it.data.alreadyImported && it.selected
        }.map { it.data }
        val pages = buildPages(selectedAccounts)
        _state.update { state ->
            state.copy(pages = pages)
        }
        nextPage()
    }

    @Suppress("UnsafeCallOnNullableType")
    fun onImportSoftwareAccounts() {
        viewModelScope.launch {
            val softwareAccountsToMigrate = softwareAccountsToMigrate()
            mnemonicWithPassphrase = MnemonicWithPassphrase(_state.value.seedPhrase, _state.value.bip39Passphrase)
            val accountsValid =
                existingFactorSourceId != null || mnemonicWithPassphrase?.validatePublicKeysOf(softwareAccountsToMigrate) == true
            if (accountsValid) {
                when (_state.value.pages.nextPage(_state.value.currentPage)) {
                    ImportPage.HardwareAccount -> nextPage()
                    else -> {
                        internalImportSoftwareAccounts()
                        nextPage()
                    }
                }
            } else {
                _state.update { it.copy(uiMessage = UiMessage.InfoMessage(InfoMessageType.InvalidMnemonic)) }
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private suspend fun internalImportSoftwareAccounts() {
        val softwareAccountsToMigrate = softwareAccountsToMigrate()
        val factorSourceID = existingFactorSourceId ?: addOlympiaFactorSourceUseCase(mnemonicWithPassphrase!!)
        val migratedAccounts = migrateOlympiaAccountsUseCase(softwareAccountsToMigrate, factorSourceID)
        _state.update { state ->
            state.copy(migratedAccounts = migratedAccounts.map { it.toUiModel() }.toPersistentList())
        }
        nextPage()
    }

    fun onToggleSelectAll() {
        val selectedAll = _state.value.olympiaAccounts.filter { !it.data.alreadyImported }.all { it.selected }
        _state.update { state ->
            state.copy(
                olympiaAccounts = state.olympiaAccounts.mapWhen(
                    predicate = { !it.data.alreadyImported }
                ) { it.copy(selected = !selectedAll) }.toPersistentList()
            )
        }
    }

    fun onHardwareImport() {
        viewModelScope.launch {
            internalImportSoftwareAccounts()
        }
    }

    fun onMnemonicAlreadyImported() {
        val softwareAccountsToMigrate = softwareAccountsToMigrate()
        viewModelScope.launch {
            val factorSourceId = checkIfFactorSourceForAccountsExistUseCase(softwareAccountsToMigrate)
            if (factorSourceId == null) {
                _state.update { state ->
                    state.copy(uiMessage = UiMessage.InfoMessage(InfoMessageType.NoMnemonicForAccounts))
                }
                return@launch
            }
            existingFactorSourceId = factorSourceId
            sendEvent(OlympiaImportEvent.BiometricPrompt)
        }
    }

    private fun softwareAccountsToMigrate(): List<OlympiaAccountDetails> {
        val softwareAccountsToMigrate = _state.value.olympiaAccounts.filter {
            it.selected && it.data.type == OlympiaAccountType.Software
        }.map { it.data }
        return softwareAccountsToMigrate
    }

    // TODO mnemonic added for ease of testing
    override fun initialState(): OlympiaImportUiState {
        return OlympiaImportUiState(
            seedPhrase = olympiaTestSeedPhrase,
            pages = initialPages.toPersistentList(),
            isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
        )
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }
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
    object BiometricPrompt : OlympiaImportEvent
}

data class OlympiaImportUiState(
    val isLoading: Boolean = false,
    val pages: ImmutableList<ImportPage> = persistentListOf(),
    val currentPage: ImportPage = ImportPage.ScanQr,
    val importButtonEnabled: Boolean = false,
    val importSoftwareAccountsEnabled: Boolean = true,
    val olympiaAccounts: ImmutableList<Selectable<OlympiaAccountDetails>> = persistentListOf(),
    val seedPhrase: String = "",
    val bip39Passphrase: String = "",
    val uiMessage: UiMessage? = null,
    val migratedAccounts: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val hideBack: Boolean = false,
    val qrChunkInfo: ChunkInfo? = null,
    val isDeviceSecure: Boolean = true
) : UiState
