package rdx.works.profile.domain

import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.FactorSources
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.append
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import kotlinx.coroutines.flow.first
import rdx.works.core.TimestampGenerator
import rdx.works.core.mapWhen
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.addMainBabylonDeviceFactorSource
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.core.sargon.olympiaBackwardsCompatible
import rdx.works.core.sargon.supportsOlympia
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.repository.updateProfile
import javax.inject.Inject

class EnsureBabylonFactorSourceExistUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val preferencesManager: PreferencesManager
) {

    suspend operator fun invoke(): Profile {
        val profile = profileRepository.profile.first()
        if (profile.mainBabylonFactorSource != null) return profile
        val deviceInfo = deviceInfoRepository.getDeviceInfo()
        val mnemonic = mnemonicRepository()
        val deviceFactorSource = DeviceFactorSource.babylon(
            mnemonicWithPassphrase = mnemonic,
            model = deviceInfo.model,
            name = deviceInfo.name,
            createdAt = TimestampGenerator(),
            isMain = true
        )
        val updatedProfile = profile.addMainBabylonDeviceFactorSource(
            mainBabylonFactorSource = deviceFactorSource.asGeneral()
        )
        profileRepository.saveProfile(updatedProfile)
        return updatedProfile
    }

    suspend fun addBabylonFactorSource(mnemonic: MnemonicWithPassphrase): Profile {
        val profile = profileRepository.profile.first()
        val deviceInfo = deviceInfoRepository.getDeviceInfo()
        val deviceFactorSource = DeviceFactorSource.babylon(
            mnemonicWithPassphrase = mnemonic,
            model = deviceInfo.model,
            name = deviceInfo.name,
            createdAt = TimestampGenerator(),
            isMain = false
        )
        val existingFactorSource = profile.factorSources().filterIsInstance<FactorSource.Device>().find {
            it.value.id == deviceFactorSource.id
        }
        val updatedProfile = if (existingFactorSource != null) {
            if (existingFactorSource.value.supportsOlympia) {
                profileRepository.updateProfile { p ->
                    p.copy(
                        factorSources = FactorSources.init(
                            p.factorSources().mapWhen(predicate = {
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
                        )
                    )
                }
            } else {
                profile
            }
        } else {
            mnemonicRepository.saveMnemonic(deviceFactorSource.id.asGeneral(), mnemonic)
            profileRepository.updateProfile { p ->
                p.copy(factorSources = p.factorSources.append(deviceFactorSource.asGeneral()))
            }
        }
        preferencesManager.markFactorSourceBackedUp(deviceFactorSource.id.asGeneral())
        return updatedProfile
    }

    suspend fun babylonFactorSourceExist(): Boolean {
        return profileRepository.profile.first().mainBabylonFactorSource != null
    }
}
