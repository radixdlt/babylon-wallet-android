package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.utils.truncatedHash

data class Address(
    val address: String,
    val truncated: String,
    val type: Type
) {

    private val isNft: Boolean
        get() = type == Type.RESOURCE && address.split(":").size > 1

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
        private const val DASHBOARD_BASE_URL = "https://betanet-dashboard.radixdlt.com"

        fun from(address: String) = Address(
            address = address,
            truncated = address.truncatedHash(),
            type = Type.from(address)
        )
    }
}
