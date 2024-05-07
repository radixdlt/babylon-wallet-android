package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.supportsBabylon
import com.radixdlt.sargon.extensions.supportsOlympia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.hasBabylonSeedPhraseLength
import rdx.works.core.sargon.notHiddenAccounts
import rdx.works.core.sargon.notHiddenPersonas
import rdx.works.core.sargon.usesEd25519
import rdx.works.core.sargon.usesSECP256k1
import javax.inject.Inject

class GetFactorSourcesWithAccountsUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    operator fun invoke(): Flow<List<DeviceFactorSourceData>> {
        return getProfileUseCase.flow.map { profile ->
            val result = mutableListOf<DeviceFactorSourceData>()
            val deviceFactorSources = profile.factorSources().filterIsInstance<FactorSource.Device>()
            val allAccountsOnNetwork = profile.currentNetwork?.accounts()?.notHiddenAccounts().orEmpty()
            val allPersonasOnNetwork = profile.currentNetwork?.personas()?.notHiddenPersonas().orEmpty()
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
                                accounts = babylonAccounts,
                                isBabylon = true,
                                personas = babylonPersonas
                            )
                        )
                    }
                    result.add(
                        DeviceFactorSourceData(
                            deviceFactorSource = deviceFactorSource,
                            accounts = olympiaAccounts,
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
                        DeviceFactorSourceData(
                            deviceFactorSource = deviceFactorSource,
                            accounts = accounts,
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
