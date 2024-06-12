package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.supportsBabylon
import com.radixdlt.sargon.extensions.supportsOlympia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.hasBabylonSeedPhraseLength
import rdx.works.core.sargon.isHidden
import rdx.works.core.sargon.usesEd25519
import rdx.works.core.sargon.usesSECP256k1
import javax.inject.Inject

class GetFactorSourcesWithAccountsUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    operator fun invoke(): Flow<List<DeviceFactorSourceData>> {
        return getProfileUseCase.flow.map { profile ->
            val result = mutableListOf<DeviceFactorSourceData>()
            val deviceFactorSources = profile.factorSources.filterIsInstance<FactorSource.Device>()
            val allAccountsOnNetwork = profile.currentNetwork?.accounts.orEmpty()
            val allPersonasOnNetwork = profile.currentNetwork?.personas.orEmpty()
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
                            DeviceFactorSourceData(
                                deviceFactorSource = deviceFactorSource,
                                allAccounts = babylonAccounts,
                                isBabylon = true,
                                personas = babylonPersonas,
                                hasOnlyHiddenAccounts = babylonAccounts.all { it.isHidden }
                            )
                        )
                    }
                    result.add(
                        DeviceFactorSourceData(
                            deviceFactorSource = deviceFactorSource,
                            allAccounts = olympiaAccounts,
                            isBabylon = false,
                            hasOnlyHiddenAccounts = olympiaAccounts.all { it.isHidden }
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
                        DeviceFactorSourceData(
                            deviceFactorSource = deviceFactorSource,
                            allAccounts = accounts,
                            isBabylon = deviceFactorSource.supportsBabylon,
                            personas = personas,
                            hasOnlyHiddenAccounts = accounts.all { it.isHidden }
                        )
                    )
                }
            }
            result
        }
    }
}
