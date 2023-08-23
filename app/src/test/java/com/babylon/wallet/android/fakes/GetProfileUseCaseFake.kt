package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.mockdata.profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase

fun fakeGetProfileUseCase(
    initialProfileState: ProfileState = ProfileState.Restored(profile = profile())
) = GetProfileUseCase(profileRepository = fakeProfileDataSource(initialProfileState = initialProfileState))

private fun fakeProfileDataSource(initialProfileState: ProfileState) = object : ProfileRepository {

    private val profileStateSource: MutableStateFlow<ProfileState> = MutableStateFlow(
        initialProfileState
    )

    override val profileState: Flow<ProfileState> = profileStateSource

    override val inMemoryProfileOrNull: Profile?
        get() = (profileStateSource.value as? ProfileState.Restored)?.profile

    override suspend fun saveProfile(profile: Profile) {
        profileStateSource.update { ProfileState.Restored(profile) }
    }

    override suspend fun clear() {
        profileStateSource.update { ProfileState.None() }
    }

    override suspend fun saveRestoringSnapshot(snapshotSerialised: String): Boolean {
        // Not needed
        return true
    }

    override suspend fun getSnapshotForCloudBackup(): String? {
        // Not needed
        return null
    }

    override suspend fun getSnapshotForFileBackup(): String? {
        // Not needed
        return null
    }

    override suspend fun isRestoredProfileFromBackupExists(): Boolean {
        // Not needed
        return false
    }

    override suspend fun getRestoringProfileFromBackup(): Profile? {
        // Not needed
        return null
    }

    override suspend fun discardBackedUpProfile() {
        // Not needed
    }

}
