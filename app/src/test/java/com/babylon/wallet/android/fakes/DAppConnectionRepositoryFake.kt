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

    private var connectedDApp: OnNetwork.ConnectedDapp? = null

    override suspend fun getConnectedDapp(dAppDefinitionAddress: String): OnNetwork.ConnectedDapp? {
        return when (state) {
            InitialState.NoDapp -> null
            InitialState.PredefinedDapp -> {
                connectedDApp = OnNetwork.ConnectedDapp(
                    11, dAppDefinitionAddress, "dApp", listOf(
                        OnNetwork.ConnectedDapp.AuthorizedPersonaSimple(
                            identityAddress = "address1",
                            fieldIDs = emptyList(),
                            lastUsedOn = "2023-01-31T10:28:14Z",
                            sharedAccounts = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts(
                                listOf(SampleDataProvider().randomAddress()),
                                OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                                    OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.AtLeast,
                                    1
                                )
                            )
                        )
                    )
                )
                connectedDApp
            }
            InitialState.SavedDapp -> connectedDApp
        }
    }

    override fun getConnectedDapps(): Flow<List<OnNetwork.ConnectedDapp>> {
        return flow {
            emit(
                listOf(
                    OnNetwork.ConnectedDapp(
                        11, "address1", "dApp 1", listOf(
                            OnNetwork.ConnectedDapp.AuthorizedPersonaSimple(
                                identityAddress = "address1",
                                fieldIDs = emptyList(),
                                lastUsedOn = "2023-01-31T10:28:14Z",
                                sharedAccounts = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts(
                                    listOf(),
                                    OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                                        OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.AtLeast,
                                        1
                                    )
                                )
                            )
                        )
                    ),
                    OnNetwork.ConnectedDapp(
                        11, "address2", "dApp 2", listOf(
                            OnNetwork.ConnectedDapp.AuthorizedPersonaSimple(
                                identityAddress = "address1",
                                fieldIDs = emptyList(),
                                lastUsedOn = "2023-01-31T10:28:14Z",
                                sharedAccounts = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts(
                                    listOf(),
                                    OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                                        OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.AtLeast,
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

    override suspend fun updateOrCreateConnectedDApp(connectedDApp: OnNetwork.ConnectedDapp) {
        this.connectedDApp = connectedDApp
    }

    override suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: String,
        personaAddress: String
    ): OnNetwork.ConnectedDapp.AuthorizedPersonaSimple? {
        return null
    }

    override suspend fun dAppConnectedPersonaAccountAddresses(
        dAppDefinitionAddress: String,
        personaAddress: String,
        numberOfAccounts: Int,
        quantifier: OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier
    ): List<String> {
        return emptyList()
    }

    override suspend fun updateDappAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: String,
        personaAddress: String,
        sharedAccounts: OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts
    ): OnNetwork.ConnectedDapp {
        return checkNotNull(connectedDApp)
    }

    override suspend fun updateConnectedDappPersonas(
        dAppDefinitionAddress: String,
        personas: List<OnNetwork.ConnectedDapp.AuthorizedPersonaSimple>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun deletePersonaForDapp(dAppDefinitionAddress: String, personaAddress: String) {
    }

    override fun getConnectedDappsByPersona(personaAddress: String): Flow<List<OnNetwork.ConnectedDapp>> {
        return getConnectedDapps()
    }

    override fun getConnectedDappFlow(dAppDefinitionAddress: String): Flow<OnNetwork.ConnectedDapp?> {
        return flow {
            emit(getConnectedDapp(dAppDefinitionAddress))
        }
    }

    override suspend fun deleteDapp(dAppDefinitionAddress: String) {
    }

}
