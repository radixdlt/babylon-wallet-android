package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.RadixWalletException
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
        val transactionSigningPublicKey = when (val state = entity.securityState) {
            is SecurityState.Unsecured -> {
                when (val badge = state.unsecuredEntityControl.transactionSigning.badge) {
                    is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
                        badge.publicKey
                    }
                }
            }
        }
        val ownerKeys = entityRepository.getEntityOwnerKeyHashes(entity.address, true).getOrNull()
        val publicKeyHashes = mutableListOf<FactorInstance.PublicKey>()
        val ownerKeysHashes = ownerKeys?.keyHashes.orEmpty()
        val authSigningPublicKey = when (val badge = authSigningFactorInstance.badge) {
            is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
                badge.publicKey
            }
        }
        val authSigningKeyHash = authSigningPublicKey.compressedData.compressedPublicKeyHash()
        val transactionSigningKeyHash = transactionSigningPublicKey.compressedData.compressedPublicKeyHash()
        if (ownerKeysHashes.none { it.hex == authSigningKeyHash }) {
            publicKeyHashes.add(authSigningPublicKey)
        }
        if (ownerKeysHashes.none { it.hex == transactionSigningKeyHash }) {
            publicKeyHashes.add(transactionSigningPublicKey)
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
        val result = collectSignersSignaturesUseCase(
            signers = listOf(entity),
            signRequest = signRequest,
            signingPurpose = SigningPurpose.SignAuth,
            deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
        )
        return when (val exception = result.exceptionOrNull()) {
            null -> result.mapCatching { signatures ->
                if (signatures.size == 1) {
                    signatures.first()
                } else {
                    throw RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
                }
            }
            else -> Result.failure(RadixWalletException.DappRequestException.FailedToSignAuthChallenge(exception))
        }
    }
}
