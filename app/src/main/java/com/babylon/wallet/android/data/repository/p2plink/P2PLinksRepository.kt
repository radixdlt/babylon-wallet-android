package com.babylon.wallet.android.data.repository.p2plink

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.coroutines.IoDispatcher
import javax.inject.Inject

interface P2PLinksRepository {

    fun observeP2PLinks(): Flow<List<P2PLink>>

    suspend fun getP2PLinks(): List<P2PLink>

    suspend fun save(p2pLinks: List<P2PLink>)

    suspend fun removeP2PLink(publicKey: String)
}

class P2PLinksRepositoryImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : P2PLinksRepository {

    override fun observeP2PLinks(): Flow<List<P2PLink>> {
        return encryptedPreferencesManager.getP2PLinks()
            .map { serializedLinksResult ->
                serializedLinksResult?.getOrNull()
                    ?.let {
                        Json.decodeFromString<List<P2PLink>>(it)
                    } ?: emptyList()
            }
    }

    override suspend fun getP2PLinks(): List<P2PLink> {
        return withContext(ioDispatcher) { getSavedP2PLinks() }
    }

    override suspend fun save(p2pLinks: List<P2PLink>) {
        withContext(ioDispatcher) { saveP2PLinks(p2pLinks) }
    }

    override suspend fun removeP2PLink(publicKey: String) {
        withContext(ioDispatcher) {
            val p2pLinks = getSavedP2PLinks()
            val p2pLink = p2pLinks.findBy(publicKey) ?: return@withContext
            val newP2PLinks = getSavedP2PLinks().filter { it.publicKey != publicKey }

            saveP2PLinks(newP2PLinks)
            peerdroidClient.deleteLink(p2pLink.connectionPassword)
        }
    }

    private suspend fun getSavedP2PLinks(): List<P2PLink> {
        return encryptedPreferencesManager.getP2PLinks()
            .firstOrNull()
            ?.getOrNull()
            ?.let {
                Json.decodeFromString<List<P2PLink>>(it)
            } ?: emptyList()
    }

    private suspend fun saveP2PLinks(links: List<P2PLink>) {
        encryptedPreferencesManager.saveP2PLinks(
            serializedP2PLinks = Json.encodeToString(links.distinctBy { it.publicKey })
        )
    }
}