package com.babylon.wallet.android.domain.usecases.connection

import com.babylon.wallet.android.data.dapp.model.ConnectorExtensionExchangeInteraction
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import com.radixdlt.sargon.extensions.hex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.ret.crypto.PrivateKey
import timber.log.Timber
import javax.inject.Inject

class AddLinkConnectionUseCase @Inject constructor(
    private val peerdroidLink: PeerdroidLink,
    private val encryptedPreferencesManager: EncryptedPreferencesManager
) {

    suspend operator fun invoke(p2PLink: P2PLink): Result<Unit> {
        val encryptionKey = parseEncryptionKeyFromConnectionPassword(
            connectionPassword = p2PLink.connectionPassword
        ) ?: return Result.failure(IllegalArgumentException("Failed to parse encryption key from connection password"))

        val privateKeyBytes = encryptedPreferencesManager.getP2PLinkPrivateKey(p2PLink.publicKey)
            ?: run {
                val newPrivateKey = PrivateKey.EddsaEd25519.newRandom().toByteArray()
                encryptedPreferencesManager.saveConnectorExtensionLinkPublicKeyPair(p2PLink.publicKey, newPrivateKey)
                return@run newPrivateKey
            }
        val walletClientPrivateKey = PrivateKey.EddsaEd25519.newFromPrivateKeyBytes(privateKeyBytes)

        val linkClientInteractionResponse = Json.encodeToString(
            ConnectorExtensionExchangeInteraction.LinkClient(
                publicKey = walletClientPrivateKey.publicKey().hex
            )
        )

        return peerdroidLink.addConnection(encryptionKey, linkClientInteractionResponse).onSuccess {
            Timber.d("Successfully connected to remote peer.")
        }.onFailure {
            Timber.d("Failed to connect to remote peer.")
        }
    }
}