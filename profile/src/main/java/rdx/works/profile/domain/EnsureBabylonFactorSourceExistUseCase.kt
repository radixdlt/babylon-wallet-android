package rdx.works.profile.domain

import kotlinx.coroutines.flow.first
import rdx.works.core.mapWhen
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.toIdentifiedArrayList
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.extensions.addMainBabylonDeviceFactorSource
import rdx.works.profile.data.model.extensions.mainBabylonFactorSource
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
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

    suspend operator fun invoke(): Profile {
        val profile = profileRepository.profile.first()
        if (profile.mainBabylonFactorSource() != null) return profile
        val deviceInfo = deviceInfoRepository.getDeviceInfo()
        val mnemonic = mnemonicRepository()
        val deviceFactorSource = DeviceFactorSource.babylon(
            mnemonicWithPassphrase = mnemonic,
            model = deviceInfo.model,
            name = deviceInfo.name,
            createdAt = Instant.now(),
            isMain = true
        )
        val updatedProfile = profile.addMainBabylonDeviceFactorSource(
            mainBabylonFactorSource = deviceFactorSource
        )
        profileRepository.saveProfile(updatedProfile)
        return updatedProfile
    }

    fun initBabylonFactorSourceWithMnemonic(mnemonic: MnemonicWithPassphrase, isMain: Boolean = false): DeviceFactorSource {
        return DeviceFactorSource.babylon(
            mnemonicWithPassphrase = mnemonic,
            model = deviceInfo.model,
            name = deviceInfo.name,
            createdAt = Instant.now(),
            isMain = isMain
        )
    }

    suspend fun addBabylonFactorSource(mnemonic: MnemonicWithPassphrase): Profile {
        val profile = profileRepository.profile.first()
        val deviceInfo = deviceInfoRepository.getDeviceInfo()
        val deviceFactorSource = DeviceFactorSource.babylon(
            mnemonicWithPassphrase = mnemonic,
            model = deviceInfo.model,
            name = deviceInfo.name,
            createdAt = Instant.now(),
            isMain = false
        )
        val existingFactorSource = profile.factorSources.filterIsInstance<DeviceFactorSource>().find {
            it.id == deviceFactorSource.id
        }
        val updatedProfile = if (existingFactorSource != null) {
            if (existingFactorSource.supportsOlympia) {
                profileRepository.updateProfile { p ->
                    p.copy(
                        factorSources = p.factorSources.mapWhen(predicate = {
                            it.id == existingFactorSource.id
                        }, mutation = {
                            existingFactorSource.copy(
                                common = existingFactorSource.common.copy(
                                    cryptoParameters = FactorSource.Common.CryptoParameters.olympiaBackwardsCompatible
                                )
                            )
                        }).toIdentifiedArrayList()
                    )
                }
            } else {
                profile
            }
        } else {
            mnemonicRepository.saveMnemonic(deviceFactorSource.id, mnemonic)
            preferencesManager.markFactorSourceBackedUp(deviceFactorSource.id.body.value)
            profileRepository.updateProfile { p ->
                p.copy(
                    factorSources = (p.factorSources + deviceFactorSource).toIdentifiedArrayList()
                )
            }
        }
        return updatedProfile
    }

    suspend fun babylonFactorSourceExist(): Boolean {
        return profileRepository.profile.first().mainBabylonFactorSource() != null
    }
}
