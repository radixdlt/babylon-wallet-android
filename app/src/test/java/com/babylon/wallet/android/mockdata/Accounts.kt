package com.babylon.wallet.android.mockdata

import rdx.works.profile.data.model.pernetwork.*

val account1 = OnNetwork.Account(
    address = "address1",
    appearanceID = 1,
    derivationPath = "derivationPath1",
    displayName = "displayName1",
    index = 1,
    networkID = 10,
    securityState = SecurityState.Unsecured(
        discriminator = "dsics",
        unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
            genesisFactorInstance = FactorInstance(
                derivationPath = DerivationPath("few", "disc"),
                factorInstanceID = "IDIDDIIDD",
                factorSourceReference = FactorSourceReference(
                    factorSourceID = "f32f3",
                    factorSourceKind = "kind"
                ),
                initializationDate = "Date1",
                publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
            )
        )
    )
)

val account2 = OnNetwork.Account(
    address = "address2",
    appearanceID = 2,
    derivationPath = "derivationPath2",
    displayName = "displayName2",
    index = 2,
    networkID = 10,
    securityState = SecurityState.Unsecured(
        discriminator = "dsics",
        unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
            genesisFactorInstance = FactorInstance(
                derivationPath = DerivationPath("few", "disc"),
                factorInstanceID = "IDIDDIIDD",
                factorSourceReference = FactorSourceReference(
                    factorSourceID = "f32f3",
                    factorSourceKind = "kind"
                ),
                initializationDate = "Date1",
                publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
            )
        )
    )
)
