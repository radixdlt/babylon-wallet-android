package com.babylon.wallet.android.presentation.accessfactorsources.access

import com.babylon.wallet.android.domain.usecases.BiometricsAuthenticateUseCase
import com.babylon.wallet.android.domain.usecases.signing.SignWithDeviceFactorSourceUseCase
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import rdx.works.profile.data.repository.PublicKeyProvider
import javax.inject.Inject

class AccessDeviceFactorSource @Inject constructor(
    private val signWithDeviceFactorSourceUseCase: SignWithDeviceFactorSourceUseCase,
    private val publicKeyProvider: PublicKeyProvider,
    private val biometricsAuthenticateUseCase: BiometricsAuthenticateUseCase,
) : AccessFactorSource<FactorSource.Device> {

    override suspend fun derivePublicKeys(
        factorSource: FactorSource.Device,
        input: KeyDerivationRequestPerFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> = biometricsAuthenticateUseCase.asResult().mapCatching {
        input.derivationPaths.map { derivationPath ->
            publicKeyProvider.deriveHDPublicKeyForDeviceFactorSource(
                deviceFactorSource = factorSource,
                derivationPath = derivationPath
            ).map { hdPublicKey ->
                HierarchicalDeterministicFactorInstance(
                    factorSourceId = factorSource.value.id,
                    publicKey = hdPublicKey
                )
            }.getOrThrow()
        }
    }

    override suspend fun signMono(
        factorSource: FactorSource.Device,
        input: PerFactorSourceInput<out Signable.Payload, out Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> = signWithDeviceFactorSourceUseCase.mono(
        deviceFactorSource = factorSource,
        input = input
    )
}
