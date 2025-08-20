package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common.ArculusCardClient
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.extensions.id
import kotlinx.coroutines.channels.Channel
import rdx.works.profile.domain.UpdateFactorSourceLastUsedUseCase
import javax.inject.Inject

class AccessArculusFactorSourceUseCase @Inject constructor(
    private val arculusCardClient: ArculusCardClient,
    private val updateFactorSourceLastUsedUseCase: UpdateFactorSourceLastUsedUseCase
) : AccessFactorSource<FactorSource.ArculusCard> {

    private val pinChannel = Channel<String>()

    override suspend fun derivePublicKeys(
        factorSource: FactorSource.ArculusCard,
        input: KeyDerivationRequestPerFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> {
        return arculusCardClient.derivePublicKeys(factorSource, input.derivationPaths)
            .onSuccess {
                updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
            }
    }

    override suspend fun signMono(
        factorSource: FactorSource.ArculusCard,
        input: AccessFactorSourcesInput.Sign
    ): Result<AccessFactorSourcesOutput.Sign> {
        val pin = pinChannel.receive()
        return arculusCardClient.sign(factorSource, input, pin).onSuccess {
            updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
        }
    }

    override suspend fun spotCheck(factorSource: FactorSource.ArculusCard): Result<Boolean> {
        TODO("Future implementation")
    }

    suspend fun onPinForSigningConfirmed(pin: String) {
        pinChannel.send(pin)
    }
}
