package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.FactorSources
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import rdx.works.core.mapWhen
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.factorSourceById
import rdx.works.core.sargon.olympia
import rdx.works.core.sargon.olympiaBackwardsCompatible
import rdx.works.core.sargon.supportsBabylon
import rdx.works.profile.data.repository.HostInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.updateProfile
import javax.inject.Inject

class AddOlympiaFactorSourceUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val profileRepository: ProfileRepository,
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager,
    private val hostInfoRepository: HostInfoRepository,
) {

    suspend operator fun invoke(mnemonicWithPassphrase: MnemonicWithPassphrase): Result<FactorSourceId.Hash> {
        val hostInfo = hostInfoRepository.getHostInfo()
        val olympiaFactorSource = FactorSource.Device.olympia(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            hostInfo = hostInfo
        )
        val existingFactorSource = getProfileUseCase().factorSourceById(olympiaFactorSource.id) as? FactorSource.Device
        val mnemonicExist = mnemonicRepository.mnemonicExist(olympiaFactorSource.value.id.asGeneral())
        return if (mnemonicExist) {
            existingFactorSource?.let { existing ->
                if (existingFactorSource.value.common.cryptoParameters.supportsBabylon) {
                    profileRepository.updateProfile { profile ->
                        profile.copy(
                            factorSources = FactorSources(
                                profile.factorSources.mapWhen(predicate = {
                                    it.id == existing.id
                                }, mutation = {
                                    existing.value.copy(
                                        common = existing.value.common.copy(
                                            cryptoParameters = FactorSourceCryptoParameters.olympiaBackwardsCompatible
                                        )
                                    ).asGeneral()
                                })
                            ).asList()
                        )
                    }
                }
            }
            Result.success(olympiaFactorSource.value.id.asGeneral())
        } else {
            // factor source exist without mnemonic, update mnemonic
            mnemonicRepository.saveMnemonic(
                key = olympiaFactorSource.value.id.asGeneral(),
                mnemonicWithPassphrase = mnemonicWithPassphrase
            ).fold(onSuccess = {
                preferencesManager.markFactorSourceBackedUp(olympiaFactorSource.value.id.asGeneral())
                // we save factor source id only if it does not exist
                if (existingFactorSource == null) {
                    profileRepository.updateProfile { profile ->
                        profile.copy(
                            factorSources = FactorSources(profile.factorSources + olympiaFactorSource).asList()
                        )
                    }
                }
                Result.success(olympiaFactorSource.value.id.asGeneral())
            }, onFailure = {
                Result.failure(ProfileException.SecureStorageAccess)
            })
        }
    }
}
