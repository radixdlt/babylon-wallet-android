package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.utils.getLedgerDeviceModel
import com.radixdlt.toolkit.models.crypto.PublicKey
import com.radixdlt.toolkit.models.crypto.Signature
import com.radixdlt.toolkit.models.crypto.SignatureWithPublicKey
import kotlinx.coroutines.flow.first
import rdx.works.core.UUIDGenerator
import rdx.works.core.decodeHex
import rdx.works.core.toHexString
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.SigningEntity
import rdx.works.profile.data.model.pernetwork.updateLastUsed
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class SignWithLedgerFactorSourceUseCase @Inject constructor(
    private val ledgerMessenger: LedgerMessenger,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        ledgerFactorSource: FactorSource,
        signers: List<SigningEntity>,
        dataToSign: ByteArray
    ): Result<List<SignatureWithPublicKey>> {
        val pathToCurve = signers.map { signer ->
            when (val securityState = signer.securityState) {
                is SecurityState.Unsecured -> {
                    val derivationPath = checkNotNull(securityState.unsecuredEntityControl.transactionSigning.derivationPath)
                    derivationPath.path to securityState.unsecuredEntityControl.transactionSigning.publicKey.curve
                }
            }
        }
        val deviceModel = requireNotNull(ledgerFactorSource.getLedgerDeviceModel())
        val signResult = ledgerMessenger.signTransactionRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            signersDerivationPathToCurve = pathToCurve,
            compiledTransactionIntent = dataToSign.toHexString(),
            ledgerDevice = DerivePublicKeyRequest.LedgerDevice(
                ledgerFactorSource.label,
                deviceModel,
                ledgerFactorSource.id.value
            )
        )
        return if (signResult.isSuccess) {
            val ledgerSignatures = signResult.getOrThrow().signatures.map { signature ->
                when (signature.curve) {
                    MessageFromDataChannel.LedgerResponse.SignatureOfSigner.Curve.Curve25519 -> {
                        SignatureWithPublicKey.EddsaEd25519(
                            signature = Signature.EddsaEd25519(signature.signature.decodeHex()),
                            publicKey = PublicKey.EddsaEd25519(signature.publicKeyHex)
                        )
                    }
                    MessageFromDataChannel.LedgerResponse.SignatureOfSigner.Curve.Secp256k1 -> {
                        SignatureWithPublicKey.EcdsaSecp256k1(
                            signature = Signature.EcdsaSecp256k1(signature.signature.decodeHex())
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
