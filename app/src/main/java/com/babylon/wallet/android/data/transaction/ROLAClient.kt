package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.assets.GetEntitiesOwnerKeysUseCase
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GenerateAuthSigningFactorInstanceUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.radixdlt.ret.SignatureWithPublicKey
import com.radixdlt.ret.TransactionManifest
import kotlinx.coroutines.flow.merge
import rdx.works.core.compressedPublicKeyHash
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.SigningPurpose
import rdx.works.profile.ret.ManifestPoet
import javax.inject.Inject

class ROLAClient @Inject constructor(
    private val getEntitiesOwnerKeysUseCase: GetEntitiesOwnerKeysUseCase,
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
        val ownerKeys = getEntitiesOwnerKeysUseCase(listOf(entity)).getOrNull()?.get(entity)
        val publicKeyHashes = mutableListOf<FactorInstance.PublicKey>()
        val ownerKeysHashes = ownerKeys.orEmpty()
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
        return ManifestPoet.buildRola(
            entityAddress = entity.address,
            publicKeyHashes = publicKeyHashes
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
