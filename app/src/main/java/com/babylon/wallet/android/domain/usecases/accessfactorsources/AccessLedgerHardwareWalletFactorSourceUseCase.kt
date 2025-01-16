package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.domain.usecases.signing.SignWithLedgerFactorSourceUseCase
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import rdx.works.core.UUIDGenerator
import javax.inject.Inject

class AccessLedgerHardwareWalletFactorSourceUseCase @Inject constructor(
    private val signWithLedgerFactorSourceUseCase: SignWithLedgerFactorSourceUseCase,
    private val ledgerMessenger: LedgerMessenger,
) : AccessFactorSourceUseCase<FactorSource.Ledger> {

    override suspend fun derivePublicKeys(
        factorSource: FactorSource.Ledger,
        input: KeyDerivationRequestPerFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> {
        val factorInstances = input.derivationPaths.map { derivationPath ->
            ledgerMessenger.sendDerivePublicKeyRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                keyParameters = listOf(LedgerInteractionRequest.KeyParameters.from(derivationPath)),
                ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(factorSource = factorSource)
            ).map { derivePublicKeyResponse ->
                HierarchicalDeterministicFactorInstance(
                    factorSourceId = factorSource.value.id,
                    publicKey = HierarchicalDeterministicPublicKey(
                        publicKey = PublicKey.init(derivePublicKeyResponse.publicKeysHex.first().publicKeyHex),
                        derivationPath = derivationPath
                    )
                )
            }.getOrElse {
                return Result.failure(it)
            }
        }

        return Result.success(factorInstances)
    }

    override suspend fun signMono(
        factorSource: FactorSource.Ledger,
        input: PerFactorSourceInput<out Signable.Payload, out Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> = signWithLedgerFactorSourceUseCase.mono(
        ledgerFactorSource = factorSource,
        input = input
    )
}
