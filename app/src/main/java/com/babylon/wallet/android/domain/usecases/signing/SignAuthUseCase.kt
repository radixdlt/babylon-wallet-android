package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.DappToWalletInteractionMetadata
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.di.DefaultDispatcher
import rdx.works.core.sargon.init
import javax.inject.Inject

class SignAuthUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
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
    ): Result<Map<ProfileEntity, SignatureWithPublicKey>> = withContext(defaultDispatcher) {
        val entitiesWithSignatures = entities.map { entity ->
            runCatching {
                val proof = sargonOsManager.sargonOs.signAuth(
                    addressOfEntity = entity.address,
                    challengeNonce = challenge,
                    metadata = DappToWalletInteractionMetadata(
                        version = metadata.version,
                        networkId = metadata.networkId,
                        origin = metadata.origin,
                        dappDefinitionAddress = AccountAddress.init(metadata.dAppDefinitionAddress)
                    )
                )

                SignatureWithPublicKey.init(
                    signature = proof.signature,
                    publicKey = proof.publicKey
                )
            }.map {
                entity to it
            }.getOrElse { error ->
                return@withContext Result.failure(error)
            }
        }.associate {
            it.first to it.second
        }

        Result.success(entitiesWithSignatures)
    }
}
