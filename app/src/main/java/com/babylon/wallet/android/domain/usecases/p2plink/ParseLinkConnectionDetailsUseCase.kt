package com.babylon.wallet.android.domain.usecases.p2plink

import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionPayload
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.LinkConnectionQrData
import com.radixdlt.sargon.PublicKeyHash
import com.radixdlt.sargon.RadixConnectPurpose
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.isValid
import com.radixdlt.sargon.extensions.messageHash
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
            SignatureWithPublicKey.Ed25519(
                publicKey = content.publicKeyOfOtherParty,
                signature = content.signature
            ).isValid(content.password.messageHash())
        }.getOrElse { throwable ->
            Timber.e("Failed to verify the link signature: ${content.signature} Error: ${throwable.message}")
            return Result.failure(RadixWalletException.LinkConnectionException.InvalidSignature)
        }

        val links = p2PLinksRepository.getP2PLinks()
        val existingLink = runCatching {
            links.getBy(PublicKeyHash.init(content.publicKeyOfOtherParty.asGeneral()))
        }.getOrNull()

        return when {
            content.purpose == RadixConnectPurpose.UNKNOWN -> {
                Result.failure(RadixWalletException.LinkConnectionException.UnknownPurpose)
            }
            existingLink != null && content.purpose != existingLink.connectionPurpose -> {
                Result.failure(RadixWalletException.LinkConnectionException.PurposeChangeNotSupported)
            }
            else -> {
                Result.success(
                    LinkConnectionPayload(
                        password = content.password,
                        publicKey = content.publicKeyOfOtherParty.asGeneral(),
                        purpose = content.purpose,
                        existingP2PLink = existingLink
                    )
                )
            }
        }
    }

    @Throws(RadixWalletException.LinkConnectionException::class)
    private fun decodeRawInput(value: String): LinkConnectionQrData {
        runCatching {
            Exactly32Bytes.init(value.hexToBagOfBytes())
        }.onSuccess {
            Timber.e("An old QR version has been scanned")
            throw RadixWalletException.LinkConnectionException.OldQRVersion
        }

        return runCatching {
            LinkConnectionQrData.fromJson(value)
        }.getOrElse { throwable ->
            Timber.e("Failed to parse the p2p link connection QR content: $value Error: ${throwable.message}")
            throw RadixWalletException.LinkConnectionException.InvalidQR
        }
    }
}
