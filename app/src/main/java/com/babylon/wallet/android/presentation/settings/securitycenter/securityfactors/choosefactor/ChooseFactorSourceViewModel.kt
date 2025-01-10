package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.choosefactor

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem.SecurityFactorCategory
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.currentSecurityFactorTypeItems
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChooseFactorSourceViewModel @Inject constructor(
    private val osManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : StateViewModel<ChooseFactorSourceViewModel.State>(), OneOffEventHandler<ChooseFactorSourceViewModel.Event> by OneOffEventHandlerImpl() {

    private var factorSourcesFromProfile = state.value.selectableFactorSources

    init {
        initData()
    }

    override fun initialState(): State = State(
        currentPagePosition = State.Page.SelectFactorSourceType.ordinal,
        securityFactorTypeItems = currentSecurityFactorTypeItems
    )

    fun initData(excludeFactorSources: List<FactorSourceId> = emptyList()) {
        viewModelScope.launch(defaultDispatcher) {
            factorSourcesFromProfile = osManager.sargonOs
                .factorSources()
                .filter { it.id !in excludeFactorSources }
                .map { factorSource ->
                    Selectable(factorSource.toFactorSourceCard())
                }
                .groupBy { it.data.kind }
                .mapValues { (_, cards) -> cards.toPersistentList() }
                .toPersistentMap()

            _state.update { state ->
                state.copy(selectableFactorSources = factorSourcesFromProfile)
            }
        }
    }

    fun onSecurityFactorTypeClick(securityFactorsSettingsItem: SecurityFactorsSettingsItem) {
        _state.update { state ->
            state.copy(
                currentPagePosition = when (securityFactorsSettingsItem) {
                    SecurityFactorsSettingsItem.ArculusCard -> State.Page.ArculusCard.ordinal
                    is SecurityFactorsSettingsItem.BiometricsPin -> State.Page.BiometricsPin.ordinal
                    SecurityFactorsSettingsItem.LedgerNano -> State.Page.LedgerNano.ordinal
                    SecurityFactorsSettingsItem.OffDeviceMnemonic -> State.Page.Passphrase.ordinal
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
                selectableFactorSources = factorSourcesFromProfile
            )
        }
    }

    fun onSheetBackClick() = viewModelScope.launch {
        _state.update { state ->
            if (state.currentPagePosition != State.Page.SelectFactorSourceType.ordinal) {
                state.copy(
                    currentPagePosition = State.Page.SelectFactorSourceType.ordinal,
                    selectableFactorSources = factorSourcesFromProfile
                )
            } else {
                sendEvent(Event.DismissSheet)
                state.copy(
                    currentPagePosition = State.Page.SelectFactorSourceType.ordinal,
                    selectableFactorSources = factorSourcesFromProfile
                )
            }
        }
    }

    fun onAddFactorSourceClick(factorSourceKind: FactorSourceKind) {
        Timber.d("onAddFactorSourceClick: $factorSourceKind")
    }

    fun onSheetCloseClick() = viewModelScope.launch {
        _state.update { state ->
            state.copy(
                currentPagePosition = State.Page.SelectFactorSourceType.ordinal,
                selectableFactorSources = factorSourcesFromProfile
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
