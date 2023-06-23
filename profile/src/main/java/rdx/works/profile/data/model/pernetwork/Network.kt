package rdx.works.profile.data.model.pernetwork

import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.crypto.PublicKey
import com.radixdlt.toolkit.models.method.DeriveVirtualAccountAddressInput
import com.radixdlt.toolkit.models.method.DeriveVirtualIdentityAddressInput
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.mapWhen
import rdx.works.core.toHexString
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.factorsources.OffDeviceMnemonicFactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.factorsources.TrustedContactFactorSource
import rdx.works.profile.data.model.factorsources.WasNotDeviceFactorSource
import rdx.works.profile.data.utils.getNextAccountDerivationIndex
import rdx.works.profile.data.utils.getNextIdentityDerivationIndex
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId

@Serializable
data class Network(
    /**
     * The ID of the network that has been used to generate the accounts, to which personas
     * have been added and dApps connected.
     */
    @SerialName("networkID")
    val networkID: Int,

    /**
     * Accounts created by the user for this network.
     */
    @SerialName("accounts")
    val accounts: List<Account>,

    /**
     * Personas created by the user for this network.
     */
    @SerialName("personas")
    val personas: List<Persona>,

    /**
     * AuthorizedDapp the user has connected with on this network.
     */
    @SerialName("authorizedDapps")
    val authorizedDapps: List<AuthorizedDapp>
) {

    val knownNetworkId: NetworkId?
        get() = NetworkId.values().find { it.value == networkID }

    @Serializable
    data class Account(
        /**
         * The globally unique and identifiable Radix component address of this account. Can be used as
         * a stable ID. Cryptographically derived from a seeding public key which typically was created by
         * the `DeviceFactorSource` (and typically the same public key as an instance of the device factor
         * typically used in the primary role of this account).
         */
        @SerialName("address")
        override val address: String,

        /**
         * An identifier for the gradient for this account, to be displayed in wallet
         * and possibly by dApps.
         */
        @SerialName("appearanceID")
        val appearanceID: Int,

        /**
         * An optional displayName or label, used by presentation layer only.
         */
        @SerialName("displayName")
        val displayName: String,

        /**
         * The ID of the network that has been used to generate the accounts, to which personas
         * have been added and dApps connected.
         */
        @SerialName("networkID")
        override val networkID: Int,

        /**
         * Security of this account
         */
        @SerialName("securityState")
        override val securityState: SecurityState
    ) : Entity {

        companion object {
            fun initAccountWithDeviceFactorSource(
                displayName: String,
                mnemonicWithPassphrase: MnemonicWithPassphrase,
                deviceFactorSource: DeviceFactorSource,
                networkId: NetworkId,
                appearanceID: Int
            ): Account {
                val index =
                    deviceFactorSource.nextDerivationIndicesPerNetwork.getNextAccountDerivationIndex(forNetworkId = networkId)

                val derivationPath = DerivationPath.forAccount(
                    networkId = networkId,
                    accountIndex = index,
                    keyType = KeyType.TRANSACTION_SIGNING
                )

                val compressedPublicKey =
                    mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath).removeLeadingZero()
                val address = deriveAccountAddress(
                    networkID = networkId,
                    publicKey = PublicKey.EddsaEd25519(compressedPublicKey)
                )

                val unsecuredSecurityState = SecurityState.unsecured(
                    publicKey = FactorInstance.PublicKey(compressedPublicKey.toHexString(), Slip10Curve.CURVE_25519),
                    derivationPath = derivationPath,
                    factorSourceId = deviceFactorSource.id
                )

                return Account(
                    address = address,
                    appearanceID = appearanceID,
                    displayName = displayName,
                    networkID = networkId.value,
                    securityState = unsecuredSecurityState
                )
            }

            @Suppress("LongParameterList")
            fun initAccountWithLedgerFactorSource(
                displayName: String,
                derivedPublicKeyHex: String,
                ledgerFactorSource: LedgerHardwareWalletFactorSource,
                networkId: NetworkId,
                derivationPath: DerivationPath,
                appearanceID: Int
            ): Account {
                val index =
                    ledgerFactorSource.nextDerivationIndicesPerNetwork.getNextAccountDerivationIndex(forNetworkId = networkId)

                val derivationPathToCheck = DerivationPath.forAccount(
                    networkId = networkId,
                    accountIndex = index,
                    keyType = KeyType.TRANSACTION_SIGNING
                )
                require(derivationPathToCheck.path == derivationPath.path)

                val address = deriveAccountAddress(
                    networkID = networkId,
                    publicKey = PublicKey.EddsaEd25519(derivedPublicKeyHex)
                )

                val unsecuredSecurityState = SecurityState.unsecured(
                    publicKey = FactorInstance.PublicKey(derivedPublicKeyHex, Slip10Curve.CURVE_25519),
                    derivationPath = derivationPath,
                    factorSourceId = ledgerFactorSource.id
                )

                return Account(
                    address = address,
                    appearanceID = appearanceID,
                    displayName = displayName,
                    networkID = networkId.value,
                    securityState = unsecuredSecurityState
                )
            }

            private fun deriveAccountAddress(
                networkID: NetworkId,
                publicKey: PublicKey
            ): String {
                val request = DeriveVirtualAccountAddressInput(networkID.value.toUByte(), publicKey)
                // TODO handle error
                val response = RadixEngineToolkit.deriveVirtualAccountAddress(request).getOrThrow()
                return response.virtualAccountAddress
            }
        }
    }

    @Serializable
    data class Persona(
        /**
         * The globally unique and identifiable Radix component address of this persona. Can be used as
         * a stable ID. Cryptographically derived from a seeding public key which typically was created by
         * the `DeviceFactorSource` (and typically the same public key as an instance of the device factor
         * typically used in the primary role of this persona).
         */
        @SerialName("address")
        override val address: String,

        /**
         * An optional displayName or label, used by presentation layer only.
         */
        @SerialName("displayName")
        val displayName: String,

        @SerialName("fields")
        val fields: List<Field>,

        /**
         * The ID of the network that has been used to generate the accounts, to which personas
         * have been added and dApps connected.
         */
        @SerialName("networkID")
        override val networkID: Int,

        /**
         * Security of this persona
         */
        @SerialName("securityState")
        override val securityState: SecurityState
    ) : Entity {

        companion object {
            @Suppress("LongParameterList") // TODO refine this later on
            fun init(
                displayName: String,
                fields: List<Field>,
                mnemonicWithPassphrase: MnemonicWithPassphrase,
                factorSource: DeviceFactorSource,
                networkId: NetworkId
            ): Persona {
                val index =
                    factorSource.nextDerivationIndicesPerNetwork.getNextIdentityDerivationIndex(forNetworkId = networkId)

                val derivationPath = DerivationPath.forIdentity(
                    networkId = networkId,
                    identityIndex = index,
                    keyType = KeyType.TRANSACTION_SIGNING
                )

                val compressedPublicKey =
                    mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath).removeLeadingZero()

                val address = deriveIdentityAddress(
                    networkID = networkId,
                    publicKey = PublicKey.EddsaEd25519(compressedPublicKey)
                )

                val unsecuredSecurityState = SecurityState.unsecured(
                    publicKey = FactorInstance.PublicKey(compressedPublicKey.toHexString(), Slip10Curve.CURVE_25519),
                    derivationPath = derivationPath,
                    factorSourceId = factorSource.id
                )

                return Persona(
                    address = address,
                    displayName = displayName,
                    fields = fields,
                    networkID = networkId.value,
                    securityState = unsecuredSecurityState
                )
            }

            private fun deriveIdentityAddress(
                networkID: NetworkId,
                publicKey: PublicKey
            ): String {
                val request = DeriveVirtualIdentityAddressInput(networkID.value.toUByte(), publicKey)
                // TODO handle error
                val response = RadixEngineToolkit.deriveVirtualIdentityAddress(request).getOrThrow()
                return response.virtualIdentityAddress
            }
        }

        @Serializable
        data class Field(
            @SerialName("id")
            val id: ID,

            @SerialName("value")
            val value: String
        ) {

            @Serializable
            enum class ID {
                @SerialName("givenName")
                GivenName,

                @SerialName("familyName")
                FamilyName,

                @SerialName("emailAddress")
                EmailAddress,

                @SerialName("phoneNumber")
                PhoneNumber
            }

            companion object {
                fun init(
                    id: ID,
                    value: String
                ): Field {
                    return Field(
                        id = id,
                        value = value
                    )
                }
            }
        }
    }

    @Serializable
    data class AuthorizedDapp(

        @SerialName("networkID")
        val networkID: Int,

        @SerialName("dAppDefinitionAddress")
        val dAppDefinitionAddress: String,

        @SerialName("displayName")
        val displayName: String,

        @SerialName("referencesToAuthorizedPersonas")
        val referencesToAuthorizedPersonas: List<AuthorizedPersonaSimple>
    ) {

        @Serializable
        data class AuthorizedPersonaSimple(
            /**
             * The globally unique identifier of a Persona is its address, used to lookup persona
             */
            @SerialName("identityAddress")
            val identityAddress: String,

            /**
             * List of "ongoing personaData" (identified by OnNetwork.Persona.Field.ID) user has given Dapp access to.
             */
            @SerialName("sharedFieldIDs")
            val fieldIDs: List<Persona.Field.ID>,

            @SerialName("lastLogin")
            val lastLogin: String,

            /**
             * List of shared accounts that user given the dApp access to.
             */
            @SerialName("sharedAccounts")
            val sharedAccounts: SharedAccounts
        ) {

            @Serializable
            data class SharedAccounts(
                @SerialName("accountsReferencedByAddress")
                val accountsReferencedByAddress: List<String>,

                @SerialName("request")
                val request: NumberOfAccounts
            ) {

                @Serializable
                data class NumberOfAccounts(
                    @SerialName("quantifier")
                    val quantifier: Quantifier,

                    @SerialName("quantity")
                    val quantity: Int
                ) {

                    enum class Quantifier {
                        @SerialName("exactly")
                        Exactly,

                        @SerialName("atLeast")
                        AtLeast
                    }
                }
            }
        }

        fun hasAuthorizedPersona(personaAddress: String): Boolean {
            return referencesToAuthorizedPersonas.any { it.identityAddress == personaAddress }
        }
    }

    @Serializable
    data class NextDerivationIndices(
        @SerialName("networkID")
        val networkId: Int,
        @SerialName("forAccount")
        val forAccount: Int,
        @SerialName("forIdentity")
        val forIdentity: Int
    )
}

fun Profile.addAccount(
    account: Network.Account,
    withFactorSourceId: FactorSource.FactorSourceID,
    onNetwork: NetworkId
): Profile {
    val networkExist = this.networks.any { onNetwork.value == it.networkID }
    val newNetworks = if (networkExist) {
        this.networks.mapWhen(predicate = { network -> network.networkID == onNetwork.value }) { network ->
            val updatedAccounts = network.accounts.toMutableList()
            updatedAccounts.add(account)
            Network(
                accounts = updatedAccounts.toList(),
                authorizedDapps = network.authorizedDapps,
                networkID = network.networkID,
                personas = network.personas
            )
        }
    } else {
        this.networks + Network(
            accounts = listOf(account),
            authorizedDapps = listOf(),
            networkID = onNetwork.value,
            personas = listOf()
        )
    }
    var updatedProfile = copy(
        networks = newNetworks,
    )
    updatedProfile = updatedProfile.incrementFactorSourceNextAccountIndex(
        forNetworkId = onNetwork,
        withFactorSourceId = withFactorSourceId
    )
    return updatedProfile.withUpdatedContentHint()
}

fun Profile.addAuthSigningFactorInstanceForEntity(
    entity: Entity,
    authSigningFactorInstance: FactorInstance
): Profile {
    val updatedNetworks =
        networks.mapWhen(predicate = { network -> network.networkID == entity.networkID }) { network ->
            when (entity) {
                is Network.Account -> network.copy(
                    accounts = network.accounts.mapWhen(predicate = { it.address == entity.address }) { account ->
                        val updatedSecurityState = when (val state = account.securityState) {
                            is SecurityState.Unsecured -> state.copy(
                                unsecuredEntityControl = state.unsecuredEntityControl.copy(
                                    authenticationSigning = authSigningFactorInstance
                                )
                            )
                        }
                        account.copy(securityState = updatedSecurityState)
                    }
                )

                is Network.Persona -> {
                    network.copy(
                        personas = network.personas.mapWhen(predicate = { it.address == entity.address }) { persona ->
                            val updatedSecurityState = when (val state = persona.securityState) {
                                is SecurityState.Unsecured -> state.copy(
                                    unsecuredEntityControl = state.unsecuredEntityControl.copy(
                                        authenticationSigning = authSigningFactorInstance
                                    )
                                )
                            }
                            persona.copy(securityState = updatedSecurityState)
                        }
                    )
                }
            }
        }
    return copy(
        networks = updatedNetworks
    )
}

fun Profile.addNetworkIfDoesNotExist(
    onNetwork: NetworkId
): Profile {
    val networkExist = this.networks.any { onNetwork.value == it.networkID }
    return if (!networkExist) {
        copy(
            networks = networks + Network(
                accounts = listOf(),
                authorizedDapps = listOf(),
                networkID = onNetwork.value,
                personas = listOf()
            )
        ).withUpdatedContentHint()
    } else {
        this
    }
}

fun Profile.incrementFactorSourceNextAccountIndex(
    forNetworkId: NetworkId,
    withFactorSourceId: FactorSource.FactorSourceID
): Profile {
    return copy(
        factorSources = factorSources.mapWhen(
            predicate = { factorSource ->
                factorSource.id == withFactorSourceId
            }
        ) { factorSource ->
            when (factorSource) {
                is DeviceFactorSource -> {
                    val nextDerivationIndices = factorSource.nextDerivationIndicesPerNetwork
                    factorSource.copy(
                        nextDerivationIndicesPerNetwork = nextDerivationIndices?.incrementAccountIndex(forNetworkId = forNetworkId)
                    )
                }

                is LedgerHardwareWalletFactorSource -> {
                    val nextDerivationIndices = factorSource.nextDerivationIndicesPerNetwork
                    factorSource.copy(
                        nextDerivationIndicesPerNetwork = nextDerivationIndices?.incrementAccountIndex(forNetworkId = forNetworkId)
                    )
                }

                is OffDeviceMnemonicFactorSource -> throw WasNotDeviceFactorSource()
                is TrustedContactFactorSource -> throw WasNotDeviceFactorSource()
            }
        }
    )
}

fun List<Network.NextDerivationIndices>.incrementAccountIndex(forNetworkId: NetworkId): List<Network.NextDerivationIndices> {
    val indicesForNetwork = find {
        it.networkId == forNetworkId.value
    }

    val mutatedList = if (indicesForNetwork == null) {
        this + Network.NextDerivationIndices(
            networkId = forNetworkId.value,
            forAccount = 1,
            forIdentity = 0
        )
    } else {
        map {
            if (it.networkId == forNetworkId.value) {
                it.copy(forAccount = it.forAccount + 1)
            } else {
                it
            }
        }
    }

    return mutatedList
}

fun Profile.updatePersona(
    persona: Network.Persona
): Profile {
    val networkId = currentGateway.network.networkId()

    return copy(
        networks = networks.mapWhen(
            predicate = { it.networkID == networkId.value },
            mutation = { network ->
                network.copy(
                    personas = network.personas.mapWhen(
                        predicate = { it.address == persona.address },
                        mutation = { persona }
                    )
                )
            }
        )
    )
}

fun Profile.addPersona(
    persona: Network.Persona,
    withFactorSourceId: FactorSource.FactorSourceID,
    onNetwork: NetworkId,
): Profile {
    val personaExists = this.networks.find {
        it.networkID == onNetwork.value
    }?.personas?.any { it.address == persona.address } ?: false

    if (personaExists) {
        return this
    }

    return copy(
        networks = this.networks.mapWhen(
            predicate = { it.networkID == onNetwork.value },
            mutation = { network ->
                network.copy(personas = network.personas + persona)
            }
        ),
        factorSources = factorSources.mapWhen(
            predicate = { it.id == withFactorSourceId }
        ) { factorSource ->
            when (factorSource) {
                is DeviceFactorSource -> {
                    val nextDerivationIndices = factorSource.nextDerivationIndicesPerNetwork
                    factorSource.copy(
                        nextDerivationIndicesPerNetwork = nextDerivationIndices?.incrementPersonaIndex(forNetworkId = onNetwork)
                    )
                }

                is LedgerHardwareWalletFactorSource -> {
                    val nextDerivationIndices = factorSource.nextDerivationIndicesPerNetwork
                    factorSource.copy(
                        nextDerivationIndicesPerNetwork = nextDerivationIndices?.incrementPersonaIndex(forNetworkId = onNetwork)
                    )
                }

                is OffDeviceMnemonicFactorSource -> throw WasNotDeviceFactorSource()
                is TrustedContactFactorSource -> throw WasNotDeviceFactorSource()
            }
        }
    ).withUpdatedContentHint()
}

fun List<Network.NextDerivationIndices>.incrementPersonaIndex(forNetworkId: NetworkId): List<Network.NextDerivationIndices> {
    val indicesForNetwork = find {
        it.networkId == forNetworkId.value
    }

    val mutatedList = if (indicesForNetwork == null) {
        this + Network.NextDerivationIndices(
            networkId = forNetworkId.value,
            forAccount = 0,
            forIdentity = 1
        )
    } else {
        map {
            if (it.networkId == forNetworkId.value) {
                it.copy(forIdentity = it.forIdentity + 1)
            } else {
                it
            }
        }
    }

    return mutatedList
}
