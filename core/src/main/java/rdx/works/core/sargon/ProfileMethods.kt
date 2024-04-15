package rdx.works.core.sargon

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Accounts
import com.radixdlt.sargon.AuthorizedDapps
import com.radixdlt.sargon.ContentHint
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.Personas
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileNetwork
import com.radixdlt.sargon.ThirdPartyDeposits
import com.radixdlt.sargon.TransactionPreferences
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import rdx.works.core.mapWhen

fun Profile.addAccounts(
    accounts: List<Account>,
    onNetwork: NetworkId
): Profile {
    val networkExist = networks.any { onNetwork == it.id }
    val newNetworks = if (networkExist) {
        this.networks.mapWhen(predicate = { network -> network.id == onNetwork }) { network ->
            ProfileNetwork(
                id = network.id,
                accounts = Accounts.init(network.accounts() + accounts),
                authorizedDapps = network.authorizedDapps,
                personas = network.personas
            )
        }
    } else {
        this.networks + ProfileNetwork(
            id = onNetwork,
            accounts = Accounts.init(accounts),
            authorizedDapps = AuthorizedDapps.init(),
            personas = Personas.init()
        )
    }
    val updatedProfile = copy(networks = newNetworks)
    return updatedProfile.withUpdatedContentHint()
}

fun Profile.addAuthSigningFactorInstanceForEntity(
    entity: ProfileEntity,
    authSigningFactorInstance: HierarchicalDeterministicFactorInstance
): Profile {
    val updatedNetworks = networks.mapWhen(predicate = { network -> network.id == entity.networkId }) { network ->
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
    return copy(networks = updatedNetworks)
}

fun Profile.updateThirdPartyDepositSettings(
    account: Account,
    thirdPartyDeposits: ThirdPartyDeposits
): Profile {
    val updatedNetworks = networks.mapWhen(predicate = { network -> network.id == account.networkId }) { network ->
        val updatedAccounts = network.accounts().mapWhen(predicate = { it.address == account.address }) { account ->
            account.copy(onLedgerSettings = account.onLedgerSettings.copy(thirdPartyDeposits = thirdPartyDeposits))
        }
        network.copy(accounts = Accounts.init(updatedAccounts))
    }
    return copy(networks = updatedNetworks)
}

fun Profile.addNetworkIfDoesNotExist(
    onNetwork: NetworkId
): Profile = if (networks.none { onNetwork == it.id }) {
    copy(
        networks = networks + ProfileNetwork(
            id = onNetwork,
            accounts = Accounts.init(),
            authorizedDapps = AuthorizedDapps.init(),
            personas = Personas.init()
        )
    ).withUpdatedContentHint()
} else {
    this
}

fun Profile.nextPersonaIndex(
    forNetworkId: NetworkId,
    derivationPathScheme: DerivationPathScheme,
    factorSourceID: FactorSourceId? = null
): UInt {
    val network = networks.firstOrNull { it.id == forNetworkId } ?: return 0u

    val factorSource = factorSources().find {
        it.id == factorSourceID
    } ?: mainBabylonFactorSource?.let { FactorSource.Device(it) } ?: return 0u

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
        networks = networks.mapWhen(predicate = { it.id == networkId }, mutation = { network ->
            network.copy(
                personas = Personas.init(
                    network.personas().mapWhen(predicate = { it.address == persona.address }, mutation = { persona })
                )
            )
        })
    )
}

fun Profile.addPersona(
    persona: Persona,
    onNetwork: NetworkId,
): Profile {
    val personaExists = this.networks.find {
        it.id == onNetwork
    }?.personas?.invoke()?.any { it.address == persona.address } ?: false

    if (personaExists) {
        return this
    }

    return copy(
        networks = networks.mapWhen(predicate = { it.id == onNetwork }, mutation = { network ->
            network.copy(personas = Personas.init(network.personas() + persona))
        })
    ).withUpdatedContentHint()
}

fun Profile.hidePersona(identityAddress: IdentityAddress): Profile {
    val networkId = currentNetwork?.id ?: return this
    val updatedNetworks = networks.mapWhen(predicate = { it.id == networkId }, mutation = { network ->
        val updatedAuthorizedDapps = network.authorizedDapps().mapWhen(predicate = { authorizedDapp ->
            authorizedDapp.referencesToAuthorizedPersonas.any { it.identityAddress == identityAddress }
        }, mutation = { authorizedDapp ->
            val updatedReferences = authorizedDapp.referencesToAuthorizedPersonas.filter { it.identityAddress != identityAddress }
            authorizedDapp.copy(referencesToAuthorizedPersonas = updatedReferences)
        })
        network.copy(
            personas = Personas.init(
                network.personas().mapWhen(
                    predicate = { it.address == identityAddress },
                    mutation = { persona ->
                        persona.copy(flags = persona.flags + EntityFlag.DELETED_BY_USER)
                    }
                )
            ),
            authorizedDapps = AuthorizedDapps.init(
                updatedAuthorizedDapps.filter { it.referencesToAuthorizedPersonas.isNotEmpty() }
            )
        )
    })
    return copy(networks = updatedNetworks).withUpdatedContentHint()
}

fun Profile.hideAccount(accountAddress: AccountAddress): Profile {
    val networkId = currentNetwork?.id ?: return this
    val updatedNetworks = networks.mapWhen(predicate = { it.id == networkId }, mutation = { network ->
        val updatedAuthorizedDapps = network.authorizedDapps().mapWhen(predicate = { authorizedDapp ->
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
            authorizedDapp.copy(referencesToAuthorizedPersonas = updatedReferences)
        })
        val updatedAccounts = network.accounts().mapWhen(
            predicate = { it.address == accountAddress },
            mutation = { account ->
                account.copy(flags = account.flags + EntityFlag.DELETED_BY_USER)
            }
        )
        network.copy(
            accounts = Accounts.init(updatedAccounts),
            authorizedDapps = AuthorizedDapps.init(
                updatedAuthorizedDapps.filter { it.referencesToAuthorizedPersonas.isNotEmpty() }
            )
        )
    })
    return copy(networks = updatedNetworks).withUpdatedContentHint()
}

fun Profile.unHideAllEntities(): Profile {
    val networkId = currentNetwork?.id ?: return this
    val updatedNetworks = networks.mapWhen(predicate = { it.id == networkId }, mutation = { network ->
        network.copy(
            personas = Personas.init(
                network.personas().map { persona ->
                    persona.copy(flags = persona.flags - EntityFlag.DELETED_BY_USER)
                }
            ),
            accounts = Accounts.init(
                network.accounts().map { persona ->
                    persona.copy(flags = persona.flags - EntityFlag.DELETED_BY_USER)
                }
            )
        )
    })
    return copy(networks = updatedNetworks).withUpdatedContentHint()
}

fun Profile.addP2PLink(
    p2pLink: P2pLink
): Profile {

    val newAppPreferences = appPreferences.copy(
        p2pLinks = appPreferences.p2pLinks + p2pLink
    )

    return copy(appPreferences = newAppPreferences)
}

fun Profile.deleteP2PLink(
    p2pLink: P2pLink
): Profile {
    val updatedP2PLinks = appPreferences.p2pLinks.toMutableList()
    updatedP2PLinks.removeIf { it == p2pLink }

    val newAppPreferences = appPreferences.copy(p2pLinks = updatedP2PLinks)

    return copy(appPreferences = newAppPreferences)
}

fun Profile.changeGateway(
    gateway: Gateway
): Profile {
    val gateways = appPreferences.gateways.changeCurrent(gateway)
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
        networks = networks.mapWhen(
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
}

private fun Profile.withUpdatedContentHint() = copy(
    header = header.copy(
        contentHint = ContentHint(
            numberOfNetworks = networks.size.toUShort(),
            numberOfAccountsOnAllNetworksInTotal = networks.sumOf { network -> network.accounts().notHiddenAccounts().size }.toUShort(),
            numberOfPersonasOnAllNetworksInTotal = networks.sumOf { network -> network.personas().notHiddenPersonas().size }.toUShort()
        )
    )
)

