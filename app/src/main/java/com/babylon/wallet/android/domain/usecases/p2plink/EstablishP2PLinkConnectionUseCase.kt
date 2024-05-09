package com.babylon.wallet.android.domain.usecases.p2plink

import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.data.repository.p2plink.isSame
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionPayload
import com.radixdlt.sargon.Entropy32Bytes
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.LENGTH
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toBagOfBytes
import rdx.works.core.mapWhen
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.toHexString
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.datastore.EncryptedPreferencesManager
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject

class EstablishP2PLinkConnectionUseCase @Inject constructor(
    private val peerdroidLink: PeerdroidLink,
    private val p2PLinksRepository: P2PLinksRepository,
    private val getP2PLinkClientSignatureMessageUseCase: GetP2PLinkClientSignatureMessageUseCase,
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val preferencesManager: PreferencesManager
) {

    suspend fun update(payload: LinkConnectionPayload): Result<Unit> {
        val existingLink = payload.existingP2PLink ?: return Result.failure(
            IllegalStateException("The existing p2p link can't be null at this point")
        )

        return addConnection(
            payload = payload,
            saveP2PLink = { p2pLinks ->
                val updatedP2PLink = payload.toP2PLink(
                    name = existingLink.displayName
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
            saveP2PLink = { p2pLinks ->
                val newP2PLink = payload.toP2PLink(
                    name = name
                )
                p2PLinksRepository.save(p2pLinks + newP2PLink)
            }
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun addConnection(
        payload: LinkConnectionPayload,
        saveP2PLink: suspend (p2pLinks: List<P2PLink>) -> Unit
    ): Result<Unit> {
        return peerdroidLink.addConnection(
            encryptionKey = payload.password,
            connectionListener = object : PeerdroidLink.ConnectionListener {

                override suspend fun completeLinking(connectionId: String): Result<Unit> {
                    val p2pLinks = p2PLinksRepository.getP2PLinks()

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
                            hash = getP2PLinkClientSignatureMessageUseCase(payload.password)
                        ).asGeneral()
                    )

                    return peerdroidLink.sendMessage(connectionId, linkClientInteraction)
                        .onSuccess {
                            preferencesManager.removeLastSyncedAccountsWithCE()
                            saveP2PLink(p2pLinks)
                        }
                }
            }
        ).onSuccess {
            Timber.d("Successfully connected to remote peer.")
        }.onFailure {
            Timber.d("Failed to connect to remote peer.")
        }
    }

    private fun LinkConnectionPayload.toP2PLink(name: String): P2PLink {
        return P2PLink(
            connectionPassword = password,
            displayName = name,
            publicKey = publicKey,
            purpose = purpose
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
