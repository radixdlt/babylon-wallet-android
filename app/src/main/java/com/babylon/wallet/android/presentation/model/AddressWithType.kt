package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.AddressHRP
import com.babylon.wallet.android.utils.truncatedHash

data class AddressWithType(
    val address: String,
    val truncated: String,
    val type: AddressType
) {

    private val isNft: Boolean
        get() = type == AddressType.RESOURCE && address.split(":").size > 1

    fun toDashboardUrl(): String {
        val suffix = when {
            isNft -> "nft/$address"
            else -> "${type.prefix}/$address"
        }

        return "$DASHBOARD_BASE_URL/$suffix"
    }

    companion object {
        private const val DASHBOARD_BASE_URL = "https://betanet-dashboard.radixdlt.com"

        fun from(address: String) = AddressWithType(
            address = address,
            truncated = address.truncatedHash(),
            type = AddressType.from(address)
        )
    }

}

enum class AddressType(
    val prefix: String
) {
    PACKAGE(AddressHRP.PACKAGE),
    RESOURCE(AddressHRP.RESOURCE),
    ACCOUNT(AddressHRP.ACCOUNT),
    TRANSACTION(AddressHRP.TRANSACTION),
    COMPONENT(AddressHRP.COMPONENT);

    companion object {

        fun from(address: String): AddressType = AddressType.values().find {
            address.startsWith(it.prefix)
        } ?: TRANSACTION

    }
}
