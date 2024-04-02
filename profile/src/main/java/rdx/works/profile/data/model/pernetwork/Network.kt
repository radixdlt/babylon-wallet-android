@file:OptIn(ExperimentalSerializationApi::class)
@file:Suppress("TooManyFunctions")

package rdx.works.profile.data.model.pernetwork

import com.radixdlt.extensions.removeLeadingZero
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import rdx.works.core.Identified
import rdx.works.core.IdentifiedArrayList
import rdx.works.core.emptyIdentifiedArrayList
import rdx.works.core.mapWhen
import rdx.works.core.toHexString
import rdx.works.core.toIdentifiedArrayList
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.model.extensions.derivationPathEntityIndex
import rdx.works.profile.data.model.extensions.derivationPathScheme
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.extensions.mainBabylonFactorSource
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.EntityFlag
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.ret.crypto.PublicKey

@Serializable
data class Network(
    /**
     * The ID of the network that has been used to generate the accounts, to which personas
     * have been added and dApps connected.
     */
    @SerialName("networkID") val networkID: Int,

    /**
     * Accounts created by the user for this network.
     */
    @SerialName("accounts") val accounts: IdentifiedArrayList<Account>,

    /**
     * Personas created by the user for this network.
     */
    @SerialName("personas") val personas: IdentifiedArrayList<Persona>,

    /**
     * AuthorizedDapp the user has connected with on this network.
     */
    @SerialName("authorizedDapps") val authorizedDapps: List<AuthorizedDapp>
) {

    val knownNetworkId: NetworkId?
        get() = NetworkId.entries.find { it.value == networkID }

    @Serializable
    data class Account(
        /**
         * The globally unique and identifiable Radix component address of this account. Can be used as
         * a stable ID. Cryptographically derived from a seeding public key which typically was created by
         * the `DeviceFactorSource` (and typically the same public key as an instance of the device factor
         * typically used in the primary role of this account).
         */
        @SerialName("address") override val address: String,

        /**
         * An identifier for the gradient for this account, to be displayed in wallet
         * and possibly by dApps.
         */
        @SerialName("appearanceID") val appearanceID: Int,

        /**
         * An optional displayName or label, used by presentation layer only.
         */
        @SerialName("displayName") val displayName: String,

        /**
         * The ID of the network that has been used to generate the accounts, to which personas
         * have been added and dApps connected.
         */
        @SerialName("networkID") override val networkID: Int,

        /**
         * Security of this account
         */
        @SerialName("securityState") override val securityState: SecurityState,

        /**
         * The on ledger synced settings for this account
         */
        @SerialName("onLedgerSettings") val onLedgerSettings: OnLedgerSettings,

        @EncodeDefault
        override val flags: Set<EntityFlag> = emptySet()

    ) : Entity(), Identified {

        @Serializable
        data class OnLedgerSettings(
            @SerialName("thirdPartyDeposits")
            @EncodeDefault
            val thirdPartyDeposits: ThirdPartyDeposits
        ) {

            @Serializable
            data class ThirdPartyDeposits(
                @SerialName("depositRule")
                @EncodeDefault
                val depositRule: DepositRule = DepositRule.AcceptAll,
                @SerialName("assetsExceptionList")
                @EncodeDefault
                val assetsExceptionList: List<AssetException>? = emptyList(),
                @SerialName("depositorsAllowList")
                @EncodeDefault
                val depositorsAllowList: List<DepositorAddress>? = emptyList(),
            ) {
                @Serializable
                enum class DepositRule {
                    @SerialName("acceptAll")
                    AcceptAll,

                    @SerialName("acceptKnown")
                    AcceptKnown,

                    @SerialName("denyAll")
                    DenyAll
                }

                @Serializable
                enum class DepositAddressExceptionRule {
                    @SerialName("allow")
                    Allow,

                    @SerialName("deny")
                    Deny
                }

                @Serializable
                @JsonClassDiscriminator(discriminator = "discriminator")
                sealed interface DepositorAddress {

                    val address: String

                    @Serializable
                    @SerialName("resourceAddress")
                    data class ResourceAddress(
                        @SerialName("value") val value: String
                    ) : DepositorAddress {
                        override val address: String
                            get() = value
                    }

                    @Serializable
                    @SerialName("nonFungibleGlobalID")
                    data class NonFungibleGlobalID(
                        @SerialName("value") val value: String
                    ) : DepositorAddress {
                        override val address: String
                            get() = value
                    }
                }

                @Serializable
                data class AssetException(
                    @SerialName("address") val address: String,
                    @SerialName("exceptionRule") val exceptionRule: DepositAddressExceptionRule,
                )

                companion object {
                    fun init(): ThirdPartyDeposits {
                        return ThirdPartyDeposits(assetsExceptionList = emptyList(), depositorsAllowList = emptyList())
                    }
                }
            }

            companion object {
                fun init(): OnLedgerSettings {
                    return OnLedgerSettings(thirdPartyDeposits = ThirdPartyDeposits.init())
                }
            }
        }

        val isLedgerAccount: Boolean
            get() = when (securityState) {
                is SecurityState.Unsecured -> {
                    securityState.unsecuredEntityControl
                        .transactionSigning.factorSourceId.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET
                }
            }

        override val identifier: String
            get() = address
    }

    @Serializable
    data class Persona(
        /**
         * The globally unique and identifiable Radix component address of this persona. Can be used as
         * a stable ID. Cryptographically derived from a seeding public key which typically was created by
         * the `DeviceFactorSource` (and typically the same public key as an instance of the device factor
         * typically used in the primary role of this persona).
         */
        @SerialName("address") override val address: String,

        /**
         * An optional displayName or label, used by presentation layer only.
         */
        @SerialName("displayName") val displayName: String,

        @SerialName("personaData") val personaData: PersonaData,

        /**
         * The ID of the network that has been used to generate the accounts, to which personas
         * have been added and dApps connected.
         */
        @SerialName("networkID") override val networkID: Int,

        /**
         * Security of this persona
         */
        @SerialName("securityState") override val securityState: SecurityState,

        @EncodeDefault
        override val flags: Set<EntityFlag> = emptySet()
    ) : Entity(), Identified {

        companion object {
            @Suppress("LongParameterList") // TODO refine this later on
            fun init(
                entityIndex: Int,
                displayName: String,
                mnemonicWithPassphrase: MnemonicWithPassphrase,
                factorSource: DeviceFactorSource,
                networkId: NetworkId,
                personaData: PersonaData = PersonaData()
            ): Persona {
                val derivationPath = DerivationPath.forIdentity(
                    networkId = networkId,
                    identityIndex = entityIndex,
                    keyType = KeyType.TRANSACTION_SIGNING
                )

                val compressedPublicKey = mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath).removeLeadingZero()

                val address = PublicKey.Ed25519(compressedPublicKey).deriveIdentityAddress(networkId = networkId.value)

                val unsecuredSecurityState = SecurityState.unsecured(
                    publicKey = FactorInstance.PublicKey(compressedPublicKey.toHexString(), Slip10Curve.CURVE_25519),
                    derivationPath = derivationPath,
                    factorSourceId = factorSource.id
                )

                return Persona(
                    address = address,
                    displayName = displayName,
                    networkID = networkId.value,
                    securityState = unsecuredSecurityState,
                    personaData = personaData
                )
            }
        }

        override val identifier: String
            get() = address
    }

    @Serializable
    data class AuthorizedDapp(

        @SerialName("networkID") val networkID: Int,

        @SerialName("dAppDefinitionAddress") val dAppDefinitionAddress: String,

        @SerialName("displayName") val displayName: String? = null,

        @SerialName("referencesToAuthorizedPersonas") val referencesToAuthorizedPersonas: List<AuthorizedPersonaSimple>
    ) {

        fun hasAuthorizedPersona(personaAddress: String): Boolean {
            return referencesToAuthorizedPersonas.any { it.identityAddress == personaAddress }
        }

        @Serializable
        data class AuthorizedPersonaSimple(
            /**
             * The globally unique identifier of a Persona is its address, used to lookup persona
             */
            @SerialName("identityAddress") val identityAddress: String,

            @SerialName("lastLogin") val lastLogin: String,

            /**
             * List of shared accounts that user given the dApp access to.
             */
            @SerialName("sharedAccounts") val sharedAccounts: Shared<String>,

            @SerialName("sharedPersonaData") val sharedPersonaData: SharedPersonaData
        )

        @Serializable
        data class SharedPersonaData(
            @SerialName("name") val name: PersonaDataEntryID? = null,

            @SerialName("dateOfBirth") val dateOfBirth: PersonaDataEntryID? = null,

            @SerialName("companyName") val companyName: PersonaDataEntryID? = null,

            @SerialName("emailAddresses") val emailAddresses: Shared<PersonaDataEntryID>? = null,

            @SerialName("phoneNumbers") val phoneNumbers: Shared<PersonaDataEntryID>? = null,

            @SerialName("urls") val urls: Shared<PersonaDataEntryID>? = null,

            @SerialName("postalAddresses") val postalAddresses: Shared<PersonaDataEntryID>? = null,

            @SerialName("creditCards") val creditCards: Shared<PersonaDataEntryID>? = null
        ) {
            fun alreadyGrantedIds(): List<PersonaDataEntryID> {
                return listOfNotNull(
                    name,
                    dateOfBirth,
                    companyName
                ) + emailAddresses?.ids.orEmpty() + phoneNumbers?.ids.orEmpty() + urls?.ids.orEmpty() + postalAddresses?.ids.orEmpty() +
                    creditCards?.ids.orEmpty()
            }

            companion object {
                fun init(personaData: PersonaData, request: RequestedNumber): SharedPersonaData {
                    return SharedPersonaData(
                        name = personaData.name?.id,
                        dateOfBirth = personaData.dateOfBirth?.id,
                        companyName = personaData.companyName?.id,
                        emailAddresses = Shared(personaData.emailAddresses.map { it.id }, request),
                        phoneNumbers = Shared(personaData.phoneNumbers.map { it.id }, request),
                        urls = Shared(personaData.urls.map { it.id }, request),
                        postalAddresses = Shared(personaData.postalAddresses.map { it.id }, request),
                        creditCards = Shared(personaData.creditCards.map { it.id }, request)
                    )
                }
            }
        }
    }
}

@Serializable
data class RequestedNumber(
    @SerialName("quantifier") val quantifier: Quantifier,

    @SerialName("quantity") val quantity: Int
) {

    enum class Quantifier {
        @SerialName("exactly")
        Exactly,

        @SerialName("atLeast")
        AtLeast
    }

    companion object {
        fun exactly(value: Int): RequestedNumber {
            return RequestedNumber(quantifier = Quantifier.Exactly, value)
        }

        fun atLeast(value: Int): RequestedNumber {
            return RequestedNumber(quantifier = Quantifier.AtLeast, value)
        }
    }
}

@Serializable
data class Shared<T>(
    @SerialName("ids") val ids: List<T>,

    @SerialName("request") val request: RequestedNumber
)

fun Profile.addAccounts(
    accounts: List<Network.Account>,
    onNetwork: NetworkId
): Profile {
    val networkExist = networks.any { onNetwork.value == it.networkID }
    val newNetworks = if (networkExist) {
        this.networks.mapWhen(predicate = { network -> network.networkID == onNetwork.value }) { network ->
            val updatedAccounts = network.accounts + accounts
            Network(
                accounts = updatedAccounts.toIdentifiedArrayList(),
                authorizedDapps = network.authorizedDapps,
                networkID = network.networkID,
                personas = network.personas
            )
        }
    } else {
        this.networks + Network(
            accounts = accounts.toIdentifiedArrayList(),
            authorizedDapps = listOf(),
            networkID = onNetwork.value,
            personas = emptyIdentifiedArrayList()
        )
    }
    val updatedProfile = copy(
        networks = newNetworks,
    )
    return updatedProfile.withUpdatedContentHint()
}

fun Profile.addAuthSigningFactorInstanceForEntity(
    entity: Entity,
    authSigningFactorInstance: FactorInstance
): Profile {
    val updatedNetworks = networks.mapWhen(predicate = { network -> network.networkID == entity.networkID }) { network ->
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
                }.toIdentifiedArrayList()
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
                    }.toIdentifiedArrayList()
                )
            }
        }
    }
    return copy(
        networks = updatedNetworks
    )
}

fun Profile.updateThirdPartyDepositSettings(
    account: Network.Account,
    thirdPartyDeposits: Network.Account.OnLedgerSettings.ThirdPartyDeposits
): Profile {
    val updatedNetworks = networks.mapWhen(predicate = { network -> network.networkID == account.networkID }) { network ->
        val updatedAccounts = network.accounts.mapWhen(predicate = { it.address == account.address }) { account ->
            account.updateThirdPartyDeposits(thirdPartyDeposits = thirdPartyDeposits)
        }.toIdentifiedArrayList()
        network.copy(accounts = updatedAccounts)
    }
    return copy(
        networks = updatedNetworks
    )
}

fun Network.Account.updateThirdPartyDeposits(thirdPartyDeposits: Network.Account.OnLedgerSettings.ThirdPartyDeposits): Network.Account {
    return copy(onLedgerSettings = onLedgerSettings.copy(thirdPartyDeposits = thirdPartyDeposits))
}

fun Profile.addNetworkIfDoesNotExist(
    onNetwork: NetworkId
): Profile {
    val networkExist = this.networks.any { onNetwork.value == it.networkID }
    return if (!networkExist) {
        copy(
            networks = networks + Network(
                accounts = emptyIdentifiedArrayList(),
                authorizedDapps = listOf(),
                networkID = onNetwork.value,
                personas = emptyIdentifiedArrayList()
            )
        ).withUpdatedContentHint()
    } else {
        this
    }
}

fun Profile.nextPersonaIndex(
    derivationPathScheme: DerivationPathScheme,
    forNetworkId: NetworkId,
    factorSourceID: FactorSource.FactorSourceID? = null
): Int {
    val network = networks.firstOrNull { it.networkID == forNetworkId.value } ?: return 0
    val factorSource = factorSources.find { it.id == factorSourceID } ?: mainBabylonFactorSource() ?: return 0
    val personasControlledByFactorSource = network.personas.filter {
        it.factorSourceId == factorSource.id && it.derivationPathScheme == derivationPathScheme
    }
    return if (personasControlledByFactorSource.isEmpty()) {
        0
    } else {
        personasControlledByFactorSource.maxOf { it.derivationPathEntityIndex } + 1
    }
}

fun Profile.updatePersona(
    persona: Network.Persona
): Profile {
    val networkId = currentGateway.network.networkId()

    return copy(
        networks = networks.mapWhen(predicate = { it.networkID == networkId.value }, mutation = { network ->
            network.copy(
                personas = network.personas.mapWhen(predicate = {
                    it.address == persona.address
                }, mutation = { persona }).toIdentifiedArrayList()
            )
        })
    )
}

fun Profile.addPersona(
    persona: Network.Persona,
    onNetwork: NetworkId,
): Profile {
    val personaExists = this.networks.find {
        it.networkID == onNetwork.value
    }?.personas?.any { it.address == persona.address } ?: false

    if (personaExists) {
        return this
    }

    return copy(
        networks = this.networks.mapWhen(predicate = { it.networkID == onNetwork.value }, mutation = { network ->
            network.copy(personas = (network.personas + persona).toIdentifiedArrayList())
        })
    ).withUpdatedContentHint()
}

fun Network.AuthorizedDapp.AuthorizedPersonaSimple.ensurePersonaDataExist(
    existingFieldIds: List<PersonaDataEntryID>
): Network.AuthorizedDapp.AuthorizedPersonaSimple {
    return copy(
        sharedPersonaData = sharedPersonaData.copy(
            name = if (existingFieldIds.contains(sharedPersonaData.name)) sharedPersonaData.name else null,
            dateOfBirth = if (existingFieldIds.contains(sharedPersonaData.dateOfBirth)) sharedPersonaData.dateOfBirth else null,
            companyName = if (existingFieldIds.contains(sharedPersonaData.companyName)) sharedPersonaData.companyName else null,
            emailAddresses = sharedPersonaData.emailAddresses?.removeNonExisting(existingFieldIds)?.let {
                if (it.ids.isEmpty()) {
                    null
                } else {
                    it
                }
            },
            phoneNumbers = sharedPersonaData.phoneNumbers?.removeNonExisting(existingFieldIds)?.let {
                if (it.ids.isEmpty()) {
                    null
                } else {
                    it
                }
            },
            urls = sharedPersonaData.urls?.removeNonExisting(existingFieldIds)?.let {
                if (it.ids.isEmpty()) {
                    null
                } else {
                    it
                }
            },
            postalAddresses = sharedPersonaData.postalAddresses?.removeNonExisting(existingFieldIds)?.let {
                if (it.ids.isEmpty()) {
                    null
                } else {
                    it
                }
            },
            creditCards = sharedPersonaData.creditCards?.removeNonExisting(existingFieldIds)?.let {
                if (it.ids.isEmpty()) {
                    null
                } else {
                    it
                }
            }
        )
    )
}

fun <T> Shared<T>.removeNonExisting(existingIds: List<T>): Shared<T> {
    return copy(ids = ids.filter { existingIds.contains(it) })
}
