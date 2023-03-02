package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.domain.SampleDataProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.repository.DAppConnectionRepository

class DAppConnectionRepositoryFake : DAppConnectionRepository {

    var state = InitialState.NoDapp

    enum class InitialState {
        NoDapp, PredefinedDapp, SavedDapp
    }

    private var authorizedDApp: OnNetwork.AuthorizedDapp? = null

    override suspend fun getAuthorizedDapp(dAppDefinitionAddress: String): OnNetwork.AuthorizedDapp? {
        return when (state) {
            InitialState.NoDapp -> null
            InitialState.PredefinedDapp -> {
                authorizedDApp = OnNetwork.AuthorizedDapp(
                    11, dAppDefinitionAddress, "dApp", listOf(
                        OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple(
                            identityAddress = "address1",
                            fieldIDs = emptyList(),
                            lastUsedOn = "2023-01-31T10:28:14Z",
                            sharedAccounts = OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts(
                                listOf(SampleDataProvider().randomAddress()),
                                OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                                    OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.AtLeast,
                                    1
                                )
                            )
                        )
                    )
                )
                authorizedDApp
            }
            InitialState.SavedDapp -> authorizedDApp
        }
    }

    override fun getAuthorizedDapps(): Flow<List<OnNetwork.AuthorizedDapp>> {
        return flow {
            emit(
                listOf(
                    OnNetwork.AuthorizedDapp(
                        11, "address1", "dApp 1", listOf(
                            OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple(
                                identityAddress = "address1",
                                fieldIDs = emptyList(),
                                lastUsedOn = "2023-01-31T10:28:14Z",
                                sharedAccounts = OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts(
                                    listOf(),
                                    OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                                        OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.AtLeast,
                                        1
                                    )
                                )
                            )
                        )
                    ),
                    OnNetwork.AuthorizedDapp(
                        11, "address2", "dApp 2", listOf(
                            OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple(
                                identityAddress = "address1",
                                fieldIDs = emptyList(),
                                lastUsedOn = "2023-01-31T10:28:14Z",
                                sharedAccounts = OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts(
                                    listOf(),
                                    OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                                        OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.AtLeast,
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

    override suspend fun updateOrCreateAuthorizedDApp(authorizedDApp: OnNetwork.AuthorizedDapp) {
        this.authorizedDApp = authorizedDApp
    }

    override suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: String,
        personaAddress: String
    ): OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple? {
        return null
    }

    override suspend fun dAppAuthorizedPersonaAccountAddresses(
        dAppDefinitionAddress: String,
        personaAddress: String,
        numberOfAccounts: Int,
        quantifier: OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier
    ): List<String> {
        return emptyList()
    }

    override suspend fun updateDappAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: String,
        personaAddress: String,
        sharedAccounts: OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts
    ): OnNetwork.AuthorizedDapp {
        return checkNotNull(authorizedDApp)
    }

    override suspend fun deletePersonaForDapp(dAppDefinitionAddress: String, personaAddress: String) {
    }

    override fun getAuthorizedDappsByPersona(personaAddress: String): Flow<List<OnNetwork.AuthorizedDapp>> {
        return getAuthorizedDapps()
    }

    override fun getAuthorizedDappFlow(dAppDefinitionAddress: String): Flow<OnNetwork.AuthorizedDapp?> {
        return flow {
            emit(getAuthorizedDapp(dAppDefinitionAddress))
        }
    }

    override suspend fun deleteAuthorizedDapp(dAppDefinitionAddress: String) {
    }

}
