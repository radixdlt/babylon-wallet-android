package com.babylon.wallet.android.fakes

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.AuthorizedPersonaSimple
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.PersonaDataEntryId
import com.radixdlt.sargon.RequestedNumberQuantifier
import com.radixdlt.sargon.RequestedQuantity
import com.radixdlt.sargon.SharedPersonaData
import com.radixdlt.sargon.SharedToDappWithPersonaAccountAddresses
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.extensions.ReferencesToAuthorizedPersonas
import com.radixdlt.sargon.extensions.atLeast
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import rdx.works.core.domain.DApp
import rdx.works.core.sargon.PersonaDataField
import rdx.works.profile.data.repository.DAppConnectionRepository

class DAppConnectionRepositoryFake : DAppConnectionRepository {

    var state = InitialState.NoDapp

    enum class InitialState {
        NoDapp, PredefinedDapp, SavedDapp
    }

    var savedDApp: AuthorizedDapp? = null

    override suspend fun getAuthorizedDApp(dAppDefinitionAddress: AccountAddress): AuthorizedDapp? {
        return when (state) {
            InitialState.NoDapp -> null
            InitialState.PredefinedDapp -> {
                val dApp = DApp.sampleMainnet().copy(dAppAddress = dAppDefinitionAddress)
                savedDApp = AuthorizedDapp(
                    networkId = NetworkId.MAINNET,
                    dappDefinitionAddress = dApp.dAppAddress,
                    displayName = dApp.name,
                    referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas(
                        AuthorizedPersonaSimple(
                            identityAddress = IdentityAddress.sampleMainnet(),
                            lastLogin = Timestamp.parse("2023-01-31T10:28:14Z"),
                            sharedAccounts = SharedToDappWithPersonaAccountAddresses(
                                request = RequestedQuantity.atLeast(1),
                                ids = listOf(AccountAddress.sampleMainnet.random(), AccountAddress.sampleMainnet.random())
                            ),
                            sharedPersonaData = SharedPersonaData(name = null, emailAddresses = null, phoneNumbers = null)
                        )
                    ).asList()
                )
                savedDApp
            }
            InitialState.SavedDapp -> savedDApp
        }
    }

    override fun getAuthorizedDApps(): Flow<List<AuthorizedDapp>> = flowOf(listOf(
            with(DApp.sampleMainnet()) {
                AuthorizedDapp(
                    networkId = dAppAddress.networkId,
                    dappDefinitionAddress = dAppAddress,
                    displayName = name,
                    referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas(
                        AuthorizedPersonaSimple(
                            identityAddress = IdentityAddress.sampleMainnet(),
                            lastLogin = Timestamp.parse("2023-01-31T10:28:14Z"),
                            sharedAccounts = SharedToDappWithPersonaAccountAddresses(
                                request = RequestedQuantity.atLeast(1),
                                ids = emptyList()
                            ),
                            sharedPersonaData = SharedPersonaData(name = null, emailAddresses = null, phoneNumbers = null)
                        )
                    ).asList()
                )
            },
            with(DApp.sampleMainnet.other()) {
                AuthorizedDapp(
                    networkId = dAppAddress.networkId,
                    dappDefinitionAddress = dAppAddress,
                    displayName = name,
                    referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas(
                        AuthorizedPersonaSimple(
                            identityAddress = IdentityAddress.sampleMainnet(),
                            lastLogin = Timestamp.parse("2023-01-31T10:28:14Z"),
                            sharedAccounts = SharedToDappWithPersonaAccountAddresses(
                                request = RequestedQuantity.atLeast(1),
                                ids = emptyList()
                            ),
                            sharedPersonaData = SharedPersonaData(name = null, emailAddresses = null, phoneNumbers = null)
                        )
                    ).asList()
                )
            }
        ))

    override suspend fun updateOrCreateAuthorizedDApp(authorizedDApp: AuthorizedDapp) {
        this.savedDApp = authorizedDApp
    }

    override suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress
    ): AuthorizedPersonaSimple? {
        return null
    }

    override suspend fun dAppAuthorizedPersonaAccountAddresses(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress,
        numberOfAccounts: Int,
        quantifier: RequestedNumberQuantifier
    ): List<AccountAddress> {
        return emptyList()
    }

    override suspend fun dAppAuthorizedPersonaHasAllDataFields(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress,
        requestedFieldKinds: Map<PersonaDataField.Kind, Int>
    ): Boolean {
        return false
    }

    override suspend fun updateDAppAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress,
        sharedAccounts: SharedToDappWithPersonaAccountAddresses
    ): AuthorizedDapp = checkNotNull(savedDApp)

    override suspend fun deletePersonaForDApp(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress
    ) {}

    override fun getAuthorizedDAppsByPersona(personaAddress: IdentityAddress): Flow<List<AuthorizedDapp>> {
        return getAuthorizedDApps()
    }

    override fun getAuthorizedDAppFlow(dAppDefinitionAddress: AccountAddress): Flow<AuthorizedDapp?> {
        return flow {
            emit(getAuthorizedDApp(dAppDefinitionAddress))
        }
    }

    override suspend fun deleteAuthorizedDApp(dAppDefinitionAddress: AccountAddress) {
    }

    override suspend fun ensureAuthorizedPersonasFieldsExist(personaAddress: IdentityAddress, existingFieldIds: List<PersonaDataEntryId>) {
    }

}
