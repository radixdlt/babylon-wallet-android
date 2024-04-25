package com.babylon.wallet.android.fakes

//class DAppConnectionRepositoryFake : DAppConnectionRepository {
//
//    var state = InitialState.NoDapp
//
//    enum class InitialState {
//        NoDapp, PredefinedDapp, SavedDapp
//    }
//
//    var savedDApp: Network.AuthorizedDapp? = null
//
//    override suspend fun getAuthorizedDApp(dAppDefinitionAddress: AccountAddress): Network.AuthorizedDapp? {
//        return when (state) {
//            InitialState.NoDapp -> null
//            InitialState.PredefinedDapp -> {
//                val dApp = DApp.sampleMainnet().copy(dAppAddress = dAppDefinitionAddress)
//                savedDApp = Network.AuthorizedDapp(
//                    NetworkId.MAINNET.discriminant.toInt(), dApp.dAppAddress.string, dApp.name, listOf(
//                        Network.AuthorizedDapp.AuthorizedPersonaSimple(
//                            identityAddress = IdentityAddress.sampleMainnet().string,
//                            lastLogin = "2023-01-31T10:28:14Z",
//                            sharedAccounts = Shared(
//                                listOf(AccountAddress.sampleMainnet.random().string, AccountAddress.sampleMainnet.random().string),
//                                RequestedNumber(
//                                    RequestedNumber.Quantifier.AtLeast,
//                                    1
//                                )
//                            ),
//                            sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData()
//                        )
//                    )
//                )
//                savedDApp
//            }
//            InitialState.SavedDapp -> savedDApp
//        }
//    }
//
//    override fun getAuthorizedDApps(): Flow<List<Network.AuthorizedDapp>> {
//        return flow {
//            emit(
//                listOf(
//                    with(DApp.sampleMainnet()) {
//                        Network.AuthorizedDapp(
//                            dAppAddress.networkId.discriminant.toInt(), dAppAddress.string, name, listOf(
//                                Network.AuthorizedDapp.AuthorizedPersonaSimple(
//                                    identityAddress = IdentityAddress.sampleMainnet().string,
//                                    lastLogin = "2023-01-31T10:28:14Z",
//                                    sharedAccounts = Shared(
//                                        listOf(),
//                                        RequestedNumber(
//                                            RequestedNumber.Quantifier.AtLeast,
//                                            1
//                                        )
//                                    ),
//                                    sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData()
//                                )
//                            )
//                        )
//                    },
//                    with(DApp.sampleMainnet.other()) {
//                        Network.AuthorizedDapp(
//                            dAppAddress.networkId.discriminant.toInt(), dAppAddress.string, name, listOf(
//                                Network.AuthorizedDapp.AuthorizedPersonaSimple(
//                                    identityAddress = IdentityAddress.sampleMainnet().string,
//                                    sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData(),
//                                    lastLogin = "2023-01-31T10:28:14Z",
//                                    sharedAccounts = Shared(
//                                        listOf(),
//                                        RequestedNumber(
//                                            RequestedNumber.Quantifier.AtLeast,
//                                            1
//                                        )
//                                    )
//                                )
//                            )
//                        )
//                    }
//                )
//            )
//        }
//    }
//
//    override suspend fun updateOrCreateAuthorizedDApp(authorizedDApp: Network.AuthorizedDapp) {
//        this.savedDApp = authorizedDApp
//    }
//
//    override suspend fun getDAppConnectedPersona(
//        dAppDefinitionAddress: AccountAddress,
//        personaAddress: String
//    ): Network.AuthorizedDapp.AuthorizedPersonaSimple? {
//        return null
//    }
//
//    override suspend fun dAppAuthorizedPersonaAccountAddresses(
//        dAppDefinitionAddress: AccountAddress,
//        personaAddress: String,
//        numberOfAccounts: Int,
//        quantifier: RequestedNumber.Quantifier
//    ): List<AccountAddress> {
//        return emptyList()
//    }
//
//    override suspend fun dAppAuthorizedPersonaHasAllDataFields(
//        dAppDefinitionAddress: AccountAddress,
//        personaAddress: String,
//        requestedFieldKinds: Map<PersonaDataField.Kind, Int>
//    ): Boolean {
//        return false
//    }
//
//    override suspend fun updateDappAuthorizedPersonaSharedAccounts(
//        dAppDefinitionAddress: AccountAddress,
//        personaAddress: String,
//        sharedAccounts: Shared<String>
//    ): Network.AuthorizedDapp {
//        return checkNotNull(savedDApp)
//    }
//
//    override suspend fun deletePersonaForDapp(dAppDefinitionAddress: AccountAddress, personaAddress: String) {
//    }
//
//    override fun getAuthorizedDappsByPersona(personaAddress: String): Flow<List<Network.AuthorizedDapp>> {
//        return getAuthorizedDApps()
//    }
//
//    override fun getAuthorizedDAppFlow(dAppDefinitionAddress: AccountAddress): Flow<Network.AuthorizedDapp?> {
//        return flow {
//            emit(getAuthorizedDApp(dAppDefinitionAddress))
//        }
//    }
//
//    override suspend fun deleteAuthorizedDApp(dAppDefinitionAddress: AccountAddress) {
//    }
//
//    override suspend fun ensureAuthorizedPersonasFieldsExist(personaAddress: String, existingFieldIds: List<PersonaDataEntryID>) {
//    }
//
//}
