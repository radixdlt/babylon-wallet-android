package rdx.works.core.sargon

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Accounts
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.AuthorizedDapps
import com.radixdlt.sargon.ContentHint
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.EntityFlags
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceFlag
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSources
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.Header
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.Personas
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileNetwork
import com.radixdlt.sargon.ProfileNetworks
import com.radixdlt.sargon.ProfileSnapshotVersion
import com.radixdlt.sargon.ReferencesToAuthorizedPersonas
import com.radixdlt.sargon.ThirdPartyDeposits
import com.radixdlt.sargon.TransactionPreferences
import com.radixdlt.sargon.extensions.HDPathValue
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.append
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.changeCurrent
import com.radixdlt.sargon.extensions.contains
import com.radixdlt.sargon.extensions.getBy
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.remove
import com.radixdlt.sargon.extensions.removeByAddress
import com.radixdlt.sargon.extensions.removeById
import com.radixdlt.sargon.extensions.size
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.profileToDebugString
import rdx.works.core.TimestampGenerator
import rdx.works.core.annotations.DebugOnly
import rdx.works.core.mapWhen

val Header.isCompatible: Boolean
    get() = snapshotVersion.value >= ProfileSnapshotVersion.V100.value

val Profile.currentGateway: Gateway
    get() = appPreferences.gateways.current

val Profile.currentNetwork: ProfileNetwork?
    get() {
        val currentGateway = currentGateway
        return networks.getBy(currentGateway.network.id)
    }

val Profile.isCurrentNetworkMainnet: Boolean
    get() = currentNetwork?.id == NetworkId.MAINNET

val Profile.activeEntitiesOnCurrentNetwork: List<ProfileEntity>
    get() = activeAccountsOnCurrentNetwork.map { it.asProfileEntity() } +
            activePersonasOnCurrentNetwork.map { it.asProfileEntity() }

val Profile.activeAccountsOnCurrentNetwork: List<Account>
    get() = currentNetwork?.accounts()?.notHiddenAccounts().orEmpty()

fun Profile.activeAccountOnCurrentNetwork(withAddress: AccountAddress): Account? =
    activeAccountsOnCurrentNetwork.firstOrNull { account ->
        account.address == withAddress
    }

val Profile.hiddenAccountsOnCurrentNetwork: List<Account>
    get() = currentNetwork?.accounts()?.hiddenAccounts().orEmpty()

val Profile.activePersonasOnCurrentNetwork: List<Persona>
    get() = currentNetwork?.personas()?.notHiddenPersonas().orEmpty()

val Profile.hiddenPersonasOnCurrentNetwork: List<Persona>
    get() = currentNetwork?.personas()?.hiddenPersonas().orEmpty()

fun Profile.activePersonaOnCurrentNetwork(withAddress: IdentityAddress): Persona? =
    activePersonasOnCurrentNetwork.firstOrNull { persona ->
        persona.address == withAddress
    }

val Profile.deviceFactorSources: List<FactorSource.Device>
    get() = factorSources().filterIsInstance<FactorSource.Device>()

val Profile.ledgerFactorSources: List<FactorSource.Ledger>
    get() = factorSources().filterIsInstance<FactorSource.Ledger>()

fun Profile.factorSourceById(id: FactorSourceId) = factorSources.getById(id = id)

val Profile.deviceFactorSourcesWithAccounts: Map<FactorSource.Device, List<Account>>
    get() {
        val allAccountsOnNetwork = activeAccountsOnCurrentNetwork
        return deviceFactorSources.associateWith { deviceFactorSource ->
            allAccountsOnNetwork.filter { it.address.string == deviceFactorSource.value.id.body.hex } // TODO integration check
        }
    }

val Profile.mainBabylonFactorSource: FactorSource.Device?
    get() {
        val babylonFactorSources = deviceFactorSources.filter { it.isBabylonDeviceFactorSource }
        return if (babylonFactorSources.size == 1) {
            babylonFactorSources.first()
        } else {
            babylonFactorSources.firstOrNull { it.value.common.flags.contains(FactorSourceFlag.MAIN) }
        }
    }

val Profile.babylonFactorSourcesWithAccounts: Map<FactorSource.Device, List<Account>>
    get() = deviceFactorSourcesWithAccounts.filter { entry ->
        entry.key.isBabylonDeviceFactorSource
    }.mapValues { entry ->
        entry.value.filter { account -> account.usesEd25519 }
    }

val Profile.olympiaFactorSourcesWithAccounts: Map<FactorSource.Device, List<Account>>
    get() = deviceFactorSourcesWithAccounts.filter { entry ->
        entry.key.value.supportsOlympia
    }.mapValues { entry ->
        entry.value.filter { account -> account.usesSECP256k1 }
    }


/**
 * Used only by debug features, like inspect profile, where it is the only place
 * in UI where the profile's json needs to be previewed.
 */
@DebugOnly
fun Profile.prettyPrinted(): String = profileToDebugString(profile = this)

fun Profile.addAccounts(
    accounts: List<Account>,
    onNetwork: NetworkId
): Profile {
    val networkExist = networks().any { onNetwork == it.id }
    val newNetworks = if (networkExist) {
        networks().mapWhen(predicate = { network -> network.id == onNetwork }) { network ->
            ProfileNetwork(
                id = network.id,
                accounts = Accounts.init(network.accounts() + accounts),
                authorizedDapps = network.authorizedDapps,
                personas = network.personas
            )
        }
    } else {
        networks() + ProfileNetwork(
            id = onNetwork,
            accounts = Accounts.init(accounts),
            authorizedDapps = AuthorizedDapps.init(),
            personas = Personas.init()
        )
    }
    val updatedProfile = copy(networks = ProfileNetworks.init(newNetworks))
    return updatedProfile.withUpdatedContentHint()
}

fun Profile.addAuthSigningFactorInstanceForEntity(
    entity: ProfileEntity,
    authSigningFactorInstance: HierarchicalDeterministicFactorInstance
): Profile {
    val updatedNetworks = networks().mapWhen(predicate = { network -> network.id == entity.networkId }) { network ->
        when (entity) {
            is ProfileEntity.AccountEntity -> network.copy(
                accounts = Accounts.init(
                    network.accounts().mapWhen(predicate = { it.address == entity.accountAddress }) { account ->
                        val updatedSecurityState = when (val state = account.securityState) {
                            is EntitySecurityState.Unsecured -> state.copy(
                                value = state.value.copy(authenticationSigning = authSigningFactorInstance)
                            )
                        }
                        account.copy(securityState = updatedSecurityState)
                    }
                )
            )

            is ProfileEntity.PersonaEntity -> network.copy(
                personas = Personas.init(
                    network.personas().mapWhen(predicate = { it.address == entity.identityAddress }) { persona ->
                        val updatedSecurityState = when (val state = persona.securityState) {
                            is EntitySecurityState.Unsecured -> state.copy(
                                value = state.value.copy(authenticationSigning = authSigningFactorInstance)
                            )
                        }
                        persona.copy(securityState = updatedSecurityState)
                    }
                )
            )
        }
    }
    return copy(networks = ProfileNetworks.init(updatedNetworks))
}

fun Profile.updateThirdPartyDepositSettings(
    account: Account,
    thirdPartyDeposits: ThirdPartyDeposits
): Profile {
    val updatedNetworks = networks().mapWhen(predicate = { network -> network.id == account.networkId }) { network ->
        val updatedAccounts = network.accounts().mapWhen(predicate = { it.address == account.address }) { account ->
            account.copy(onLedgerSettings = account.onLedgerSettings.copy(thirdPartyDeposits = thirdPartyDeposits))
        }
        network.copy(accounts = Accounts.init(updatedAccounts))
    }
    return copy(networks = ProfileNetworks.init(updatedNetworks))
}

fun Profile.addNetworkIfDoesNotExist(
    onNetwork: NetworkId
): Profile = if (networks().none { onNetwork == it.id }) {
    copy(
        networks = ProfileNetworks.init(
            networks() + ProfileNetwork(
                id = onNetwork,
                accounts = Accounts.init(),
                authorizedDapps = AuthorizedDapps.init(),
                personas = Personas.init()
            )
        )
    ).withUpdatedContentHint()
} else {
    this
}

fun Profile.nextAccountIndex(
    forNetworkId: NetworkId,
    factorSourceId: FactorSourceId,
    derivationPathScheme: DerivationPathScheme,
): HDPathValue {
    val forNetwork = networks.getBy(forNetworkId) ?: return 0u
    val accountsControlledByFactorSource = forNetwork.accounts().filter {
        it.factorSourceId == factorSourceId && it.derivationPathScheme == derivationPathScheme
    }
    return if (accountsControlledByFactorSource.isEmpty()) {
        0u
    } else {
        accountsControlledByFactorSource.maxOf { it.derivationPathEntityIndex } + 1u
    }
}

fun Profile.nextPersonaIndex(
    forNetworkId: NetworkId,
    derivationPathScheme: DerivationPathScheme,
    factorSourceID: FactorSourceId? = null
): HDPathValue {
    val network = networks().firstOrNull { it.id == forNetworkId } ?: return 0u

    val factorSource = factorSources().find {
        it.id == factorSourceID
    } ?: mainBabylonFactorSource ?: return 0u

    val personasControlledByFactorSource = network.personas().filter {
        it.factorSourceId == factorSource.id && it.derivationPathScheme == derivationPathScheme
    }
    return if (personasControlledByFactorSource.isEmpty()) {
        0u
    } else {
        personasControlledByFactorSource.maxOf { it.derivationPathEntityIndex } + 1u
    }
}

fun Profile.updatePersona(
    persona: Persona
): Profile {
    val networkId = currentNetwork?.id ?: return this

    return copy(
        networks = ProfileNetworks.init(
            networks().mapWhen(predicate = { it.id == networkId }, mutation = { network ->
                network.copy(
                    personas = Personas.init(
                        network.personas().mapWhen(predicate = { it.address == persona.address }, mutation = { persona })
                    )
                )
            })
        )
    )
}

fun Profile.addPersona(
    persona: Persona,
    onNetwork: NetworkId,
): Profile {
    val personaExists = this.networks().find {
        it.id == onNetwork
    }?.personas?.invoke()?.any { it.address == persona.address } ?: false

    if (personaExists) {
        return this
    }

    return copy(
        networks = ProfileNetworks.init(
            networks().mapWhen(predicate = { it.id == onNetwork }, mutation = { network ->
                network.copy(personas = Personas.init(network.personas() + persona))
            })
        )
    ).withUpdatedContentHint()
}

fun Profile.hidePersona(identityAddress: IdentityAddress): Profile {
    val networkId = currentNetwork?.id ?: return this
    val updatedNetworks = networks().mapWhen(predicate = { it.id == networkId }, mutation = { network ->
        val updatedAuthorizedDapps = network.authorizedDapps().mapWhen(predicate = { authorizedDapp ->
            authorizedDapp.referencesToAuthorizedPersonas.getBy(identityAddress) != null
        }, mutation = { authorizedDapp ->
            authorizedDapp.copy(
                referencesToAuthorizedPersonas = authorizedDapp.referencesToAuthorizedPersonas.removeByAddress(
                    identityAddress
                )
            )
        })
        network.copy(
            personas = Personas.init(
                network.personas().mapWhen(
                    predicate = { it.address == identityAddress },
                    mutation = { persona ->
                        persona.copy(flags = EntityFlags.init(persona.flags() + EntityFlag.DELETED_BY_USER))
                    }
                )
            ),
            authorizedDapps = AuthorizedDapps.init(
                updatedAuthorizedDapps.filter { it.referencesToAuthorizedPersonas().isNotEmpty() }
            )
        )
    })
    return copy(networks = ProfileNetworks.init(updatedNetworks)).withUpdatedContentHint()
}

fun Profile.hideAccount(accountAddress: AccountAddress): Profile {
    val networkId = currentNetwork?.id ?: return this
    val updatedNetworks = networks().mapWhen(predicate = { it.id == networkId }, mutation = { network ->
        val updatedAuthorizedDapps = network.authorizedDapps().mapWhen(predicate = { authorizedDapp ->
            authorizedDapp.referencesToAuthorizedPersonas().any { reference ->
                reference.sharedAccounts?.ids.orEmpty().any { it == accountAddress }
            }
        }, mutation = { authorizedDapp ->
            val updatedReferences = authorizedDapp.referencesToAuthorizedPersonas().mapWhen(
                predicate = { reference ->
                    reference.sharedAccounts?.ids.orEmpty().any { it == accountAddress }
                },
                mutation = { reference ->
                    reference.copy(
                        sharedAccounts = reference.sharedAccounts?.copy(
                            ids = reference.sharedAccounts?.ids.orEmpty().filter {
                                it != accountAddress
                            }
                        )
                    )
                }
            )
            authorizedDapp.copy(referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas.init(updatedReferences))
        })
        val updatedAccounts = network.accounts().mapWhen(
            predicate = { it.address == accountAddress },
            mutation = { account ->
                account.copy(flags = EntityFlags.init(account.flags() + EntityFlag.DELETED_BY_USER))
            }
        )
        network.copy(
            accounts = Accounts.init(updatedAccounts),
            authorizedDapps = AuthorizedDapps.init(
                updatedAuthorizedDapps.filter { it.referencesToAuthorizedPersonas().isNotEmpty() }
            )
        )
    })
    return copy(networks = ProfileNetworks.init(updatedNetworks)).withUpdatedContentHint()
}

fun Profile.unHideAllEntities(): Profile {
    val networkId = currentNetwork?.id ?: return this
    val updatedNetworks = networks().mapWhen(predicate = { it.id == networkId }, mutation = { network ->
        network.copy(
            personas = Personas.init(
                network.personas().map { persona ->
                    persona.copy(flags = EntityFlags.init(persona.flags() - EntityFlag.DELETED_BY_USER))
                }
            ),
            accounts = Accounts.init(
                network.accounts().map { persona ->
                    persona.copy(flags = EntityFlags.init(persona.flags() - EntityFlag.DELETED_BY_USER))
                }
            )
        )
    })
    return copy(networks = ProfileNetworks.init(updatedNetworks)).withUpdatedContentHint()
}

fun Profile.addP2PLink(
    p2pLink: P2pLink
): Profile {
    val newAppPreferences = appPreferences.copy(p2pLinks = appPreferences.p2pLinks.append(p2pLink))

    return copy(appPreferences = newAppPreferences)
}

fun Profile.deleteP2PLink(
    p2pLink: P2pLink
): Profile {
    val newAppPreferences = appPreferences.copy(p2pLinks = appPreferences.p2pLinks.removeById(p2pLink.id))

    return copy(appPreferences = newAppPreferences)
}

fun Profile.changeGatewayToNetworkId(
    networkId: NetworkId
): Profile {
    if (currentGateway.network.id == networkId) return this

    val gatewayToChange = appPreferences.gateways.other().find { it.network.id == networkId } ?: return this

    return changeGateway(gatewayToChange)
}

fun Profile.changeGateway(
    gateway: Gateway
): Profile {
    if (gateway !in appPreferences.gateways.other) return this

    return copy(appPreferences = appPreferences.copy(gateways = appPreferences.gateways.changeCurrent(gateway)))
}

fun Profile.addGateway(
    gateway: Gateway
): Profile {
    val gateways = appPreferences.gateways.copy(other = appPreferences.gateways.other.append(gateway)) // TODO integration
    val appPreferences = appPreferences.copy(gateways = gateways)
    return copy(appPreferences = appPreferences)
}

fun Profile.deleteGateway(
    gateway: Gateway
): Profile {
    val gateways = appPreferences.gateways.copy(other = appPreferences.gateways.other.remove(gateway)) // TODO integration
    val appPreferences = appPreferences.copy(gateways = gateways)
    return copy(appPreferences = appPreferences)
}

fun Profile.changeDefaultDepositGuarantee(
    defaultDepositGuarantee: Decimal192
): Profile = copy(
    appPreferences = appPreferences.copy(
        transaction = TransactionPreferences(defaultDepositGuarantee = defaultDepositGuarantee)
    )
)

fun Profile.changeBalanceVisibility(
    isVisible: Boolean
): Profile = copy(
    appPreferences = appPreferences.copy(
        display = appPreferences.display.copy(isCurrencyAmountVisible = isVisible)
    )
)

fun Profile.updateDeveloperMode(isEnabled: Boolean): Profile = copy(
    appPreferences = appPreferences.copy(
        security = appPreferences.security.copy(
            isDeveloperModeEnabled = isEnabled
        )
    )
)

fun Profile.updateCloudSyncEnabled(isEnabled: Boolean) = copy(
    appPreferences = appPreferences.copy(
        security = appPreferences.security.copy(
            isCloudProfileSyncEnabled = isEnabled
        )
    )
)

fun Profile.renameAccountDisplayName(
    accountToRename: Account,
    newDisplayName: DisplayName
): Profile {
    val networkId = currentNetwork?.id ?: return this
    val renamedAccount = accountToRename.copy(displayName = newDisplayName)

    return copy(
        networks = ProfileNetworks.init(
            networks().mapWhen(
                predicate = { it.id == networkId },
                mutation = { network ->
                    network.copy(
                        accounts = Accounts.init(
                            network.accounts().mapWhen(
                                predicate = { it == accountToRename },
                                mutation = { renamedAccount }
                            )
                        )
                    )
                }
            )
        )
    )
}

fun Profile.updateLastUsed(id: FactorSourceId): Profile {
    return copy(
        factorSources = FactorSources.init(
            factorSources().mapWhen(predicate = { it.id == id }) { factorSource ->
                when (factorSource) {
                    is FactorSource.Device -> factorSource.value.copy(
                        common = factorSource.value.common.copy(lastUsedOn = TimestampGenerator())
                    ).asGeneral()

                    is FactorSource.Ledger -> factorSource.value.copy(
                        common = factorSource.value.common.copy(lastUsedOn = TimestampGenerator())
                    ).asGeneral()
                }
            }
        )
    )
}

fun Profile.addMainBabylonDeviceFactorSource(
    mainBabylonFactorSource: FactorSource.Device
): Profile {
    val existingBabylonDeviceFactorSources = factorSources().map { factorSource ->
        if (factorSource is FactorSource.Device && factorSource.value.supportsBabylon) {
            factorSource.copy(
                value = factorSource.value.copy(common = factorSource.value.common.copy(
                    flags = factorSource.value.common.flags.filterNot { flag -> flag == FactorSourceFlag.MAIN }
                ))
            )
        } else {
            factorSource
        }
    }

    return copy(factorSources = FactorSources.init(listOf(mainBabylonFactorSource) + existingBabylonDeviceFactorSources))
}

fun Profile.nextAppearanceId(forNetworkId: NetworkId): AppearanceId {
    val forNetwork = networks.getBy(forNetworkId) ?: return AppearanceId(0u)
    return AppearanceId.from(offset = forNetwork.accounts.size.toUInt())
}

fun Profile.getAuthorizedDApp(dAppDefinitionAddress: AccountAddress): AuthorizedDapp? =
    currentNetwork?.authorizedDapps?.getBy(dAppDefinitionAddress)

fun Profile.getAuthorizedDApps(): List<AuthorizedDapp> = currentNetwork?.authorizedDapps().orEmpty()

fun Profile.createOrUpdateAuthorizedDApp(
    unverifiedAuthorizedDApp: AuthorizedDapp
): Profile {
    val updatedNetworks = networks().mapWhen(
        predicate = { it.id == unverifiedAuthorizedDApp.networkId },
        mutation = { network ->
            val existingDApp = network.authorizedDapps.getBy(unverifiedAuthorizedDApp.dappDefinitionAddress)
            if (existingDApp == null) {
                network.copy(authorizedDapps = network.authorizedDapps.append(unverifiedAuthorizedDApp))
            } else {
                val authorizedDApp = network.validateAuthorizedPersonas(unverifiedAuthorizedDApp)
                // Replace old authorizedDApp
                val updatedDApps = network.authorizedDapps().mapWhen(
                    predicate = { it.dappDefinitionAddress == existingDApp.dappDefinitionAddress },
                    mutation = { authorizedDApp }
                )
                network.copy(authorizedDapps = AuthorizedDapps.init(updatedDApps))
            }
        }
    )

    return copy(networks = ProfileNetworks.init(updatedNetworks))
}

fun Profile.deleteAuthorizedDApp(
    dApp: AuthorizedDapp
): Profile {
    val updatedNetwork = networks().mapWhen(
        predicate = { it.id == dApp.networkId },
        mutation = { network ->
            network.copy(authorizedDapps = network.authorizedDapps.removeByAddress(dApp.dappDefinitionAddress))
        }
    )

    return copy(networks = ProfileNetworks.init(updatedNetwork))
}

private fun Profile.withUpdatedContentHint() = copy(
    header = header.copy(
        contentHint = ContentHint(
            numberOfNetworks = networks.size.toUShort(),
            numberOfAccountsOnAllNetworksInTotal = networks().sumOf { network -> network.accounts().notHiddenAccounts().size }.toUShort(),
            numberOfPersonasOnAllNetworksInTotal = networks().sumOf { network -> network.personas().notHiddenPersonas().size }.toUShort()
        )
    )
)