package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.utils.truncatedHash

data class ActionableAddress(
    val address: String,
    val type: Type
) {

    val isNft: Boolean = type == Type.RESOURCE && address.split(NFT_DELIMITER).size > 1

    val isCopyPrimaryAction: Boolean = type != Type.TRANSACTION

    val displayAddress: String = if (isNft) {
        val localId = address.split(NFT_DELIMITER)[1]
        localId.filterNot {
            it == '<' || it == '>' ||
                    it == '#' ||
                    it == '[' || it == ']' ||
                    it == '{' || it == '}'
        }
    } else {
        address.truncatedHash()
    }

    fun toDashboardUrl(): String {
        val suffix = when {
            isNft -> "nft/$address"
            else -> "${type.prefix}/$address"
        }

        return "$DASHBOARD_BASE_URL/$suffix"
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
        private const val DASHBOARD_BASE_URL = "https://rcnet-dashboard.radixdlt.com"

        fun from(address: String) = ActionableAddress(
            address = address,
            type = Type.from(address)
        )
    }
}
