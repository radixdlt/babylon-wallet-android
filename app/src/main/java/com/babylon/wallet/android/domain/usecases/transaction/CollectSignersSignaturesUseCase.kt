package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.utils.removeTrailingSlash
import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Hash
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.bagOfBytesOf
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.toByteArray
import rdx.works.profile.domain.signing.GetSigningEntitiesByFactorSourceUseCase
import javax.inject.Inject

@Deprecated("will be removed after refactoring. Now it is used only in ROLAClient")
class CollectSignersSignaturesUseCase @Inject constructor(
    private val signWithDeviceFactorSourceUseCase: SignWithDeviceFactorSourceUseCase,
    private val signWithLedgerFactorSourceUseCase: SignWithLedgerFactorSourceUseCase,
    private val getSigningEntitiesByFactorSourceUseCase: GetSigningEntitiesByFactorSourceUseCase,
) {

    private val _interactionState = MutableStateFlow<InteractionState?>(null)
    val interactionState: Flow<InteractionState?> = _interactionState.asSharedFlow()

    @Suppress("ReturnCount", "LongMethod")
    suspend operator fun invoke(
        signers: List<ProfileEntity>,
        signRequest: SignRequest,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean,
    ): Result<List<SignatureWithPublicKey>> {
        var deviceAuthenticated = false
        val signaturesWithPublicKeys = mutableListOf<SignatureWithPublicKey>()
        val signersPerFactorSource = getSigningEntitiesByFactorSourceUseCase(signers)
        signersPerFactorSource.forEach { (factorSource, signers) ->
            when (factorSource) {
                is FactorSource.Device -> {
                    // here I assume that in the future we will grant
                    // access to keystore key for few seconds, so I ask for auth only once, instead of on every DEVICE signature
                    if (!deviceAuthenticated) {
                        deviceAuthenticated = deviceBiometricAuthenticationProvider()
                    }
                    if (!deviceAuthenticated) {
                        _interactionState.update { null }
                        return Result.failure(RadixWalletException.SignatureCancelled)
                    }
                    _interactionState.update {
                        InteractionState.Device.Pending(
                            factorSource,
                            InteractionState.SigningPurpose.from(signRequest)
                        )
                    }
                    signWithDeviceFactorSourceUseCase(
                        deviceFactorSource = factorSource,
                        signers = signers,
                        signRequest = signRequest
                    ).onSuccess { signatures ->
                        signaturesWithPublicKeys.addAll(signatures)
                    }.onFailure {
                        _interactionState.update { null }
                        return Result.failure(it)
                    }
                }

                is FactorSource.Ledger -> {
                    if (!deviceAuthenticated) {
                        deviceAuthenticated = deviceBiometricAuthenticationProvider()
                    }
                    if (!deviceAuthenticated) {
                        _interactionState.update { null }
                        return Result.failure(RadixWalletException.SignatureCancelled)
                    }
                    _interactionState.update {
                        InteractionState.Ledger.Pending(
                            factorSource,
                            InteractionState.SigningPurpose.from(signRequest)
                        )
                    }
                    signWithLedgerFactorSourceUseCase(
                        ledgerFactorSource = factorSource,
                        signers = signers,
                        signRequest = signRequest
                    ).onSuccess { signatures ->
                        _interactionState.update { null }
                        signaturesWithPublicKeys.addAll(signatures)
                    }.onFailure { error ->
                        _interactionState.update { null }
                        return Result.failure(error)
                    }
                }
            }
        }
        return Result.success(signaturesWithPublicKeys)
    }

    fun cancel() {
        _interactionState.update { null }
    }
}

sealed interface SignRequest {

    val dataToSign: BagOfBytes
    val hashedDataToSign: Hash

    class SignTransactionRequest(
        intent: TransactionIntent
    ) : SignRequest {
        // Used when signing with Ledger
        override val dataToSign: BagOfBytes = intent.compile()

        // Used when signing with device
        override val hashedDataToSign: Hash = intent.hash().hash
    }

    class SignAuthChallengeRequest(
        val challengeHex: String,
        val origin: String,
        val dAppDefinitionAddress: String
    ) : SignRequest {

        // TODO removeTrailingSlash is a hack to fix the issue with dapp login, it should be removed after logic is moved to sargon
        override val dataToSign: BagOfBytes
            get() {
                require(dAppDefinitionAddress.length <= UByte.MAX_VALUE.toInt())
                return bagOfBytesOf(
                    byteArrayOf(ROLA_PAYLOAD_PREFIX.toByte()) +
                        challengeHex.hexToBagOfBytes().toByteArray() +
                        dAppDefinitionAddress.length.toUByte().toByte() +
                        dAppDefinitionAddress.toByteArray() +
                        origin.removeTrailingSlash().toByteArray()
                )
            }

        override val hashedDataToSign: Hash
            get() = dataToSign.hash()

        companion object {
            const val ROLA_PAYLOAD_PREFIX = 0x52
        }
    }
}
