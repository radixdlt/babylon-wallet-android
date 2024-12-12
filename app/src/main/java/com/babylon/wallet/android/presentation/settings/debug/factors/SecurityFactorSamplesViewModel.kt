package com.babylon.wallet.android.presentation.settings.debug.factors

import android.text.format.DateUtils
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceInstanceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.model.factors.StatusMessage
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.kind
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
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class SecurityFactorSamplesViewModel @Inject constructor() : StateViewModel<SecurityFactorSamplesViewModel.State>() {

    @Suppress("LongMethod")
    @OptIn(UsesSampleValues::class)
    override fun initialState(): State = State(
        displayOnlyInstanceItems = persistentListOf(
            LedgerHardwareWalletFactorSource.sample().toInstanceCard(
                accounts = persistentListOf(Account.sampleStokenet.nadia),
                personas = persistentListOf(Persona.sampleStokenet.leiaSkywalker)
            ),
            LedgerHardwareWalletFactorSource.sample().toInstanceCard(
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
            LedgerHardwareWalletFactorSource.sample().toInstanceCard(
                accounts = persistentListOf(
                    Account.sampleMainnet.alice,
                    Account.sampleMainnet.bob,
                    Account.sampleMainnet.carol,
                    Account.sampleStokenet.nadia,
                    Account.sampleStokenet.olivia,
                    Account.sampleStokenet.paige
                )
            ),
            LedgerHardwareWalletFactorSource.sample().toInstanceCard(
                personas = persistentListOf(
                    Persona.sampleMainnet.satoshi,
                    Persona.sampleMainnet.batman,
                    Persona.sampleMainnet.ripley,
                    Persona.sampleStokenet.leiaSkywalker,
                    Persona.sampleStokenet.hermione,
                    Persona.sampleStokenet.connor
                )
            ),
            DeviceFactorSource.sample().toInstanceCard(
                messages = persistentListOf(
                    FactorSourceStatusMessage.Dynamic(
                        message = StatusMessage(
                            message = "Some error",
                            type = StatusMessage.Type.ERROR
                        )
                    )
                )
            )
        ),
        displayOnlySourceItems = persistentListOf(
            FactorSourceCard(
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
        singleChoiceItems = getSupportedKinds().map {
            Selectable(
                data = FactorSourceCard(
                    kind = it,
                    messages = persistentListOf()
                ),
                selected = false
            )
        }.toPersistentList(),
        multiChoiceItems = getSupportedKinds().map {
            Selectable(
                data = FactorSourceInstanceCard(
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
                    personas = persistentListOf()
                ),
                selected = false
            )
        }.toPersistentList(),
        removableItems = persistentListOf(
            DeviceFactorSource.sample().toInstanceCard(includeDescription = true),
            LedgerHardwareWalletFactorSource.sample().toInstanceCard(includeDescription = true)
        )
    )

    fun onSelect(item: FactorSourceCard) {
        _state.update { state ->
            state.copy(
                singleChoiceItems = state.singleChoiceItems.map { selectableItem ->
                    selectableItem.copy(
                        selected = selectableItem.data.kind == item.kind
                    )
                }.toPersistentList()
            )
        }
    }

    fun onCheckedChange(item: FactorSourceInstanceCard, isChecked: Boolean) {
        _state.update { state ->
            state.copy(
                multiChoiceItems = state.multiChoiceItems.mapWhen(
                    predicate = { it.data.kind == item.kind },
                    mutation = { it.copy(selected = isChecked) }
                ).toPersistentList()
            )
        }
    }

    fun onRemoveClick(item: FactorSourceInstanceCard) {
        Timber.d("Remove clicked: $item")
    }

    private fun getSupportedKinds(): List<FactorSourceKind> {
        return FactorSourceKind.entries.filter { it.isSupported() }
    }

    private fun FactorSourceKind.isSupported(): Boolean {
        return this !in unsupportedKinds
    }

    private fun DeviceFactorSource.toInstanceCard(
        includeDescription: Boolean = false,
        messages: PersistentList<FactorSourceStatusMessage> = persistentListOf(),
        accounts: PersistentList<Account> = persistentListOf(),
        personas: PersistentList<Persona> = persistentListOf()
    ): FactorSourceInstanceCard {
        return FactorSourceInstanceCard(
            id = id.asGeneral(),
            name = hint.label,
            includeDescription = includeDescription,
            lastUsedOn = common.lastUsedOn.formatted(),
            kind = kind,
            messages = messages,
            accounts = accounts,
            personas = personas
        )
    }

    private fun LedgerHardwareWalletFactorSource.toInstanceCard(
        includeDescription: Boolean = false,
        messages: PersistentList<FactorSourceStatusMessage> = persistentListOf(),
        accounts: PersistentList<Account> = persistentListOf(),
        personas: PersistentList<Persona> = persistentListOf()
    ): FactorSourceInstanceCard {
        return FactorSourceInstanceCard(
            id = id.asGeneral(),
            name = hint.label,
            includeDescription = includeDescription,
            lastUsedOn = common.lastUsedOn.formatted(),
            kind = kind,
            messages = messages,
            accounts = accounts,
            personas = personas
        )
    }

    private fun Timestamp.formatted(): String {
        val millis = toEpochSecond().seconds.inWholeMilliseconds
        return DateUtils.getRelativeTimeSpanString(millis).toString()
    }

    data class State(
        val displayOnlySourceItems: PersistentList<FactorSourceCard> = persistentListOf(),
        val displayOnlyInstanceItems: PersistentList<FactorSourceInstanceCard> = persistentListOf(),
        val singleChoiceItems: PersistentList<Selectable<FactorSourceCard>> = persistentListOf(),
        val multiChoiceItems: PersistentList<Selectable<FactorSourceInstanceCard>> = persistentListOf(),
        val removableItems: PersistentList<FactorSourceInstanceCard> = persistentListOf()
    ) : UiState

    companion object {

        private val unsupportedKinds = setOf(
            FactorSourceKind.TRUSTED_CONTACT,
            FactorSourceKind.SECURITY_QUESTIONS
        )
    }
}
