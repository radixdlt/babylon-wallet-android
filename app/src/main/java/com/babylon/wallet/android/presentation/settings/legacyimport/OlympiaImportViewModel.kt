@file:Suppress("LongParameterList")

package com.babylon.wallet.android.presentation.settings.legacyimport

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.createaccount.withledger.UseLedgerDelegate
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import com.babylon.wallet.android.utils.getLedgerDeviceModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.validatePublicKeysOf
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.AddOlympiaFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.CheckOlympiaFactorSourceForAccountsExistUseCase
import rdx.works.profile.domain.account.MigrateOlympiaAccountsUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.currentNetworkAccountHashes
import rdx.works.profile.olympiaimport.ChunkInfo
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import rdx.works.profile.olympiaimport.OlympiaAccountType
import rdx.works.profile.olympiaimport.OlympiaWalletData
import rdx.works.profile.olympiaimport.OlympiaWalletDataParser
import rdx.works.profile.olympiaimport.olympiaTestSeedPhrase
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class OlympiaImportViewModel @Inject constructor(
    private val ledgerMessenger: LedgerMessenger,
    private val addOlympiaFactorSourceUseCase: AddOlympiaFactorSourceUseCase,
    addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
    private val migrateOlympiaAccountsUseCase: MigrateOlympiaAccountsUseCase,
    private val checkOlympiaFactorSourceForAccountsExistUseCase: CheckOlympiaFactorSourceForAccountsExistUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val deviceSecurityHelper: DeviceSecurityHelper,
    private val olympiaWalletDataParser: OlympiaWalletDataParser
) : StateViewModel<OlympiaImportUiState>(), OneOffEventHandler<OlympiaImportEvent> by OneOffEventHandlerImpl() {

    private var mnemonicWithPassphrase: MnemonicWithPassphrase? = null
    private val scannedData = mutableSetOf<String>()
    private var olympiaWalletData: OlympiaWalletData? = null
    private val initialPages = listOf(ImportPage.ScanQr, ImportPage.AccountList, ImportPage.ImportComplete)
    private var existingFactorSourceId: FactorSource.ID? = null
    private val validatedHardwareAccounts = mutableMapOf<FactorSource, List<OlympiaAccountDetails>>()

    private val useLedgerDelegate = UseLedgerDelegate(
        getProfileUseCase = getProfileUseCase,
        ledgerMessenger = ledgerMessenger,
        addLedgerFactorSourceUseCase = addLedgerFactorSourceUseCase,
        scope = viewModelScope,
        onUseLedger = { ledgerFactorSource ->
            sendEvent(OlympiaImportEvent.UseLedger)
            onUseLedger(ledgerFactorSource)
        }
    )

    init {
        viewModelScope.launch {
            useLedgerDelegate.state.collect { delegateState ->
                _state.update { uiState ->
                    uiState.copy(
                        addLedgerSheetState = delegateState.addLedgerSheetState,
                        usedLedgerFactorSources = delegateState.usedLedgerFactorSources,
                        waitingForLedgerResponse = delegateState.waitingForLedgerResponse,
                        recentlyConnectedLedgerDevice = delegateState.recentlyConnectedLedgerDevice,
                        hasP2pLinks = delegateState.hasP2pLinks,
                        uiMessage = delegateState.uiMessage
                    )
                }
            }
        }
    }

    private suspend fun processLedgerResponse(
        ledgerFactorSource: FactorSource,
        ledgerResponse: MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse
    ) {
        _state.update { it.copy(waitingForLedgerResponse = false) }
        val hardwareAccountsToMigrate = hardwareAccountsLeftToMigrate()
        val derivedKeys = ledgerResponse.publicKeysHex.map { it.publicKeyHex }.toSet()
        val validatedAccounts = hardwareAccountsToMigrate.filter { derivedKeys.contains(it.publicKey) }
        if (validatedAccounts.isEmpty()) {
            _state.update { it.copy(uiMessage = UiMessage.InfoMessage.NoAccountsForLedger) }
            return
        }
        validatedHardwareAccounts[ledgerFactorSource] = validatedAccounts
        updateHardwareAccountLeftToMigrateCount()
        val hardwareAccountsLeftToMigrate = hardwareAccountsLeftToMigrate()
        if (hardwareAccountsLeftToMigrate.isEmpty()) {
            viewModelScope.launch {
                internalImportOlympiaAccounts()
            }
        }
    }

    fun onQrCodeScanned(qrData: String) {
        if (!olympiaWalletDataParser.isProperQrPayload(qrData)) {
            _state.update {
                it.copy(uiMessage = UiMessage.InfoMessage.InvalidPayload)
            }
            return
        }
        scannedData.add(qrData)
        if (!olympiaWalletDataParser.verifyPayload(scannedData)) {
            _state.update {
                it.copy(qrChunkInfo = olympiaWalletDataParser.chunkInfo(scannedData))
            }
            return
        }

        viewModelScope.launch {
            val olympiaWalletData = olympiaWalletDataParser.parseOlympiaWalletAccountData(
                olympiaWalletDataChunks = scannedData,
                existingAccountHashes = getProfileUseCase.currentNetworkAccountHashes()
            )
            olympiaWalletData?.let { data ->
                this@OlympiaImportViewModel.olympiaWalletData = data
                val allImported = data.accountData.all { it.alreadyImported }
                val nextPage = if (allImported) ImportPage.ImportComplete else ImportPage.AccountList
                _state.update { state ->
                    state.copy(
                        currentPage = nextPage,
                        olympiaAccounts = data.accountData.toPersistentList(),
                        importButtonEnabled = data.accountData.any { !it.alreadyImported },
                        totalHardwareAccounts = data.accountData.count { it.type == OlympiaAccountType.Hardware },
                        migratedAccounts = if (allImported) {
                            data.accountData.mapNotNull { getProfileUseCase.accountOnCurrentNetwork(it.newBabylonAddress)?.toUiModel() }
                                .toPersistentList()
                        } else {
                            persistentListOf()
                        }
                    )
                }
                sendEvent(OlympiaImportEvent.NextPage(nextPage))
            }
        }
    }

    fun onBackClick() {
        previousPage()
    }

    @Suppress("MagicNumber")
    private fun nextPage(page: ImportPage? = null) {
        viewModelScope.launch {
            if (page != null) {
                _state.update { it.copy(currentPage = page, hideBack = page == ImportPage.ImportComplete) }
                delay(300)
                sendEvent(OlympiaImportEvent.NextPage(page))
            } else {
                val currentPage = _state.value.currentPage
                _state.value.pages.nextPage(currentPage)?.let { nextPage ->
                    _state.update { it.copy(currentPage = nextPage, hideBack = nextPage == ImportPage.ImportComplete) }
                    delay(300)
                    sendEvent(OlympiaImportEvent.NextPage(nextPage))
                }
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
        var pages = listOf(ImportPage.ScanQr, ImportPage.AccountList)
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
            !it.alreadyImported
        }
        val pages = buildPages(selectedAccounts)
        updateHardwareAccountLeftToMigrateCount()
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
                        internalImportOlympiaAccounts()
                        nextPage()
                    }
                }
            } else {
                _state.update { it.copy(uiMessage = UiMessage.InfoMessage.InvalidMnemonic) }
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private suspend fun internalImportOlympiaAccounts() {
        val softwareAccountsToMigrate = softwareAccountsToMigrate()
        if (softwareAccountsToMigrate.isEmpty() && validatedHardwareAccounts.isEmpty()) {
            nextPage()
            return
        }
        if (softwareAccountsToMigrate.isNotEmpty()) {
            val factorSourceID = existingFactorSourceId ?: addOlympiaFactorSourceUseCase(mnemonicWithPassphrase!!)
            migrateOlympiaAccountsUseCase(softwareAccountsToMigrate, factorSourceID)
        }
        if (validatedHardwareAccounts.isNotEmpty()) {
            validatedHardwareAccounts.entries.forEach { entry ->
                migrateOlympiaAccountsUseCase(entry.value, entry.key.id)
            }
        }
        _state.update { state ->
            state.copy(
                migratedAccounts = state.olympiaAccounts.mapNotNull {
                    getProfileUseCase.accountOnCurrentNetwork(it.newBabylonAddress)?.toUiModel()
                }.toPersistentList()
            )
        }
        nextPage()
    }

    private fun updateHardwareAccountLeftToMigrateCount() {
        val hardwareAccountsLeftToImport = hardwareAccountsLeftToMigrate().map {
            it.address
        }.toSet().minus(validatedHardwareAccounts.values.flatten().map { it.address }.toSet()).size
        _state.update { it.copy(hardwareAccountsLeftToImport = hardwareAccountsLeftToImport) }
    }

    fun onConfirmLedgerName(name: String) {
        useLedgerDelegate.onConfirmLedgerName(name)
    }

    fun onUseLedger(ledgerFactorSource: FactorSource) {
        updateHardwareAccountLeftToMigrateCount()
        viewModelScope.launch {
            _state.update { it.copy(waitingForLedgerResponse = true) }
            val hardwareAccountsDerivationPaths = hardwareAccountsLeftToMigrate().map { it.derivationPath.path }
            val interactionId = UUIDGenerator.uuid().toString()
            val deviceModel = requireNotNull(ledgerFactorSource.getLedgerDeviceModel())
            val ledgerDevice = DerivePublicKeyRequest.LedgerDevice(
                name = ledgerFactorSource.label,
                model = deviceModel,
                id = ledgerFactorSource.id.value
            )
            ledgerMessenger.sendDerivePublicKeyRequest(
                interactionId = interactionId,
                keyParameters = hardwareAccountsDerivationPaths.map { derivationPath ->
                    DerivePublicKeyRequest.KeyParameters(Curve.Secp256k1, derivationPath)
                },
                ledgerDevice = ledgerDevice
            ).onFailure { error ->
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage.from(error), waitingForLedgerResponse = false) }
            }.onSuccess { r ->
                processLedgerResponse(ledgerFactorSource, r)
            }
        }
    }

    fun onMnemonicAlreadyImported() {
        val softwareAccountsToMigrate = softwareAccountsToMigrate()
        viewModelScope.launch {
            val factorSourceId = checkOlympiaFactorSourceForAccountsExistUseCase(softwareAccountsToMigrate)
            if (factorSourceId == null) {
                _state.update { state ->
                    state.copy(uiMessage = UiMessage.InfoMessage.NoMnemonicForAccounts)
                }
                return@launch
            }
            existingFactorSourceId = factorSourceId
            sendEvent(OlympiaImportEvent.BiometricPrompt)
        }
    }

    private fun softwareAccountsToMigrate(): List<OlympiaAccountDetails> {
        val softwareAccountsToMigrate = _state.value.olympiaAccounts.filter {
            it.type == OlympiaAccountType.Software && !it.alreadyImported
        }
        return softwareAccountsToMigrate
    }

    private fun hardwareAccountsLeftToMigrate(): List<OlympiaAccountDetails> {
        val alreadyValidatedKeys = validatedHardwareAccounts.values.flatten().map { it.publicKey }
        val hardwareAccountsToMigrate = _state.value.olympiaAccounts.filter {
            it.type == OlympiaAccountType.Hardware && !it.alreadyImported && !alreadyValidatedKeys.contains(
                it.publicKey
            )
        }
        return hardwareAccountsToMigrate
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

    fun onSendAddLedgerRequest() {
        useLedgerDelegate.onSendAddLedgerRequest()
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
    object UseLedger : OlympiaImportEvent
    object BiometricPrompt : OlympiaImportEvent
}

data class OlympiaImportUiState(
    val isLoading: Boolean = false,
    val pages: ImmutableList<ImportPage> = persistentListOf(),
    val currentPage: ImportPage = ImportPage.ScanQr,
    val importButtonEnabled: Boolean = false,
    val importSoftwareAccountsEnabled: Boolean = true,
    val olympiaAccounts: ImmutableList<OlympiaAccountDetails> = persistentListOf(),
    val seedPhrase: String = "",
    val bip39Passphrase: String = "",
    val uiMessage: UiMessage? = null,
    val migratedAccounts: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val hideBack: Boolean = false,
    val qrChunkInfo: ChunkInfo? = null,
    val isDeviceSecure: Boolean = true,
    val hardwareAccountsLeftToImport: Int = 0,
    val waitingForLedgerResponse: Boolean = false,
    val hasP2pLinks: Boolean = false,
    val usedLedgerFactorSources: ImmutableList<FactorSource> = persistentListOf(),
    val recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null,
    val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.Connect,
    val totalHardwareAccounts: Int = 0
) : UiState
