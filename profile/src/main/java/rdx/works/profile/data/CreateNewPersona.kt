package rdx.works.profile.data

import rdx.works.profile.enginetoolkit.EngineToolkit
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Persona
import rdx.works.profile.data.model.pernetwork.PersonaField

fun createNewPersona(
    displayName: String,
    fields: List<PersonaField>,
    engineToolkit: EngineToolkit,
    entityDerivationPath: EntityDerivationPath,
    entityIndex: EntityIndex,
    derivePublicKey: DerivePublicKey,
    createSecurityState: CreateSecurityState
): Persona {
    val derivationPath = entityDerivationPath.path()

    val compressedPublicKey = derivePublicKey.derive(derivationPath)
    val address = engineToolkit.deriveAddress(compressedPublicKey)

    val unsecuredSecurityState = createSecurityState.create(
        derivationPath = DerivationPath.identityDerivationPath(
            derivationPath = derivationPath
        ),
        compressedPublicKey = compressedPublicKey
    )

    return Persona(
        address = address,
        derivationPath = derivationPath,
        displayName = displayName,
        fields = fields,
        index = entityIndex.index(),
        securityState = unsecuredSecurityState
    )
}