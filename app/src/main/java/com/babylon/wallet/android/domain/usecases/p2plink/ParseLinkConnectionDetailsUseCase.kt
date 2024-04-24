package com.babylon.wallet.android.domain.usecases.p2plink

import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.data.repository.p2plink.findBy
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionPayload
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionQRContent
import com.babylon.wallet.android.utils.getSignatureMessageFromConnectionPassword
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
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
            decodeRawInput(raw)
        }.getOrElse { throwable ->
            return Result.failure(throwable)
        }

        runCatching {
            PrivateKey.EddsaEd25519.verifySignature(
                signature = content.signature.decodeHex(),
                hashedData = getSignatureMessageFromConnectionPassword(content.password),
                publicKey = content.publicKey.decodeHex()
            )
        }.getOrElse { throwable ->
            Timber.e("Failed to verify the link signature: ${content.signature} Error: ${throwable.message}")
            return Result.failure(RadixWalletException.LinkConnectionException.InvalidSignature)
        }

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
                        password = RadixConnectPassword(Exactly32Bytes.init(content.password.hexToBagOfBytes())),
                        publicKey = PublicKey.Ed25519.init(content.publicKey),
                        purpose = newLinkPurpose,
                        existingP2PLink = existingLink
                    )
                )
            }
        }
    }

    @Throws(RadixWalletException.LinkConnectionException::class)
    private fun decodeRawInput(value: String): LinkConnectionQRContent {
        runCatching {
            Exactly32Bytes.init(value.hexToBagOfBytes())
        }.onSuccess {
            Timber.e("An old QR version has been scanned")
            throw RadixWalletException.LinkConnectionException.OldQRVersion
        }

        return runCatching {
            val content = Serializer.kotlinxSerializationJson.decodeFromString<LinkConnectionQRContent>(value)

            // Validate connection password
            Exactly32Bytes.init(content.password.hexToBagOfBytes())

            content
        }.getOrElse { throwable ->
            Timber.e("Failed to parse the p2p link connection QR content: $value Error: ${throwable.message}")
            throw RadixWalletException.LinkConnectionException.InvalidQR
        }
    }
}
