package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GenerateAuthSigningFactorInstanceUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.radixdlt.ret.SignatureWithPublicKey
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.compressedPublicKeyHash
import rdx.works.core.ret.ManifestBuilder
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.SigningPurpose
import javax.inject.Inject

class ROLAClient @Inject constructor(
    private val entityRepository: EntityRepository,
    private val generateAuthSigningFactorInstanceUseCase: GenerateAuthSigningFactorInstanceUseCase,
    private val collectSignersSignaturesUseCase: CollectSignersSignaturesUseCase
) {

    val signingState = collectSignersSignaturesUseCase.signingState

    suspend fun generateAuthSigningFactorInstance(entity: Entity): Result<FactorInstance> {
        return generateAuthSigningFactorInstanceUseCase(entity)
    }

    suspend fun createAuthKeyManifestWithStringInstructions(
        entity: Entity,
        authSigningFactorInstance: FactorInstance
    ): TransactionManifest {
        val transactionSigningKey = when (val state = entity.securityState) {
            is SecurityState.Unsecured -> state.unsecuredEntityControl.transactionSigning.publicKey
        }
        val ownerKeys = entityRepository.getEntityOwnerKeyHashes(entity.address, true).value()
        val publicKeyHashes = mutableListOf<FactorInstance.PublicKey>()
        val ownerKeysHashes = ownerKeys?.toPublicKeyHashes().orEmpty()
        val authSigningKeyHash = authSigningFactorInstance.publicKey.compressedData.compressedPublicKeyHash()
        val transactionSigningKeyHash = transactionSigningKey.compressedData.compressedPublicKeyHash()
        if (!ownerKeysHashes.contains(authSigningKeyHash)) {
            publicKeyHashes.add(authSigningFactorInstance.publicKey)
        }
        if (!ownerKeysHashes.contains(transactionSigningKeyHash)) {
            publicKeyHashes.add(transactionSigningKey)
        }
        return ManifestBuilder()
            .addSetMetadataInstructionForOwnerKeys(entity.address, publicKeyHashes)
            .build(entity.networkID)
    }

    // TODO RET
    fun ManifestBuilder.addSetMetadataInstructionForOwnerKeys(
        entityAddress: String,
        ownerKeysHashes: List<FactorInstance.PublicKey>
    ): ManifestBuilder {
//        val keyHashesAdsEngineValues: Array<ManifestAstValue> = ownerKeysHashes.map { key ->
//            ManifestAstValue.Enum(
//                variant = key.curveKindScryptoDiscriminator(),
//                fields = arrayOf(ManifestAstValue.Bytes(key.compressedData.compressedPublicKeyHashBytes()))
//            )
//        }.toTypedArray()
//        return addInstruction(
//            Instruction.SetMetadata(
//                entityAddress = ManifestAstValue.Address(
//                    value = entityAddress
//                ),
//                key = ManifestAstValue.String(ExplicitMetadataKey.OWNER_KEYS.key),
//                value = ManifestAstValue.Enum(
//                    variant = EnumDiscriminator.U8(143u),
//                    fields = arrayOf(ManifestAstValue.Array(ValueKind.Enum, keyHashesAdsEngineValues))
//                )
//            )
//        )
        return this
    }

//    fun FactorInstance.PublicKey.curveKindScryptoDiscriminator(): EnumDiscriminator.U8 {
//        return when (curve) {
//            Slip10Curve.SECP_256K1 -> EnumDiscriminator.U8(0x00u)
//            Slip10Curve.CURVE_25519 -> EnumDiscriminator.U8(0x01u)
//        }
//    }

    suspend fun signAuthChallenge(
        entity: Entity,
        challengeHex: String,
        dAppDefinitionAddress: String,
        origin: String
    ): Result<SignatureWithPublicKey> {
        return collectSignersSignaturesUseCase(
            signers = listOf(entity),
            signRequest = SignRequest.SignAuthChallengeRequest(
                challengeHex = challengeHex,
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
