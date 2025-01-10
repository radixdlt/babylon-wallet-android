package rdx.works.core.sargon

import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.sign
import com.radixdlt.sargon.os.signing.HdSignature
import com.radixdlt.sargon.os.signing.HdSignatureInput
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable

// TODO: Move to sargon
fun MnemonicWithPassphrase.signInteractorInput(
    input: PerFactorSourceInput<Signable.Payload, Signable.ID>,
) = input.perTransaction.map { perTransaction ->
    perTransaction.ownedFactorInstances.map { perFactorInstance ->
        val signatureWithPublicKey = sign(
            hash = perTransaction.payload.getSignable().hash(),
            path = perFactorInstance.factorInstance.publicKey.derivationPath
        )

        HdSignature(
            input = HdSignatureInput(
                payloadId = perTransaction.payload.getSignable().getId(),
                ownedFactorInstance = perFactorInstance
            ),
            signature = signatureWithPublicKey
        )
    }
}.flatten()