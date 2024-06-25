package rdx.works.profile

import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.fromJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import rdx.works.core.domain.ProfileState
import rdx.works.profile.data.repository.ProfileRepository

class FakeProfileRepository(
    initialProfileSate: ProfileState = ProfileState.None
): ProfileRepository {

    constructor(profile: Profile): this(initialProfileSate = ProfileState.Restored(profile))

    override val profileState: MutableStateFlow<ProfileState> = MutableStateFlow(initialProfileSate)
    override val inMemoryProfileOrNull: Profile?
        get() = (profileState.value as? ProfileState.Restored)?.profile

    override suspend fun saveProfile(profile: Profile) {
        profileState.update { ProfileState.Restored(profile) }
    }

    override suspend fun clearProfileDataOnly() {
        profileState.update { ProfileState.None }
    }

    override suspend fun clearAllWalletData() {
        profileState.update { ProfileState.None }
    }

    override fun deriveProfileState(content: String): ProfileState = runCatching {
        Profile.fromJson(jsonString = content)
    }.fold(
        onSuccess = { ProfileState.Restored(it) },
        onFailure = { ProfileState.Incompatible(it) }
    )

    fun update(onUpdate: (Profile) -> Profile): Profile {
        val newState = profileState.updateAndGet {
            val current = (it as ProfileState.Restored).profile
            ProfileState.Restored(onUpdate(current))
        } as ProfileState.Restored

        return newState.profile
    }
}