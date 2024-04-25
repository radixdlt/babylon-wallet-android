package rdx.works.profile.data.repository

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.AuthorizedPersonaSimple
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.PersonaDataEntryID
import com.radixdlt.sargon.ReferencesToAuthorizedPersonas
import com.radixdlt.sargon.RequestedNumberQuantifier
import com.radixdlt.sargon.SharedToDappWithPersonaAccountAddresses
import com.radixdlt.sargon.extensions.getBy
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.removeByAddress
import com.radixdlt.sargon.extensions.updateOrAppend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import rdx.works.core.mapWhen
import rdx.works.core.sargon.PersonaDataField
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.alreadyGrantedIds
import rdx.works.core.sargon.createOrUpdateAuthorizedDApp
import rdx.works.core.sargon.deleteAuthorizedDApp
import rdx.works.core.sargon.ensurePersonaDataExist
import rdx.works.core.sargon.getAuthorizedDApp
import rdx.works.core.sargon.getAuthorizedDApps
import rdx.works.core.sargon.getDataFieldKind
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("TooManyFunctions")
interface DAppConnectionRepository {

    suspend fun getAuthorizedDApp(dAppDefinitionAddress: AccountAddress): AuthorizedDapp?
    fun getAuthorizedDApps(): Flow<List<AuthorizedDapp>>

    suspend fun updateOrCreateAuthorizedDApp(authorizedDApp: AuthorizedDapp)

    suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress
    ): AuthorizedPersonaSimple?

    suspend fun dAppAuthorizedPersonaAccountAddresses(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress,
        numberOfAccounts: Int,
        quantifier: RequestedNumberQuantifier
    ): List<AccountAddress>

    suspend fun dAppAuthorizedPersonaHasAllDataFields(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress,
        requestedFieldKinds: Map<PersonaDataField.Kind, Int>
    ): Boolean

    suspend fun updateDAppAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress,
        sharedAccounts: SharedToDappWithPersonaAccountAddresses
    ): AuthorizedDapp

    suspend fun deletePersonaForDApp(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress
    )

    fun getAuthorizedDAppsByPersona(personaAddress: IdentityAddress): Flow<List<AuthorizedDapp>>

    fun getAuthorizedDAppFlow(dAppDefinitionAddress: AccountAddress): Flow<AuthorizedDapp?>
    suspend fun deleteAuthorizedDApp(dAppDefinitionAddress: AccountAddress)

    suspend fun ensureAuthorizedPersonasFieldsExist(personaAddress: IdentityAddress, existingFieldIds: List<PersonaDataEntryID>)
}

@Suppress("TooManyFunctions")
class DAppConnectionRepositoryImpl @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val getProfileUseCase: GetProfileUseCase
) : DAppConnectionRepository {

    override fun getAuthorizedDAppFlow(dAppDefinitionAddress: AccountAddress): Flow<AuthorizedDapp?> {
        return profileRepository.profile.map {
            Timber.d("Authorized dapps $it")
            it.getAuthorizedDApp(dAppDefinitionAddress)
        }
    }

    override suspend fun getAuthorizedDApp(dAppDefinitionAddress: AccountAddress): AuthorizedDapp? {
        return profileRepository.profile.first().getAuthorizedDApp(dAppDefinitionAddress)
    }

    override fun getAuthorizedDApps(): Flow<List<AuthorizedDapp>> {
        return profileRepository.profile.map { profile -> profile.getAuthorizedDApps() }
    }

    override suspend fun updateOrCreateAuthorizedDApp(authorizedDApp: AuthorizedDapp) {
        val profile = profileRepository.profile.first()

        Timber.d("Authorized dapps updating profile dApp: $authorizedDApp")
        val updatedProfile = profile.createOrUpdateAuthorizedDApp(authorizedDApp)
        profileRepository.saveProfile(updatedProfile)
    }

    override suspend fun deleteAuthorizedDApp(dAppDefinitionAddress: AccountAddress) {
        val profile = profileRepository.profile.first()

        getAuthorizedDApp(dAppDefinitionAddress)?.let {
            val updatedProfile = profile.deleteAuthorizedDApp(it)
            profileRepository.saveProfile(updatedProfile)
        }
    }

    override suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress
    ): AuthorizedPersonaSimple? {
        return getAuthorizedDApp(dAppDefinitionAddress)?.referencesToAuthorizedPersonas?.getBy(personaAddress)
    }

    override suspend fun dAppAuthorizedPersonaAccountAddresses(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress,
        numberOfAccounts: Int,
        quantifier: RequestedNumberQuantifier
    ): List<AccountAddress> {
        val sharedAccounts = getAuthorizedDApp(
            dAppDefinitionAddress
        )?.referencesToAuthorizedPersonas?.getBy(personaAddress)?.sharedAccounts
        return if (quantifier == sharedAccounts?.request?.quantifier && numberOfAccounts == sharedAccounts.request.quantity.toInt()) {
            sharedAccounts.ids
        } else {
            emptyList()
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    override suspend fun dAppAuthorizedPersonaHasAllDataFields(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress,
        requestedFieldKinds: Map<PersonaDataField.Kind, Int>
    ): Boolean {
        val requestedFieldsMutableMap = requestedFieldKinds.toMutableMap()
        val personaData = checkNotNull(getProfileUseCase().activePersonaOnCurrentNetwork(personaAddress)).personaData
        val alreadyGrantedIds = getAuthorizedDApp(
            dAppDefinitionAddress
        )?.referencesToAuthorizedPersonas?.getBy(personaAddress)?.sharedPersonaData?.alreadyGrantedIds.orEmpty()
        alreadyGrantedIds.forEach { entryId ->
            val entryKind = personaData.getDataFieldKind(entryId)
            if (requestedFieldsMutableMap.containsKey(entryKind)) {
                if (requestedFieldsMutableMap[entryKind]!! > 1) {
                    requestedFieldsMutableMap[entryKind!!] = requestedFieldsMutableMap[entryKind]!! - 1
                } else {
                    requestedFieldsMutableMap.remove(entryKind)
                }
            }
        }
        return requestedFieldsMutableMap.isEmpty()
    }

    override suspend fun updateDAppAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: AccountAddress,
        personaAddress: IdentityAddress,
        sharedAccounts: SharedToDappWithPersonaAccountAddresses
    ): AuthorizedDapp {
        val dApp = getAuthorizedDApp(dAppDefinitionAddress)
        val persona = dApp?.referencesToAuthorizedPersonas?.getBy(personaAddress)
        requireNotNull(persona)
        return dApp.copy(
            referencesToAuthorizedPersonas = dApp.referencesToAuthorizedPersonas.updateOrAppend(
                persona.copy(sharedAccounts = sharedAccounts)
            )
        )
    }

    override suspend fun deletePersonaForDApp(dAppDefinitionAddress: AccountAddress, personaAddress: IdentityAddress) {
        getAuthorizedDApp(dAppDefinitionAddress)?.let { dApp ->
            val updatedDapp = dApp.copy(
                referencesToAuthorizedPersonas = dApp.referencesToAuthorizedPersonas.removeByAddress(personaAddress)
            )
            if (updatedDapp.referencesToAuthorizedPersonas().isEmpty()) {
                deleteAuthorizedDApp(dApp.dappDefinitionAddress)
            } else {
                updateOrCreateAuthorizedDApp(updatedDapp)
            }
        }
    }

    override suspend fun ensureAuthorizedPersonasFieldsExist(personaAddress: IdentityAddress, existingFieldIds: List<PersonaDataEntryID>) {
        getAuthorizedDAppsByPersona(personaAddress).firstOrNull()?.forEach { dApp ->
            val updatedDApp = dApp.copy(
                referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas.init(
                    dApp.referencesToAuthorizedPersonas().mapWhen(
                        predicate = { it.identityAddress == personaAddress }
                    ) { authorizedPersona ->
                        authorizedPersona.ensurePersonaDataExist(existingFieldIds)
                    }
                )
            )
            updateOrCreateAuthorizedDApp(updatedDApp)
        }
    }

    override fun getAuthorizedDAppsByPersona(personaAddress: IdentityAddress): Flow<List<AuthorizedDapp>> {
        return getAuthorizedDApps().map { authorizedDapps ->
            authorizedDapps.filter { dApp ->
                dApp.referencesToAuthorizedPersonas.getBy(personaAddress) != null
            }
        }
    }
}
