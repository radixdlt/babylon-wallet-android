package com.babylon.wallet.android.domain.model.resources.metadata

enum class AccountType(val asString: String) {
    DAPP_DEFINITION("dapp definition");

    companion object {
        fun from(value: String) = AccountType.values()
            .find { it.asString == value }
    }
}
