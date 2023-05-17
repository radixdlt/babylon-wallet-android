package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.transaction.SigningEvent
import com.radixdlt.toolkit.models.crypto.SignatureWithPublicKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import rdx.works.profile.data.model.SigningEntity
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.domain.signing.SignWithDeviceFactorSourceUseCase
import javax.inject.Inject

class CollectSignersSignaturesUseCase @Inject constructor(
    private val signWithDeviceFactorSourceUseCase: SignWithDeviceFactorSourceUseCase,
    private val signWithLedgerFactorSourceUseCase: SignWithLedgerFactorSourceUseCase
) {

    private val _signingEvent = MutableStateFlow<SigningEvent?>(null)
    val signingEvent: Flow<SigningEvent?> = _signingEvent.asSharedFlow()

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
                    _signingEvent.update { SigningEvent.SigningWithLedger(factorSource) }
                    signWithLedgerFactorSourceUseCase(factorSource, signers, dataToSign).onSuccess { signatures ->
                        _signingEvent.update { SigningEvent.SigningWithLedgerSuccess(factorSource.id) }
                        signaturesWithPublicKeys.addAll(signatures)
                    }.onFailure {
                        _signingEvent.update { SigningEvent.SigningWithLedgerFailed(factorSource.id) }
                        return Result.failure(it)
                    }
                }
            }
        }
        return Result.success(signaturesWithPublicKeys)
    }
}
