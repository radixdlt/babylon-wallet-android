package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.presentation.mobileconnect.DappLink
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.mapWhen
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.coroutines.IoDispatcher
import timber.log.Timber
import javax.inject.Inject

interface DappLinkRepository {
    suspend fun getDappLink(sessionId: String): Result<DappLink>
    suspend fun persistDappLinkForSessionId(sessionId: String): Result<DappLink>

    suspend fun saveAsTemporary(link: DappLink): Result<DappLink>
}

class DappLinkRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DappLinkRepository {

    private val pendingDappLinks: MutableSet<DappLink> = mutableSetOf()
    private suspend fun getPersistedDappLinks(): Result<Set<DappLink>> {
        return runCatching {
            val linksSerialized = encryptedPreferencesManager.getDappLinks().orEmpty()
            if (linksSerialized.isEmpty()) {
                emptySet()
            } else {
                json.decodeFromString<Set<DappLink>>(linksSerialized)
            }
        }
    }

    override suspend fun getDappLink(sessionId: String): Result<DappLink> {
        pendingDappLinks.firstOrNull { it.sessionId == sessionId }?.let {
            return Result.success(it)
        }
        return getPersistedDappLinks().mapCatching { links ->
            links.firstOrNull { it.sessionId == sessionId }
                ?: error("No dapp link found for session id $sessionId")
        }
    }

    override suspend fun persistDappLinkForSessionId(sessionId: String): Result<DappLink> {
        return runCatching {
            withContext(ioDispatcher) {
                val toPersist =
                    pendingDappLinks.firstOrNull { it.sessionId == sessionId } ?: error("No dapp link found for session id $sessionId")
                val links = getPersistedDappLinks().getOrThrow().toMutableSet()
                val updatedLinks = if (links.any { it.dAppDefinitionAddress == toPersist.dAppDefinitionAddress }) {
                    links.mapWhen({ it.dAppDefinitionAddress == toPersist.dAppDefinitionAddress }) {
                        Timber.d("Dapp link: Updating existing link")
                        toPersist
                    }
                } else {
                    Timber.d("Dapp link: Adding new link")
                    links.apply { add(toPersist) }
                }.toSet()
                val linksSerialized = json.encodeToString(updatedLinks)
                encryptedPreferencesManager.putDappLinks(linksSerialized)
                pendingDappLinks.removeIf { it.sessionId == sessionId }
                Timber.d("Pending dApp links: ${pendingDappLinks.size}")
                toPersist
            }
        }
    }

    override suspend fun saveAsTemporary(link: DappLink): Result<DappLink> {
        return runCatching {
            pendingDappLinks.removeIf { it.sessionId == link.sessionId}
            pendingDappLinks.add(link)
            link
        }
    }
}
