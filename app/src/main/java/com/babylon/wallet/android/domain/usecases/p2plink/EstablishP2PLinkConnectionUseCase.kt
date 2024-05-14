package com.babylon.wallet.android.domain.usecases.p2plink

import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionPayload
import com.radixdlt.sargon.Entropy32Bytes
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.LENGTH
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.messageHash
import com.radixdlt.sargon.extensions.toBagOfBytes
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.toHexString
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.profile.datastore.EncryptedPreferencesManager
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject

class EstablishP2PLinkConnectionUseCase @Inject constructor(
    private val peerdroidLink: PeerdroidLink,
    private val p2PLinksRepository: P2PLinksRepository,
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val preferencesManager: PreferencesManager
) {

    suspend fun update(payload: LinkConnectionPayload): Result<Unit> {
        val existingLink = payload.existingP2PLink ?: return Result.failure(
            IllegalStateException("The existing p2p link can't be null at this point")
        )

        return addConnection(
            payload.toP2PLink(
                name = existingLink.displayName
            )
        )
    }

    suspend fun add(payload: LinkConnectionPayload, name: String): Result<Unit> {
        return addConnection(
            payload.toP2PLink(
                name = name
            )
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun addConnection(p2pLink: P2pLink): Result<Unit> {
        return peerdroidLink.addConnection(
            p2pLink = p2pLink,
            connectionListener = object : PeerdroidLink.ConnectionListener {

                override suspend fun completeLinking(connectionId: String): Result<Unit> {
                    val walletClientKeyBytes = encryptedPreferencesManager.getP2PLinksWalletPrivateKey()
                        ?.hexToByteArray()
                        ?: run {
                            val newPrivateKey = newRandomPrivateKey()
                            encryptedPreferencesManager.saveP2PLinksWalletPrivateKey(newPrivateKey.toHexString())
                            newPrivateKey
                        }
                    val walletClientKey = Curve25519SecretKey(Exactly32Bytes.init(walletClientKeyBytes.toBagOfBytes()))

                    val linkClientInteraction = PeerdroidLink.LinkClientExchangeInteraction(
                        publicKey = walletClientKey.toPublicKey(),
                        signature = walletClientKey.sign(
                            hash = p2pLink.connectionPassword.messageHash()
                        ).asGeneral()
                    )

                    return peerdroidLink.sendMessage(connectionId, linkClientInteraction)
                        .onSuccess {
                            preferencesManager.removeLastSyncedAccountsWithCE()
                            p2PLinksRepository.addOrUpdateP2PLink(p2pLink)
                        }
                }
            }
        ).onSuccess {
            Timber.d("Successfully connected to remote peer.")
        }.onFailure {
            Timber.d("Failed to connect to remote peer.")
        }
    }

    private fun LinkConnectionPayload.toP2PLink(name: String): P2pLink {
        return P2pLink(
            connectionPassword = password,
            displayName = name,
            publicKey = publicKey.v1,
            connectionPurpose = purpose
        )
    }

    private fun newRandomPrivateKey(): ByteArray {
        return with(SecureRandom()) {
            val bytes = ByteArray(Entropy32Bytes.LENGTH)
            nextBytes(bytes)
            bytes
        }
    }
}
