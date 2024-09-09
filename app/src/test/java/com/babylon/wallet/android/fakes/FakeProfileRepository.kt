package com.babylon.wallet.android.fakes

import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileState
import com.radixdlt.sargon.extensions.fromJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import rdx.works.profile.data.repository.ProfileRepository

class FakeProfileRepository(
    initialProfileSate: ProfileState = ProfileState.None
): ProfileRepository {

    constructor(profile: Profile): this(initialProfileSate = ProfileState.Loaded(profile))

    override val profileState: MutableStateFlow<ProfileState> = MutableStateFlow(initialProfileSate)
    override val inMemoryProfileOrNull: Profile?
        get() = (profileState.value as? ProfileState.Loaded)?.v1

    override suspend fun saveProfile(profile: Profile) {
        profileState.update { ProfileState.Loaded(profile) }
    }

    override suspend fun clearAllWalletData() {
        profileState.update { ProfileState.None }
    }

    override fun deriveProfileState(content: String): ProfileState = runCatching {
        Profile.fromJson(jsonString = content)
    }.fold(
        onSuccess = { ProfileState.Loaded(it) },
        onFailure = { ProfileState.Incompatible(it as CommonException) }
    )

    fun update(onUpdate: (Profile) -> Profile): Profile {
        val newState = profileState.updateAndGet {
            val current = (it as ProfileState.Loaded).v1
            ProfileState.Loaded(onUpdate(current))
        } as ProfileState.Loaded

        return newState.v1
    }
}