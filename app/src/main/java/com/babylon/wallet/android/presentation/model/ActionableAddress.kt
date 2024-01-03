package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.utils.encodeUtf8
import com.babylon.wallet.android.utils.truncatedHash
import rdx.works.profile.data.model.apppreferences.Radix.dashboardUrl
import rdx.works.profile.derivation.model.NetworkId

data class ActionableAddress(
    val address: String,
    val truncateAddress: Boolean = true
) {

    val type: Type? = Type.from(address)

    val isNft: Boolean = type == Type.Global.RESOURCE && address.split(NFT_DELIMITER).size > 1

    val displayAddress: String = if (isNft) {
        val localId = address.split(NFT_DELIMITER)[1]
        Resource.NonFungibleResource.Item.ID.from(localId).displayable
    } else if (type is Type.LocalId) {
        type.id.displayable
    } else {
        if (truncateAddress) address.truncatedHash() else address
    }

    fun toDashboardUrl(networkId: NetworkId): String? {
        val addressUrlEncoded = address.encodeUtf8()
        val suffix = when (type) {
            is Type.LocalId -> return null
            is Type.Global -> {
                when {
                    isNft -> "nft/$addressUrlEncoded"
                    type == Type.Global.TRANSACTION -> "transaction/$addressUrlEncoded"
                    type == Type.Global.VALIDATOR -> "component/$addressUrlEncoded"
                    else -> "${type.hrp}/$addressUrlEncoded"
                }
            }

            null -> addressUrlEncoded
        }

        val url = networkId.dashboardUrl()

        return "$url/$suffix"
    }

    sealed interface Type {

        enum class Global(val hrp: String) : Type {
            PACKAGE(HRP.PACKAGE),
            RESOURCE(HRP.RESOURCE),
            ACCOUNT(HRP.ACCOUNT),
            VALIDATOR(HRP.VALIDATOR),
            TRANSACTION(HRP.TRANSACTION),
            COMPONENT(HRP.COMPONENT);

            companion object {
                private object HRP {
                    const val ACCOUNT = "account"
                    const val RESOURCE = "resource"
                    const val PACKAGE = "package"
                    const val VALIDATOR = "validator"
                    const val COMPONENT = "component"
                    const val TRANSACTION = "txid"
                }

                fun from(address: String): Type? = Type.Global.entries.find {
                    address.startsWith(it.hrp)
                }
            }
        }

        data class LocalId(
            val id: Resource.NonFungibleResource.Item.ID
        ) : Type

        companion object {
            fun from(address: String): Type? {
                val globalType = Global.from(address)
                return if (globalType == null) {
                    val localId = runCatching {
                        Resource.NonFungibleResource.Item.ID.from(address)
                    }.getOrNull()

                    if (localId != null) {
                        LocalId(localId)
                    } else {
                        null
                    }
                } else {
                    globalType
                }
            }
        }
    }

    companion object {
        private const val NFT_DELIMITER = ":"
    }
}
