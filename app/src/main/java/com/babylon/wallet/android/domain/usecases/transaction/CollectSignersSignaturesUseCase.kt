package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.transaction.FactorSourceInteractionState
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.ret.SignatureWithPublicKey
import com.radixdlt.ret.hash
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.core.toByteArray
import rdx.works.core.toUByteList
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
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

    private val _factorSourceInteractionState = MutableStateFlow<FactorSourceInteractionState?>(null)
    val factorSourceInteractionState: Flow<FactorSourceInteractionState?> = _factorSourceInteractionState.asSharedFlow()

    suspend operator fun invoke(
        signers: List<Entity>,
        signRequest: SignRequest,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean,
        signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
    ): Result<List<SignatureWithPublicKey>> {
        var deviceAuthenticated = false
        val signaturesWithPublicKeys = mutableListOf<SignatureWithPublicKey>()
        val signersPerFactorSource = getSigningEntitiesByFactorSourceUseCase(signers)
        signersPerFactorSource.forEach { (factorSource, signers) ->
            when (factorSource.id.kind) {
                FactorSourceKind.DEVICE -> {
                    factorSource as DeviceFactorSource
                    //here I assume that in the future we will grant
                    // access to keystore key for few seconds, so I ask for auth only once, instead of on every DEVICE signature
                    if (!deviceAuthenticated) {
                        _factorSourceInteractionState.update { FactorSourceInteractionState.Device.Pending(factorSource) }
                        deviceAuthenticated = deviceBiometricAuthenticationProvider()
                    }
                    if (!deviceAuthenticated) {
                        _factorSourceInteractionState.update { null }
                        return Result.failure(SignatureCancelledException("Failed to collect device factor source signatures"))
                    }
                    val signatures = signWithDeviceFactorSourceUseCase(
                        deviceFactorSource = factorSource,
                        signers = signers,
                        dataToSign = signRequest.hashedDataToSign,
                        signingPurpose = signingPurpose
                    )
                    signaturesWithPublicKeys.addAll(signatures)
                    // _signingState.update { SigningState.Device.Success(factorSource) }
                }

                FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> {
                    factorSource as LedgerHardwareWalletFactorSource
                    _factorSourceInteractionState.update { FactorSourceInteractionState.Ledger.Pending(factorSource) }
                    signWithLedgerFactorSourceUseCase(
                        ledgerFactorSource = factorSource,
                        signers = signers,
                        signRequest = signRequest,
                        signingPurpose = signingPurpose
                    ).onSuccess { signatures ->
                        _factorSourceInteractionState.update { FactorSourceInteractionState.Ledger.Success(factorSource) }
                        signaturesWithPublicKeys.addAll(signatures)
                    }.onFailure {
                        _factorSourceInteractionState.update { FactorSourceInteractionState.Ledger.Failure(factorSource) }
                        return Result.failure(it)
                    }
                }

                FactorSourceKind.OFF_DEVICE_MNEMONIC -> {
                    // TODO when we have off device mnemonic
                }

                FactorSourceKind.TRUSTED_CONTACT -> {
                    error("trusted contact cannot sign")
                }
            }
        }
        return Result.success(signaturesWithPublicKeys)
    }

    fun cancel() {
        _factorSourceInteractionState.update { null }
    }
}

sealed interface SignRequest {

    val dataToSign: ByteArray
    val hashedDataToSign: ByteArray

    class SignTransactionRequest(
        override val dataToSign: ByteArray,
        override val hashedDataToSign: ByteArray
    ) : SignRequest

    class SignAuthChallengeRequest(
        val challengeHex: String,
        val origin: String,
        val dAppDefinitionAddress: String
    ) : SignRequest {

        override val dataToSign: ByteArray
            get() {
                require(dAppDefinitionAddress.length <= UByte.MAX_VALUE.toInt())
                return byteArrayOf(ROLA_PAYLOAD_PREFIX.toByte()) + challengeHex.decodeHex() + dAppDefinitionAddress.length.toUByte()
                    .toByte() + dAppDefinitionAddress.toByteArray() + origin.toByteArray()
            }

        val payloadHex: String
            get() = dataToSign.toHexString()

        override val hashedDataToSign: ByteArray
            get() = hash(dataToSign.toUByteList()).bytes().toByteArray()

        companion object {
            const val ROLA_PAYLOAD_PREFIX = 0x52
        }
    }
}
