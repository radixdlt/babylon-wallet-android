package rdx.works.profile.domain

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.supportsBabylon
import com.radixdlt.sargon.extensions.supportsOlympia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rdx.works.core.sargon.active
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.hasBabylonSeedPhraseLength
import rdx.works.core.sargon.isDeleted
import rdx.works.core.sargon.isHidden
import rdx.works.core.sargon.usesEd25519
import rdx.works.core.sargon.usesSECP256k1
import javax.inject.Inject

class GetProfileEntitiesConnectedToSeedPhrasesUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    operator fun invoke(): Flow<List<DeviceFactorSourceWithEntities>> {
        return getProfileUseCase.flow.map { profile ->
            val result = mutableListOf<DeviceFactorSourceWithEntities>()

            val deviceFactorSources = profile.factorSources.filterIsInstance<FactorSource.Device>()
            val allAccountsOnNetwork = profile.currentNetwork?.accounts?.filterNot { it.isDeleted }.orEmpty()
            val allPersonasOnNetwork = profile.currentNetwork?.personas?.filterNot { it.isDeleted }.orEmpty()

            deviceFactorSources.forEach { deviceFactorSource ->
                if (deviceFactorSource.supportsOlympia && deviceFactorSource.supportsBabylon) {
                    val olympiaAccounts = allAccountsOnNetwork.filter {
                        it.factorSourceId == deviceFactorSource.id && it.usesSECP256k1
                    }
                    val babylonAccounts = allAccountsOnNetwork.filter {
                        it.factorSourceId == deviceFactorSource.id && it.usesEd25519
                    }
                    val babylonPersonas = allPersonasOnNetwork.filter {
                        it.factorSourceId == deviceFactorSource.id && it.usesEd25519
                    }
                    if (deviceFactorSource.hasBabylonSeedPhraseLength) {
                        result.add(
                            DeviceFactorSourceWithEntities(
                                deviceFactorSource = deviceFactorSource,
                                allAccounts = babylonAccounts,
                                isBabylon = true,
                                personas = babylonPersonas
                            )
                        )
                    }
                    result.add(
                        DeviceFactorSourceWithEntities(
                            deviceFactorSource = deviceFactorSource,
                            allAccounts = olympiaAccounts,
                            isBabylon = false
                        )
                    )
                } else {
                    val accounts = allAccountsOnNetwork.filter { it.factorSourceId == deviceFactorSource.id }
                    val personas = if (deviceFactorSource.supportsBabylon) {
                        allPersonasOnNetwork.filter { it.factorSourceId == deviceFactorSource.id }
                    } else {
                        emptyList()
                    }
                    result.add(
                        DeviceFactorSourceWithEntities(
                            deviceFactorSource = deviceFactorSource,
                            allAccounts = accounts,
                            isBabylon = deviceFactorSource.supportsBabylon,
                            personas = personas
                        )
                    )
                }
            }
            result
        }
    }
}

data class DeviceFactorSourceWithEntities(
    val deviceFactorSource: FactorSource.Device,
    val allAccounts: List<Account> = emptyList(),
    val personas: List<Persona> = emptyList(),
    val mnemonicState: MnemonicState = MnemonicState.NotBackedUp,
    val isBabylon: Boolean = false
) {
    enum class MnemonicState {
        BackedUp, NotBackedUp, NeedRecover
    }

    val activeAccounts = allAccounts.active()

    val hasOnlyHiddenAccounts = allAccounts.isNotEmpty() && allAccounts.all { it.isHidden }
}
