package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.ret.SignatureWithPublicKey
import com.radixdlt.ret.hash
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.core.toByteArray
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

    private val _interactionState = MutableStateFlow<InteractionState?>(null)
    val interactionState: Flow<InteractionState?> = _interactionState.asSharedFlow()

    @Suppress("ReturnCount", "LongMethod")
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
                    // here I assume that in the future we will grant
                    // access to keystore key for few seconds, so I ask for auth only once, instead of on every DEVICE signature
                    if (!deviceAuthenticated) {
                        deviceAuthenticated = deviceBiometricAuthenticationProvider()
                    }
                    if (!deviceAuthenticated) {
                        _interactionState.update { null }
                        return Result.failure(RadixWalletException.SignatureCancelled)
                    }
                    _interactionState.update { InteractionState.Device.Pending(factorSource, signingPurpose) }
                    signWithDeviceFactorSourceUseCase(
                        deviceFactorSource = factorSource,
                        signers = signers,
                        dataToSign = signRequest.hashedDataToSign,
                        signingPurpose = signingPurpose
                    ).onSuccess { signatures ->
                        signaturesWithPublicKeys.addAll(signatures)
                    }.onFailure {
                        _interactionState.update { null }
                        return Result.failure(it)
                    }
                }

                FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> {
                    if (!deviceAuthenticated) {
                        deviceAuthenticated = deviceBiometricAuthenticationProvider()
                    }
                    if (!deviceAuthenticated) {
                        _interactionState.update { null }
                        return Result.failure(RadixWalletException.SignatureCancelled)
                    }
                    factorSource as LedgerHardwareWalletFactorSource
                    _interactionState.update { InteractionState.Ledger.Pending(factorSource, signingPurpose) }
                    signWithLedgerFactorSourceUseCase(
                        ledgerFactorSource = factorSource,
                        signers = signers,
                        signRequest = signRequest,
                        signingPurpose = signingPurpose
                    ).onSuccess { signatures ->
                        _interactionState.update { null }
                        signaturesWithPublicKeys.addAll(signatures)
                    }.onFailure { error ->
                        _interactionState.update { null }
                        return Result.failure(error)
                    }
                }

                FactorSourceKind.OFF_DEVICE_MNEMONIC -> {
                    /*TODO when we have off device mnemonic*/
                }

                FactorSourceKind.TRUSTED_CONTACT -> error("trusted contact cannot sign")
            }
        }
        return Result.success(signaturesWithPublicKeys)
    }

    fun cancel() {
        _interactionState.update { null }
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
            get() = hash(dataToSign).bytes()

        companion object {
            const val ROLA_PAYLOAD_PREFIX = 0x52
        }
    }
}
