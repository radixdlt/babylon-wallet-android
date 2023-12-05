package com.babylon.wallet.android.domain.model

import android.net.Uri
import com.babylon.wallet.android.domain.model.resources.metadata.AccountType
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.accountType
import com.babylon.wallet.android.domain.model.resources.metadata.claimedEntities
import com.babylon.wallet.android.domain.model.resources.metadata.claimedWebsites
import com.babylon.wallet.android.domain.model.resources.metadata.dAppDefinitions
import com.babylon.wallet.android.domain.model.resources.metadata.description
import com.babylon.wallet.android.domain.model.resources.metadata.iconUrl
import com.babylon.wallet.android.domain.model.resources.metadata.name

data class DApp(
    val dAppAddress: String,
    val metadata: List<Metadata> = listOf()
) {

    val name: String?
        get() = metadata.name()

    val description: String?
        get() = metadata.description()

    val iconUrl: Uri?
        get() = metadata.iconUrl()

    val isDappDefinition: Boolean
        get() = metadata.accountType() == AccountType.DAPP_DEFINITION

    val definitionAddresses: List<String>
        get() = metadata.dAppDefinitions().orEmpty()

    val claimedWebsites: List<String>
        get() = metadata.claimedWebsites().orEmpty()

    val claimedEntities: List<String>
        get() = metadata.claimedEntities().orEmpty()

    @Suppress("SwallowedException")
    fun isRelatedWith(origin: String): Boolean {
        return claimedWebsites.any {
            try {
                val claimedUri = Uri.parse(it)
                val originUri = Uri.parse(origin)
                claimedUri.scheme != null && claimedUri.host == originUri.host
            } catch (e: Exception) {
                false
            }
        }
    }
}
