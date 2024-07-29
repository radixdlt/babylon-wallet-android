package com.babylon.wallet.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig.EXPERIMENTAL_FEATURES_ENABLED
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.domain.usecases.GetSecurityProblemsUseCase
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.DebugSettings
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.Personas
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.Preferences
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.SargonBuildInformation
import com.radixdlt.sargon.extensions.Sargon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import rdx.works.core.mapWhen
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getSecurityProblemsUseCase: GetSecurityProblemsUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val defaultSettings = listOf(
        SettingsUiItem.Settings(SettingsItem.TopLevelSettings.SecurityCenter()),
        SettingsUiItem.Spacer,
        SettingsUiItem.Settings(Personas()),
        SettingsUiItem.Settings(SettingsItem.TopLevelSettings.ApprovedDapps),
        SettingsUiItem.Settings(SettingsItem.TopLevelSettings.LinkedConnectors),
        SettingsUiItem.Spacer,
        SettingsUiItem.Settings(Preferences),
        SettingsUiItem.Spacer,
        SettingsUiItem.Settings(SettingsItem.TopLevelSettings.Troubleshooting),
        if (EXPERIMENTAL_FEATURES_ENABLED) SettingsUiItem.Settings(DebugSettings) else null
    ).mapNotNull { it }

    val state: StateFlow<SettingsUiState> = getSecurityProblemsUseCase().map { securityProblems ->
        var mutated = defaultSettings
        if (securityProblems.isNotEmpty()) {
            mutated = mutated.mapWhen(
                predicate = { it is SettingsUiItem.Settings && it.item is SettingsItem.TopLevelSettings.SecurityCenter },
                mutation = { SettingsUiItem.Settings(SettingsItem.TopLevelSettings.SecurityCenter(securityProblems)) }
            )
            mutated = mutated.mapWhen(
                predicate = { it is SettingsUiItem.Settings && it.item is Personas },
                mutation = { _ ->

                    SettingsUiItem.Settings(
                        Personas(
                            isCloudBackupNotWorking = securityProblems.find { securityProblem ->
                                securityProblem is SecurityProblem.CloudBackupNotWorking
                            }?.let { it as SecurityProblem.CloudBackupNotWorking },
                            isBackupNeeded = securityProblems.any {
                                (it is SecurityProblem.EntitiesNotRecoverable && it.personasNeedBackup > 0)
                            },
                            isRecoveryNeeded = securityProblems.any {
                                it is SecurityProblem.SeedPhraseNeedRecovery && it.isAnyActivePersonaAffected
                            }
                        )
                    )
                }
            )
        }
        SettingsUiState(mutated.toPersistentList())
    }.flowOn(defaultDispatcher).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(Constants.VM_STOP_TIMEOUT_MS),
        SettingsUiState(defaultSettings.toPersistentList())
    )
}

data class SettingsUiState(
    val settings: ImmutableList<SettingsUiItem>
) {

    val debugBuildInformation: DebugBuildInformation?
        get() = if (EXPERIMENTAL_FEATURES_ENABLED) DebugBuildInformation else null
}

data object DebugBuildInformation {

    const val SIGNALING_SERVER: String = rdx.works.peerdroid.BuildConfig.SIGNALING_SERVER_URL
    val sargonInfo: SargonBuildInformation = Sargon.buildInformation.apply {
        Timber.d(this.dependencies.toString())
    }
}

sealed interface SettingsUiItem {
    data object Spacer : SettingsUiItem
    data class Settings(val item: SettingsItem.TopLevelSettings) : SettingsUiItem
}
