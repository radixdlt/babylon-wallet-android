package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.DappToWalletInteractionMetadata
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.signatureWithPublicKey
import com.radixdlt.sargon.os.SargonOsManager
import javax.inject.Inject

// Until we decide on how to tackle Rola with MFA, this usecase will just redirect the load to the pre-existing AccessFactorSourcesProxy
class SignAuthUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
) {

    suspend fun persona(
        challenge: Exactly32Bytes,
        persona: Persona,
        metadata: DappToWalletInteraction.RequestMetadata
    ): Result<SignatureWithPublicKey> = runCatching {
        sargonOsManager.sargonOs.signAuthPersona(
            identityAddress = persona.address,
            challengeNonce = challenge,
            metadata = DappToWalletInteractionMetadata(
                version = metadata.version,
                networkId = metadata.networkId,
                origin = metadata.origin,
                dappDefinitionAddress = AccountAddress.init(metadata.dAppDefinitionAddress)
            )
        )
    }.mapCatching { signedAuthIntent ->
        signedAuthIntent.intentSignaturesPerOwner.first().intentSignature.signatureWithPublicKey
    }

    suspend fun accounts(
        challenge: Exactly32Bytes,
        accounts: List<Account>,
        metadata: DappToWalletInteraction.RequestMetadata
    ): Result<Map<Account, SignatureWithPublicKey>> = runCatching {
        sargonOsManager.sargonOs.signAuthAccounts(
            accountAddresses = accounts.map { it.address },
            challengeNonce = challenge,
            metadata = DappToWalletInteractionMetadata(
                version = metadata.version,
                networkId = metadata.networkId,
                origin = metadata.origin,
                dappDefinitionAddress = AccountAddress.init(metadata.dAppDefinitionAddress)
            )
        )
    }.mapCatching { signedAuthIntent ->
        val signaturesPerOwner = signedAuthIntent.intentSignaturesPerOwner.associate {
            (it.owner as AddressOfAccountOrPersona.Account).v1 to it.intentSignature.signatureWithPublicKey
        }

        val signaturesPerAccount = mutableMapOf<Account, SignatureWithPublicKey>()
        accounts.forEach { account ->
            val signature = signaturesPerOwner[account.address]
            if (signature != null) {
                signaturesPerAccount[account] = signature
            }
        }

        signaturesPerAccount
    }
}
