package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.manifest.addSetMetadataInstructionForOwnerKeys
import com.babylon.wallet.android.data.manifest.convertManifestInstructionsToString
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.metadata.OwnerKeysMetadataItem
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.crypto.SignatureWithPublicKey
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import rdx.works.core.decodeHex
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.SigningEntity
import rdx.works.profile.data.model.pernetwork.SigningPurpose
import rdx.works.profile.domain.GenerateAuthSigningFactorInstanceUseCase
import javax.inject.Inject

class ROLAClient @Inject constructor(
    private val entityRepository: EntityRepository,
    private val generateAuthSigningFactorInstanceUseCase: GenerateAuthSigningFactorInstanceUseCase,
    private val collectSignersSignaturesUseCase: CollectSignersSignaturesUseCase
) {

    suspend fun generateAuthSigningFactorInstance(signingEntity: SigningEntity): FactorInstance {
        return generateAuthSigningFactorInstanceUseCase(signingEntity)
    }

    suspend fun createAuthKeyManifestWithStringInstructions(
        signingEntity: SigningEntity,
        authSigningFactorInstance: FactorInstance
    ): TransactionManifest? {
        var resultManifest: TransactionManifest? = null
        val transactionSigningKey = when (val state = signingEntity.securityState) {
            is SecurityState.Unsecured -> state.unsecuredEntityControl.transactionSigning.publicKey
        }
        entityRepository.getEntityMetadata(signingEntity.address, true).onValue { metadata ->
            val ownerKeys = metadata.filterIsInstance<OwnerKeysMetadataItem>().firstOrNull()?.toPublicKeys().orEmpty().toMutableList()
            ownerKeys.add(authSigningFactorInstance.publicKey)
            if (!ownerKeys.contains(transactionSigningKey)) {
                ownerKeys.add(transactionSigningKey)
            }
            resultManifest = ManifestBuilder().addSetMetadataInstructionForOwnerKeys(signingEntity.address, ownerKeys).build()
        }
        return resultManifest?.convertManifestInstructionsToString(signingEntity.networkID)?.getOrNull()
    }

    suspend fun signAuthChallenge(
        signingEntity: SigningEntity,
        challengeHex: String,
        dAppDefinitionAddress: String,
        origin: String
    ): Result<SignatureWithPublicKey> {
        val dataToSign = payloadToHash(challengeHex, dAppDefinitionAddress, origin)
        return collectSignersSignaturesUseCase(listOf(signingEntity), dataToSign, SigningPurpose.SignAuth).mapCatching { signatures ->
            if (signatures.size == 1) {
                signatures.first()
            } else {
                throw DappRequestFailure.FailedToSignAuthChallenge(
                    msg = "Failed to sign challenge $challengeHex by entity: ${signingEntity.address}"
                )
            }
        }
    }
}

fun payloadToHash(
    challengeHex: String,
    dAppDefinitionAddress: String,
    origin: String
): ByteArray {
    require(dAppDefinitionAddress.length <= UByte.MAX_VALUE.toInt())
    return challengeHex.decodeHex() + dAppDefinitionAddress.length.toUByte()
        .toByte() + dAppDefinitionAddress.toByteArray() + origin.toByteArray()
}
