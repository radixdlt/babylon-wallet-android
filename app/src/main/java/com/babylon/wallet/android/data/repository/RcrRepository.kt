package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.dapp.model.WalletInteraction
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import com.babylon.wallet.android.data.gateway.apis.RcrApi
import com.babylon.wallet.android.data.gateway.model.RcrHandshakeRequest
import com.babylon.wallet.android.data.gateway.model.RcrRequest
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.decodeHex
import rdx.works.core.decrypt
import rdx.works.core.encrypt
import rdx.works.core.toHexString
import java.nio.charset.StandardCharsets
import javax.inject.Inject

interface RcrRepository {
    suspend fun sendResponse(sessionId: String, data: String): Result<Unit>
    suspend fun getRequest(sessionId: String, interactionId: String): Result<WalletInteraction>
    suspend fun sendHandshakeResponse(sessionId: String, publicKeyHex: String): Result<Unit>
    suspend fun getHandshake(sessionId: String): Result<String>
}

class RcrRepositoryImpl @Inject constructor(
    private val api: RcrApi,
    private val dappLinkRepository: DappLinkRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,

) : RcrRepository {

    override suspend fun getRequest(sessionId: String, interactionId: String) = withContext(ioDispatcher) {
        api.executeRequest(RcrRequest.GetRequests(sessionId)).toResult().mapCatching { response ->
            val dappLink = dappLinkRepository.getDappLink(sessionId).getOrThrow()
            response.mapNotNull { d ->
                val decryptedBytes = d.decodeHex().decrypt(dappLink.secret.decodeHex()).getOrNull() ?: return@mapNotNull null
                val decryptedRequestString = String(decryptedBytes, StandardCharsets.UTF_8)
                peerdroidRequestJson.decodeFromString<WalletInteraction>(decryptedRequestString)
            }.find { it.interactionId == interactionId } ?: error("No interaction with id $interactionId")
        }
    }

    override suspend fun sendResponse(sessionId: String, data: String) = withContext(ioDispatcher) {
        val dappLink = dappLinkRepository.getDappLink(sessionId).getOrThrow()
        val encryptedData = data.toByteArray().encrypt(dappLink.secret.decodeHex()).getOrThrow().toHexString()
        api.executeRequest(RcrRequest.SendResponse(sessionId, encryptedData)).toResult().onSuccess {
            dappLinkRepository.persistDappLinkForSessionId(sessionId)
        }.map { }
    }

    override suspend fun sendHandshakeResponse(sessionId: String, publicKeyHex: String) = withContext(ioDispatcher) {
        api.executeHandshakeRequest(RcrHandshakeRequest.SendHandshakeResponse(sessionId, publicKeyHex)).toResult().map { }
    }

    override suspend fun getHandshake(sessionId: String) = withContext(ioDispatcher) {
        api.executeHandshakeRequest(RcrHandshakeRequest.GetHandshake(sessionId)).toResult().map {
            it.publicKeyHex
        }
    }
}
