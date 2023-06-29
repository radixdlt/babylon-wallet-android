package com.babylon.wallet.android.mockdata

import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId

fun account(
    name: String = "account-name",
    address: String = "address-$name",
    networkId: NetworkId = Radix.Gateway.default.network.networkId()
) = Network.Account(
    address = address,
    appearanceID = 1,
    displayName = name,
    networkID = networkId.value,
    securityState = SecurityState.Unsecured(
        unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
            transactionSigning = FactorInstance(
                derivationPath = DerivationPath.forAccount(
                    networkId = NetworkId.Mainnet,
                    accountIndex = 0,
                    keyType = KeyType.TRANSACTION_SIGNING
                ),
                factorSourceId = FactorSource.ID("IDIDDIIDD"),
                publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
            )
        )
    )
)
