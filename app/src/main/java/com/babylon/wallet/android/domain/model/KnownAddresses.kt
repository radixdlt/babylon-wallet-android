package com.babylon.wallet.android.domain.model

import rdx.works.profile.derivation.model.NetworkId

data class KnownAddresses(val faucetAddress: String, val createAccountComponent: String, val xrdAddress: String) {
    companion object {

        val addressMap = mapOf(
            NetworkId.Nebunet to KnownAddresses(
                "component_tdx_b_1qftacppvmr9ezmekxqpq58en0nk954x0a7jv2zz0hc7qdxyth4",
                "package_tdx_b_1qy4hrp8a9apxldp5cazvxgwdj80cxad4u8cpkaqqnhlssf7lg2",
                "resource_tdx_b_1qzkcyv5dwq3r6kawy6pxpvcythx8rh8ntum6ws62p95s9hhz9x"
            ),
            NetworkId.Gilganet to KnownAddresses(
                "component_tdx_20_1qftacppvmr9ezmekxqpq58en0nk954x0a7jv2zz0hc7qlnye7x",
                "package_tdx_20_1qy4hrp8a9apxldp5cazvxgwdj80cxad4u8cpkaqqnhlskx38d7",
                "resource_tdx_20_1qzxcrac59cy2v9lpcpmf82qel3cjj25v3k5m09rxurgqfgndge"
            ),
            NetworkId.Enkinet to KnownAddresses(
                "component_tdx_21_1qftacppvmr9ezmekxqpq58en0nk954x0a7jv2zz0hc7qlrqh7e",
                "package_tdx_21_1qy4hrp8a9apxldp5cazvxgwdj80cxad4u8cpkaqqnhlskk4fdp",
                "resource_tdx_21_1qzxcrac59cy2v9lpcpmf82qel3cjj25v3k5m09rxurgqfchrgx"
            ),
            NetworkId.Hammunet to KnownAddresses(
                "component_tdx_22_1qftacppvmr9ezmekxqpq58en0nk954x0a7jv2zz0hc7ql6v973",
                "package_tdx_22_1qy4hrp8a9apxldp5cazvxgwdj80cxad4u8cpkaqqnhlsk0emdf",
                "resource_tdx_22_1qzxcrac59cy2v9lpcpmf82qel3cjj25v3k5m09rxurgqfpm3gw"
            ),
            NetworkId.Mardunet to KnownAddresses(
                "component_tdx_24_1qftacppvmr9ezmekxqpq58en0nk954x0a7jv2zz0hc7qlp5g7p",
                "package_tdx_24_1qy4hrp8a9apxldp5cazvxgwdj80cxad4u8cpkaqqnhlsk5pkde",
                "resource_tdx_24_1qzxcrac59cy2v9lpcpmf82qel3cjj25v3k5m09rxurgqf6rug7"
            )
        )
    }
}
