@file:Suppress("TooManyFunctions")

package rdx.works.core.sargon

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.AuthorizedDappPreferenceDeposits
import com.radixdlt.sargon.ContentHint
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.DeviceInfo
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceFlag
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.Header
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileNetwork
import com.radixdlt.sargon.ProfileSnapshotVersion
import com.radixdlt.sargon.ThirdPartyDeposits
import com.radixdlt.sargon.TransactionPreferences
import com.radixdlt.sargon.extensions.Accounts
import com.radixdlt.sargon.extensions.AuthorizedDapps
import com.radixdlt.sargon.extensions.EntityFlags
import com.radixdlt.sargon.extensions.FactorSources
import com.radixdlt.sargon.extensions.Personas
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.ProfileNetworks
import com.radixdlt.sargon.extensions.ReferencesToAuthorizedPersonas
import com.radixdlt.sargon.extensions.ResourceAppPreferences
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.changeCurrent
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.isLegacy
import com.radixdlt.sargon.extensions.unsecuredControllingFactorInstance
import com.radixdlt.sargon.profileToDebugString
import rdx.works.core.TimestampGenerator
import rdx.works.core.annotations.DebugOnly
import rdx.works.core.mapWhen

val Header.isCompatible: Boolean
    get() = snapshotVersion.value >= ProfileSnapshotVersion.V100.value

/**
 * in order to do a cloud backup we must ensure that:
 * - profile has at least one network
 * - isCloudProfileSyncEnabled is true
 *
 * The first indicates if the profile is in an initialization process, e.g. onboarding flow.
 *
 */
val Profile.canBackupToCloud: Boolean
    get() = hasNetworks && appPreferences.security.isCloudProfileSyncEnabled

val Profile.currentGateway: Gateway
    get() = appPreferences.gateways.current

val Profile.hasNetworks: Boolean
    get() = networks.isNotEmpty()

val Profile.currentNetwork: ProfileNetwork?
    get() {
        val currentGateway = currentGateway
        return networks.asIdentifiable().getBy(currentGateway.network.id)
    }

val Profile.isCurrentNetworkMainnet: Boolean
    get() = currentNetwork?.id == NetworkId.MAINNET

val Profile.allAccountsOnCurrentNetwork: List<Account>
    get() = currentNetwork?.accounts.orEmpty()

val Profile.allPersonasOnCurrentNetwork: List<Persona>
    get() = currentNetwork?.personas.orEmpty()

val Profile.allEntitiesOnCurrentNetwork: List<ProfileEntity>
    get() = allAccountsOnCurrentNetwork.map { it.asProfileEntity() } +
        allPersonasOnCurrentNetwork.map { it.asProfileEntity() }

val Profile.activeAccountsOnCurrentNetwork: List<Account>
    get() = currentNetwork?.accounts?.active().orEmpty()

fun Profile.activeAccountOnCurrentNetwork(withAddress: AccountAddress): Account? =
    activeAccountsOnCurrentNetwork.firstOrNull { account ->
        account.address == withAddress
    }

val Profile.hiddenAccountsOnCurrentNetwork: List<Account>
    get() = currentNetwork?.accounts?.filter { it.isHidden }.orEmpty()

val Profile.activePersonasOnCurrentNetwork: List<Persona>
    get() = currentNetwork?.personas?.active().orEmpty()

val Profile.hiddenPersonasOnCurrentNetwork: List<Persona>
    get() = currentNetwork?.personas?.filter { it.isHidden }.orEmpty()

fun Profile.activePersonaOnCurrentNetwork(withAddress: IdentityAddress): Persona? =
    activePersonasOnCurrentNetwork.firstOrNull { persona ->
        persona.address == withAddress
    }

val Profile.deviceFactorSources: List<FactorSource.Device>
    get() = factorSources.filterIsInstance<FactorSource.Device>()

val Profile.ledgerFactorSources: List<FactorSource.Ledger>
    get() = factorSources.filterIsInstance<FactorSource.Ledger>()

fun Profile.factorSourceById(id: FactorSourceId) = factorSources.asIdentifiable().getBy(id)

val Profile.isAdvancedLockEnabled: Boolean
    get() = appPreferences.security.isAdvancedLockEnabled

private val Profile.deviceFactorSourcesWithAccounts: Map<FactorSource.Device, List<Account>>
    get() {
        val activeAccountsOnCurrentNetwork = activeAccountsOnCurrentNetwork
        return deviceFactorSources.associateWith { deviceFactorSource ->
            activeAccountsOnCurrentNetwork.filter {
                it.unsecuredControllingFactorInstance?.factorSourceId?.asGeneral() == deviceFactorSource.id
            }
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
        entry.value.filter { account -> !account.isLegacy }
    }

val Profile.olympiaFactorSourcesWithAccounts: Map<FactorSource.Device, List<Account>>
    get() = deviceFactorSourcesWithAccounts.filter { entry ->
        entry.key.supportsOlympia
    }.mapValues { entry ->
        entry.value.filter { account -> account.isLegacy }
    }

/**
 * Used only by debug features, like inspect profile, where it is the only place
 * in UI where the profile's json needs to be previewed.
 */
@DebugOnly
fun Profile.prettyPrinted(): String = profileToDebugString(profile = this)

fun Profile.claim(
    deviceInfo: DeviceInfo
): Profile = copy(
    header = header.copy(lastUsedOnDevice = deviceInfo)
)

fun Profile.addAccounts(
    accounts: List<Account>,
    onNetwork: NetworkId
): Profile {
    val networkExist = networks.any { onNetwork == it.id }
    val newNetworks = if (networkExist) {
        networks.mapWhen(predicate = { network -> network.id == onNetwork }) { network ->
            ProfileNetwork(
                id = network.id,
                accounts = Accounts(network.accounts + accounts).asList(),
                authorizedDapps = network.authorizedDapps,
                personas = network.personas,
                resourcePreferences = network.resourcePreferences
            )
        }
    } else {
        networks + ProfileNetwork(
            id = onNetwork,
            accounts = Accounts(accounts).asList(),
            authorizedDapps = AuthorizedDapps().asList(),
            personas = Personas().asList(),
            resourcePreferences = ResourceAppPreferences().asList()
        )
    }
    val updatedProfile = copy(networks = ProfileNetworks(newNetworks).asList())
    return updatedProfile.withUpdatedContentHint()
}

fun Profile.updateThirdPartyDepositSettings(
    account: Account,
    thirdPartyDeposits: ThirdPartyDeposits
): Profile {
    val updatedNetworks = networks.mapWhen(predicate = { network -> network.id == account.networkId }) { network ->
        val updatedAccounts = network.accounts.mapWhen(predicate = { it.address == account.address }) { account ->
            account.copy(onLedgerSettings = account.onLedgerSettings.copy(thirdPartyDeposits = thirdPartyDeposits))
        }
        network.copy(accounts = Accounts(updatedAccounts).asList())
    }
    return copy(networks = ProfileNetworks(updatedNetworks).asList())
}

fun Profile.updatePersona(
    persona: Persona
): Profile {
    val networkId = currentNetwork?.id ?: return this

    return copy(
        networks = ProfileNetworks(
            networks.mapWhen(predicate = { it.id == networkId }, mutation = { network ->
                network.copy(
                    personas = Personas(
                        network.personas.mapWhen(predicate = { it.address == persona.address }, mutation = { persona })
                    ).asList()
                )
            })
        ).asList()
    )
}

fun Profile.changePersonaVisibility(identityAddress: IdentityAddress, isHidden: Boolean): Profile {
    val networkId = currentNetwork?.id ?: return this
    val updatedNetworks = networks.mapWhen(predicate = { it.id == networkId }, mutation = { network ->
        val updatedAuthorizedDapps = network.authorizedDapps.mapWhen(predicate = { authorizedDapp ->
            authorizedDapp.referencesToAuthorizedPersonas.asIdentifiable().getBy(identityAddress) != null
        }, mutation = { authorizedDapp ->
            authorizedDapp.copy(
                referencesToAuthorizedPersonas = authorizedDapp.referencesToAuthorizedPersonas.asIdentifiable().removeBy(
                    identityAddress
                ).asList()
            )
        })
        network.copy(
            personas = Personas(
                network.personas.mapWhen(
                    predicate = { it.address == identityAddress },
                    mutation = { persona ->
                        val updatedFlags = if (isHidden) {
                            persona.flags + EntityFlag.HIDDEN_BY_USER
                        } else {
                            persona.flags - EntityFlag.HIDDEN_BY_USER
                        }
                        persona.copy(flags = EntityFlags(updatedFlags).asList())
                    }
                )
            ).asList(),
            authorizedDapps = AuthorizedDapps(
                updatedAuthorizedDapps.filter { it.referencesToAuthorizedPersonas.isNotEmpty() }
            ).asList()
        )
    })
    return copy(networks = ProfileNetworks(updatedNetworks).asList()).withUpdatedContentHint()
}

fun Profile.changeAccountVisibility(accountAddress: AccountAddress, hide: Boolean): Profile {
    val networkId = currentNetwork?.id ?: return this
    val updatedNetworks = networks.mapWhen(predicate = { it.id == networkId }, mutation = { network ->
        val updatedAuthorizedDapps = network.authorizedDapps.mapWhen(predicate = { authorizedDapp ->
            authorizedDapp.referencesToAuthorizedPersonas.any { reference ->
                reference.sharedAccounts?.ids.orEmpty().any { it == accountAddress }
            }
        }, mutation = { authorizedDapp ->
            val updatedReferences = authorizedDapp.referencesToAuthorizedPersonas.mapWhen(
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
            authorizedDapp.copy(referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas(updatedReferences).asList())
        })
        val updatedAccounts = network.accounts.mapWhen(
            predicate = { it.address == accountAddress },
            mutation = { account ->
                val updatedFlags = if (hide) {
                    account.flags + EntityFlag.HIDDEN_BY_USER
                } else {
                    account.flags - EntityFlag.HIDDEN_BY_USER
                }
                account.copy(flags = EntityFlags(updatedFlags).asList())
            }
        )
        network.copy(
            accounts = Accounts(updatedAccounts).asList(),
            authorizedDapps = AuthorizedDapps(
                updatedAuthorizedDapps.filter { it.referencesToAuthorizedPersonas.isNotEmpty() }
            ).asList()
        )
    })
    return copy(networks = ProfileNetworks(updatedNetworks).asList()).withUpdatedContentHint()
}

fun Profile.unHideAllEntities(): Profile {
    val networkId = currentNetwork?.id ?: return this
    val updatedNetworks = networks.mapWhen(predicate = { it.id == networkId }, mutation = { network ->
        network.copy(
            personas = Personas(
                network.personas.map { persona ->
                    persona.copy(flags = persona.flags.asIdentifiable().remove(EntityFlag.HIDDEN_BY_USER).asList())
                }
            ).asList(),
            accounts = Accounts(
                network.accounts.map { persona ->
                    persona.copy(flags = persona.flags.asIdentifiable().remove(EntityFlag.HIDDEN_BY_USER).asList())
                }
            ).asList()
        )
    })
    return copy(networks = ProfileNetworks(updatedNetworks).asList()).withUpdatedContentHint()
}

fun Profile.changeGatewayToNetworkId(
    networkId: NetworkId
): Profile {
    if (currentGateway.network.id == networkId) return this

    val gatewayToChange = appPreferences.gateways.other.find { it.network.id == networkId } ?: return this

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
    val gateways = appPreferences.gateways.copy(other = appPreferences.gateways.other.asIdentifiable().append(gateway).asList())
    val appPreferences = appPreferences.copy(gateways = gateways)
    return copy(appPreferences = appPreferences)
}

fun Profile.deleteGateway(
    gateway: Gateway
): Profile {
    val gateways = appPreferences.gateways.copy(other = appPreferences.gateways.other.asIdentifiable().remove(gateway).asList())
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

fun Profile.updateAdvancedLockEnabled(isEnabled: Boolean) = copy(
    appPreferences = appPreferences.copy(
        security = appPreferences.security.copy(
            isAdvancedLockEnabled = isEnabled
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
        networks = ProfileNetworks(
            networks.mapWhen(
                predicate = { it.id == networkId },
                mutation = { network ->
                    network.copy(
                        accounts = Accounts(
                            network.accounts.mapWhen(
                                predicate = { it == accountToRename },
                                mutation = { renamedAccount }
                            )
                        ).asList()
                    )
                }
            )
        ).asList()
    )
}

fun Profile.updateLastUsed(id: FactorSourceId): Profile {
    return copy(
        factorSources = FactorSources(
            factorSources.mapWhen(predicate = { it.id == id }) { factorSource ->
                when (factorSource) {
                    is FactorSource.Device -> factorSource.value.copy(
                        common = factorSource.value.common.copy(lastUsedOn = TimestampGenerator())
                    ).asGeneral()

                    is FactorSource.Ledger -> factorSource.value.copy(
                        common = factorSource.value.common.copy(lastUsedOn = TimestampGenerator())
                    ).asGeneral()

                    is FactorSource.ArculusCard -> factorSource.value.copy(
                        common = factorSource.value.common.copy(lastUsedOn = TimestampGenerator())
                    ).asGeneral()

                    is FactorSource.OffDeviceMnemonic -> factorSource.value.copy(
                        common = factorSource.value.common.copy(lastUsedOn = TimestampGenerator())
                    ).asGeneral()

                    is FactorSource.Password -> factorSource.value.copy(
                        common = factorSource.value.common.copy(lastUsedOn = TimestampGenerator())
                    ).asGeneral()
                }
            }
        ).asList()
    )
}

fun Profile.addMainBabylonDeviceFactorSource(
    mainBabylonFactorSource: FactorSource.Device
): Profile {
    val existingBabylonDeviceFactorSources = factorSources.map { factorSource ->
        if (factorSource is FactorSource.Device && factorSource.supportsBabylon) {
            factorSource.copy(
                value = factorSource.value.copy(
                    common = factorSource.value.common.copy(
                        flags = factorSource.value.common.flags.filterNot { flag -> flag == FactorSourceFlag.MAIN }
                    )
                )
            )
        } else {
            factorSource
        }
    }

    return copy(factorSources = FactorSources(listOf(mainBabylonFactorSource) + existingBabylonDeviceFactorSources).asList())
}

fun Profile.nextAppearanceId(forNetworkId: NetworkId): AppearanceId {
    val forNetwork = networks.asIdentifiable().getBy(forNetworkId) ?: return AppearanceId(0u)
    return AppearanceId.from(offset = forNetwork.accounts.size.toUInt())
}

fun Profile.getAuthorizedDApp(dAppDefinitionAddress: AccountAddress): AuthorizedDapp? =
    currentNetwork?.authorizedDapps?.asIdentifiable()?.getBy(dAppDefinitionAddress)

fun Profile.getAuthorizedDApps(): List<AuthorizedDapp> = currentNetwork?.authorizedDapps.orEmpty()

fun Profile.createOrUpdateAuthorizedDApp(
    unverifiedAuthorizedDApp: AuthorizedDapp
): Profile {
    val updatedNetworks = networks.mapWhen(
        predicate = { it.id == unverifiedAuthorizedDApp.networkId },
        mutation = { network ->
            val authorizedDapps = network.authorizedDapps.asIdentifiable()
            val existingDApp = authorizedDapps.getBy(unverifiedAuthorizedDApp.dappDefinitionAddress)
            if (existingDApp == null) {
                network.copy(authorizedDapps = authorizedDapps.append(unverifiedAuthorizedDApp).asList())
            } else {
                val authorizedDApp = network.validateAuthorizedPersonas(unverifiedAuthorizedDApp)
                // Replace old authorizedDApp
                val updatedDApps = authorizedDapps.asList().mapWhen(
                    predicate = { it.dappDefinitionAddress == existingDApp.dappDefinitionAddress },
                    mutation = { authorizedDApp }
                )
                network.copy(authorizedDapps = AuthorizedDapps(updatedDApps).asList())
            }
        }
    )

    return copy(networks = ProfileNetworks(updatedNetworks).asList())
}

fun Profile.deleteAuthorizedDApp(
    dApp: AuthorizedDapp
): Profile {
    val updatedNetwork = networks.mapWhen(
        predicate = { it.id == dApp.networkId },
        mutation = { network ->
            network.copy(authorizedDapps = network.authorizedDapps.asIdentifiable().removeBy(dApp.dappDefinitionAddress).asList())
        }
    )

    return copy(networks = ProfileNetworks(updatedNetwork).asList())
}

fun Profile.getResourcePreferences(): ResourceAppPreferences {
    val networkId = currentNetwork?.id ?: return ResourceAppPreferences()
    return networks.asIdentifiable().getBy(networkId)?.resourcePreferences.orEmpty().asIdentifiable()
}

fun Profile.updateResourcePreferences(preferences: ResourceAppPreferences): Profile {
    val networkId = currentNetwork?.id ?: return this
    val updatedNetworks = networks.mapWhen(
        predicate = { it.id == networkId },
        mutation = { network ->
            network.copy(
                resourcePreferences = preferences.asList()
            )
        }
    )
    return copy(networks = ProfileNetworks(updatedNetworks).asList())
}

fun Profile.changeDAppLockersVisibility(dApp: AuthorizedDapp, isVisible: Boolean): Profile {
    val updatedNetwork = networks.mapWhen(
        predicate = { it.id == dApp.networkId },
        mutation = { network ->
            val authorizedDapps = network.authorizedDapps.asIdentifiable()
            val updatedDApps = authorizedDapps.asList().mapWhen(
                predicate = { it.dappDefinitionAddress == dApp.dappDefinitionAddress },
                mutation = {
                    dApp.copy(
                        preferences = dApp.preferences.copy(
                            deposits = if (isVisible) {
                                AuthorizedDappPreferenceDeposits.VISIBLE
                            } else {
                                AuthorizedDappPreferenceDeposits.HIDDEN
                            }
                        )
                    )
                }
            )
            network.copy(
                authorizedDapps = updatedDApps
            )
        }
    )

    return copy(networks = ProfileNetworks(updatedNetwork).asList())
}

private fun Profile.withUpdatedContentHint() = copy(
    header = header.copy(
        contentHint = ContentHint(
            numberOfNetworks = networks.size.toUShort(),
            numberOfAccountsOnAllNetworksInTotal = networks.sumOf { network -> network.accounts.active().size }.toUShort(),
            numberOfPersonasOnAllNetworksInTotal = networks.sumOf { network -> network.personas.active().size }.toUShort()
        )
    )
)
