package rdx.works.profile.data.model.pernetwork

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.toolkit.models.crypto.PublicKey
import java.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.extensions.compressedPublicKey
import rdx.works.profile.data.extensions.deriveAccountAddress
import rdx.works.profile.data.extensions.deriveIdentityAddress
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.repository.AccountDerivationPath
import rdx.works.profile.data.repository.IdentityDerivationPath
import rdx.works.profile.derivation.model.NetworkId

@Serializable
data class OnNetwork(
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
    init {
        /**
         * We always require any per network instance to have at least one account
         */
        assert(accounts.isNotEmpty())
    }

    @Serializable
    data class Account(
        /**
         * The globally unique and identifiable Radix component address of this account. Can be used as
         * a stable ID. Cryptographically derived from a seeding public key which typically was created by
         * the `DeviceFactorSource` (and typically the same public key as an instance of the device factor
         * typically used in the primary role of this account).
         */
        @SerialName("address")
        val address: String,

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
         * The index of this account, in the list of accounts for a certain network. This means that
         * profile on network `mainnet` will have an account with `accountIndex = 0`, but so can an
         * account on network `testnet` too! However, their `address`es will differ!
         */
        @SerialName("index")
        val index: Int,

        /**
         * The ID of the network that has been used to generate the accounts, to which personas
         * have been added and dApps connected.
         */
        @SerialName("networkID")
        val networkID: Int,

        /**
         * Security of this account
         */
        @SerialName("securityState")
        val securityState: SecurityState
    ) {

        enum class AppearanceIdGradient {
            Gradient1,
            Gradient2,
            Gradient3,
            Gradient4,
            Gradient5,
            Gradient6,
            Gradient7,
            Gradient8,
            Gradient9,
            Gradient10,
            Gradient11,
            Gradient12,
        }

        companion object {
            /**
             * Creates initial account upon new profile creation
             */
            fun initial(
                mnemonic: MnemonicWords,
                factorSource: FactorSource,
                networkId: NetworkId,
                displayName: String
            ): Account {
                return createNewVirtualAccount(
                    displayName = displayName,
                    entityIndex = 0,
                    mnemonic = mnemonic,
                    factorSource = factorSource,
                    networkId = networkId
                )
            }

            fun createNewVirtualAccount(
                displayName: String,
                entityIndex: Int,
                mnemonic: MnemonicWords,
                factorSource: FactorSource,
                networkId: NetworkId
            ): Account {
                val derivationPath = AccountDerivationPath(
                    entityIndex = entityIndex,
                    networkId = networkId
                ).path()

                val compressedPublicKey = mnemonic.compressedPublicKey(
                    derivationPath = derivationPath
                ).removeLeadingZero()

                val address = deriveAccountAddress(
                    networkID = networkId,
                    publicKey = PublicKey.EddsaEd25519(compressedPublicKey)
                )

                val unsecuredSecurityState = SecurityState.unsecured(
                    compressedPublicKey = compressedPublicKey,
                    derivationPath = DerivationPath.accountDerivationPath(
                        derivationPath = derivationPath
                    ),
                    factorSourceId = factorSource.id
                )

                return Account(
                    address = address,
                    appearanceID = entityIndex % Account.AppearanceIdGradient.values().count(),
                    displayName = displayName,
                    index = entityIndex,
                    networkID = networkId.value,
                    securityState = unsecuredSecurityState
                )
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
        val address: String,

        /**
         * An optional displayName or label, used by presentation layer only.
         */
        @SerialName("displayName")
        val displayName: String,

        @SerialName("fields")
        val fields: List<Field>,

        /**
         * The index of this persona, in the list of personas for a certain network. This means that
         * profile on network `mainnet` will have a persona with `accountIndex = 0`, but so can person
         * on network `testnet` too! However, their `address`es will differ!
         */
        @SerialName("index")
        val index: Int,

        /**
         * The ID of the network that has been used to generate the accounts, to which personas
         * have been added and dApps connected.
         */
        @SerialName("networkID")
        val networkID: Int,

        /**
         * Security of this persona
         */
        @SerialName("securityState")
        val securityState: SecurityState
    ) {

        companion object {
            @Suppress("LongParameterList") // TODO refine this later on
            fun createNewPersona(
                displayName: String,
                fields: List<Field>,
                entityIndex: Int,
                mnemonicWords: MnemonicWords,
                factorSource: FactorSource,
                networkId: NetworkId
            ): Persona {
                val derivationPath = IdentityDerivationPath(
                    entityIndex = entityIndex,
                    networkId = networkId
                ).path()

                val compressedPublicKey = mnemonicWords.compressedPublicKey(
                    derivationPath = derivationPath
                ).removeLeadingZero()

                val address = deriveIdentityAddress(
                    networkID = networkId,
                    publicKey = PublicKey.EddsaEd25519(compressedPublicKey)
                )

                val unsecuredSecurityState = SecurityState.unsecured(
                    compressedPublicKey = compressedPublicKey,
                    derivationPath = DerivationPath.identityDerivationPath(
                        derivationPath = derivationPath
                    ),
                    factorSourceId = factorSource.id
                )

                return Persona(
                    address = address,
                    displayName = displayName,
                    fields = fields,
                    index = entityIndex,
                    networkID = networkId.value,
                    securityState = unsecuredSecurityState
                )
            }
        }

        @Serializable
        data class Field(
            @SerialName("id")
            val id: String,

            @SerialName("kind")
            val kind: Kind,

            @SerialName("value")
            val value: String
        ) {

            @Serializable
            enum class Kind {
                @SerialName("firstName")
                FirstName,

                @SerialName("lastName")
                LastName,

                @SerialName("email")
                Email,

                @SerialName("personalIdentificationNumber")
                PersonalIdentificationNumber,

                @SerialName("zipCode")
                ZipCode
            }

            companion object {
                fun init(
                    id: String = UUIDGenerator.uuid().toString(),
                    kind: Kind,
                    value: String
                ): Field {
                    return Field(
                        id = id,
                        kind = kind,
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
            @SerialName("fieldIDs")
            val fieldIDs: List<String>,

            @SerialName("lastUsedOn")
            val lastUsedOn: String,

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

    data class AuthorizedPersona(

        /**
         * The globally unique identifier of a Persona is its address, used to lookup persona
         */
        val identityAddress: String,

        /**
         * The display name of the Persona, as stored in `OnNetwork.Persona`
         */
        val displayName: String,

        /**
         * The persona data that the user has given the Dapp access to, being the tripple: `(id, kind, value)`
         */
        val fields: Set<Persona.Field>,

        /**
         * Information of accounts the user has given the Dapp access to,
         * being the tripple `(accountAddress, displayName, appearanceID)`
         */
        val walletUiAccounts: Set<WalletUiAccount>
    ) {
        data class WalletUiAccount(
            val accountAddress: String,
            val displayName: String,
            val appearanceID: Int
        )
    }

    fun authorizedPersonas(dApp: AuthorizedDapp): Set<AuthorizedPersona> {
        require(dApp.networkID != networkID) {
            throw java.lang.IllegalArgumentException("NetworkId does not match")
        }

        return dApp.referencesToAuthorizedPersonas.map { authorizedPersonaSimple ->

            // Fetch existing persona from the to authorized personas reference
            val persona = personas.first { it.address == authorizedPersonaSimple.identityAddress }

            // Find correct persona fields from reference to persona field id
            val fieldIds = authorizedPersonaSimple.fieldIDs.map { simpleFieldIds ->
                persona.fields.first { it.id == simpleFieldIds }
            }.toSet()

            // Find referenced accounts from its persona reference
            val walletUiAccounts = authorizedPersonaSimple.sharedAccounts.accountsReferencedByAddress.map { accRefs ->
                val referencedAccount = accounts.first { it.address == accRefs }
                AuthorizedPersona.WalletUiAccount(
                    accountAddress = referencedAccount.address,
                    displayName = referencedAccount.displayName,
                    appearanceID = referencedAccount.appearanceID
                )
            }.toSet()

            AuthorizedPersona(
                identityAddress = persona.address,
                displayName = persona.displayName,
                fields = fieldIds,
                walletUiAccounts = walletUiAccounts
            )
        }.toSet()
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
