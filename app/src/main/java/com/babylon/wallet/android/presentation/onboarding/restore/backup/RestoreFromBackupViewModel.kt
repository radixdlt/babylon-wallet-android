package com.babylon.wallet.android.presentation.onboarding.restore.backup

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.CanSignInToGoogle
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.Header
import com.radixdlt.sargon.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.isCompatible
import rdx.works.profile.cloudbackup.data.GoogleSignInManager
import rdx.works.profile.cloudbackup.model.GoogleAccount
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.CloudBackupFileEntity
import rdx.works.profile.domain.backup.DownloadBackedUpProfileFromCloud
import rdx.works.profile.domain.backup.FetchBackedUpProfilesMetadataFromCloud
import rdx.works.profile.domain.backup.GetTemporaryRestoringProfileForBackupUseCase
import rdx.works.profile.domain.backup.SaveTemporaryRestoringSnapshotUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
@Suppress("TooManyFunctions")
class RestoreFromBackupViewModel @Inject constructor(
    private val fetchBackedUpProfilesMetadataFromCloud: FetchBackedUpProfilesMetadataFromCloud,
    private val downloadBackedUpProfileFromCloud: DownloadBackedUpProfileFromCloud,
    private val getTemporaryRestoringProfileForBackupUseCase: GetTemporaryRestoringProfileForBackupUseCase,
    private val saveTemporaryRestoringSnapshotUseCase: SaveTemporaryRestoringSnapshotUseCase,
    private val googleSignInManager: GoogleSignInManager,
    private val deviceInfoRepository: DeviceInfoRepository
) : StateViewModel<RestoreFromBackupViewModel.State>(),
    CanSignInToGoogle,
    OneOffEventHandler<RestoreFromBackupViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        // if user has enabled cloud backup (from previous screen) then restore profiles from cloud backup
        googleSignInManager.getSignedInGoogleAccount()?.email?.let { email ->
            _state.update { it.copy(backupEmail = email) }
            viewModelScope.launch {
                restoreProfilesFromCloudBackup()
            }
        }
    }

    fun onRestoringProfileSelected(restoringProfile: State.RestoringProfile) {
        _state.update { state ->
            state.copy(
                restoringProfiles = state.restoringProfiles?.map {
                    if (restoringProfile == it.data) {
                        it.copy(selected = true)
                    } else {
                        it.copy(selected = false)
                    }
                }?.toImmutableList()
            )
        }
    }

    fun onRestoreFromFile(uri: Uri) = viewModelScope.launch {
        saveTemporaryRestoringSnapshotUseCase.forFile(uri = uri, BackupType.File.PlainText)
            .onSuccess {
                sendEvent(Event.OnRestoreConfirmed(BackupType.File.PlainText))
            }.onFailure { error ->
                when (error) {
                    is ProfileException.InvalidPassword -> _state.update {
                        it.copy(passwordSheetState = State.PasswordSheet.Open(file = uri))
                    }

                    is ProfileException.InvalidSnapshot -> _state.update {
                        it.copy(uiMessage = UiMessage.ErrorMessage(error))
                    }
                }
            }
    }

    fun onLoginToGoogleClick() = viewModelScope.launch {
        _state.update { it.copy(isAccessToGoogleDriveInProgress = true) }

        sendEvent(Event.SignInToGoogle)
    }

    override fun signInManager(): GoogleSignInManager = googleSignInManager

    override fun onSignInResult(result: Result<GoogleAccount>) {
        _state.update { state ->
            state.copy(isAccessToGoogleDriveInProgress = false)
        }

        viewModelScope.launch {
            result.onSuccess { googleAccount ->
                _state.update { it.copy(backupEmail = googleAccount.email) }
                Timber.tag("CloudBackup").d("\uD83D\uDD11 Authorized for email: ${googleAccount.email}")
                restoreProfilesFromCloudBackup()
            }.onFailure { exception ->
                Timber.tag("CloudBackup").w("cloud backup authorization failed: $exception")
                if (exception !is CancellationException) {
                    _state.update { state ->
                        state.copy(
                            backupEmail = "",
                            uiMessage = UiMessage.ErrorMessage(exception)
                        )
                    }
                }
            }
        }
    }

    fun onPasswordTyped(password: String) {
        val sheet = state.value.passwordSheetState as? State.PasswordSheet.Open ?: return
        _state.update {
            it.copy(passwordSheetState = sheet.copy(password = password, isPasswordInvalid = false))
        }
    }

    fun onPasswordRevealToggle() {
        val sheet = state.value.passwordSheetState as? State.PasswordSheet.Open ?: return
        _state.update {
            it.copy(passwordSheetState = sheet.copy(isPasswordRevealed = !sheet.isPasswordRevealed))
        }
    }

    fun onPasswordSubmitted() {
        val sheet = state.value.passwordSheetState as? State.PasswordSheet.Open ?: return
        if (sheet.isSubmitEnabled) {
            viewModelScope.launch {
                saveTemporaryRestoringSnapshotUseCase.forFile(
                    uri = sheet.file,
                    fileBackupType = BackupType.File.Encrypted(sheet.password)
                )
                    .onSuccess {
                        _state.update { state -> state.copy(passwordSheetState = State.PasswordSheet.Closed) }
                        delay(Constants.DELAY_300_MS)
                        sendEvent(Event.OnRestoreConfirmed(BackupType.File.Encrypted(sheet.password)))
                    }.onFailure { error ->
                        when (error) {
                            is ProfileException.InvalidPassword -> _state.update {
                                it.copy(passwordSheetState = sheet.copy(isPasswordInvalid = true))
                            }

                            is ProfileException.InvalidSnapshot -> _state.update {
                                it.copy(
                                    passwordSheetState = State.PasswordSheet.Closed,
                                    uiMessage = UiMessage.ErrorMessage(ProfileException.InvalidSnapshot)
                                )
                            }
                        }
                    }
            }
        }
    }

    fun onBackClick() {
        if (!state.value.isPasswordSheetVisible) {
            viewModelScope.launch { sendEvent(Event.OnDismiss) }
        } else {
            _state.update { it.copy(passwordSheetState = State.PasswordSheet.Closed) }
        }
    }

    fun onContinueClick() = viewModelScope.launch {
        val restoringProfile = state.value.restoringProfiles?.first { selectable -> selectable.selected }?.data ?: return@launch

        when (restoringProfile) {
            is State.RestoringProfile.GoogleDrive -> {
                _state.update { it.copy(isDownloadingSelectedCloudBackup = true) }

                downloadBackedUpProfileFromCloud(
                    entity = restoringProfile.entity
                ).fold(
                    onSuccess = { cloudBackupFile ->
                        val backupType = BackupType.Cloud(restoringProfile.entity)
                        saveTemporaryRestoringSnapshotUseCase.forCloud(cloudBackupFile.serializedProfile, backupType)
                        _state.update { it.copy(isDownloadingSelectedCloudBackup = false) }
                        sendEvent(Event.OnRestoreConfirmed(backupType))
                    },
                    onFailure = { exception ->
                        _state.update {
                            it.copy(
                                uiMessage = UiMessage.ErrorMessage(exception),
                                isDownloadingSelectedCloudBackup = false
                            )
                        }
                    }
                )
            }

            is State.RestoringProfile.DeprecatedCloudBackup -> sendEvent(Event.OnRestoreConfirmed(BackupType.DeprecatedCloud))
        }
    }

    fun onMessageShown() = _state.update { it.copy(uiMessage = null) }

    private suspend fun restoreProfilesFromCloudBackup() {
        val availableCloudBackedUpProfiles = fetchBackedUpProfilesMetadataFromCloud()
            .onFailure {
                Timber.tag("CloudBackup").w(it)
            }
            .getOrNull()

        val deviceInfo = deviceInfoRepository.getDeviceInfo()
        if (availableCloudBackedUpProfiles?.isNotEmpty() == true) {
            val restoringProfiles = availableCloudBackedUpProfiles.map { fileEntity ->
                Selectable<State.RestoringProfile>(
                    data = State.RestoringProfile.GoogleDrive(
                        entity = fileEntity,
                        isBackedUpByTheSameDevice = fileEntity.header.lastUsedOnDevice.id == deviceInfo.id
                    )
                )
            }
            _state.update {
                it.copy(restoringProfiles = restoringProfiles.toPersistentList())
            }
        } else {
            getTemporaryRestoringProfileForBackupUseCase(BackupType.DeprecatedCloud)?.let { profile ->
                if (profile.header.isCompatible) {
                    val restoringProfile = Selectable<State.RestoringProfile>(
                        data = State.RestoringProfile.DeprecatedCloudBackup(
                            header = profile.header,
                            isBackedUpByTheSameDevice = profile.header.lastUsedOnDevice.id == deviceInfo.id
                        )
                    )
                    _state.update {
                        it.copy(restoringProfiles = persistentListOf(restoringProfile))
                    }
                }
            } ?: _state.update { it.copy(restoringProfiles = persistentListOf()) }
        }
    }

    data class State(
        private val backupEmail: String = "",
        val isAccessToGoogleDriveInProgress: Boolean = false,
        val restoringProfiles: ImmutableList<Selectable<RestoringProfile>>? = null,
        val isDownloadingSelectedCloudBackup: Boolean = false,
        val passwordSheetState: PasswordSheet = PasswordSheet.Closed,
        val uiMessage: UiMessage? = null
    ) : UiState {

        sealed interface RestoringProfile {

            val isBackedUpByTheSameDevice: Boolean
            val deviceDescription: String
            val lastModified: Timestamp
            val totalNumberOfAccountsOnAllNetworks: Int
            val totalNumberOfPersonasOnAllNetworks: Int

            data class GoogleDrive(
                val entity: CloudBackupFileEntity,
                override val isBackedUpByTheSameDevice: Boolean
            ) : RestoringProfile {
                override val deviceDescription: String = entity.header.lastUsedOnDevice.description
                override val lastModified: Timestamp = entity.header.lastModified
                override val totalNumberOfAccountsOnAllNetworks: Int =
                    entity.header.contentHint.numberOfAccountsOnAllNetworksInTotal.toInt()
                override val totalNumberOfPersonasOnAllNetworks: Int =
                    entity.header.contentHint.numberOfPersonasOnAllNetworksInTotal.toInt()
            }

            data class DeprecatedCloudBackup(
                val header: Header,
                override val isBackedUpByTheSameDevice: Boolean
            ) : RestoringProfile {
                override val deviceDescription: String = header.lastUsedOnDevice.description
                override val lastModified: Timestamp = header.lastModified
                override val totalNumberOfAccountsOnAllNetworks: Int = header.contentHint.numberOfAccountsOnAllNetworksInTotal.toInt()
                override val totalNumberOfPersonasOnAllNetworks: Int = header.contentHint.numberOfPersonasOnAllNetworksInTotal.toInt()
            }
        }

        val isCloudBackupAuthorized: Boolean
            get() = backupEmail.isEmpty().not()

        val isPasswordSheetVisible: Boolean
            get() = passwordSheetState is PasswordSheet.Open

        val isContinueEnabled: Boolean
            get() {
                val isRestoringProfileSelected = restoringProfiles?.any { restoringProfile ->
                    restoringProfile.selected
                } == true

                return if (isRestoringProfileSelected) {
                    isDownloadingSelectedCloudBackup.not()
                } else {
                    false
                }
            }

        sealed interface PasswordSheet {
            data object Closed : PasswordSheet
            data class Open(
                val password: String = "",
                val isPasswordInvalid: Boolean = false,
                val isPasswordRevealed: Boolean = false,
                val file: Uri
            ) : PasswordSheet {

                val isSubmitEnabled: Boolean
                    get() = password.isNotBlank() && !isPasswordInvalid
            }
        }
    }

    sealed interface Event : OneOffEvent {
        data object OnDismiss : Event
        data object SignInToGoogle : Event
        data class OnRestoreConfirmed(val backupType: BackupType) : Event
    }
}
