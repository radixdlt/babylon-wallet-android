package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.domain.SampleDataProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaDataEntryID
import rdx.works.profile.data.model.pernetwork.RequestedNumber
import rdx.works.profile.data.model.pernetwork.Shared
import rdx.works.profile.data.repository.DAppConnectionRepository

class DAppConnectionRepositoryFake : DAppConnectionRepository {

    var state = InitialState.NoDapp

    enum class InitialState {
        NoDapp, PredefinedDapp, SavedDapp
    }

    private var authorizedDApp: Network.AuthorizedDapp? = null

    override suspend fun getAuthorizedDapp(dAppDefinitionAddress: String): Network.AuthorizedDapp? {
        return when (state) {
            InitialState.NoDapp -> null
            InitialState.PredefinedDapp -> {
                authorizedDApp = Network.AuthorizedDapp(
                    11, dAppDefinitionAddress, "dApp", listOf(
                        Network.AuthorizedDapp.AuthorizedPersonaSimple(
                            identityAddress = "address1",
                            lastLogin = "2023-01-31T10:28:14Z",
                            sharedAccounts = Shared(
                                listOf("address-acc-1", SampleDataProvider().randomAddress()),
                                RequestedNumber(
                                    RequestedNumber.Quantifier.AtLeast,
                                    1
                                )
                            ),
                            sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData()
                        )
                    )
                )
                authorizedDApp
            }
            InitialState.SavedDapp -> authorizedDApp
        }
    }

    override fun getAuthorizedDapps(): Flow<List<Network.AuthorizedDapp>> {
        return flow {
            emit(
                listOf(
                    Network.AuthorizedDapp(
                        11, "address1", "dApp 1", listOf(
                            Network.AuthorizedDapp.AuthorizedPersonaSimple(
                                identityAddress = "address1",
                                lastLogin = "2023-01-31T10:28:14Z",
                                sharedAccounts = Shared(
                                    listOf(),
                                    RequestedNumber(
                                        RequestedNumber.Quantifier.AtLeast,
                                        1
                                    )
                                ),
                                sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData()
                            )
                        )
                    ),
                    Network.AuthorizedDapp(
                        11, "address2", "dApp 2", listOf(
                            Network.AuthorizedDapp.AuthorizedPersonaSimple(
                                identityAddress = "address1",
                                sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData(),
                                lastLogin = "2023-01-31T10:28:14Z",
                                sharedAccounts = Shared(
                                    listOf(),
                                    RequestedNumber(
                                        RequestedNumber.Quantifier.AtLeast,
                                        1
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    override suspend fun updateOrCreateAuthorizedDApp(authorizedDApp: Network.AuthorizedDapp) {
        this.authorizedDApp = authorizedDApp
    }

    override suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: String,
        personaAddress: String
    ): Network.AuthorizedDapp.AuthorizedPersonaSimple? {
        return null
    }

    override suspend fun dAppAuthorizedPersonaAccountAddresses(
        dAppDefinitionAddress: String,
        personaAddress: String,
        numberOfAccounts: Int,
        quantifier: RequestedNumber.Quantifier
    ): List<String> {
        return emptyList()
    }

    override suspend fun dAppAuthorizedPersonaHasAllDataFields(
        dAppDefinitionAddress: String,
        personaAddress: String,
        fieldIds: List<PersonaDataEntryID>
    ): Boolean {
        return false
    }

    override suspend fun updateDappAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: String,
        personaAddress: String,
        sharedAccounts: Shared<String>
    ): Network.AuthorizedDapp {
        return checkNotNull(authorizedDApp)
    }

    override suspend fun deletePersonaForDapp(dAppDefinitionAddress: String, personaAddress: String) {
    }

    override fun getAuthorizedDappsByPersona(personaAddress: String): Flow<List<Network.AuthorizedDapp>> {
        return getAuthorizedDapps()
    }

    override fun getAuthorizedDappFlow(dAppDefinitionAddress: String): Flow<Network.AuthorizedDapp?> {
        return flow {
            emit(getAuthorizedDapp(dAppDefinitionAddress))
        }
    }

    override suspend fun deleteAuthorizedDapp(dAppDefinitionAddress: String) {
    }

    override suspend fun ensureAuthorizedPersonasFieldsExist(personaAddress: String, existingFieldIds: List<PersonaDataEntryID>) {
    }

}
