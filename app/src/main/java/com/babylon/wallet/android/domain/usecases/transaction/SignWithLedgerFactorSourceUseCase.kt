package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel
import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel.Companion.getLedgerDeviceModel
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.radixdlt.ret.SignatureWithPublicKey
import kotlinx.coroutines.flow.first
import rdx.works.core.UUIDGenerator
import rdx.works.core.decodeHex
import rdx.works.core.toHexString
import rdx.works.core.toUByteList
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.SigningPurpose
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.utils.updateLastUsed
import javax.inject.Inject

typealias SignatureProviderResult = Result<List<MessageFromDataChannel.LedgerResponse.SignatureOfSigner>>
typealias SignatureProviderCall = suspend (List<Pair<String, Slip10Curve>>, LedgerDeviceModel) -> SignatureProviderResult

class SignWithLedgerFactorSourceUseCase @Inject constructor(
    private val ledgerMessenger: LedgerMessenger,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(
        ledgerFactorSource: LedgerHardwareWalletFactorSource,
        signers: List<Entity>,
        signRequest: SignRequest,
        signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
    ): Result<List<SignatureWithPublicKey>> {
        return when (signingPurpose) {
            SigningPurpose.SignAuth -> {
                require(signRequest is SignRequest.SignAuthChallengeRequest)
                signAuth(
                    signers = signers,
                    ledgerFactorSource = ledgerFactorSource,
                    request = signRequest,
                    signingPurpose = signingPurpose
                )
            }
            SigningPurpose.SignTransaction -> {
                signTransaction(
                    signers = signers,
                    ledgerFactorSource = ledgerFactorSource,
                    dataToSign = signRequest.dataToSign,
                    signingPurpose = signingPurpose
                )
            }
        }
    }

    private suspend fun signTransaction(
        signers: List<Entity>,
        ledgerFactorSource: LedgerHardwareWalletFactorSource,
        dataToSign: ByteArray,
        signingPurpose: SigningPurpose
    ): Result<List<SignatureWithPublicKey>> {
        return signCommon(
            signers = signers,
            ledgerFactorSource = ledgerFactorSource,
            signingPurpose = signingPurpose
        ) { pathToCurve, deviceModel ->
            ledgerMessenger.signTransactionRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                signersDerivationPathToCurve = pathToCurve,
                compiledTransactionIntent = dataToSign.toHexString(),
                ledgerDevice = DerivePublicKeyRequest.LedgerDevice(
                    name = ledgerFactorSource.hint.name,
                    model = deviceModel,
                    id = ledgerFactorSource.id.body.value
                )
            ).mapCatching { response ->
                response.signatures
            }
        }
    }

    private suspend fun signAuth(
        signers: List<Entity>,
        ledgerFactorSource: LedgerHardwareWalletFactorSource,
        request: SignRequest.SignAuthChallengeRequest,
        signingPurpose: SigningPurpose
    ): Result<List<SignatureWithPublicKey>> {
        return signCommon(
            signers = signers,
            ledgerFactorSource = ledgerFactorSource,
            signingPurpose = signingPurpose
        ) { pathToCurve, deviceModel ->
            ledgerMessenger.signChallengeRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                ledgerDevice = DerivePublicKeyRequest.LedgerDevice(
                    name = ledgerFactorSource.hint.name,
                    model = deviceModel,
                    id = ledgerFactorSource.id.body.value
                ),
                signersDerivationPathToCurve = pathToCurve,
                challengeHex = request.challengeHex,
                origin = request.origin,
                dAppDefinitionAddress = request.dAppDefinitionAddress
            ).mapCatching { response ->
                response.signatures
            }
        }
    }

    private suspend fun signCommon(
        signers: List<Entity>,
        ledgerFactorSource: LedgerHardwareWalletFactorSource,
        signingPurpose: SigningPurpose,
        signaturesProvider: SignatureProviderCall
    ): Result<List<SignatureWithPublicKey>> {
        val derivationPathToCurve = signers.map { signer ->
            when (val securityState = signer.securityState) {
                is SecurityState.Unsecured -> {
                    val factorInstance = when (signingPurpose) {
                        SigningPurpose.SignAuth ->
                            securityState.unsecuredEntityControl.authenticationSigning
                                ?: securityState.unsecuredEntityControl.transactionSigning
                        SigningPurpose.SignTransaction -> securityState.unsecuredEntityControl.transactionSigning
                    }
                    val derivationPath = checkNotNull(factorInstance.derivationPath)
                    val curve = factorInstance.publicKey.curve
                    derivationPath.path to curve
                }
            }
        }
        val deviceModel = requireNotNull(ledgerFactorSource.getLedgerDeviceModel())
        val signaturesResult = signaturesProvider(derivationPathToCurve, deviceModel)
        return if (signaturesResult.isSuccess) {
            val ledgerSignatures = signaturesResult.getOrThrow().map { signatureOfSigner ->
                when (signatureOfSigner.derivedPublicKey.curve) {
                    MessageFromDataChannel.LedgerResponse.DerivedPublicKey.Curve.Curve25519 -> {
                        SignatureWithPublicKey.EddsaEd25519(
                            signature = signatureOfSigner.signature.decodeHex().toUByteList(),
                            publicKey = signatureOfSigner.derivedPublicKey.publicKeyHex.decodeHex().toUByteList()
                        )
                    }
                    MessageFromDataChannel.LedgerResponse.DerivedPublicKey.Curve.Secp256k1 -> {
                        SignatureWithPublicKey.EcdsaSecp256k1(
                            signature = signatureOfSigner.signature.decodeHex().toUByteList()
                        )
                    }
                }
            }
            val profile = profileRepository.profile.first()
            profileRepository.saveProfile(profile.updateLastUsed(ledgerFactorSource.id))
            Result.success(ledgerSignatures)
        } else {
            Result.failure(Exception("Failed to collect ledger signatures"))
        }
    }
}
