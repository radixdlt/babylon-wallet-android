package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.transaction.SigningEvent
import com.radixdlt.toolkit.models.crypto.SignatureWithPublicKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.SigningPurpose
import rdx.works.profile.domain.signing.GetSigningEntitiesByFactorSourceUseCase
import rdx.works.profile.domain.signing.SignWithDeviceFactorSourceUseCase
import javax.inject.Inject

class CollectSignersSignaturesUseCase @Inject constructor(
    private val signWithDeviceFactorSourceUseCase: SignWithDeviceFactorSourceUseCase,
    private val signWithLedgerFactorSourceUseCase: SignWithLedgerFactorSourceUseCase,
    private val getSigningEntitiesByFactorSourceUseCase: GetSigningEntitiesByFactorSourceUseCase,
) {

    private val _signingEvent = MutableStateFlow<SigningEvent?>(null)
    val signingEvent: Flow<SigningEvent?> = _signingEvent.asSharedFlow()

    suspend operator fun invoke(
        signers: List<Entity>,
        signRequest: SignRequest,
        signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
    ): Result<List<SignatureWithPublicKey>> {
        val signaturesWithPublicKeys = mutableListOf<SignatureWithPublicKey>()
        val signersPerFactorSource = getSigningEntitiesByFactorSourceUseCase(signers)
        signersPerFactorSource.forEach { (factorSource, signers) ->
            when (factorSource.kind) {
                FactorSourceKind.DEVICE -> {
                    val signatures = signWithDeviceFactorSourceUseCase(
                        deviceFactorSource = factorSource,
                        signers = signers,
                        dataToSign = signRequest.dataToSign,
                        signingPurpose = signingPurpose
                    )
                    signaturesWithPublicKeys.addAll(signatures)
                }
                FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> {
                    _signingEvent.update { SigningEvent.SigningWithLedger(factorSource) }
                    signWithLedgerFactorSourceUseCase(
                        ledgerFactorSource = factorSource,
                        signers = signers,
                        signRequest = signRequest,
                        signingPurpose = signingPurpose
                    ).onSuccess { signatures ->
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

sealed class SignRequest(val dataToSign: ByteArray) {
    data class SignTransactionRequest(val compiledTransactionIntent: ByteArray) : SignRequest(compiledTransactionIntent)
    data class SignAuthChallengeRequest(
        private val data: ByteArray,
        val origin: String,
        val dAppDefinitionAddress: String
    ) : SignRequest(data)
}
