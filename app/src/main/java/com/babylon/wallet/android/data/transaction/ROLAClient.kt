package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.manifest.addSetMetadataInstructionForOwnerKeys
import com.babylon.wallet.android.data.manifest.convertManifestInstructionsToString
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.crypto.SignatureWithPublicKey
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import rdx.works.core.decodeHex
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.SigningPurpose
import rdx.works.profile.domain.GenerateAuthSigningFactorInstanceUseCase
import javax.inject.Inject

class ROLAClient @Inject constructor(
    private val entityRepository: EntityRepository,
    private val generateAuthSigningFactorInstanceUseCase: GenerateAuthSigningFactorInstanceUseCase,
    private val collectSignersSignaturesUseCase: CollectSignersSignaturesUseCase
) {

    @Suppress("RedundantSuspendModifier")
    suspend fun generateAuthSigningFactorInstance(entity: Entity): FactorInstance {
        return generateAuthSigningFactorInstanceUseCase(entity)
    }

    suspend fun createAuthKeyManifestWithStringInstructions(
        entity: Entity,
        authSigningFactorInstance: FactorInstance
    ): TransactionManifest? {
        var resultManifest: TransactionManifest? = null
        val transactionSigningKey = when (val state = entity.securityState) {
            is SecurityState.Unsecured -> state.unsecuredEntityControl.transactionSigning.publicKey
        }
        entityRepository.getEntityOwnerKeys(entity.address, true).onValue { ownerKeys ->
            val publicKeys = ownerKeys?.toPublicKeys().orEmpty().toMutableList()
            publicKeys.add(authSigningFactorInstance.publicKey)
            if (!publicKeys.contains(transactionSigningKey)) {
                publicKeys.add(transactionSigningKey)
            }
            resultManifest = ManifestBuilder().addSetMetadataInstructionForOwnerKeys(entity.address, publicKeys).build()
        }
        return resultManifest?.convertManifestInstructionsToString(entity.networkID)?.getOrNull()
    }

    suspend fun signAuthChallenge(
        entity: Entity,
        challengeHex: String,
        dAppDefinitionAddress: String,
        origin: String
    ): Result<SignatureWithPublicKey> {
        val dataToSign = payloadToHash(challengeHex, dAppDefinitionAddress, origin)
        return collectSignersSignaturesUseCase(
            signers = listOf(entity),
            signRequest = SignRequest.SignAuthChallengeRequest(
                data = dataToSign,
                origin = origin,
                dAppDefinitionAddress = dAppDefinitionAddress
            ),
            signingPurpose = SigningPurpose.SignAuth
        ).mapCatching { signatures ->
            if (signatures.size == 1) {
                signatures.first()
            } else {
                throw DappRequestFailure.FailedToSignAuthChallenge(
                    msg = "Failed to sign challenge $challengeHex by entity: ${entity.address}"
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
