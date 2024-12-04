package com.babylon.wallet.android.presentation.settings.debug.factors

import android.text.format.DateUtils
import com.babylon.wallet.android.domain.model.factors.FactorSourceCard
import com.babylon.wallet.android.domain.model.factors.StatusMessage
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.newAccountSampleMainnetAlice
import com.radixdlt.sargon.newAccountSampleMainnetBob
import com.radixdlt.sargon.newAccountSampleMainnetCarol
import com.radixdlt.sargon.newAccountSampleStokenetNadia
import com.radixdlt.sargon.newAccountSampleStokenetOlivia
import com.radixdlt.sargon.newAccountSampleStokenetPaige
import com.radixdlt.sargon.newDeviceFactorSourceSample
import com.radixdlt.sargon.newLedgerHardwareWalletFactorSourceSample
import com.radixdlt.sargon.newPersonaSampleMainnetBatman
import com.radixdlt.sargon.newPersonaSampleMainnetRipley
import com.radixdlt.sargon.newPersonaSampleMainnetSatoshi
import com.radixdlt.sargon.newPersonaSampleStokenetConnor
import com.radixdlt.sargon.newPersonaSampleStokenetHermione
import com.radixdlt.sargon.newPersonaSampleStokenetLeiaSkywalker
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
    override fun initialState(): State = State(
        displayOnlyItems = persistentListOf(
            newLedgerHardwareWalletFactorSourceSample().toCard(
                accounts = persistentListOf(newAccountSampleStokenetNadia()),
                personas = persistentListOf(newPersonaSampleStokenetLeiaSkywalker())
            ),
            newLedgerHardwareWalletFactorSourceSample().toCard(
                accounts = persistentListOf(
                    newAccountSampleMainnetAlice(),
                    newAccountSampleMainnetBob(),
                    newAccountSampleMainnetCarol(),
                    newAccountSampleStokenetNadia(),
                    newAccountSampleStokenetOlivia(),
                    newAccountSampleStokenetPaige()
                ),
                personas = persistentListOf(
                    newPersonaSampleMainnetSatoshi(),
                    newPersonaSampleMainnetBatman(),
                    newPersonaSampleMainnetRipley(),
                    newPersonaSampleStokenetLeiaSkywalker(),
                    newPersonaSampleStokenetHermione(),
                    newPersonaSampleStokenetConnor()
                )
            ),
            newLedgerHardwareWalletFactorSourceSample().toCard(
                accounts = persistentListOf(
                    newAccountSampleMainnetAlice(),
                    newAccountSampleMainnetBob(),
                    newAccountSampleMainnetCarol(),
                    newAccountSampleStokenetNadia(),
                    newAccountSampleStokenetOlivia(),
                    newAccountSampleStokenetPaige()
                )
            ),
            newLedgerHardwareWalletFactorSourceSample().toCard(
                personas = persistentListOf(
                    newPersonaSampleMainnetSatoshi(),
                    newPersonaSampleMainnetBatman(),
                    newPersonaSampleMainnetRipley(),
                    newPersonaSampleStokenetLeiaSkywalker(),
                    newPersonaSampleStokenetHermione(),
                    newPersonaSampleStokenetConnor()
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
            newDeviceFactorSourceSample().toCard(
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
