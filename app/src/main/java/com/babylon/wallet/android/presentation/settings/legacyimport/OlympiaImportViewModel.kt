@file:Suppress("LongParameterList")

package com.babylon.wallet.android.presentation.settings.legacyimport

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel.Companion.getLedgerDeviceModel
import com.babylon.wallet.android.domain.model.AppConstants.DELAY_300_MS
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.createaccount.withledger.UseLedgerDelegate
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.validatePublicKeysOf
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.AddOlympiaFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.GetFactorSourceIdForOlympiaAccountsUseCase
import rdx.works.profile.domain.account.MigrateOlympiaAccountsUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.currentNetworkAccountHashes
import rdx.works.profile.olympiaimport.ChunkInfo
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import rdx.works.profile.olympiaimport.OlympiaAccountType
import rdx.works.profile.olympiaimport.OlympiaWalletData
import rdx.works.profile.olympiaimport.OlympiaWalletDataParser
import timber.log.Timber
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class OlympiaImportViewModel @Inject constructor(
    private val ledgerMessenger: LedgerMessenger,
    private val addOlympiaFactorSourceUseCase: AddOlympiaFactorSourceUseCase,
    addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
    private val migrateOlympiaAccountsUseCase: MigrateOlympiaAccountsUseCase,
    private val getFactorSourceIdForOlympiaAccountsUseCase: GetFactorSourceIdForOlympiaAccountsUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val deviceSecurityHelper: DeviceSecurityHelper,
    private val olympiaWalletDataParser: OlympiaWalletDataParser
) : StateViewModel<OlympiaImportUiState>(), OneOffEventHandler<OlympiaImportEvent> by OneOffEventHandlerImpl() {

    private var mnemonicWithPassphrase: MnemonicWithPassphrase? = null
    private val scannedData = mutableSetOf<String>()
    private var olympiaWalletData: OlympiaWalletData? = null
    private val initialPages = listOf(
        OlympiaImportUiState.Page.ScanQr,
        OlympiaImportUiState.Page.AccountsToImportList,
        OlympiaImportUiState.Page.ImportComplete
    )
    private var existingFactorSourceId: FactorSourceID.FromHash? = null
    private val verifiedHardwareAccounts = mutableMapOf<FactorSource, List<OlympiaAccountDetails>>()

    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

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
                    val state = delegateState.addLedgerSheetState
                    uiState.copy(
                        addLedgerSheetState = state,
                        shouldShowBottomSheet = state == AddLedgerSheetState.InputLedgerName,
                        waitingForLedgerResponse = delegateState.waitingForLedgerResponse,
                        recentlyConnectedLedgerDevice = delegateState.recentlyConnectedLedgerDevice,
                        uiMessage = delegateState.uiMessage
                    )
                }
            }
        }
        viewModelScope.launch {
            seedPhraseInputDelegate.state.collect { delegateState ->
                _state.update { uiState ->
                    uiState.copy(
                        bip39Passphrase = delegateState.bip39Passphrase,
                        seedPhraseWords = delegateState.seedPhraseWords,
                        wordAutocompleteCandidates = delegateState.wordAutocompleteCandidates
                    )
                }
            }
        }
    }

    private suspend fun processLedgerResponse(
        ledgerFactorSource: LedgerHardwareWalletFactorSource,
        derivePublicKeyResponse: MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse
    ) {
        _state.update { it.copy(waitingForLedgerResponse = false) }
        val hardwareAccountsToMigrate = hardwareAccountsLeftToMigrate()
        val derivedKeys = derivePublicKeyResponse.publicKeysHex.map { it.publicKeyHex }.toSet()
        val verifiedAccounts = hardwareAccountsToMigrate.filter { derivedKeys.contains(it.publicKey) }
        if (verifiedAccounts.isEmpty()) {
            _state.update {
                it.copy(
                    uiMessage = UiMessage.InfoMessage.NoAccountsForLedger,
                    shouldShowBottomSheet = true
                )
            }
            return
        }
        _state.update {
            it.copy(
                verifiedLedgerDevices = (it.verifiedLedgerDevices + ledgerFactorSource)
                    .distinct()
                    .toPersistentList()
            )
        }
        verifiedHardwareAccounts[ledgerFactorSource] = verifiedAccounts
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
                seedPhraseInputDelegate.setSeedPhraseSize(data.mnemonicWordCount)
                val allImported = data.accountData.all { it.alreadyImported }
                val nextPage =
                    if (allImported) OlympiaImportUiState.Page.ImportComplete else OlympiaImportUiState.Page.AccountsToImportList
                _state.update { state ->
                    state.copy(
                        currentPage = nextPage,
                        olympiaAccountsToImport = data.accountData.toPersistentList(),
                        importButtonEnabled = data.accountData.any { !it.alreadyImported },
                        migratedAccounts = if (allImported) {
                            data.accountData.mapNotNull {
                                getProfileUseCase.accountOnCurrentNetwork(it.newBabylonAddress)?.toUiModel()
                            }
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
        backToPreviousPage()
    }

    private fun proceedToNextPage() {
        viewModelScope.launch {
            val currentPage = _state.value.currentPage
            _state.value.pages.nextPage(currentPage)?.let { nextPage ->
                _state.update {
                    it.copy(
                        currentPage = nextPage,
                        hideBack = nextPage == OlympiaImportUiState.Page.ImportComplete
                    )
                }
                delay(DELAY_300_MS)
                if (nextPage == OlympiaImportUiState.Page.MnemonicInput) {
                    if (checkIfMnemonicForSoftwareAccountsExists()) {
                        sendEvent(OlympiaImportEvent.BiometricPrompt)
                        return@launch
                    }
                }
                sendEvent(OlympiaImportEvent.NextPage(nextPage))
            }
        }
    }

    private fun backToPreviousPage() {
        viewModelScope.launch {
            val newPage = _state.value.pages.previousPage(_state.value.currentPage)
            if (newPage == OlympiaImportUiState.Page.ScanQr) {
                scannedData.clear()
                _state.update { it.copy(qrChunkInfo = null) }
            }
            newPage?.let { page -> _state.update { it.copy(currentPage = page) } }
            sendEvent(OlympiaImportEvent.PreviousPage(newPage))
        }
    }

    private fun buildPages(olympiaAccounts: List<OlympiaAccountDetails>): PersistentList<OlympiaImportUiState.Page> {
        val hasSoftwareAccounts = olympiaAccounts.any { it.type == OlympiaAccountType.Software }
        val hasHardwareAccounts = olympiaAccounts.any { it.type == OlympiaAccountType.Hardware }
        var pages = listOf(OlympiaImportUiState.Page.ScanQr, OlympiaImportUiState.Page.AccountsToImportList)
        when {
            hasSoftwareAccounts -> {
                pages = pages + listOf(OlympiaImportUiState.Page.MnemonicInput)
                if (hasHardwareAccounts) {
                    pages = pages + listOf(OlympiaImportUiState.Page.HardwareAccounts)
                }
            }

            hasHardwareAccounts -> {
                pages = pages + listOf(OlympiaImportUiState.Page.HardwareAccounts)
            }

            else -> {}
        }
        return (pages + listOf(OlympiaImportUiState.Page.ImportComplete)).toPersistentList()
    }

    fun onWordChanged(index: Int, value: String) {
        Timber.d("Seed phrase: $index $value")
        seedPhraseInputDelegate.onWordChanged(index, value) {
            sendEvent(OlympiaImportEvent.MoveFocusToNextWord)
        }
    }

    fun onPassphraseChanged(value: String) {
        seedPhraseInputDelegate.onPassphraseChanged(value)
    }

    fun onImportAccounts() {
        val selectedAccounts = _state.value.olympiaAccountsToImport.filter {
            !it.alreadyImported
        }
        val pages = buildPages(selectedAccounts)
        updateHardwareAccountLeftToMigrateCount()
        _state.update { state ->
            state.copy(pages = pages)
        }
        proceedToNextPage()
    }

    fun onImportSoftwareAccounts() {
        viewModelScope.launch {
            val softwareAccountsToMigrate = softwareAccountsToMigrate()
            mnemonicWithPassphrase =
                MnemonicWithPassphrase(
                    mnemonic = _state.value.seedPhraseWords.joinToString(" ") { it.value },
                    bip39Passphrase = _state.value.bip39Passphrase
                )
            val accountsValid = existingFactorSourceId != null ||
                mnemonicWithPassphrase?.validatePublicKeysOf(softwareAccountsToMigrate) == true
            if (accountsValid) {
                when (_state.value.pages.nextPage(_state.value.currentPage)) {
                    OlympiaImportUiState.Page.HardwareAccounts -> proceedToNextPage()
                    else -> {
                        internalImportOlympiaAccounts()
                        proceedToNextPage()
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
        if (softwareAccountsToMigrate.isEmpty() && verifiedHardwareAccounts.isEmpty()) {
            proceedToNextPage()
            return
        }
        if (softwareAccountsToMigrate.isNotEmpty()) {
            val factorSourceID = existingFactorSourceId ?: addOlympiaFactorSourceUseCase(mnemonicWithPassphrase!!)
            migrateOlympiaAccountsUseCase(
                olympiaAccounts = softwareAccountsToMigrate,
                factorSourceId = factorSourceID
            )
        }
        if (verifiedHardwareAccounts.isNotEmpty()) {
            verifiedHardwareAccounts.entries.forEach { entry ->
                migrateOlympiaAccountsUseCase(
                    olympiaAccounts = entry.value,
                    factorSourceId = entry.key.id as FactorSourceID.FromHash
                )
            }
        }
        _state.update { state ->
            state.copy(
                migratedAccounts = state.olympiaAccountsToImport.mapNotNull {
                    getProfileUseCase.accountOnCurrentNetwork(it.newBabylonAddress)?.toUiModel()
                }.toPersistentList()
            )
        }
        proceedToNextPage()
    }

    private fun updateHardwareAccountLeftToMigrateCount() {
        val hardwareAccountsLeftToImport = hardwareAccountsLeftToMigrate().map {
            it.address
        }.toSet().minus(verifiedHardwareAccounts.values.flatten().map { it.address }.toSet()).size
        _state.update { it.copy(hardwareAccountsLeftToImport = hardwareAccountsLeftToImport) }
    }

    fun onConfirmLedgerName(name: String) {
        useLedgerDelegate.onConfirmLedgerName(name)
    }

    private fun onUseLedger(ledgerFactorSource: LedgerHardwareWalletFactorSource) {
        updateHardwareAccountLeftToMigrateCount()
        viewModelScope.launch {
            _state.update { it.copy(waitingForLedgerResponse = true) }
            val hardwareAccountsDerivationPaths = hardwareAccountsLeftToMigrate().map { it.derivationPath.path }
            val interactionId = UUIDGenerator.uuid().toString()
            val deviceModel = requireNotNull(ledgerFactorSource.getLedgerDeviceModel())
            val ledgerDevice = DerivePublicKeyRequest.LedgerDevice(
                name = ledgerFactorSource.hint.name,
                model = deviceModel,
                id = ledgerFactorSource.id.body.value
            )
            ledgerMessenger.sendDerivePublicKeyRequest(
                interactionId = interactionId,
                keyParameters = hardwareAccountsDerivationPaths.map { derivationPath ->
                    DerivePublicKeyRequest.KeyParameters(Curve.Secp256k1, derivationPath)
                },
                ledgerDevice = ledgerDevice
            ).onFailure { error ->
                _state.update {
                    it.copy(
                        uiMessage = UiMessage.ErrorMessage.from(error),
                        waitingForLedgerResponse = false
                    )
                }
            }.onSuccess { derivePublicKeyResponse ->
                processLedgerResponse(ledgerFactorSource, derivePublicKeyResponse)
            }
        }
    }

    private suspend fun checkIfMnemonicForSoftwareAccountsExists(): Boolean {
        val softwareAccountsToMigrate = softwareAccountsToMigrate()
        if (softwareAccountsToMigrate.isEmpty()) { // if no software accounts then no need to check
            return false
        }
        val factorSourceId = getFactorSourceIdForOlympiaAccountsUseCase(softwareAccountsToMigrate) ?: return false

        existingFactorSourceId = factorSourceId
        return true
    }

    private fun softwareAccountsToMigrate(): List<OlympiaAccountDetails> {
        val softwareAccountsToMigrate = _state.value.olympiaAccountsToImport.filter {
            it.type == OlympiaAccountType.Software && !it.alreadyImported
        }
        return softwareAccountsToMigrate
    }

    private fun hardwareAccountsLeftToMigrate(): List<OlympiaAccountDetails> {
        val alreadyVerifiedKeys = verifiedHardwareAccounts.values.flatten().map { it.publicKey }
        val hardwareAccountsToMigrate = _state.value.olympiaAccountsToImport.filter {
            it.type == OlympiaAccountType.Hardware && !it.alreadyImported && !alreadyVerifiedKeys.contains(
                it.publicKey
            )
        }
        return hardwareAccountsToMigrate
    }

    override fun initialState(): OlympiaImportUiState {
        return OlympiaImportUiState(
            seedPhraseWords = persistentListOf(),
            pages = initialPages.toPersistentList(),
            isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
        )
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onImportWithLedger() {
        viewModelScope.launch {
            if (useLedgerDelegate.state.first().hasP2PLinks) {
                useLedgerDelegate.onSendAddLedgerRequest()
            } else {
                _state.update {
                    it.copy(
                        addLedgerSheetState = AddLedgerSheetState.LinkConnector,
                        shouldShowBottomSheet = true,
                    )
                }
            }
        }
    }

    fun onHideBottomSheet() {
        viewModelScope.launch {
            _state.update {
                it.copy(shouldShowBottomSheet = false)
            }
        }
    }
}

sealed interface OlympiaImportEvent : OneOffEvent {
    object MoveFocusToNextWord : OlympiaImportEvent
    data class NextPage(val page: OlympiaImportUiState.Page) : OlympiaImportEvent
    data class PreviousPage(val page: OlympiaImportUiState.Page?) : OlympiaImportEvent
    object UseLedger : OlympiaImportEvent
    object BiometricPrompt : OlympiaImportEvent
}

data class OlympiaImportUiState(
    val pages: ImmutableList<Page> = persistentListOf(),
    val currentPage: Page = Page.ScanQr,
    val importButtonEnabled: Boolean = false,
    val olympiaAccountsToImport: ImmutableList<OlympiaAccountDetails> = persistentListOf(),
    val bip39Passphrase: String = "",
    val uiMessage: UiMessage? = null,
    val migratedAccounts: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val hideBack: Boolean = false,
    val qrChunkInfo: ChunkInfo? = null,
    val isDeviceSecure: Boolean = true,
    val hardwareAccountsLeftToImport: Int = 0,
    val waitingForLedgerResponse: Boolean = false,
    val verifiedLedgerDevices: ImmutableList<LedgerHardwareWalletFactorSource> = persistentListOf(),
    val recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null,
    val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.Connect,
    val seedPhraseWords: ImmutableList<SeedPhraseInputDelegate.SeedPhraseWord> = persistentListOf(),
    val wordAutocompleteCandidates: ImmutableList<String> = persistentListOf(),
    val shouldShowBottomSheet: Boolean = false
) : UiState {

    enum class Page {
        ScanQr, AccountsToImportList, MnemonicInput, HardwareAccounts, ImportComplete
    }
}

private fun List<OlympiaImportUiState.Page>.nextPage(currentPage: OlympiaImportUiState.Page): OlympiaImportUiState.Page? {
    val currentIndex = indexOf(currentPage)
    if (currentIndex == -1 || currentIndex == size - 1) return null
    return this[currentIndex + 1]
}

private fun List<OlympiaImportUiState.Page>.previousPage(currentPage: OlympiaImportUiState.Page): OlympiaImportUiState.Page? {
    val currentIndex = indexOf(currentPage)
    if (currentIndex == -1 || currentIndex == 0) return null
    return this[currentIndex - 1]
}
