package com.babylon.wallet.android.presentation.settings.debug.factors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem.SecurityFactorCategory
import com.babylon.wallet.android.presentation.settings.debug.factors.SecurityFactorSamplesViewModel.State.Page
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.currentSecurityFactorTypeItems
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.model.factors.StatusMessage
import com.babylon.wallet.android.utils.relativeTimeFormatted
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SecurityFactorSamplesViewModel @Inject constructor() : StateViewModel<SecurityFactorSamplesViewModel.State>() {

    @Suppress("LongMethod")
    @OptIn(UsesSampleValues::class)
    override fun initialState(): State = State(
        displayOnlyFactorSourceItems = persistentListOf(
            LedgerHardwareWalletFactorSource.sample().toFactorSourceCard(
                accounts = persistentListOf(Account.sampleStokenet.nadia),
                personas = persistentListOf(Persona.sampleStokenet.leiaSkywalker),
                hasHiddenEntities = true
            ),
            LedgerHardwareWalletFactorSource.sample().toFactorSourceCard(
                accounts = persistentListOf(
                    Account.sampleMainnet.alice,
                    Account.sampleMainnet.bob,
                    Account.sampleMainnet.carol,
                    Account.sampleStokenet.nadia,
                    Account.sampleStokenet.olivia,
                    Account.sampleStokenet.paige
                ),
                personas = persistentListOf(
                    Persona.sampleMainnet.satoshi,
                    Persona.sampleMainnet.batman,
                    Persona.sampleMainnet.ripley,
                    Persona.sampleStokenet.leiaSkywalker,
                    Persona.sampleStokenet.hermione,
                    Persona.sampleStokenet.connor
                )
            ),
            LedgerHardwareWalletFactorSource.sample().toFactorSourceCard(
                accounts = persistentListOf(
                    Account.sampleMainnet.alice,
                    Account.sampleMainnet.bob,
                    Account.sampleMainnet.carol,
                    Account.sampleStokenet.nadia,
                    Account.sampleStokenet.olivia,
                    Account.sampleStokenet.paige
                )
            ),
            LedgerHardwareWalletFactorSource.sample().toFactorSourceCard(
                personas = persistentListOf(
                    Persona.sampleMainnet.satoshi,
                    Persona.sampleMainnet.batman,
                    Persona.sampleMainnet.ripley,
                    Persona.sampleStokenet.leiaSkywalker,
                    Persona.sampleStokenet.hermione,
                    Persona.sampleStokenet.connor
                ),
                hasHiddenEntities = true
            ),
            DeviceFactorSource.sample().toFactorSourceCard(
                messages = persistentListOf(
                    FactorSourceStatusMessage.Dynamic(
                        message = StatusMessage(
                            message = "Some error",
                            type = StatusMessage.Type.ERROR
                        )
                    )
                ),
                hasHiddenEntities = true
            )
        ),
        displayOnlyFactorSourceKindItems = persistentListOf(
            FactorSourceKindCard(
                kind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
                messages = persistentListOf(
                    FactorSourceStatusMessage.Dynamic(
                        message = StatusMessage(
                            message = "Some warning",
                            type = StatusMessage.Type.WARNING
                        )
                    ),
                    FactorSourceStatusMessage.Dynamic(
                        message = StatusMessage(
                            message = "This seed phrase has been written down",
                            type = StatusMessage.Type.SUCCESS
                        )
                    )
                )
            )
        ),
        singleChoiceFactorSourceKindItems = getSupportedKinds().map {
            Selectable(
                data = FactorSourceKindCard(
                    kind = it,
                    messages = persistentListOf()
                ),
                selected = false
            )
        }.toPersistentList(),
        singleChoiceFactorSourceItems = persistentListOf(
            Selectable(
                data = DeviceFactorSource.sample().toFactorSourceCard(
                    messages = persistentListOf(),
                    accounts = persistentListOf(Account.sampleMainnet()),
                    hasHiddenEntities = false
                ),
                selected = false
            )
        ),
        multiChoiceItems = getSupportedKinds().map {
            Selectable(
                data = FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = it,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = when (it) {
                        FactorSourceKind.DEVICE -> "My Phone"
                        FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> "Highly Secretive Stick"
                        FactorSourceKind.OFF_DEVICE_MNEMONIC -> "ShizzleWords"
                        FactorSourceKind.ARCULUS_CARD -> "Arculus Card Secret"
                        FactorSourceKind.PASSWORD -> "My Password"
                        else -> it.name
                    },
                    includeDescription = false,
                    lastUsedOn = null,
                    kind = it,
                    messages = persistentListOf(),
                    accounts = persistentListOf(),
                    personas = persistentListOf(),
                    hasHiddenEntities = true
                ),
                selected = false
            )
        }.toPersistentList(),
        removableItems = persistentListOf(
            DeviceFactorSource.sample().toFactorSourceCard(includeDescription = true),
            LedgerHardwareWalletFactorSource.sample()
                .toFactorSourceCard(includeDescription = true, hasHiddenEntities = true)
        ),
        securityFactorSettingItems = currentSecurityFactorTypeItems,
        selectableFactorSources = availableFactorSources
    )

    fun onSelectFactorSourceKind(item: FactorSourceKindCard) {
        _state.update { state ->
            state.copy(
                singleChoiceFactorSourceKindItems = state.singleChoiceFactorSourceKindItems.map { selectableItem ->
                    selectableItem.copy(
                        selected = selectableItem.data.kind == item.kind
                    )
                }.toPersistentList()
            )
        }
    }

    fun onSelectFactorSource(factorSourceCard: FactorSourceCard) {
        _state.update { state ->
            state.copy(
                singleChoiceFactorSourceItems = state.singleChoiceFactorSourceItems.map { selectableItem ->
                    selectableItem.copy(
                        selected = selectableItem.data == factorSourceCard
                    )
                }.toPersistentList()
            )
        }
    }

    fun onCheckedChange(item: FactorSourceCard, isChecked: Boolean) {
        _state.update { state ->
            state.copy(
                multiChoiceItems = state.multiChoiceItems.mapWhen(
                    predicate = { it.data.kind == item.kind },
                    mutation = { it.copy(selected = isChecked) }
                ).toPersistentList()
            )
        }
    }

    fun onRemoveClick(item: FactorSourceCard) {
        Timber.d("Remove clicked: $item")
    }

    private fun getSupportedKinds(): List<FactorSourceKind> {
        return FactorSourceKind.entries.filter { it.isSupported() }
    }

    private fun FactorSourceKind.isSupported(): Boolean {
        return this !in unsupportedKinds
    }

    private fun DeviceFactorSource.toFactorSourceCard(
        includeDescription: Boolean = false,
        messages: PersistentList<FactorSourceStatusMessage> = persistentListOf(),
        accounts: PersistentList<Account> = persistentListOf(),
        personas: PersistentList<Persona> = persistentListOf(),
        hasHiddenEntities: Boolean = false
    ): FactorSourceCard {
        return FactorSourceCard(
            id = id.asGeneral(),
            name = hint.label,
            includeDescription = includeDescription,
            lastUsedOn = common.lastUsedOn.relativeTimeFormatted(),
            kind = kind,
            messages = messages,
            accounts = accounts,
            personas = personas,
            hasHiddenEntities = hasHiddenEntities
        )
    }

    private fun LedgerHardwareWalletFactorSource.toFactorSourceCard(
        includeDescription: Boolean = false,
        messages: PersistentList<FactorSourceStatusMessage> = persistentListOf(),
        accounts: PersistentList<Account> = persistentListOf(),
        personas: PersistentList<Persona> = persistentListOf(),
        hasHiddenEntities: Boolean = false
    ): FactorSourceCard {
        return FactorSourceCard(
            id = id.asGeneral(),
            name = hint.label,
            includeDescription = includeDescription,
            lastUsedOn = common.lastUsedOn.relativeTimeFormatted(),
            kind = kind,
            messages = messages,
            accounts = accounts,
            personas = personas,
            hasHiddenEntities = hasHiddenEntities
        )
    }

    fun onChooseFactorSourceClick() = _state.update { it.copy(isBottomSheetVisible = true) }

    fun onSecurityFactorTypeClick(securityFactorsSettingsItem: SecurityFactorsSettingsItem) {
        _state.update { state ->
            state.copy(
                currentPagePosition = when (securityFactorsSettingsItem) {
                    SecurityFactorsSettingsItem.ArculusCard -> Page.ArculusCard.ordinal
                    is SecurityFactorsSettingsItem.BiometricsPin -> Page.BiometricsPin.ordinal
                    SecurityFactorsSettingsItem.LedgerNano -> Page.LedgerNano.ordinal
                    SecurityFactorsSettingsItem.Passphrase -> Page.Passphrase.ordinal
                    SecurityFactorsSettingsItem.Password -> Page.Password.ordinal
                }
            )
        }
    }

    fun onFactorSourceFromSheetSelect(factorSourceCard: FactorSourceCard) {
        _state.update { state ->
            val targetKind = factorSourceCard.kind
            val targetList = state.selectableFactorSources[targetKind] ?: return@update state
            val updatedList = targetList.map { selectableItem ->
                selectableItem.copy(selected = selectableItem.data == factorSourceCard)
            }.toPersistentList()
            state.copy(
                selectableFactorSources = state.selectableFactorSources.put(targetKind, updatedList)
            )
        }
    }

    fun onSelectedFactorSourceConfirm() {
        _state.update {
            it.copy(
                currentPagePosition = Page.SelectFactorSourceType.ordinal,
                isBottomSheetVisible = false,
                selectableFactorSources = availableFactorSources
            )
        }
    }

    fun onSheetBackClick() = viewModelScope.launch {
        _state.update { state ->
            if (state.currentPagePosition != Page.SelectFactorSourceType.ordinal) {
                state.copy(
                    currentPagePosition = Page.SelectFactorSourceType.ordinal,
                    selectableFactorSources = availableFactorSources
                )
            } else {
                state.copy(
                    isBottomSheetVisible = false,
                    currentPagePosition = Page.SelectFactorSourceType.ordinal,
                    selectableFactorSources = availableFactorSources
                )
            }
        }
    }

    fun onSheetClosed() = _state.update { state ->
        state.copy(
            isBottomSheetVisible = false,
            currentPagePosition = Page.SelectFactorSourceType.ordinal,
            selectableFactorSources = availableFactorSources
        )
    }

    data class State(
        val displayOnlyFactorSourceKindItems: PersistentList<FactorSourceKindCard> = persistentListOf(),
        val displayOnlyFactorSourceItems: PersistentList<FactorSourceCard> = persistentListOf(),
        val singleChoiceFactorSourceItems: PersistentList<Selectable<FactorSourceCard>> = persistentListOf(),
        val singleChoiceFactorSourceKindItems: PersistentList<Selectable<FactorSourceKindCard>> = persistentListOf(),
        val multiChoiceItems: PersistentList<Selectable<FactorSourceCard>> = persistentListOf(),
        val removableItems: PersistentList<FactorSourceCard> = persistentListOf(),
        val securityFactorSettingItems: ImmutableMap<SecurityFactorCategory, ImmutableSet<SecurityFactorsSettingsItem>>,
        val selectableFactorSources: PersistentMap<FactorSourceKind, PersistentList<Selectable<FactorSourceCard>>> = persistentMapOf(),
        val bottomSheetPages: List<Page> = Page.entries.toList(),
        val currentPagePosition: Int = Page.SelectFactorSourceType.ordinal,
        val isBottomSheetVisible: Boolean = false
    ) : UiState {

        enum class Page {
            SelectFactorSourceType, BiometricsPin, LedgerNano, ArculusCard, Password, Passphrase
        }
    }

    companion object {

        private val unsupportedKinds = setOf(
            FactorSourceKind.TRUSTED_CONTACT,
            FactorSourceKind.SECURITY_QUESTIONS
        )

        @OptIn(UsesSampleValues::class)
        private val availableDeviceFactorSources = persistentListOf(
            Selectable(
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.DEVICE,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Fotis Ioannidis",
                    includeDescription = false,
                    lastUsedOn = "Today",
                    kind = FactorSourceKind.DEVICE,
                    messages = persistentListOf(FactorSourceStatusMessage.NoSecurityIssues),
                    accounts = persistentListOf(
                        Account.sampleMainnet()
                    ),
                    personas = persistentListOf(
                        Persona.sampleMainnet(),
                        Persona.sampleStokenet()
                    ),
                    hasHiddenEntities = false
                )
            ),
            Selectable(
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.DEVICE,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "666",
                    includeDescription = false,
                    lastUsedOn = "Today",
                    kind = FactorSourceKind.DEVICE,
                    messages = persistentListOf(FactorSourceStatusMessage.SecurityPrompt.RecoveryRequired),
                    accounts = persistentListOf(
                        Account.sampleMainnet()
                    ),
                    personas = persistentListOf(
                        Persona.sampleMainnet(),
                        Persona.sampleStokenet()
                    ),
                    hasHiddenEntities = true
                )
            ),
            Selectable(
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.DEVICE,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "999",
                    includeDescription = false,
                    lastUsedOn = "Yesterday",
                    kind = FactorSourceKind.DEVICE,
                    messages = persistentListOf(FactorSourceStatusMessage.SecurityPrompt.WriteDownSeedPhrase),
                    accounts = persistentListOf(),
                    personas = persistentListOf(),
                    hasHiddenEntities = false
                )
            ),
            Selectable(
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.DEVICE,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "XXX",
                    includeDescription = false,
                    lastUsedOn = "Today",
                    kind = FactorSourceKind.DEVICE,
                    messages = persistentListOf(),
                    accounts = persistentListOf(
                        Account.sampleMainnet()
                    ),
                    personas = persistentListOf(
                        Persona.sampleMainnet(),
                    ),
                    hasHiddenEntities = true
                )
            )
        )

        @OptIn(UsesSampleValues::class)
        private val availableLedgerFactorSources = persistentListOf(
            Selectable(
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "ALFZ PSF",
                    includeDescription = false,
                    lastUsedOn = "every year",
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    messages = persistentListOf(FactorSourceStatusMessage.NoSecurityIssues),
                    accounts = persistentListOf(
                        Account.sampleMainnet()
                    ),
                    personas = persistentListOf(
                        Persona.sampleMainnet()
                    ),
                    hasHiddenEntities = false
                )
            ),
            Selectable(
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "DPG7000",
                    includeDescription = false,
                    lastUsedOn = "Today",
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    messages = persistentListOf(FactorSourceStatusMessage.SecurityPrompt.RecoveryRequired),
                    accounts = persistentListOf(),
                    personas = persistentListOf(),
                    hasHiddenEntities = true
                )
            )
        )

        val availableFactorSources = mapOf(
            FactorSourceKind.DEVICE to availableDeviceFactorSources,
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET to availableLedgerFactorSources
        ).toPersistentMap()
    }
}
