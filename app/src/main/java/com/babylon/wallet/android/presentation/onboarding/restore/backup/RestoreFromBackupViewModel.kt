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
import com.radixdlt.sargon.ProfileId
import com.radixdlt.sargon.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.cloudbackup.GoogleDriveFileId
import rdx.works.core.sargon.isCompatible
import rdx.works.profile.cloudbackup.GoogleSignInManager
import rdx.works.profile.cloudbackup.model.GoogleAccount
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
    private val googleSignInManager: GoogleSignInManager
) : StateViewModel<RestoreFromBackupViewModel.State>(),
    CanSignInToGoogle,
    OneOffEventHandler<RestoreFromBackupViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        // if user has enabled cloud backup (from previous screen) then restore profiles from cloud backup
        googleSignInManager.getSignedInGoogleAccount()?.email?.let { email ->
            _state.update { it.copy(backupEmail = email) }
            viewModelScope.launch {
                println("☁\uFE0F -----> restoreProfilesFromCloudBackup at init")
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
            result.onSuccess {googleAccount ->
                _state.update { it.copy(backupEmail = googleAccount.email) }
                Timber.d("cloud backup is authorized")
                println("☁\uFE0F -----> restoreProfilesFromCloudBackup at handleSignInResult")
                restoreProfilesFromCloudBackup()
            }.onFailure { exception ->
                Timber.e("cloud backup authorization failed: $exception")
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
        val profileToRestore = state.value.restoringProfiles?.first { selectable -> selectable.selected }

        profileToRestore?.let { selectableRestoringProfile ->
            if (selectableRestoringProfile.data.isFromGoogleDrive && selectableRestoringProfile.data.googleDriveFileId != null) {
                _state.update { it.copy(isDownloadingSelectedCloudBackup = true) }

                val entity = CloudBackupFileEntity(
                    id = selectableRestoringProfile.data.googleDriveFileId,
                    profileId = selectableRestoringProfile.data.profileId,
                    lastUsedOnDeviceName = selectableRestoringProfile.data.deviceDescription,
                    lastUsedOnDeviceModified = selectableRestoringProfile.data.lastModified,
                    totalNumberOfAccountsOnAllNetworks = selectableRestoringProfile.data.totalNumberOfAccountsOnAllNetworks,
                    totalNumberOfPersonasOnAllNetworks = selectableRestoringProfile.data.totalNumberOfPersonasOnAllNetworks
                )

                downloadBackedUpProfileFromCloud(
                    entity = entity
                ).fold(
                    onSuccess = { cloudBackupFile ->
                        val backupType = BackupType.Cloud(entity)
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
            } else {
                sendEvent(Event.OnRestoreConfirmed(BackupType.DeprecatedCloud))
            }
        }
    }

    fun onMessageShown() = _state.update { it.copy(uiMessage = null) }

    private suspend fun restoreProfilesFromCloudBackup() {
        val availableCloudBackedUpProfiles = fetchBackedUpProfilesMetadataFromCloud()
            .onFailure {
                Timber.tag("CloudBackup").w(it)
            }
            .getOrNull()

        if (availableCloudBackedUpProfiles?.isNotEmpty() == true) {
            val restoringProfiles = availableCloudBackedUpProfiles.mapNotNull { fileEntity ->
                println("☁\uFE0F -----> at restore, profileId: ${fileEntity.profileId}")
                Selectable(
                    data = State.RestoringProfile(
                        googleDriveFileId = fileEntity.id,
                        profileId = fileEntity.profileId,
                        deviceDescription = fileEntity.lastUsedOnDeviceName,
                        lastModified = fileEntity.lastUsedOnDeviceModified,
                        totalNumberOfAccountsOnAllNetworks = fileEntity.totalNumberOfAccountsOnAllNetworks,
                        totalNumberOfPersonasOnAllNetworks = fileEntity.totalNumberOfPersonasOnAllNetworks
                    )
                )
            }
            _state.update {
                it.copy(restoringProfiles = restoringProfiles.toPersistentList())
            }
        } else {
            getTemporaryRestoringProfileForBackupUseCase(BackupType.DeprecatedCloud)?.let { profile ->
                if (profile.header.isCompatible) {
                    println("☁\uFE0F -----> profileId of old: ${profile.header.id}")
                    val restoringProfile = Selectable(
                        data = State.RestoringProfile(
                            googleDriveFileId = null,
                            profileId = profile.header.id,
                            deviceDescription = profile.header.lastUsedOnDevice.description,
                            lastModified = profile.header.lastUsedOnDevice.date,
                            totalNumberOfAccountsOnAllNetworks = profile.header.contentHint.numberOfAccountsOnAllNetworksInTotal.toInt(),
                            totalNumberOfPersonasOnAllNetworks = profile.header.contentHint.numberOfPersonasOnAllNetworksInTotal.toInt()
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

        data class RestoringProfile(
            val googleDriveFileId: GoogleDriveFileId?,
            val profileId: ProfileId,
            val deviceDescription: String,
            val lastModified: Timestamp,
            val totalNumberOfAccountsOnAllNetworks: Int,
            val totalNumberOfPersonasOnAllNetworks: Int
        ) {
            val isFromGoogleDrive = googleDriveFileId?.id.isNullOrEmpty().not()
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
        data class OnRestoreConfirmed(val backupType: BackupType): Event
    }
}
