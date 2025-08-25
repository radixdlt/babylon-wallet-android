package com.babylon.wallet.android.presentation.settings.debug.factors

import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.shared.StatusMessage
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
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import rdx.works.core.mapWhen
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SecurityFactorSamplesViewModel @Inject constructor() : StateViewModel<SecurityFactorSamplesViewModel.State>() {

    @Suppress("LongMethod")
    @OptIn(UsesSampleValues::class)
    override fun initialState(): State = State(
        displayOnlyFactorSourceItems = persistentListOf(
            LedgerHardwareWalletFactorSource.sample().asGeneral().toFactorSourceCard(
                accounts = persistentListOf(Account.sampleStokenet.nadia),
                personas = persistentListOf(Persona.sampleStokenet.leiaSkywalker),
                hasHiddenEntities = true
            ),
            LedgerHardwareWalletFactorSource.sample().asGeneral().toFactorSourceCard(
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
            LedgerHardwareWalletFactorSource.sample().asGeneral().toFactorSourceCard(
                accounts = persistentListOf(
                    Account.sampleMainnet.alice,
                    Account.sampleMainnet.bob,
                    Account.sampleMainnet.carol,
                    Account.sampleStokenet.nadia,
                    Account.sampleStokenet.olivia,
                    Account.sampleStokenet.paige
                )
            ),
            LedgerHardwareWalletFactorSource.sample().asGeneral().toFactorSourceCard(
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
            DeviceFactorSource.sample().asGeneral().toFactorSourceCard(
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
        singleChoiceFactorSourceKindItems = getKinds().map {
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
                data = DeviceFactorSource.sample().asGeneral().toFactorSourceCard(
                    messages = persistentListOf(),
                    accounts = persistentListOf(Account.sampleMainnet()),
                    hasHiddenEntities = false
                ),
                selected = false
            )
        ),
        multiChoiceItems = getKinds().map {
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
                    },
                    includeDescription = false,
                    lastUsedOn = null,
                    kind = it,
                    messages = persistentListOf(),
                    accounts = persistentListOf(),
                    personas = persistentListOf(),
                    hasHiddenEntities = true,
                    isEnabled = true
                ),
                selected = false
            )
        }.toPersistentList(),
        removableItems = persistentListOf(
            DeviceFactorSource.sample().asGeneral()
                .toFactorSourceCard(
                    includeDescription = true
                ),
            LedgerHardwareWalletFactorSource.sample().asGeneral()
                .toFactorSourceCard(
                    includeDescription = true,
                    hasHiddenEntities = true
                )
        )
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

    private fun getKinds(): List<FactorSourceKind> {
        return FactorSourceKind.entries
    }

    fun onChooseFactorSourceClick() = _state.update { it.copy(isBottomSheetVisible = true) }

    fun onSelectedFactorSourceConfirm(factorSourceCard: FactorSourceCard) {
        _state.update {
            it.copy(isBottomSheetVisible = false)
        }
        Timber.d("factor source selected: ${factorSourceCard.name}")
    }

    fun onSheetClosed() = _state.update { state ->
        state.copy(isBottomSheetVisible = false)
    }

    data class State(
        val displayOnlyFactorSourceKindItems: PersistentList<FactorSourceKindCard> = persistentListOf(),
        val displayOnlyFactorSourceItems: PersistentList<FactorSourceCard> = persistentListOf(),
        val singleChoiceFactorSourceItems: PersistentList<Selectable<FactorSourceCard>> = persistentListOf(),
        val singleChoiceFactorSourceKindItems: PersistentList<Selectable<FactorSourceKindCard>> = persistentListOf(),
        val multiChoiceItems: PersistentList<Selectable<FactorSourceCard>> = persistentListOf(),
        val removableItems: PersistentList<FactorSourceCard> = persistentListOf(),
        val isBottomSheetVisible: Boolean = false
    ) : UiState
}
