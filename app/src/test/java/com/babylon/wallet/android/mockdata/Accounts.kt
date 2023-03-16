package com.babylon.wallet.android.mockdata

import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.*

val account1 = OnNetwork.Account(
    address = "address1",
    appearanceID = 1,
    displayName = "displayName1",
    networkID = 10,
    securityState = SecurityState.Unsecured(
        unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
            genesisFactorInstance = FactorInstance(
                derivationPath = DerivationPath.accountDerivationPath("derivationPath1"),
                factorSourceId = FactorSource.ID("IDIDDIIDD"),
                publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
            )
        )
    )
)

val account2 = OnNetwork.Account(
    address = "address2",
    appearanceID = 2,
    displayName = "displayName2",
    networkID = 10,
    securityState = SecurityState.Unsecured(
        unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
            genesisFactorInstance = FactorInstance(
                derivationPath = DerivationPath.accountDerivationPath("derivationPath2"),
                factorSourceId = FactorSource.ID("IDIDDIIDD"),
                publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
            )
        )
    )
)
