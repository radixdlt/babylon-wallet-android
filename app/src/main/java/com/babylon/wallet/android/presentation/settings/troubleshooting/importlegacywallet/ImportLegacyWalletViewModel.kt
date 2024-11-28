@file:Suppress("LongParameterList")

package com.babylon.wallet.android.presentation.settings.troubleshooting.importlegacywallet

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.domain.model.messages.LedgerResponse
import com.babylon.wallet.android.domain.usecases.BiometricsAuthenticateUseCase
import com.babylon.wallet.android.domain.usecases.settings.MarkImportOlympiaWalletCompleteUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import com.babylon.wallet.android.presentation.settings.securitycenter.ledgerhardwarewallets.AddLedgerDeviceUiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.Constants.DELAY_300_MS
import com.babylon.wallet.android.utils.Constants.ENTITY_NAME_MAX_LENGTH
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.validate
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
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.core.sargon.supportsOlympia
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.AddOlympiaFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.account.GetFactorSourceIdForOlympiaAccountsUseCase
import rdx.works.profile.domain.account.MigrateOlympiaAccountsUseCase
import rdx.works.profile.olympiaimport.ChunkInfo
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import rdx.works.profile.olympiaimport.OlympiaAccountType
import rdx.works.profile.olympiaimport.OlympiaWalletData
import rdx.works.profile.olympiaimport.OlympiaWalletDataParser
import timber.log.Timber
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class ImportLegacyWalletViewModel @Inject constructor(
    private val ledgerMessenger: LedgerMessenger,
    private val addOlympiaFactorSourceUseCase: AddOlympiaFactorSourceUseCase,
    addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
    private val migrateOlympiaAccountsUseCase: MigrateOlympiaAccountsUseCase,
    private val getFactorSourceIdForOlympiaAccountsUseCase: GetFactorSourceIdForOlympiaAccountsUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val olympiaWalletDataParser: OlympiaWalletDataParser,
    private val markImportOlympiaWalletCompleteUseCase: MarkImportOlympiaWalletCompleteUseCase,
    private val appEventBus: AppEventBus,
    private val p2PLinksRepository: P2PLinksRepository,
    private val biometricsAuthenticateUseCase: BiometricsAuthenticateUseCase,
) : StateViewModel<ImportLegacyWalletUiState>(), OneOffEventHandler<OlympiaImportEvent> by OneOffEventHandlerImpl() {

    private val scannedData = mutableSetOf<String>()
    private var olympiaWalletData: OlympiaWalletData? = null
    private val initialPages = listOf(
        ImportLegacyWalletUiState.Page.ScanQr,
        ImportLegacyWalletUiState.Page.AccountsToImportList,
        ImportLegacyWalletUiState.Page.ImportComplete
    )
    private val verifiedHardwareAccounts = mutableMapOf<FactorSource, List<OlympiaAccountDetails>>()

    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    private val useLedgerDelegate = UseLedgerDelegate(
        getProfileUseCase = getProfileUseCase,
        ledgerMessenger = ledgerMessenger,
        addLedgerFactorSourceUseCase = addLedgerFactorSourceUseCase,
        scope = viewModelScope,
        onUseLedger = { ledgerFactorSource ->
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
                        shouldShowAddLedgerDeviceScreen = state == AddLedgerDeviceUiState.ShowContent.NameLedgerDevice,
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
                        seedPhraseInputState = delegateState,
                    )
                }
            }
        }
    }

    private fun processLedgerResponse(
        ledgerFactorSource: FactorSource.Ledger,
        derivePublicKeyResponse: LedgerResponse.DerivePublicKeyResponse
    ) {
        _state.update { it.copy(waitingForLedgerResponse = false) }
        val hardwareAccountsToMigrate = hardwareAccountsLeftToMigrate()
        val derivedKeys = derivePublicKeyResponse.publicKeysHex.map { it.publicKeyHex }.toSet()
        val verifiedAccounts = hardwareAccountsToMigrate.filter { derivedKeys.contains(it.publicKey.hex) }
        if (verifiedAccounts.isEmpty()) {
            _state.update {
                it.copy(
                    uiMessage = UiMessage.InfoMessage.NoAccountsForLedger,
                    shouldShowAddLedgerDeviceScreen = true
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
        migrateAccounts()
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
            val olympiaWalletData = olympiaWalletDataParser.parseOlympiaWalletAccountData(olympiaWalletDataChunks = scannedData)
            olympiaWalletData?.let { data ->
                this@ImportLegacyWalletViewModel.olympiaWalletData = data
                seedPhraseInputDelegate.setSeedPhraseSize(data.mnemonicWordCount)
                val olympiaFactorSourceIdsForAccounts = getFactorSourceIdForOlympiaAccountsUseCase(data.accountData.toList()).getOrNull()
                val allImported = data.accountData.all { it.alreadyImported } && olympiaFactorSourceIdsForAccounts != null

                val nextPage =
                    if (allImported) ImportLegacyWalletUiState.Page.ImportComplete else ImportLegacyWalletUiState.Page.AccountsToImportList
                _state.update { state ->
                    state.copy(
                        currentPage = nextPage,
                        olympiaAccountsToImport = data.accountData
                            .map {
                                // truncate the name, max 30 chars
                                it.copy(accountName = it.accountName.take(ENTITY_NAME_MAX_LENGTH))
                            }
                            .toPersistentList(),
                        importButtonEnabled = !allImported,
                        migratedAccounts = if (allImported) {
                            data.accountData.mapNotNull {
                                getProfileUseCase().activeAccountOnCurrentNetwork(it.newBabylonAddress)?.toUiModel()
                            }.toPersistentList()
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

    private suspend fun proceedToNextPage() {
        val currentPage = _state.value.currentPage
        _state.value.pages.nextPage(currentPage)?.let { nextPage ->
            Timber.d("Proceeding to: $nextPage")
            _state.update {
                it.copy(
                    currentPage = nextPage,
                    hideBack = nextPage == ImportLegacyWalletUiState.Page.ImportComplete
                )
            }
            delay(DELAY_300_MS)
            if (nextPage == ImportLegacyWalletUiState.Page.ImportComplete) {
                markImportOlympiaWalletCompleteUseCase()
            }
            sendEvent(OlympiaImportEvent.NextPage(nextPage))
        }
    }

    private fun backToPreviousPage() {
        viewModelScope.launch {
            val newPage = _state.value.pages.previousPage(_state.value.currentPage)
            if (newPage == ImportLegacyWalletUiState.Page.ScanQr) {
                scannedData.clear()
                _state.update { it.copy(qrChunkInfo = null) }
            }
            newPage?.let { page -> _state.update { it.copy(currentPage = page) } }
            sendEvent(OlympiaImportEvent.PreviousPage(newPage))
        }
    }

    private fun buildPages(
        olympiaAccounts: List<OlympiaAccountDetails>,
        needSeedPhraseInput: Boolean
    ): PersistentList<ImportLegacyWalletUiState.Page> {
        val hasSoftwareAccounts = olympiaAccounts.any { it.type == OlympiaAccountType.Software && !it.alreadyImported }
        val hasHardwareAccounts = olympiaAccounts.any { it.type == OlympiaAccountType.Hardware && !it.alreadyImported }
        var pages = listOf(ImportLegacyWalletUiState.Page.ScanQr, ImportLegacyWalletUiState.Page.AccountsToImportList)
        when {
            hasSoftwareAccounts && needSeedPhraseInput -> {
                pages = pages + listOf(ImportLegacyWalletUiState.Page.MnemonicInput)
                if (hasHardwareAccounts) {
                    pages = pages + listOf(ImportLegacyWalletUiState.Page.HardwareAccounts)
                }
            }

            hasHardwareAccounts -> {
                pages = pages + listOf(ImportLegacyWalletUiState.Page.HardwareAccounts)
            }

            else -> {}
        }
        return (pages + listOf(ImportLegacyWalletUiState.Page.ImportComplete)).toPersistentList()
    }

    fun onWordChanged(index: Int, value: String) {
        seedPhraseInputDelegate.onWordChanged(index, value)
    }

    fun onWordSelected(index: Int, value: String) {
        seedPhraseInputDelegate.onWordSelected(index, value)
    }

    fun onPassphraseChanged(value: String) {
        seedPhraseInputDelegate.onPassphraseChanged(value)
    }

    fun onImportSubmit() = viewModelScope.launch {
        val allAccounts = _state.value.olympiaAccountsToImport
        val softwareAccounts = softwareAccountsToMigrate()
        val hardwareAccounts = hardwareAccountsLeftToMigrate()
        if (allAccounts.isNotEmpty()) {
            val olympiaDeviceFactorSourceExists = getProfileUseCase().deviceFactorSources.any { it.supportsOlympia }
            val needToVerifyExistingOlympiaFactorSource = softwareAccounts.isNotEmpty() && olympiaDeviceFactorSourceExists
            val needsSeedPhraseInput = if (needToVerifyExistingOlympiaFactorSource) {
                // If such factor source exists we need to verify if it can derive the public keys of the accounts
                // about to be imported
                val authenticated = biometricsAuthenticateUseCase()
                if (!authenticated) return@launch

                val validFactorSourceId = getFactorSourceIdForOlympiaAccountsUseCase(softwareAccounts).getOrElse { error ->
                    if (error is ProfileException.SecureStorageAccess) {
                        appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                    } else {
                        _state.update { state ->
                            state.copy(uiMessage = UiMessage.ErrorMessage(error))
                        }
                    }
                    return@launch
                }
                _state.update {
                    it.copy(existingOlympiaFactorSourceId = validFactorSourceId)
                }
                validFactorSourceId == null
            } else {
                // Such factor source does not exist for sure. We need to ask user to provide the seed phrase if software accounts exist.
                softwareAccounts.isNotEmpty()
            }

            val pages = buildPages(
                olympiaAccounts = allAccounts,
                needSeedPhraseInput = needsSeedPhraseInput
            )
            updateHardwareAccountLeftToMigrateCount()
            _state.update { state ->
                state.copy(pages = pages)
            }

            if (!needsSeedPhraseInput && hardwareAccounts.isEmpty()) {
                migrateAccounts(biometricsAlreadyProvided = needToVerifyExistingOlympiaFactorSource)
            } else {
                proceedToNextPage()
            }
        } else {
            val pages = buildPages(_state.value.olympiaAccountsToImport, false)
            updateHardwareAccountLeftToMigrateCount()
            _state.update { state ->
                state.copy(pages = pages)
            }
            proceedToNextPage()
        }
    }

    fun onValidateSoftwareAccounts() {
        viewModelScope.launch {
            val softwareAccountsToMigrate = softwareAccountsToMigrate()
            val accountsValid = state.value.existingOlympiaFactorSourceId != null || state.value.mnemonicWithPassphrase()
                .validatePublicKeysOf(softwareAccountsToMigrate)
            if (accountsValid) {
                when (_state.value.pages.nextPage(_state.value.currentPage)) {
                    ImportLegacyWalletUiState.Page.HardwareAccounts -> proceedToNextPage()
                    else -> migrateAccounts()
                }
            } else {
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(ProfileException.InvalidMnemonic)) }
            }
        }
    }

    private fun migrateAccounts(biometricsAlreadyProvided: Boolean = false) {
        viewModelScope.launch {
            val softwareAccounts = softwareAccountsToMigrate()
            if (softwareAccounts.isNotEmpty()) {
                val deviceFactorSourceId = state.value.existingOlympiaFactorSourceId ?: run {
                    if (!biometricsAlreadyProvided) {
                        val authenticated = biometricsAuthenticateUseCase()
                        if (!authenticated) return@launch
                    }

                    addOlympiaFactorSourceUseCase(
                        state.value.mnemonicWithPassphrase()
                    ).getOrElse { error ->
                        if (error is ProfileException.SecureStorageAccess) {
                            appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                        } else {
                            _state.update { state ->
                                state.copy(uiMessage = UiMessage.ErrorMessage(error))
                            }
                        }
                        return@launch
                    }
                }

                migrateOlympiaAccountsUseCase(
                    olympiaAccounts = softwareAccounts,
                    factorSourceId = deviceFactorSourceId
                )
            }

            if (verifiedHardwareAccounts.isNotEmpty()) {
                verifiedHardwareAccounts.entries.forEach { entry ->
                    migrateOlympiaAccountsUseCase(
                        olympiaAccounts = entry.value,
                        factorSourceId = entry.key.id as FactorSourceId.Hash
                    )
                }
            }

            _state.update { state ->
                state.copy(
                    migratedAccounts = state.olympiaAccountsToImport.mapNotNull {
                        getProfileUseCase().activeAccountOnCurrentNetwork(it.newBabylonAddress)?.toUiModel()
                    }.toPersistentList()
                )
            }

            proceedToNextPage()
        }
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

    private fun onUseLedger(ledgerFactorSource: FactorSource.Ledger) {
        updateHardwareAccountLeftToMigrateCount()
        viewModelScope.launch {
            _state.update { it.copy(waitingForLedgerResponse = true) }
            val hardwareAccountsDerivationPaths = hardwareAccountsLeftToMigrate().map { it.derivationPath }
            val interactionId = UUIDGenerator.uuid().toString()
            ledgerMessenger.sendDerivePublicKeyRequest(
                interactionId = interactionId,
                keyParameters = hardwareAccountsDerivationPaths.map { derivationPath ->
                    LedgerInteractionRequest.KeyParameters(Curve.Secp256k1, derivationPath.string)
                },
                ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(ledgerFactorSource)
            ).onFailure {
                _state.update { uiState ->
                    uiState.copy(waitingForLedgerResponse = false)
                }
            }.onSuccess { derivePublicKeyResponse ->
                processLedgerResponse(ledgerFactorSource, derivePublicKeyResponse)
            }
        }
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

    override fun initialState(): ImportLegacyWalletUiState {
        return ImportLegacyWalletUiState(
            pages = initialPages.toPersistentList()
        )
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onContinueWithLedgerClick() {
        val hardwareAccountsLeft = hardwareAccountsLeftToMigrate()
        if (hardwareAccountsLeft.isEmpty()) {
            migrateAccounts()
        } else {
            viewModelScope.launch {
                val hasAtLeastOneLinkedConnector = p2PLinksRepository.getP2PLinks()
                    .asList()
                    .isNotEmpty()

                if (hasAtLeastOneLinkedConnector) {
                    useLedgerDelegate.onSendAddLedgerRequest()
                } else {
                    _state.update {
                        it.copy(shouldShowAddLinkConnectorScreen = true)
                    }
                }
            }
        }
    }

    fun onCloseSettings() {
        _state.update {
            it.copy(shouldShowAddLedgerDeviceScreen = false)
        }
    }

    fun onNewConnectorAdded() {
        viewModelScope.launch {
            _state.update {
                it.copy(shouldShowAddLinkConnectorScreen = false)
            }
            if (useLedgerDelegate.state.first().hasLedgerDevices.not()) {
                _state.update {
                    it.copy(
                        addLedgerSheetState = AddLedgerDeviceUiState.ShowContent.AddLedgerDeviceInfo,
                        shouldShowAddLedgerDeviceScreen = true,
                    )
                }
            }
        }
    }

    fun onNewConnectorCloseClick() {
        _state.update {
            it.copy(shouldShowAddLinkConnectorScreen = false)
        }
    }

    private fun MnemonicWithPassphrase.validatePublicKeysOf(accounts: List<OlympiaAccountDetails>): Boolean {
        val hdPublicKeys = accounts.map {
            HierarchicalDeterministicPublicKey(
                publicKey = it.publicKey,
                derivationPath = it.derivationPath
            )
        }
        return validate(hdPublicKeys = hdPublicKeys)
    }
}

sealed interface OlympiaImportEvent : OneOffEvent {
    data class NextPage(val page: ImportLegacyWalletUiState.Page) : OlympiaImportEvent
    data class PreviousPage(val page: ImportLegacyWalletUiState.Page?) : OlympiaImportEvent
}

data class ImportLegacyWalletUiState(
    val pages: ImmutableList<Page> = persistentListOf(),
    val currentPage: Page = Page.ScanQr,
    val importButtonEnabled: Boolean = false,
    val olympiaAccountsToImport: ImmutableList<OlympiaAccountDetails> = persistentListOf(),
    val uiMessage: UiMessage? = null,
    val migratedAccounts: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val hideBack: Boolean = false,
    val qrChunkInfo: ChunkInfo? = null,
    val hardwareAccountsLeftToImport: Int = 0,
    val waitingForLedgerResponse: Boolean = false,
    val verifiedLedgerDevices: ImmutableList<FactorSource.Ledger> = persistentListOf(),
    val recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null,
    val addLedgerSheetState: AddLedgerDeviceUiState.ShowContent = AddLedgerDeviceUiState.ShowContent.AddLedgerDeviceInfo,
    val seedPhraseInputState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State(),
    val shouldShowAddLinkConnectorScreen: Boolean = false,
    val shouldShowAddLedgerDeviceScreen: Boolean = false,
    var existingOlympiaFactorSourceId: FactorSourceId.Hash? = null
) : UiState {

    fun mnemonicWithPassphrase(): MnemonicWithPassphrase {
        return seedPhraseInputState.toMnemonicWithPassphrase()
    }

    enum class Page {
        ScanQr, AccountsToImportList, MnemonicInput, HardwareAccounts, ImportComplete
    }
}

private fun List<ImportLegacyWalletUiState.Page>.nextPage(
    currentPage: ImportLegacyWalletUiState.Page
): ImportLegacyWalletUiState.Page? {
    val currentIndex = indexOf(currentPage)
    if (currentIndex == -1 || currentIndex == size - 1) return null
    return this[currentIndex + 1]
}

private fun List<ImportLegacyWalletUiState.Page>.previousPage(
    currentPage: ImportLegacyWalletUiState.Page
): ImportLegacyWalletUiState.Page? {
    val currentIndex = indexOf(currentPage)
    if (currentIndex == -1 || currentIndex == 0) return null
    return this[currentIndex - 1]
}
