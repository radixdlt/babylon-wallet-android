package com.babylon.wallet.android.presentation.accessfactorsources.access

import com.babylon.wallet.android.domain.usecases.signing.SignWithDeviceFactorSourceUseCase
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import javax.inject.Inject

class AccessDeviceFactorSource @Inject constructor(
    private val signWithDeviceFactorSourceUseCase: SignWithDeviceFactorSourceUseCase,
): AccessFactorSource<FactorSource.Device> {

    override suspend fun signMono(
        factorSource: FactorSource.Device,
        input: PerFactorSourceInput<Signable.Payload, Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> = signWithDeviceFactorSourceUseCase.mono(
        deviceFactorSource = factorSource,
        input = input
    )
}