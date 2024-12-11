package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.signing.SignPurpose
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.hex
import javax.inject.Inject

// Until we decide on how to tackle Rola with MFA, this usecase will just redirect the load to the pre-existing AccessFactorSourcesProxy
class SignAuthUseCase @Inject constructor(
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy
) {

    suspend operator fun invoke(
        challenge: Exactly32Bytes,
        entity: ProfileEntity,
        metadata: DappToWalletInteraction.RequestMetadata
    ): Result<SignatureWithPublicKey> = invoke(
        challenge = challenge,
        entities = listOf(entity),
        metadata = metadata
    ).mapCatching { signaturesWithEntities ->
        signaturesWithEntities[entity] ?: throw CommonException.SigningRejected()
    }

    suspend operator fun invoke(
        challenge: Exactly32Bytes,
        entities: List<ProfileEntity>,
        metadata: DappToWalletInteraction.RequestMetadata
    ): Result<Map<ProfileEntity, SignatureWithPublicKey>>  {
        val result = accessFactorSourcesProxy.getSignatures(
            accessFactorSourcesInput = AccessFactorSourcesInput.ToGetSignatures(
                signPurpose = SignPurpose.SignAuth,
                signers = entities.map { it.address },
                signRequest = SignRequest.RolaSignRequest(
                    challengeHex = challenge.hex,
                    origin = metadata.origin,
                    dAppDefinitionAddress = metadata.dAppDefinitionAddress
                )
            )
        )

        return when (result) {
            is AccessFactorSourcesOutput.EntitiesWithSignatures.Success -> Result.success(result.signersWithSignatures)
            is AccessFactorSourcesOutput.EntitiesWithSignatures.Failure -> Result.failure(result.error.commonException)
        }
    }
}
