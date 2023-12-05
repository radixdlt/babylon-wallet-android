package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.utils.encodeUtf8
import com.babylon.wallet.android.utils.truncatedHash
import rdx.works.profile.data.model.apppreferences.Radix.dashboardUrl
import rdx.works.profile.derivation.model.NetworkId

data class ActionableAddress(
    val address: String,
    val shouldTruncateAddressForDisplay: Boolean = true
) {

    val type: Type? = Type.from(address)

    val isNft: Boolean = type is Type.Global.Resource && address.split(NFT_DELIMITER).size > 1

    val displayAddress: String = if (isNft) {
        val localId = address.split(NFT_DELIMITER)[1]
        Resource.NonFungibleResource.Item.ID.from(localId).displayable
    } else if (type is Type.LocalId) {
        type.id.displayable
    } else {
        if (shouldTruncateAddressForDisplay) address.truncatedHash() else address
    }

    fun toDashboardUrl(networkId: NetworkId): String? {
        val addressUrlEncoded = address.encodeUtf8()
        val suffix = when (type) {
            is Type.LocalId -> return null
            is Type.Global -> {
                when {
                    isNft -> "nft/$addressUrlEncoded"
                    type == Type.Global.Transaction -> "transaction/$addressUrlEncoded"
                    type == Type.Global.Validator -> "component/$addressUrlEncoded"
                    else -> "${type.hrp}/$addressUrlEncoded"
                }
            }

            null -> addressUrlEncoded
        }

        val url = networkId.dashboardUrl()

        return "$url/$suffix"
    }

    sealed interface Type {

        sealed interface Global : Type {
            val hrp: String

            data object Package : Global {
                override val hrp: String
                    get() = "package"
            }

            data object Resource : Global {
                override val hrp: String
                    get() = "resource"
            }

            data object Account : Global {
                override val hrp: String
                    get() = "account"
            }

            data object Validator : Global {
                override val hrp: String
                    get() = "validator"
            }

            data object Transaction : Global {
                override val hrp: String
                    get() = "txid"
            }

            data object Component : Global {
                override val hrp: String
                    get() = "component"
            }

            companion object {
                val types = setOf(Package, Resource, Account, Validator, Transaction, Component)
            }
        }

        data class LocalId(
            val id: Resource.NonFungibleResource.Item.ID
        ) : Type

        companion object {
            fun from(address: String): Type? {
                val globalType = Global.types.find { address.startsWith(it.hrp) }
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
