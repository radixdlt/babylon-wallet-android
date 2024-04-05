package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.presentation.m2m.DappLink
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.IdentifiedArrayList
import rdx.works.core.emptyIdentifiedArrayList
import rdx.works.core.mapWhen
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.coroutines.IoDispatcher
import timber.log.Timber
import javax.inject.Inject

interface DappLinkRepository {
    suspend fun getDappLinks(): Result<List<DappLink>>

    suspend fun getDappLink(sessionId: String): Result<DappLink>
    suspend fun saveDappLink(link: DappLink): Result<Unit>
}

class DappLinkRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DappLinkRepository {

    override suspend fun getDappLinks(): Result<IdentifiedArrayList<DappLink>> {
        return runCatching {
            val linksSerialized = encryptedPreferencesManager.getDappLinks().orEmpty()
            if (linksSerialized.isEmpty()) {
                emptyIdentifiedArrayList()
            } else {
                json.decodeFromString<IdentifiedArrayList<DappLink>>(linksSerialized)
            }
        }
    }

    override suspend fun getDappLink(sessionId: String): Result<DappLink> {
        return getDappLinks().mapCatching { links ->
            links.firstOrNull { it.sessionId == sessionId }
                ?: throw IllegalStateException("No dapp link found for session id $sessionId")
        }
    }

    override suspend fun saveDappLink(link: DappLink): Result<Unit> {
        return runCatching {
            withContext(ioDispatcher) {
                val links = getDappLinks().getOrThrow()
                val updatedLinks = if (links.any { it.dAppDefinitionAddress == link.dAppDefinitionAddress }) {
                    links.mapWhen({ it.dAppDefinitionAddress == link.dAppDefinitionAddress }) {
                        Timber.d("Dapp link: Updating existing link")
                        link
                    }
                } else {
                    Timber.d("Dapp link: Adding new link")
                    links.apply { add(link) }
                }
                val linksSerialized = json.encodeToString(updatedLinks)
                encryptedPreferencesManager.putDappLinks(linksSerialized)
            }
        }
    }

}
