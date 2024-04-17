package com.babylon.wallet.android.domain.usecases.connection

import com.babylon.wallet.android.data.dapp.model.ConnectorExtensionExchangeInteraction
import com.babylon.wallet.android.utils.getSignatureMessageFromConnectionPassword
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.string
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

        return peerdroidLink.addConnection(
            encryptionKey = encryptionKey,
            connectionListener = object : PeerdroidLink.ConnectionListener {

                override suspend fun completeLinking(connectionId: String): Result<Unit> {
                    val walletClientPrivateKey = getWalletClientLinkPrivateKey(p2PLink.publicKey)
                    val linkClientInteractionResponse = getLinkClientInteractionResponse(
                        walletClientPrivateKey = walletClientPrivateKey,
                        connectionPassword = p2PLink.connectionPassword
                    )

                    return peerdroidLink.send(linkClientInteractionResponse, connectionId)
                        .onSuccess {
                            encryptedPreferencesManager.saveConnectorExtensionLinkPublicKeyPair(
                                cePublicKey = p2PLink.publicKey,
                                privateKey = walletClientPrivateKey.toByteArray()
                            )
                        }
                }
            }
        ).onSuccess {
            Timber.d("Successfully connected to remote peer.")
        }.onFailure {
            Timber.d("Failed to connect to remote peer.")
        }
    }

    private suspend fun getWalletClientLinkPrivateKey(cePublicKey: String): PrivateKey.EddsaEd25519 {
        val privateKeyBytes = encryptedPreferencesManager.getP2PLinkPrivateKey(cePublicKey)
            ?: PrivateKey.EddsaEd25519.newRandom().toByteArray()
        return PrivateKey.EddsaEd25519.newFromPrivateKeyBytes(privateKeyBytes)
    }

    private fun getLinkClientInteractionResponse(
        walletClientPrivateKey: PrivateKey.EddsaEd25519,
        connectionPassword: String
    ): String {
        return Json.encodeToString(
            ConnectorExtensionExchangeInteraction.LinkClient(
                publicKey = walletClientPrivateKey.publicKey().hex,
                signature = walletClientPrivateKey.signToSignature(
                    hashedData = getSignatureMessageFromConnectionPassword(connectionPassword)
                ).string
            )
        )
    }
}