package com.babylon.wallet.android.presentation.dialogs.info

// the enums are defined the same way they are written in Strings ("glossaryAnchor" tags)
@Suppress("EnumNaming", "EnumEntryNameCase")
enum class GlossaryItem {
    web3,
    radixnetwork,
    radixwallet,
    radixconnect,
    radixconnector,
    xrd,
    dashboard,
    dapps,
    connectbutton,
    dex,
    accounts,
    personas,
    tokens,
    nfts,
    claimnfts,
    networkstaking,
    poolunits,
    liquidstakeunits,
    badges,
    behaviors,
    transfers,
    transactions,
    transactionfee,
    guarantees,
    payingaccount,
    validators,
    bridging,
    gateways,
    preauthorizations,
    possibledappcalls,
    securityshields,
    buildingshield,
    emergencyfallback,
    biometricspin,
    arculus,
    ledgernano,
    passwords,
    mnemonics;

    companion object {

        val mfaRelated = listOf(
            securityshields,
            buildingshield,
            emergencyfallback,
            arculus,
            passwords,
            mnemonics
        )
    }
}
