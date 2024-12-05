package com.babylon.wallet.android.presentation.settings.debug.factors

import android.text.format.DateUtils
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.StatusMessage
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
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
        displayOnlyItems = persistentListOf(
            LedgerHardwareWalletFactorSource.sample().toCard(
                accounts = persistentListOf(Account.sampleStokenet.nadia),
                personas = persistentListOf(Persona.sampleStokenet.leiaSkywalker)
            ),
            LedgerHardwareWalletFactorSource.sample().toCard(
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
            LedgerHardwareWalletFactorSource.sample().toCard(
                accounts = persistentListOf(
                    Account.sampleMainnet.alice,
                    Account.sampleMainnet.bob,
                    Account.sampleMainnet.carol,
                    Account.sampleStokenet.nadia,
                    Account.sampleStokenet.olivia,
                    Account.sampleStokenet.paige
                )
            ),
            LedgerHardwareWalletFactorSource.sample().toCard(
                personas = persistentListOf(
                    Persona.sampleMainnet.satoshi,
                    Persona.sampleMainnet.batman,
                    Persona.sampleMainnet.ripley,
                    Persona.sampleStokenet.leiaSkywalker,
                    Persona.sampleStokenet.hermione,
                    Persona.sampleStokenet.connor
                )
            ),
            FactorSourceCard(
                kind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
                header = FactorSourceCard.Header.New,
                messages = persistentListOf(
                    StatusMessage(
                        message = "Some warning",
                        type = StatusMessage.Type.WARNING
                    ),
                    StatusMessage(
                        message = "This seed phrase has been written down",
                        type = StatusMessage.Type.SUCCESS
                    )
                ),
                accounts = persistentListOf(),
                personas = persistentListOf()
            ),
            DeviceFactorSource.sample().toCard(
                messages = persistentListOf(
                    StatusMessage(
                        message = "Some error",
                        type = StatusMessage.Type.ERROR
                    )
                )
            )
        ),
        singleChoiceItems = getSupportedKinds().map {
            SelectableFactorSourceCard(
                item = FactorSourceCard(
                    kind = it,
                    header = FactorSourceCard.Header.New,
                    messages = persistentListOf(),
                    accounts = persistentListOf(),
                    personas = persistentListOf()
                ),
                isSelected = false
            )
        }.toPersistentList(),
        multiChoiceItems = getSupportedKinds().map {
            SelectableFactorSourceCard(
                item = FactorSourceCard(
                    kind = it,
                    header = FactorSourceCard.Header.New,
                    messages = persistentListOf(),
                    accounts = persistentListOf(),
                    personas = persistentListOf()
                ),
                isSelected = false
            )
        }.toPersistentList(),
        removableItems = persistentListOf(
            FactorSourceCard(
                kind = FactorSourceKind.DEVICE,
                header = FactorSourceCard.Header.New,
                messages = persistentListOf(),
                accounts = persistentListOf(),
                personas = persistentListOf()
            ),
            FactorSourceCard(
                kind = FactorSourceKind.ARCULUS_CARD,
                header = FactorSourceCard.Header.New,
                messages = persistentListOf(),
                accounts = persistentListOf(),
                personas = persistentListOf()
            )
        )
    )

    fun onSelect(item: FactorSourceCard) {
        _state.update { state ->
            state.copy(
                singleChoiceItems = state.singleChoiceItems.map { selectableItem ->
                    selectableItem.copy(
                        isSelected = selectableItem.item.kind == item.kind
                    )
                }.toPersistentList()
            )
        }
    }

    fun onCheckedChange(item: FactorSourceCard, isChecked: Boolean) {
        _state.update { state ->
            state.copy(
                multiChoiceItems = state.multiChoiceItems.mapWhen(
                    predicate = { it.item.kind == item.kind },
                    mutation = { it.copy(isSelected = isChecked) }
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

    private fun DeviceFactorSource.toCard(
        messages: PersistentList<StatusMessage> = persistentListOf(),
        accounts: PersistentList<Account> = persistentListOf(),
        personas: PersistentList<Persona> = persistentListOf()
    ): FactorSourceCard {
        return FactorSourceCard(
            kind = kind,
            header = FactorSourceCard.Header.Instance(
                id = id.asGeneral(),
                name = hint.label,
                lastUsedOn = common.lastUsedOn.formatted()
            ),
            messages = messages,
            accounts = accounts,
            personas = personas
        )
    }

    private fun LedgerHardwareWalletFactorSource.toCard(
        messages: PersistentList<StatusMessage> = persistentListOf(),
        accounts: PersistentList<Account> = persistentListOf(),
        personas: PersistentList<Persona> = persistentListOf()
    ): FactorSourceCard {
        return FactorSourceCard(
            kind = kind,
            header = FactorSourceCard.Header.Instance(
                id = id.asGeneral(),
                name = hint.label,
                lastUsedOn = common.lastUsedOn.formatted()
            ),
            messages = messages,
            accounts = accounts,
            personas = personas
        )
    }

    private fun Timestamp.formatted(): String {
        val millis = toEpochSecond().seconds.inWholeMilliseconds
        return DateUtils.getRelativeTimeSpanString(millis).toString()
    }

    data class SelectableFactorSourceCard(
        val item: FactorSourceCard,
        val isSelected: Boolean
    )

    data class State(
        val displayOnlyItems: PersistentList<FactorSourceCard> = persistentListOf(),
        val singleChoiceItems: PersistentList<SelectableFactorSourceCard> = persistentListOf(),
        val multiChoiceItems: PersistentList<SelectableFactorSourceCard> = persistentListOf(),
        val removableItems: PersistentList<FactorSourceCard> = persistentListOf()
    ) : UiState

    companion object {

        private val unsupportedKinds = setOf(
            FactorSourceKind.TRUSTED_CONTACT,
            FactorSourceKind.SECURITY_QUESTIONS
        )
    }
}
