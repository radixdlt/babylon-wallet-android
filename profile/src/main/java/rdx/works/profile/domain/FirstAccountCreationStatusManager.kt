package rdx.works.profile.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirstAccountCreationStatusManager @Inject constructor() {

    private val _firstAccountCreationStatus: MutableStateFlow<FirstAccountCreationStatus> =
        MutableStateFlow(FirstAccountCreationStatus.None)
    val firstAccountCreationStatus: StateFlow<FirstAccountCreationStatus> =
        _firstAccountCreationStatus.asStateFlow()

    fun onFirstAccountCreationInProgress() {
        _firstAccountCreationStatus.update { FirstAccountCreationStatus.InProgress }
    }

    fun onFirstAccountCreationConfirmed() {
        _firstAccountCreationStatus.update { FirstAccountCreationStatus.None }
    }

    fun onFirstAccountCreationAborted() {
        _firstAccountCreationStatus.update { FirstAccountCreationStatus.None }
    }

}

enum class FirstAccountCreationStatus {
    None,
    InProgress,
}