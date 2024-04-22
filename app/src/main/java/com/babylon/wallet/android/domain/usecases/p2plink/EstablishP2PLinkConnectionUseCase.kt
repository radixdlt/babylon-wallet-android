package com.babylon.wallet.android.domain.usecases.p2plink

import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.data.repository.p2plink.findBy
import com.babylon.wallet.android.data.repository.p2plink.isSame
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionPayload
import com.babylon.wallet.android.utils.getSignatureMessageFromConnectionPassword
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.hex
import rdx.works.core.decodeHex
import rdx.works.core.mapWhen
import rdx.works.core.toByteArray
import rdx.works.core.toHexString
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.peerdroid.domain.ConnectorExtensionExchangeInteraction
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.ret.crypto.PrivateKey
import timber.log.Timber
import javax.inject.Inject

class EstablishP2PLinkConnectionUseCase @Inject constructor(
    private val peerdroidLink: PeerdroidLink,
    private val p2PLinksRepository: P2PLinksRepository
) {

    suspend fun update(payload: LinkConnectionPayload): Result<Unit> {
        val existingLink = payload.existingP2PLink ?: return Result.failure(
            IllegalStateException("The existing p2p link can't be null at this point")
        )

        return addConnection(
            payload = payload,
            saveP2PLink = { p2pLinks, privateKey ->
                val updatedP2PLink = payload.toP2PLink(
                    name = existingLink.displayName,
                    privateKey = privateKey
                )
                p2PLinksRepository.save(
                    p2pLinks.mapWhen(
                        predicate = { it.isSame(updatedP2PLink) },
                        mutation = { updatedP2PLink }
                    )
                )
            }
        )
    }

    suspend fun add(payload: LinkConnectionPayload, name: String): Result<Unit> {
        return addConnection(
            payload = payload,
            saveP2PLink = { p2pLinks, privateKey ->
                val newP2PLink = payload.toP2PLink(
                    name = name,
                    privateKey = privateKey
                )
                p2PLinksRepository.save(p2pLinks + newP2PLink)
            }
        )
    }

    private suspend fun addConnection(
        payload: LinkConnectionPayload,
        saveP2PLink: suspend (p2pLinks: List<P2PLink>, privateKey: String) -> Unit
    ): Result<Unit> {
        return peerdroidLink.addConnection(
            encryptionKey = payload.password.value.bytes.toByteArray(),
            connectionListener = object : PeerdroidLink.ConnectionListener {

                override suspend fun completeLinking(connectionId: String): Result<Unit> {
                    val p2pLinks = p2PLinksRepository.getP2PLinks()

                    val walletClientKeyBytes = p2pLinks.findBy(payload.publicKey.hex)?.walletPrivateKey
                        ?.decodeHex() ?: PrivateKey.EddsaEd25519.newRandom().toByteArray()
                    val walletClientKey = PrivateKey.EddsaEd25519.newFromPrivateKeyBytes(walletClientKeyBytes)

                    val linkClientInteractionResponse = buildLinkClientInteractionResponse(
                        walletClientPrivateKey = walletClientKey,
                        connectionPassword = payload.password
                    )

                    return peerdroidLink.sendMessage(connectionId, linkClientInteractionResponse)
                        .onSuccess {
                            saveP2PLink(p2pLinks, walletClientKey.toByteArray().toHexString())
                        }
                }
            }
        ).onSuccess {
            Timber.d("Successfully connected to remote peer.")
        }.onFailure {
            Timber.d("Failed to connect to remote peer.")
        }
    }

    private fun buildLinkClientInteractionResponse(
        walletClientPrivateKey: PrivateKey.EddsaEd25519,
        connectionPassword: RadixConnectPassword
    ): ConnectorExtensionExchangeInteraction.LinkClient {
        return ConnectorExtensionExchangeInteraction.LinkClient(
            publicKey = walletClientPrivateKey.publicKey(),
            signature = walletClientPrivateKey.signToSignature(
                hashedData = getSignatureMessageFromConnectionPassword(connectionPassword.value.hex)
            )
        )
    }

    private fun LinkConnectionPayload.toP2PLink(name: String, privateKey: String): P2PLink {
        return P2PLink(
            connectionPassword = password.value.hex,
            displayName = name,
            publicKey = publicKey.hex,
            purpose = purpose,
            walletPrivateKey = privateKey
        )
    }
}
