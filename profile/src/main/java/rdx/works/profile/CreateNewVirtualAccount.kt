package rdx.works.profile

import rdx.works.profile.enginetoolkit.EngineToolkit
import rdx.works.profile.model.pernetwork.Account
import rdx.works.profile.model.pernetwork.DerivationPath

fun createNewVirtualAccount(
    displayName: String,
    engineToolkit: EngineToolkit,
    entityDerivationPath: EntityDerivationPath,
    entityIndex: EntityIndex,
    derivePublicKey: DerivePublicKey,
    createSecurityState: CreateSecurityState
): Account {
    val derivationPath = entityDerivationPath.path()

    val compressedPublicKey = derivePublicKey.derive(derivationPath)
    val address = engineToolkit.deriveAddress(compressedPublicKey)

    val unsecuredSecurityState = createSecurityState.create(
        derivationPath = DerivationPath.accountDerivationPath(
            derivationPath = derivationPath
        ),
        compressedPublicKey = compressedPublicKey
    )

    return Account(
        address = address,
        appearanceID = 0,//TODO add some gradient later on
        derivationPath = derivationPath,
        displayName = displayName,
        index = entityIndex.index(),
        securityState = unsecuredSecurityState
    )
}
