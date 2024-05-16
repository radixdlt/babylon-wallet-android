package com.babylon.wallet.android.data.repository.p2plink

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.PublicKeyHash
import com.radixdlt.sargon.RadixConnectPurpose
import com.radixdlt.sargon.extensions.P2pLinks
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.coroutines.IoDispatcher
import javax.inject.Inject

interface P2PLinksRepository {

    fun observeP2PLinks(): Flow<P2pLinks>

    suspend fun getP2PLinks(): P2pLinks

    suspend fun getP2PLinks(purpose: RadixConnectPurpose): P2pLinks

    suspend fun addOrUpdateP2PLink(p2pLink: P2pLink)

    suspend fun removeP2PLink(id: PublicKeyHash)

    fun showRelinkConnectors(): Flow<Boolean>

    suspend fun showRelinkConnectorsAfterUpdate(): Boolean

    suspend fun showRelinkConnectorsAfterProfileRestore(): Boolean

    suspend fun clearShowRelinkConnectors()
}

class P2PLinksRepositoryImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val preferencesManager: PreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : P2PLinksRepository {

    override fun observeP2PLinks(): Flow<P2pLinks> {
        return encryptedPreferencesManager.getP2PLinkListJson()
            .map { serializedLinksResult ->
                serializedLinksResult?.getOrNull()
                    ?.let {
                        runCatching {
                            P2pLinks.fromJson(it)
                        }.getOrNull()
                    }
                    .orEmpty()
                    .asIdentifiable()
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getP2PLinks(): P2pLinks {
        return withContext(ioDispatcher) {
            getSavedP2PLinks()
        }
    }

    override suspend fun getP2PLinks(purpose: RadixConnectPurpose): P2pLinks {
        return withContext(ioDispatcher) {
            getSavedP2PLinks().asList()
                .filter { it.connectionPurpose == purpose }
                .asIdentifiable()
        }
    }

    override suspend fun addOrUpdateP2PLink(p2pLink: P2pLink) {
        withContext(ioDispatcher) {
            saveP2PLinks(
                getSavedP2PLinks()
                    .updateOrAppend(p2pLink)
                    .asList()
            )
        }
    }

    override suspend fun removeP2PLink(id: PublicKeyHash) {
        withContext(ioDispatcher) {
            val p2pLinks = getSavedP2PLinks()
            val p2pLink = p2pLinks.getBy(id) ?: return@withContext
            val newP2PLinks = getSavedP2PLinks().remove(p2pLink)

            saveP2PLinks(newP2PLinks.asList())
            peerdroidClient.deleteLink(p2pLink.connectionPassword)
        }
    }

    override fun showRelinkConnectors(): Flow<Boolean> {
        return combine(
            preferencesManager.showRelinkConnectorsAfterUpdate.map { it ?: false },
            preferencesManager.showRelinkConnectorsAfterProfileRestore
        ) { showAfterUpdate, showAfterProfileRestore ->
            showAfterUpdate || showAfterProfileRestore
        }.distinctUntilChanged()
    }

    override suspend fun showRelinkConnectorsAfterUpdate(): Boolean {
        return preferencesManager.showRelinkConnectorsAfterUpdate.firstOrNull() ?: false
    }

    override suspend fun showRelinkConnectorsAfterProfileRestore(): Boolean {
        return preferencesManager.showRelinkConnectorsAfterProfileRestore.firstOrNull() ?: false
    }

    override suspend fun clearShowRelinkConnectors() {
        preferencesManager.clearShowRelinkConnectors()
    }

    private suspend fun getSavedP2PLinks(): P2pLinks {
        return encryptedPreferencesManager.getP2PLinkListJson()
            .firstOrNull()
            ?.getOrNull()
            ?.let {
                runCatching {
                    P2pLinks.fromJson(it)
                }.getOrNull()
            }
            .orEmpty()
            .asIdentifiable()
    }

    private suspend fun saveP2PLinks(links: List<P2pLink>) {
        encryptedPreferencesManager.saveP2PLinkListJson(
            p2pLinkListJson = links.asIdentifiable()
                .toJson()
        )
    }
}
