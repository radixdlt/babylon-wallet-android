package com.babylon.wallet.android.domain.usecases.transaction

import com.radixdlt.toolkit.models.crypto.SignatureWithPublicKey
import rdx.works.profile.data.model.SigningEntity
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.domain.signing.SignWithDeviceFactorSourceUseCase
import javax.inject.Inject

class CollectSignersSignaturesUseCase @Inject constructor(
    private val signWithDeviceFactorSourceUseCase: SignWithDeviceFactorSourceUseCase,
    private val signWithLedgerFactorSourceUseCase: SignWithLedgerFactorSourceUseCase
) {
    suspend operator fun invoke(
        signersPerFactorSource: Map<FactorSource, List<SigningEntity>>,
        dataToSign: ByteArray
    ): Result<List<SignatureWithPublicKey>> {
        val signaturesWithPublicKeys = mutableListOf<SignatureWithPublicKey>()
        signersPerFactorSource.forEach { (factorSource, signers) ->
            when (factorSource.kind) {
                FactorSourceKind.DEVICE -> {
                    val signatures = signWithDeviceFactorSourceUseCase(factorSource, signers, dataToSign)
                    signaturesWithPublicKeys.addAll(signatures)
                }
                FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> {
                    val signaturesResult = signWithLedgerFactorSourceUseCase(factorSource, signers, dataToSign)
                    signaturesResult.onSuccess { signatures ->
                        signaturesWithPublicKeys.addAll(signatures)
                    }
                    signaturesResult.onFailure {
                        return Result.failure(it)
                    }
                }
                else -> {}
            }
        }
        return Result.success(signaturesWithPublicKeys)
    }
}
