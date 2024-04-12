package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.babylon.wallet.android.domain.model.IncomingMessage
import kotlinx.coroutines.flow.first
import rdx.works.core.UUIDGenerator
import rdx.works.core.sargon.authenticationSigningFactorInstance
import rdx.works.core.sargon.transactionSigningFactorInstance
import rdx.works.core.sargon.updateLastUsed
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

typealias SignatureProviderResult = Result<List<IncomingMessage.LedgerResponse.SignatureOfSigner>>
typealias SignatureProviderCall = suspend (
    List<HierarchicalDeterministicPublicKey>,
    LedgerDeviceModel
) -> SignatureProviderResult

class SignWithLedgerFactorSourceUseCase @Inject constructor(
    private val ledgerMessenger: LedgerMessenger,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(
        ledgerFactorSource: FactorSource.Ledger,
        signers: List<ProfileEntity>,
        signRequest: SignRequest
    ): Result<List<SignatureWithPublicKey>> {
        return when (signRequest) {
            is SignRequest.SignAuthChallengeRequest -> signAuth(
                signers = signers,
                ledgerFactorSource = ledgerFactorSource,
                request = signRequest
            )
            is SignRequest.SignTransactionRequest -> signTransaction(
                signers = signers,
                ledgerFactorSource = ledgerFactorSource,
                request = signRequest
            )
        }
    }

    private suspend fun signTransaction(
        signers: List<ProfileEntity>,
        ledgerFactorSource: FactorSource.Ledger,
        request: SignRequest.SignTransactionRequest,
    ): Result<List<SignatureWithPublicKey>> {
        return signCommon(
            signers = signers,
            ledgerFactorSource = ledgerFactorSource,
            signRequest = request
        ) { hdPublicKeys, deviceModel ->
            ledgerMessenger.signTransactionRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                hdPublicKeys = hdPublicKeys,
                compiledTransactionIntent = request.dataToSign.hex,
                ledgerDevice = LedgerInteractionRequest.LedgerDevice(
                    name = ledgerFactorSource.value.hint.name,
                    model = deviceModel,
                    id = ledgerFactorSource.value.id.body.hex
                )
            ).mapCatching { response ->
                response.signatures
            }
        }
    }

    private suspend fun signAuth(
        signers: List<ProfileEntity>,
        ledgerFactorSource: FactorSource.Ledger,
        request: SignRequest.SignAuthChallengeRequest
    ): Result<List<SignatureWithPublicKey>> {
        return signCommon(
            signers = signers,
            ledgerFactorSource = ledgerFactorSource,
            signRequest = request
        ) { hdPublicKeys, deviceModel ->
            ledgerMessenger.signChallengeRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                ledgerDevice = LedgerInteractionRequest.LedgerDevice(
                    name = ledgerFactorSource.value.hint.name,
                    model = deviceModel,
                    id = ledgerFactorSource.value.id.body.hex
                ),
                hdPublicKeys = hdPublicKeys,
                challengeHex = request.challengeHex,
                origin = request.origin,
                dAppDefinitionAddress = request.dAppDefinitionAddress
            ).mapCatching { response ->
                response.signatures
            }
        }
    }

    private suspend fun signCommon(
        signers: List<ProfileEntity>,
        ledgerFactorSource: FactorSource.Ledger,
        signRequest: SignRequest,
        signaturesProvider: SignatureProviderCall
    ): Result<List<SignatureWithPublicKey>> {
        val hdPublicKey = signers.map { signer ->
            val securityState = signer.securityState
            when (signRequest) {
                is SignRequest.SignAuthChallengeRequest ->
                    securityState.authenticationSigningFactorInstance
                        ?: securityState.transactionSigningFactorInstance
                is SignRequest.SignTransactionRequest -> securityState.transactionSigningFactorInstance
            }.publicKey
        }
        val deviceModel = LedgerDeviceModel.from(ledgerFactorSource.value.hint.model)
        return signaturesProvider(hdPublicKey, deviceModel).map { signatures ->
            signatures.map { signatureOfSigner ->
                when (signatureOfSigner.derivedPublicKey.curve) {
                    IncomingMessage.LedgerResponse.DerivedPublicKey.Curve.Curve25519 -> {
                        SignatureWithPublicKey.Ed25519(
                            signature = Signature.Ed25519.init(signatureOfSigner.signature.hexToBagOfBytes()).value,
                            publicKey = PublicKey.Ed25519.init(signatureOfSigner.derivedPublicKey.publicKeyHex).v1
                        )
                    }

                    IncomingMessage.LedgerResponse.DerivedPublicKey.Curve.Secp256k1 -> {
                        SignatureWithPublicKey.Secp256k1(
                            signature = Signature.Secp256k1.init(signatureOfSigner.signature.hexToBagOfBytes()).value,
                            publicKey = PublicKey.Secp256k1.init(signatureOfSigner.derivedPublicKey.publicKeyHex).v1
                        )
                    }
                }
            }
        }.onSuccess {
            val profile = profileRepository.profile.first()
            profileRepository.saveProfile(profile.updateLastUsed(ledgerFactorSource.id))
        }
    }
}
