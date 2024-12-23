package com.babylon.wallet.android.presentation.accessfactorsources.signatures

import com.babylon.wallet.android.presentation.accessfactorsources.signatures.HDSignatureInput.Companion.into
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.HdSignature.Companion.into
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.SignaturesPerFactorSource.Companion.into
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.HdSignatureInputOfAuthIntentHash
import com.radixdlt.sargon.HdSignatureInputOfSubintentHash
import com.radixdlt.sargon.HdSignatureInputOfTransactionIntentHash
import com.radixdlt.sargon.HdSignatureOfAuthIntentHash
import com.radixdlt.sargon.HdSignatureOfSubintentHash
import com.radixdlt.sargon.HdSignatureOfTransactionIntentHash
import com.radixdlt.sargon.NeglectFactorReason
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.SignResponseOfAuthIntentHash
import com.radixdlt.sargon.SignResponseOfSubintentHash
import com.radixdlt.sargon.SignResponseOfTransactionIntentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfAuthIntentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfSubintentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.SignaturesPerFactorSourceOfAuthIntentHash
import com.radixdlt.sargon.SignaturesPerFactorSourceOfSubintentHash
import com.radixdlt.sargon.SignaturesPerFactorSourceOfTransactionIntentHash
import rdx.works.core.sargon.Signable

// /// Inputs
data class InputPerFactorSource<P : Signable.Payload>(
    val factorSourceId: FactorSourceIdFromHash,
    val transactions: List<InputPerTransaction<P>>
)

data class InputPerTransaction<P : Signable.Payload>(
    /**
     * Payload to sign
     */
    val payload: P,
    /**
     * ID of factor to use to sign
     */
    val factorSourceId: FactorSourceIdFromHash,
    /**
     * The derivation paths to use to derive the private keys to sign with. The
     * `factor_source_id` of each item must match `factor_source_id`.
     */
    val ownedFactorInstances: List<OwnedFactorInstance>
)

// /// Outputs
sealed interface OutputPerFactorSource<ID : Signable.ID> {
    data class Signed<ID : Signable.ID>(
        val signatures: SignaturesPerFactorSource<ID>
    ) : OutputPerFactorSource<ID>

    data class Neglected<ID : Signable.ID>(
        val factorSourceId: FactorSourceIdFromHash,
        val reason: NeglectFactorReason
    ) : OutputPerFactorSource<ID>

    companion object {

        /**
         * A simplistic approach for now. If any factor source is neglected, we reject signing.
         *
         * This will change in the future when we accommodate multiple transaction signing.
         */
        private fun <ID : Signable.ID> List<OutputPerFactorSource<ID>>.signaturesOrThrow(): List<SignaturesPerFactorSource<ID>> =
            map { output ->
                when (output) {
                    is Signed<ID> -> output.signatures
                    is Neglected -> throw CommonException.SigningRejected()
                }
            }

        fun List<OutputPerFactorSource<Signable.ID.Transaction>>.into() = SignWithFactorsOutcomeOfTransactionIntentHash.Signed(
            producedSignatures = SignResponseOfTransactionIntentHash(
                perFactorSource = signaturesOrThrow().map { it.into() }
            )
        )

        fun List<OutputPerFactorSource<Signable.ID.Subintent>>.into() = SignWithFactorsOutcomeOfSubintentHash.Signed(
            producedSignatures = SignResponseOfSubintentHash(
                perFactorSource = signaturesOrThrow().map { it.into() }
            )
        )

        fun List<OutputPerFactorSource<Signable.ID.Auth>>.into() = SignWithFactorsOutcomeOfAuthIntentHash.Signed(
            producedSignatures = SignResponseOfAuthIntentHash(
                perFactorSource = signaturesOrThrow().map { it.into() }
            )
        )
    }
}

data class SignaturesPerFactorSource<ID : Signable.ID>(
    val factorSourceId: FactorSourceIdFromHash,
    val hdSignatures: List<HdSignature<ID>>
) {

    companion object {
        fun SignaturesPerFactorSource<Signable.ID.Transaction>.into() = SignaturesPerFactorSourceOfTransactionIntentHash(
            factorSourceId = factorSourceId,
            hdSignatures = hdSignatures.map { it.into() }
        )

        fun SignaturesPerFactorSource<Signable.ID.Subintent>.into() = SignaturesPerFactorSourceOfSubintentHash(
            factorSourceId = factorSourceId,
            hdSignatures = hdSignatures.map { it.into() }
        )

        fun SignaturesPerFactorSource<Signable.ID.Auth>.into() = SignaturesPerFactorSourceOfAuthIntentHash(
            factorSourceId = factorSourceId,
            hdSignatures = hdSignatures.map { it.into() }
        )
    }
}

data class HdSignature<ID : Signable.ID>(
    /**
     * The input used to produce this `HDSignature`
     */
    val input: HDSignatureInput<ID>,
    /**
     * The ECDSA/EdDSA signature produced by the private key of the
     * `owned_hd_factor_instance.public_key`,
     * derived by the HDFactorSource identified by
     * `owned_hd_factor_
     * instance.factor_s
     * ource_id` and which
     * was derived at `owned_hd_factor_instance.derivation_path`.
     */
    val signature: SignatureWithPublicKey
) {

    companion object {
        fun HdSignature<Signable.ID.Transaction>.into() = HdSignatureOfTransactionIntentHash(
            input = input.into(),
            signature = signature
        )

        fun HdSignature<Signable.ID.Subintent>.into() = HdSignatureOfSubintentHash(
            input = input.into(),
            signature = signature
        )

        fun HdSignature<Signable.ID.Auth>.into() = HdSignatureOfAuthIntentHash(
            input = input.into(),
            signature = signature
        )
    }
}

data class HDSignatureInput<ID : Signable.ID>(
    /**
     * Hash which was signed.
     */
    val payloadId: ID,
    /**
     * The account or identity address of the entity which signed the hash,
     * with expected public key and with derivation path to derive PrivateKey
     * with.
     */
    val ownedFactorInstance: OwnedFactorInstance
) {

    companion object {
        fun HDSignatureInput<Signable.ID.Transaction>.into() = HdSignatureInputOfTransactionIntentHash(
            payloadId = payloadId.value,
            ownedFactorInstance = ownedFactorInstance
        )

        fun HDSignatureInput<Signable.ID.Subintent>.into() = HdSignatureInputOfSubintentHash(
            payloadId = payloadId.value,
            ownedFactorInstance = ownedFactorInstance
        )

        fun HDSignatureInput<Signable.ID.Auth>.into() = HdSignatureInputOfAuthIntentHash(
            payloadId = payloadId.value,
            ownedFactorInstance = ownedFactorInstance
        )
    }
}
