package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.dapp.model.WalletInteraction
import com.babylon.wallet.android.data.gateway.apis.RcrApi
import com.babylon.wallet.android.data.gateway.model.RcrRequest
import com.babylon.wallet.android.data.gateway.model.RcrResponse
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import rdx.works.core.decodeHex
import rdx.works.core.decrypt
import rdx.works.core.encrypt
import rdx.works.core.toHexString
import java.nio.charset.StandardCharsets
import javax.inject.Inject

interface RcrRepository {
    suspend fun sendResponse(sessionId: String, data: String): Result<RcrResponse>
    suspend fun getRequest(sessionId: String, interactionId: String): Result<WalletInteraction>
    suspend fun sendTest(sessionId: String, data: String): Result<RcrResponse>
}

class RcrRepositoryImpl @Inject constructor(
    private val api: RcrApi,
    private val json: Json,
    private val dappLinkRepository: DappLinkRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,

    ) : RcrRepository {

    override suspend fun getRequest(sessionId: String, interactionId: String) = withContext(ioDispatcher) {
        api.executeRequest(RcrRequest.GetRequests(sessionId)).toResult().mapCatching { response ->
            val encryptedData = response.data ?: throw IllegalStateException("No data in response")
            val dappLink = dappLinkRepository.getDappLinks().getOrThrow().first { it.sessionId == sessionId }
            encryptedData.map { d ->
                val decryptedBytes = d.decodeHex().decrypt(dappLink.secret.value.decodeHex()).getOrThrow()
                val decryptedRequestString = String(decryptedBytes, StandardCharsets.UTF_8)
                json.decodeFromString<WalletInteraction>(decryptedRequestString)
            }.find { it.interactionId == interactionId } ?: throw IllegalStateException("No interaction with id $interactionId")
        }
    }

    override suspend fun sendResponse(sessionId: String, data: String) = withContext(ioDispatcher) {
        val dappLink = dappLinkRepository.getDappLinks().getOrThrow().first { it.sessionId == sessionId }
        val encryptedData = data.toByteArray().encrypt(dappLink.secret.value.decodeHex()).getOrThrow().toHexString()
        api.executeRequest(RcrRequest.SendRequest(sessionId, encryptedData)).toResult()
    }

    override suspend fun sendTest(sessionId: String, data: String) = withContext(ioDispatcher) {
        api.executeRequest(RcrRequest.SendRequest(sessionId, data)).toResult()
    }
}
