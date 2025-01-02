package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.choosefactor

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem.SecurityFactorCategory
import com.babylon.wallet.android.presentation.settings.debug.factors.SecurityFactorSamplesViewModel.Companion.availableFactorSources
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.currentSecurityFactorTypeItems
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.radixdlt.sargon.FactorSourceKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseFactorSourceViewModel @Inject constructor() :
    StateViewModel<ChooseFactorSourceViewModel.State>(),
    OneOffEventHandler<ChooseFactorSourceViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(
        currentPagePosition = State.Page.SelectFactorSourceType.ordinal,
        securityFactorTypeItems = currentSecurityFactorTypeItems,
        selectableFactorSources = availableFactorSources // this will be replaced with the actual factor sources
    )

    fun onSecurityFactorTypeClick(securityFactorsSettingsItem: SecurityFactorsSettingsItem) {
        _state.update { state ->
            state.copy(
                currentPagePosition = when (securityFactorsSettingsItem) {
                    SecurityFactorsSettingsItem.ArculusCard -> State.Page.ArculusCard.ordinal
                    is SecurityFactorsSettingsItem.BiometricsPin -> State.Page.BiometricsPin.ordinal
                    SecurityFactorsSettingsItem.LedgerNano -> State.Page.LedgerNano.ordinal
                    SecurityFactorsSettingsItem.Passphrase -> State.Page.Passphrase.ordinal
                    SecurityFactorsSettingsItem.Password -> State.Page.Password.ordinal
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

    fun onSelectedFactorSourceConfirm() = viewModelScope.launch {
        val selectedFactorSource = state.value.selectableFactorSources.values.flatten().find { it.selected }
        selectedFactorSource?.let {
            sendEvent(Event.SelectedFactorSourceConfirm(selectedFactorSource.data))
        }
        _state.update { state ->
            state.copy(
                currentPagePosition = State.Page.SelectFactorSourceType.ordinal,
                selectableFactorSources = availableFactorSources
            )
        }
    }

    fun onSheetBackClick() = viewModelScope.launch {
        _state.update { state ->
            if (state.currentPagePosition != State.Page.SelectFactorSourceType.ordinal) {
                state.copy(
                    currentPagePosition = State.Page.SelectFactorSourceType.ordinal,
                    selectableFactorSources = availableFactorSources
                )
            } else {
                sendEvent(Event.DismissSheet)
                state.copy(
                    currentPagePosition = State.Page.SelectFactorSourceType.ordinal,
                    selectableFactorSources = availableFactorSources
                )
            }
        }
    }

    fun onAddFactorSourceClick(factorSourceKind: FactorSourceKind) {
        TODO("Not yet implemented")
    }

    fun onSheetCloseClick() = viewModelScope.launch {
        _state.update { state ->
            state.copy(
                currentPagePosition = State.Page.SelectFactorSourceType.ordinal,
                selectableFactorSources = availableFactorSources
            )
        }
        sendEvent(Event.DismissSheet)
    }

    data class State(
        val securityFactorTypeItems: ImmutableMap<SecurityFactorCategory, ImmutableSet<SecurityFactorsSettingsItem>>,
        val selectableFactorSources: PersistentMap<FactorSourceKind, PersistentList<Selectable<FactorSourceCard>>> = persistentMapOf(),
        val pages: List<Page> = Page.entries.toList(),
        val currentPagePosition: Int = Page.SelectFactorSourceType.ordinal,
    ) : UiState {

        enum class Page {
            SelectFactorSourceType, BiometricsPin, LedgerNano, ArculusCard, Password, Passphrase
        }
    }

    sealed interface Event : OneOffEvent {

        data object DismissSheet : Event

        data class SelectedFactorSourceConfirm(val factorSourceCard: FactorSourceCard) : Event
    }
}
