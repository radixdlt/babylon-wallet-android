package com.babylon.wallet.android.fakes

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
                            lastUsedOn = "",
                            sharedAccounts = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts(
                                listOf(), OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.Mode.AtLeast
                            )
                        )
                    )
                )
                connectedDApp
            }
            InitialState.SavedDapp -> connectedDApp
        }
    }

    override suspend fun addConnectedDApp(connectedDApp: OnNetwork.ConnectedDapp) {
        this.connectedDApp = connectedDApp
    }

    override suspend fun updateConnectedDApp(connectedDApp: OnNetwork.ConnectedDapp) {
        this.connectedDApp = connectedDApp
    }

    override suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: String,
        personaAddress: String
    ): OnNetwork.ConnectedDapp.AuthorizedPersonaSimple? {
        TODO("Not yet implemented")
    }

    override suspend fun dAppConnectedPersonaAccountAddresses(
        dAppDefinitionAddress: String,
        personaAddress: String,
        numberOfAccounts: Int,
        mode: OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.Mode
    ): List<String> {
        return emptyList()
    }

    override suspend fun updateAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: String,
        personaAddress: String,
        sharedAccounts: OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts
    ) {
    }

    override suspend fun updateConnectedDappPersonas(
        dAppDefinitionAddress: String,
        personas: List<OnNetwork.ConnectedDapp.AuthorizedPersonaSimple>
    ) {
    }
}
