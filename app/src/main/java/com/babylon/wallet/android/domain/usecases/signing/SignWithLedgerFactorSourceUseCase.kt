package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.domain.model.messages.LedgerResponse
import com.babylon.wallet.android.domain.model.signing.EntityWithSignature
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import kotlinx.coroutines.flow.first
import rdx.works.core.UUIDGenerator
import rdx.works.core.sargon.transactionSigningFactorInstance
import rdx.works.core.sargon.updateLastUsed
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

typealias SignatureProviderCall = suspend (
    Map<ProfileEntity, HierarchicalDeterministicPublicKey>,
    LedgerDeviceModel
) -> Result<Map<ProfileEntity, LedgerResponse.SignatureOfSigner>>

class SignWithLedgerFactorSourceUseCase @Inject constructor(
    private val ledgerMessenger: LedgerMessenger,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        ledgerFactorSource: FactorSource.Ledger,
        signers: List<ProfileEntity>,
        signRequest: SignRequest
    ): Result<List<EntityWithSignature>> {
        return when (signRequest) {
            is SignRequest.RolaSignRequest -> signAuth(
                signers = signers,
                ledgerFactorSource = ledgerFactorSource,
                request = signRequest
            )

            is SignRequest.TransactionIntentSignRequest -> signTransaction(
                signers = signers,
                ledgerFactorSource = ledgerFactorSource,
                request = signRequest
            )

            is SignRequest.SubintentSignRequest -> signTransaction(
                signers = signers,
                ledgerFactorSource = ledgerFactorSource,
                request = signRequest
            )
        }
    }

    private suspend fun signTransaction(
        signers: List<ProfileEntity>,
        ledgerFactorSource: FactorSource.Ledger,
        request: SignRequest.TransactionIntentSignRequest,
    ): Result<List<EntityWithSignature>> {
        return signCommon(
            signers = signers,
            ledgerFactorSource = ledgerFactorSource,
            signRequest = request
        ) { signersWithPublicKeys, deviceModel ->
            ledgerMessenger.signTransactionRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                hdPublicKeys = signersWithPublicKeys.values.toList(),
                compiledTransactionIntent = request.compiledTransactionIntent.bytes.hex,
                ledgerDevice = LedgerInteractionRequest.LedgerDevice(
                    name = ledgerFactorSource.value.hint.label,
                    model = deviceModel,
                    id = ledgerFactorSource.value.id.body.hex
                )
            ).mapCatching { response ->
                mapEntitiesWithSignatures(
                    signersWithPublicKeys = signersWithPublicKeys,
                    responseWithSignaturesOfSigners = response.signatures
                )
            }
        }
    }

    private suspend fun signTransaction(
        signers: List<ProfileEntity>,
        ledgerFactorSource: FactorSource.Ledger,
        request: SignRequest.SubintentSignRequest,
    ): Result<List<EntityWithSignature>> {
        return signCommon(
            signers = signers,
            ledgerFactorSource = ledgerFactorSource,
            signRequest = request
        ) { signersWithPublicKeys, deviceModel ->
            ledgerMessenger.signSubintentHashRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                hdPublicKeys = signersWithPublicKeys.values.toList(),
                subintentHash = request.intoHash().hex,
                ledgerDevice = LedgerInteractionRequest.LedgerDevice(
                    name = ledgerFactorSource.value.hint.label,
                    model = deviceModel,
                    id = ledgerFactorSource.value.id.body.hex
                )
            ).mapCatching { response ->
                mapEntitiesWithSignatures(
                    signersWithPublicKeys = signersWithPublicKeys,
                    responseWithSignaturesOfSigners = response.signatures
                )
            }
        }
    }

    private suspend fun signAuth(
        signers: List<ProfileEntity>,
        ledgerFactorSource: FactorSource.Ledger,
        request: SignRequest.RolaSignRequest
    ): Result<List<EntityWithSignature>> {
        return signCommon(
            signers = signers,
            ledgerFactorSource = ledgerFactorSource,
            signRequest = request
        ) { signersWithPublicKeys, deviceModel ->
            ledgerMessenger.signChallengeRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                ledgerDevice = LedgerInteractionRequest.LedgerDevice(
                    name = ledgerFactorSource.value.hint.label,
                    model = deviceModel,
                    id = ledgerFactorSource.value.id.body.hex
                ),
                hdPublicKeys = signersWithPublicKeys.values.toList(),
                challengeHex = request.challengeHex,
                origin = request.origin,
                dAppDefinitionAddress = request.dAppDefinitionAddress
            ).mapCatching { response ->
                mapEntitiesWithSignatures(
                    signersWithPublicKeys = signersWithPublicKeys,
                    responseWithSignaturesOfSigners = response.signatures
                )
            }
        }
    }

    // Returns a map of entity as key and its signature as value.
    // This method iterates through the requested signers and from the responseWithSignaturesOfSigners,
    // it tries to find their signatures by comparing the publicKeyHex
    // of the requested signer and of the signature response
    private fun mapEntitiesWithSignatures(
        signersWithPublicKeys: Map<ProfileEntity, HierarchicalDeterministicPublicKey>,
        responseWithSignaturesOfSigners: List<LedgerResponse.SignatureOfSigner>
    ): Map<ProfileEntity, LedgerResponse.SignatureOfSigner> {
        val entitiesWithSignatures = mutableMapOf<ProfileEntity, LedgerResponse.SignatureOfSigner>()

        signersWithPublicKeys.forEach { (profileEntity, hdPublicKey) ->
            val signatureOfSigner = responseWithSignaturesOfSigners.find {
                it.derivedPublicKey.publicKeyHex == hdPublicKey.publicKey.hex
            }
            if (signatureOfSigner != null) {
                entitiesWithSignatures[profileEntity] = signatureOfSigner
            }
        }
        return entitiesWithSignatures
    }

    private suspend fun signCommon(
        signers: List<ProfileEntity>,
        ledgerFactorSource: FactorSource.Ledger,
        signRequest: SignRequest,
        signaturesProvider: SignatureProviderCall
    ): Result<List<EntityWithSignature>> {
        val signersWithPublicKeys = signers.associateWith { signer ->
            val securityState = signer.securityState
            when (signRequest) {
                is SignRequest.RolaSignRequest -> securityState.transactionSigningFactorInstance

                is SignRequest.TransactionIntentSignRequest,
                is SignRequest.SubintentSignRequest -> securityState.transactionSigningFactorInstance
            }.publicKey
        }

        val deviceModel = LedgerDeviceModel.from(ledgerFactorSource.value.hint.model)
        return signaturesProvider(signersWithPublicKeys, deviceModel).map { entitiesWithSignaturesResponse ->
            entitiesWithSignaturesResponse.map { (entity, signatureOfSigner) ->
                when (signatureOfSigner.derivedPublicKey.curve) {
                    LedgerResponse.DerivedPublicKey.Curve.Curve25519 -> {
                        val signatureWithPublicKey = SignatureWithPublicKey.Ed25519(
                            signature = Signature.Ed25519.init(signatureOfSigner.signature.hexToBagOfBytes()).value,
                            publicKey = PublicKey.Ed25519.init(signatureOfSigner.derivedPublicKey.publicKeyHex).v1
                        )
                        EntityWithSignature(
                            entity = entity,
                            signatureWithPublicKey = signatureWithPublicKey
                        )
                    }

                    LedgerResponse.DerivedPublicKey.Curve.Secp256k1 -> {
                        val signatureWithPublicKey = SignatureWithPublicKey.Secp256k1(
                            signature = Signature.Secp256k1.init(signatureOfSigner.signature.hexToBagOfBytes()).value,
                            publicKey = PublicKey.Secp256k1.init(signatureOfSigner.derivedPublicKey.publicKeyHex).v1
                        )
                        EntityWithSignature(
                            entity = entity,
                            signatureWithPublicKey = signatureWithPublicKey
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
