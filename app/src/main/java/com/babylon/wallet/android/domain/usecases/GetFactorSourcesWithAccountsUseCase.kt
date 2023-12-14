package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.DeviceFactorSourceData
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.extensions.usesCurve25519
import rdx.works.profile.data.model.extensions.usesSecp256k1
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.notHiddenAccounts
import rdx.works.profile.domain.notHiddenPersonas
import javax.inject.Inject

class GetFactorSourcesWithAccountsUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    operator fun invoke(): Flow<List<DeviceFactorSourceData>> {
        return getProfileUseCase.invoke().map { profile ->
            val result = mutableListOf<DeviceFactorSourceData>()
            val deviceFactorSources = profile.factorSources.filterIsInstance<DeviceFactorSource>()
            val allAccountsOnNetwork = profile.currentNetwork?.accounts?.notHiddenAccounts().orEmpty()
            val allPersonasOnNetwork = profile.currentNetwork?.personas?.notHiddenPersonas().orEmpty()
            deviceFactorSources.forEach { deviceFactorSource ->
                if (deviceFactorSource.supportsOlympia && deviceFactorSource.supportsBabylon) {
                    val olympiaAccounts = allAccountsOnNetwork.filter {
                        it.factorSourceId == deviceFactorSource.id && it.usesSecp256k1
                    }
                    val babylonAccounts = allAccountsOnNetwork.filter {
                        it.factorSourceId == deviceFactorSource.id && it.usesCurve25519
                    }
                    val babylonPersonas = allPersonasOnNetwork.filter {
                        it.factorSourceId == deviceFactorSource.id && it.usesCurve25519
                    }
                    result.add(
                        DeviceFactorSourceData(
                            deviceFactorSource = deviceFactorSource,
                            accounts = babylonAccounts.toPersistentList(),
                            isBabylon = true,
                            personas = babylonPersonas.toPersistentList()
                        )
                    )
                    result.add(
                        DeviceFactorSourceData(
                            deviceFactorSource = deviceFactorSource,
                            accounts = olympiaAccounts.toPersistentList(),
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
                            accounts = accounts.toPersistentList(),
                            isBabylon = deviceFactorSource.supportsBabylon,
                            personas = personas.toPersistentList()
                        )
                    )
                }
            }
            result
        }
    }
}
