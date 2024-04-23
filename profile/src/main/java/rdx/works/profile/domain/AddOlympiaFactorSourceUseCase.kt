package rdx.works.profile.domain

import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSources
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import rdx.works.core.mapWhen
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.factorSourceById
import rdx.works.core.sargon.olympia
import rdx.works.core.sargon.olympiaBackwardsCompatible
import rdx.works.core.sargon.supportsBabylon
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.updateProfile
import javax.inject.Inject

class AddOlympiaFactorSourceUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val profileRepository: ProfileRepository,
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager
) {

    suspend operator fun invoke(mnemonicWithPassphrase: MnemonicWithPassphrase): FactorSourceId.Hash {
        val olympiaFactorSource = DeviceFactorSource.olympia(mnemonicWithPassphrase)
        val existingFactorSource = getProfileUseCase().factorSourceById(olympiaFactorSource.id.asGeneral()) as? FactorSource.Device
        val mnemonicExist = mnemonicRepository.mnemonicExist(olympiaFactorSource.id.asGeneral())
        if (mnemonicExist) {
            existingFactorSource?.let { existing ->
                if (existingFactorSource.value.common.cryptoParameters.supportsBabylon) {
                    profileRepository.updateProfile { profile ->
                        profile.copy(
                            factorSources = FactorSources.init(
                                profile.factorSources().mapWhen(predicate = {
                                    it.id == existing.id
                                }, mutation = {
                                    existing.value.copy(
                                        common = existing.value.common.copy(
                                            cryptoParameters = FactorSourceCryptoParameters.olympiaBackwardsCompatible
                                        )
                                    ).asGeneral()
                                })
                            )
                        )
                    }
                }
            }
            return olympiaFactorSource.id.asGeneral()
        } else {
            // factor source exist without mnemonic, update mnemonic
            mnemonicRepository.saveMnemonic(
                key = olympiaFactorSource.id.asGeneral(),
                mnemonicWithPassphrase = mnemonicWithPassphrase
            )
            // Seed phrase was just typed by the user, mark it as backed up
            preferencesManager.markFactorSourceBackedUp(olympiaFactorSource.id.asGeneral())
            // we save factor source id only if it does not exist
            if (existingFactorSource == null) {
                profileRepository.updateProfile { profile ->
                    profile.copy(
                        factorSources = FactorSources.init(profile.factorSources() + olympiaFactorSource.asGeneral())
                    )
                }
            }
            return olympiaFactorSource.id.asGeneral()
        }
    }
}
