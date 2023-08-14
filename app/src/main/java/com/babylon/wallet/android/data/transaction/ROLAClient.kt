package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GenerateAuthSigningFactorInstanceUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.radixdlt.ret.Address
import com.radixdlt.ret.PublicKeyHash
import com.radixdlt.ret.SignatureWithPublicKey
import com.radixdlt.ret.TransactionManifest
import kotlinx.coroutines.flow.merge
import rdx.works.core.compressedPublicKeyHash
import rdx.works.core.compressedPublicKeyHashBytes
import rdx.works.core.ret.BabylonManifestBuilder
import rdx.works.core.ret.buildSafely
import rdx.works.core.toUByteList
import rdx.works.profile.data.model.factorsources.Slip10Curve
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

    val signingState = merge(
        collectSignersSignaturesUseCase.interactionState,
        generateAuthSigningFactorInstanceUseCase.interactionState
    )

    suspend fun generateAuthSigningFactorInstance(entity: Entity): Result<FactorInstance> {
        return generateAuthSigningFactorInstanceUseCase(entity)
    }

    suspend fun createAuthKeyManifestWithStringInstructions(
        entity: Entity,
        authSigningFactorInstance: FactorInstance
    ): Result<TransactionManifest> {
        val transactionSigningKey = when (val state = entity.securityState) {
            is SecurityState.Unsecured -> state.unsecuredEntityControl.transactionSigning.publicKey
        }
        val ownerKeys = entityRepository.getEntityOwnerKeyHashes(entity.address, true).value()
        val publicKeyHashes = mutableListOf<FactorInstance.PublicKey>()
        val ownerKeysHashes = ownerKeys?.keyHashes.orEmpty()
        val authSigningKeyHash = authSigningFactorInstance.publicKey.compressedData.compressedPublicKeyHash()
        val transactionSigningKeyHash = transactionSigningKey.compressedData.compressedPublicKeyHash()
        if (ownerKeysHashes.none { it.hex == authSigningKeyHash }) {
            publicKeyHashes.add(authSigningFactorInstance.publicKey)
        }
        if (ownerKeysHashes.none { it.hex == transactionSigningKeyHash }) {
            publicKeyHashes.add(transactionSigningKey)
        }
        return BabylonManifestBuilder()
            .addSetMetadataInstructionForOwnerKeys(entity.address, publicKeyHashes)
            .buildSafely(entity.networkID)
    }

    private fun BabylonManifestBuilder.addSetMetadataInstructionForOwnerKeys(
        entityAddress: String,
        ownerPublicKeys: List<FactorInstance.PublicKey>
    ): BabylonManifestBuilder {
        return setOwnerKeys(
            address = Address(entityAddress),
            ownerKeyHashes = ownerPublicKeys.map { key ->
                val bytes = key.compressedData.compressedPublicKeyHashBytes()
                when (key.curve) {
                    Slip10Curve.SECP_256K1 -> PublicKeyHash.Secp256k1(bytes.toUByteList())
                    Slip10Curve.CURVE_25519 -> PublicKeyHash.Secp256k1(bytes.toUByteList())
                }
            }
        )
    }

    suspend fun signAuthChallenge(
        entity: Entity,
        signRequest: SignRequest,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ): Result<SignatureWithPublicKey> {
        return collectSignersSignaturesUseCase(
            signers = listOf(entity),
            signRequest = signRequest,
            signingPurpose = SigningPurpose.SignAuth,
            deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
        ).mapCatching { signatures ->
            if (signatures.size == 1) {
                signatures.first()
            } else {
                throw DappRequestFailure.FailedToSignAuthChallenge(
                    msg = "Failed to sign request $signRequest by entity: ${entity.address}"
                )
            }
        }
    }
}
