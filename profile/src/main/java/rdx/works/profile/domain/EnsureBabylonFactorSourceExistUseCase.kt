package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.FactorSources
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import kotlinx.coroutines.flow.first
import rdx.works.core.TimestampGenerator
import rdx.works.core.mapWhen
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.addMainBabylonDeviceFactorSource
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.core.sargon.olympiaBackwardsCompatible
import rdx.works.core.sargon.supportsOlympia
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.repository.updateProfile
import rdx.works.profile.domain.TestData.deviceInfo
import java.time.Instant
import javax.inject.Inject

class EnsureBabylonFactorSourceExistUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val preferencesManager: PreferencesManager
) {

    suspend operator fun invoke(): Result<Profile> {
        val profile = profileRepository.profile.first()
        if (profile.mainBabylonFactorSource() != null) return Result.success(profile)
        val deviceInfo = deviceInfoRepository.getDeviceInfo()
        return mnemonicRepository.createNew().fold(onSuccess = { mnemonic ->
            val deviceFactorSource = FactorSource.Device.babylon(
                mnemonicWithPassphrase = mnemonic,
                model = deviceInfo.model,
                name = deviceInfo.name,
                createdAt = TimestampGenerator(),
                isMain = true
            )
            val updatedProfile = profile.addMainBabylonDeviceFactorSource(
                mainBabylonFactorSource = deviceFactorSource
            )
            profileRepository.saveProfile(updatedProfile)
            updatedProfile
        }
    }

    suspend fun addBabylonFactorSource(mnemonic: MnemonicWithPassphrase): Result<Profile> {
        val profile = profileRepository.profile.first()
        val deviceInfo = deviceInfoRepository.getDeviceInfo()
        val deviceFactorSource = FactorSource.Device.babylon(
            mnemonicWithPassphrase = mnemonic,
            model = deviceInfo.model,
            name = deviceInfo.name,
            createdAt = TimestampGenerator(),
            isMain = false
        )
        val existingFactorSource = profile.factorSources.filterIsInstance<FactorSource.Device>().find {
            it.id == deviceFactorSource.id
        }
        return if (existingFactorSource != null) {
            val updatedProfile = if (existingFactorSource.supportsOlympia) {
                profileRepository.updateProfile { p ->
                    p.copy(
                        factorSources = FactorSources(
                            p.factorSources.mapWhen(predicate = {
                                it.id == existingFactorSource.id
                            }, mutation = {
                                existingFactorSource.copy(
                                    value = existingFactorSource.value.copy(
                                        common = existingFactorSource.value.common.copy(
                                            cryptoParameters = FactorSourceCryptoParameters.olympiaBackwardsCompatible
                                        )
                                    )
                                )
                            })
                        ).asList()
                    )
                }
            } else {
                profile
            }
            preferencesManager.markFactorSourceBackedUp(deviceFactorSource.value.id.asGeneral())
            Result.success(updatedProfile)
        } else {
            mnemonicRepository.saveMnemonic(deviceFactorSource.value.id.asGeneral(), mnemonic).fold(onSuccess = {
                preferencesManager.markFactorSourceBackedUp(deviceFactorSource.value.id.asGeneral())
                Result.success(profileRepository.updateProfile { p ->
                    p.copy(factorSources = p.factorSources.asIdentifiable().append(deviceFactorSource).asList())
                })
            }, onFailure = {
                Result.failure(ProfileException.SecureStorageAccess)
            })
        }
    }

    suspend fun babylonFactorSourceExist(): Boolean {
        return profileRepository.profile.first().mainBabylonFactorSource != null
    }
}
