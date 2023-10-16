package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.utils.truncatedHash
import rdx.works.core.AddressValidator
import rdx.works.profile.data.model.apppreferences.Radix.dashboardUrl
import rdx.works.profile.derivation.model.NetworkId

data class ActionableAddress(
    val address: String,
    val shouldTruncateAddressForDisplay: Boolean = true
) {

    val type: Type? = Type.from(address)

    val isNft: Boolean = type == Type.RESOURCE && address.split(NFT_DELIMITER).size > 1

    val isCopyPrimaryAction: Boolean = type != Type.TRANSACTION

    val displayAddress: String = if (isNft) {
        val localId = address.split(NFT_DELIMITER)[1]
        Resource.NonFungibleResource.Item.ID.from(localId).displayable
    } else {
        if (shouldTruncateAddressForDisplay) address.truncatedHash() else address
    }

    fun toDashboardUrl(): String {
        val suffix = when {
            isNft -> "nft/$address"
            type == Type.TRANSACTION -> "transaction/$address"
            type != null -> "${type.prefix}/$address"
            else -> address
        }

        val url = if (type == Type.TRANSACTION) {
            NetworkId.Mainnet.dashboardUrl()
        } else {
            val globalAddress = address.split(NFT_DELIMITER)[0]
            AddressValidator.getValidNetworkId(globalAddress)?.let {
                NetworkId.from(it)
            }?.dashboardUrl() ?: NetworkId.Mainnet.dashboardUrl()
        }
        return "$url/$suffix"
    }

    enum class Type(
        val prefix: String
    ) {
        PACKAGE(HRP.PACKAGE),
        RESOURCE(HRP.RESOURCE),
        ACCOUNT(HRP.ACCOUNT),
        TRANSACTION(HRP.TRANSACTION),
        COMPONENT(HRP.COMPONENT);

        companion object {
            private object HRP {
                const val ACCOUNT = "account"
                const val RESOURCE = "resource"
                const val PACKAGE = "package"
                const val COMPONENT = "component"
                const val TRANSACTION = "txid"
            }

            fun from(address: String): Type? = Type.values().find {
                address.startsWith(it.prefix)
            }
        }
    }

    companion object {
        private const val NFT_DELIMITER = ":"
    }
}
