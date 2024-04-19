package com.babylon.wallet.android.domain.usecases.p2plink

import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.data.repository.p2plink.findBy
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionPayload
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionQRContent
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.utils.getSignatureMessageFromConnectionPassword
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.decodeHex
import rdx.works.profile.data.model.apppreferences.P2PLinkPurpose
import rdx.works.profile.ret.crypto.PrivateKey
import timber.log.Timber
import javax.inject.Inject

class ParseLinkConnectionDetailsUseCase @Inject constructor(
    private val p2PLinksRepository: P2PLinksRepository
) {

    suspend operator fun invoke(raw: String): Result<LinkConnectionPayload> {
        val content = runCatching {
            Serializer.kotlinxSerializationJson.decodeFromString<LinkConnectionQRContent>(raw)
                .also {
                    // Validate password format
                    HexCoded32Bytes(it.password)

                    // Validate client signature
                    val isSignatureValid = runCatching {
                        PrivateKey.EddsaEd25519.verifySignature(
                            signature = it.signature.decodeHex(),
                            hashedData = getSignatureMessageFromConnectionPassword(it.password),
                            publicKey = it.publicKey.decodeHex()
                        )
                    }.onFailure { throwable ->
                        Timber.e("Failed to verify the link signature: ${it.signature} Error: ${throwable.message}")
                    }.getOrNull() ?: false

                    if (!isSignatureValid) {
                        return Result.failure(RadixWalletException.LinkConnectionException.InvalidSignature)
                    }
                }
        }.onFailure { throwable ->
            Timber.e("Failed to parse the p2p link connection QR content: $raw Error: ${throwable.message}")
            return Result.failure(RadixWalletException.LinkConnectionException.InvalidQR)
        }.getOrThrow()

        val newLinkPurpose = P2PLinkPurpose.fromValue(content.purpose)
        val links = p2PLinksRepository.getP2PLinks()
        val existingLink = links.findBy(content.publicKey)

        return when {
            newLinkPurpose == null -> {
                Result.failure(RadixWalletException.LinkConnectionException.UnknownPurpose)
            }
            existingLink != null && newLinkPurpose != existingLink.purpose -> {
                Result.failure(RadixWalletException.LinkConnectionException.PurposeChangeNotSupported)
            }
            else -> {
                Result.success(
                    LinkConnectionPayload(
                        password = content.password,
                        publicKey = content.publicKey,
                        purpose = newLinkPurpose,
                        existingP2PLink = existingLink
                    )
                )
            }
        }
    }
}