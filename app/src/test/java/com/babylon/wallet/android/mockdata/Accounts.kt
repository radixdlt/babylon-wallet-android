package com.babylon.wallet.android.mockdata

import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.*

fun account(
    name: String = "account-name",
    address: String = "address-$name"
) = Network.Account(
    address = address,
    appearanceID = 1,
    displayName = name,
    networkID = 10,
    securityState = SecurityState.Unsecured(
        unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
            genesisFactorInstance = FactorInstance(
                derivationPath = DerivationPath.forAccount("derivationPath1"),
                factorSourceId = FactorSource.ID("IDIDDIIDD"),
                publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
            )
        )
    )
)
