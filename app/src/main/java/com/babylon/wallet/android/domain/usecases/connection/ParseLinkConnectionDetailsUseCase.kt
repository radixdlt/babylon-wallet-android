package com.babylon.wallet.android.domain.usecases.connection

import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.connection.LinkConnectionPayload
import com.babylon.wallet.android.domain.model.connection.LinkConnectionQRContent
import com.babylon.wallet.android.utils.getSignatureMessageFromConnectionPassword
import kotlinx.coroutines.flow.first
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.decodeHex
import rdx.works.profile.data.model.apppreferences.P2PLinkPurpose
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.p2pLinks
import rdx.works.profile.ret.crypto.PrivateKey
import timber.log.Timber
import javax.inject.Inject

class ParseLinkConnectionDetailsUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(rawContent: String): Result<LinkConnectionPayload> {
        val content = runCatching {
            Serializer.kotlinxSerializationJson.decodeFromString<LinkConnectionQRContent>(rawContent)
                .also {
                    // Validate password format
                    HexCoded32Bytes(it.password)

                    // Validate client signature
                    val isSignatureValid = PrivateKey.EddsaEd25519.verifySignature(
                        signature = it.signature.decodeHex(),
                        hashedData = getSignatureMessageFromConnectionPassword(it.password),
                        publicKey = it.publicKey.decodeHex()
                    )

                    if (!isSignatureValid) {
                        return Result.failure(RadixWalletException.LinkConnectionException.InvalidSignature)
                    }
                }
        }.onFailure { throwable ->
            Timber.e("Failed to parse the p2p link connection QR content: $rawContent Error: ${throwable.message}")
            return Result.failure(RadixWalletException.LinkConnectionException.InvalidQR)
        }.getOrThrow()

        val newLinkPurpose = P2PLinkPurpose.fromValue(content.purpose)
        val existingLink = getProfileUseCase.p2pLinks.first()
            .firstOrNull { it.publicKey == content.publicKey }

        return when {
            newLinkPurpose == null || newLinkPurpose != P2PLinkPurpose.General -> {
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