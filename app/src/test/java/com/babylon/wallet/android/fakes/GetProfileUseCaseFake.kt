package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.mockdata.profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    override suspend fun saveProfile(profile: Profile) {
        profileStateSource.value = ProfileState.Restored(profile)
    }

    override suspend fun clear() {
        profileStateSource.value = ProfileState.None
    }

}
