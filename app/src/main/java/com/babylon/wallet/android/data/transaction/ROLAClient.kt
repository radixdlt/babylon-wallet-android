package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.assets.GetEntitiesOwnerKeysUseCase
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GenerateAuthSigningFactorInstanceUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.PublicKeyHash
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.setOwnerKeysHashes
import kotlinx.coroutines.flow.merge
import rdx.works.core.domain.SigningPurpose
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.sargon.transactionSigningFactorInstance
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

    suspend fun generateAuthSigningFactorInstance(entity: ProfileEntity): Result<HierarchicalDeterministicFactorInstance> {
        return generateAuthSigningFactorInstanceUseCase(entity)
    }

    suspend fun createAuthKeyManifest(
        entity: ProfileEntity,
        authSigningFactorInstance: HierarchicalDeterministicFactorInstance
    ): Result<TransactionManifestData> {
        val transactionSigningPublicKey = entity.securityState.transactionSigningFactorInstance.publicKey.publicKey
        val authSigningPublicKey = authSigningFactorInstance.publicKey.publicKey

        return getEntitiesOwnerKeysUseCase(listOf(entity)).mapCatching { ownerKeysPerEntity ->
            val ownerKeys = ownerKeysPerEntity[entity].orEmpty()
            val publicKeys = mutableListOf<PublicKey>()
            if (ownerKeys.none { it.hex == authSigningPublicKey.hex }) {
                publicKeys.add(authSigningPublicKey)
            }
            if (ownerKeys.none { it.hex == transactionSigningPublicKey.hex }) {
                publicKeys.add(transactionSigningPublicKey)
            }
            publicKeys
        }.mapCatching { publicKeys ->
            TransactionManifest.setOwnerKeysHashes(
                addressOfAccountOrPersona = entity.address,
                ownerKeyHashes = publicKeys.map { PublicKeyHash.init(it) }
            )
        }.mapCatching {
            TransactionManifestData.from(manifest = it)
        }
    }

    suspend fun signAuthChallenge(
        entity: ProfileEntity,
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
