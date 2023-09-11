package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.utils.truncatedHash
import rdx.works.core.AddressValidator
import rdx.works.profile.data.model.apppreferences.Radix.dashboardUrl
import rdx.works.profile.derivation.model.NetworkId

data class ActionableAddress(
    val address: String,
    val type: Type
) {

    val isNft: Boolean = type == Type.RESOURCE && address.split(NFT_DELIMITER).size > 1

    val isCopyPrimaryAction: Boolean = type != Type.TRANSACTION

    val displayAddress: String = if (isNft) {
        val localId = address.split(NFT_DELIMITER)[1]
        Resource.NonFungibleResource.Item.ID.from(localId).displayable
    } else {
        address.truncatedHash()
    }

    fun toDashboardUrl(): String {
        val suffix = when {
            isNft -> "nft/$address"
            else -> "${type.prefix}/$address"
        }

        val url = AddressValidator.getValidNetworkId(address)?.let {
            NetworkId.from(it)
        }?.dashboardUrl() ?: NetworkId.Mainnet.dashboardUrl()
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
                const val TRANSACTION = "transaction"
            }

            fun from(address: String): Type = Type.values().find {
                address.startsWith(it.prefix)
            } ?: TRANSACTION
        }
    }
    companion object {
        private const val NFT_DELIMITER = ":"

        fun from(address: String) = ActionableAddress(
            address = address,
            type = Type.from(address)
        )
    }
}
