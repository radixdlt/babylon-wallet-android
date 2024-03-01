package com.babylon.wallet.android.presentation.mocks

import com.babylon.wallet.android.domain.SampleDataProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase

fun fakeGetProfileUseCase(
    initialProfileState: ProfileState = ProfileState.Restored(profile = SampleDataProvider().sampleProfile())
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

    override suspend fun clearProfileDataOnly() {
        profileStateSource.update { ProfileState.None }
    }

    override suspend fun clearAllWalletData() {
        profileStateSource.update { ProfileState.None }
    }

    override fun deriveProfileState(content: String): ProfileState {
        error("Not needed")
    }
}
