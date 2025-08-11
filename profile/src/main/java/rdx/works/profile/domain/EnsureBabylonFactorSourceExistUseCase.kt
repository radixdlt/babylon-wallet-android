package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Profile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.di.DefaultDispatcher
import rdx.works.core.sargon.addFirstBabylonDeviceFactorSource
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.babylonFactorSources
import rdx.works.profile.data.repository.HostInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import timber.log.Timber
import javax.inject.Inject

class EnsureBabylonFactorSourceExistUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository,
    private val hostInfoRepository: HostInfoRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(): Result<Profile> {
        val profile = profileRepository.profile.first()

        if (profile.babylonFactorSources.isNotEmpty()) {
            return Result.success(profile)
        }

        val hostInfo = hostInfoRepository.getHostInfo()

        return withContext(dispatcher) {
            mnemonicRepository.createNew().fold(
                onSuccess = { mnemonic ->
                    val deviceFactorSource = FactorSource.Device.babylon(
                        mnemonicWithPassphrase = mnemonic,
                        hostInfo = hostInfo
                    )
                    val updatedProfile = profile.addFirstBabylonDeviceFactorSource(
                        firstBabylonFactorSource = deviceFactorSource
                    )
                    profileRepository.saveProfile(updatedProfile)
                    Result.success(updatedProfile)
                },
                onFailure = {
                    Timber.d(it)
                    Result.failure(ProfileException.SecureStorageAccess)
                }
            )
        }
    }

    suspend fun babylonFactorSourceExist(): Boolean {
        return profileRepository.profile.first().babylonFactorSources.isNotEmpty()
    }
}
