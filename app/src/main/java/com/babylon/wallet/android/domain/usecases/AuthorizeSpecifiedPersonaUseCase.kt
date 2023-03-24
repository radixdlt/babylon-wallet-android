package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.toKind
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.domain.model.toProfileShareAccountsQuantifier
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.utils.toISO8601String
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.AccountRepository
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.PersonaRepository
import rdx.works.profile.data.repository.updateAuthorizedDappPersonas
import java.time.LocalDateTime
import javax.inject.Inject

class AuthorizeSpecifiedPersonaUseCase @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val dAppMessenger: DappMessenger,
    private val accountRepository: AccountRepository,
    private val personaRepository: PersonaRepository
) {

    suspend operator fun invoke(request: IncomingRequest): Result<String> =
        coroutineScope {
            var operationResult: Result<String> = Result.Error()
            (request as? IncomingRequest.AuthorizedRequest)?.let { request ->
                val authorizedDapp = dAppConnectionRepository.getAuthorizedDapp(
                    request.metadata.dAppDefinitionAddress
                ) ?: return@coroutineScope Result.Error()
                val authorizedPersonaSimple =
                    authorizedDapp.referencesToAuthorizedPersonas.firstOrNull {
                        it.identityAddress == (request.authRequest as? IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest)
                            ?.personaAddress
                    } ?: return@coroutineScope Result.Error()
                val persona = personaRepository.getPersonaByAddress(
                    authorizedPersonaSimple.identityAddress
                ) ?: return@coroutineScope Result.Error()
                if (request.hasOngoingRequestItemsOnly()) {
                    val hasOngoingAccountsRequest = request.ongoingAccountsRequestItem != null
                    val hasOngoingPersonaDataRequest = request.ongoingPersonaDataRequestItem != null
                    val selectedAccounts: List<AccountItemUiModel> = emptyList()
                    val selectedPersonaData: List<Network.Persona.Field>
                    when {
                        hasOngoingAccountsRequest -> {
                            handleOngoingAccountsRequest(
                                request,
                                authorizedDapp,
                                authorizedPersonaSimple,
                                hasOngoingPersonaDataRequest,
                                persona
                            )
                        }
                        hasOngoingPersonaDataRequest -> {
                            selectedPersonaData = getAlreadyGrantedPersonaData(request, authorizedDapp, authorizedPersonaSimple)
                            if (selectedPersonaData.isNotEmpty()) {
                                operationResult = sendSuccessResponse(
                                    request = request,
                                    persona = persona,
                                    selectedAccounts = selectedAccounts,
                                    selectedPersonaData = selectedPersonaData,
                                    authorizedDapp = authorizedDapp
                                )
                            }
                        }
                    }
                }
            }
            operationResult
        }

    private suspend fun handleOngoingAccountsRequest(
        request: IncomingRequest.AuthorizedRequest,
        authorizedDapp: Network.AuthorizedDapp,
        authorizedPersonaSimple: Network.AuthorizedDapp.AuthorizedPersonaSimple,
        hasOngoingPersonaDataRequest: Boolean,
        persona: Network.Persona
    ): Result<String> {
        var operationResult: Result<String> = Result.Error()
        val selectedAccounts: List<AccountItemUiModel> = getAccountsWithGrantedAccess(request, authorizedDapp, authorizedPersonaSimple)
        var selectedPersonaData: List<Network.Persona.Field> = emptyList()
        when {
            hasOngoingPersonaDataRequest -> {
                selectedPersonaData = getAlreadyGrantedPersonaData(request, authorizedDapp, authorizedPersonaSimple)
                if (selectedAccounts.isNotEmpty() && selectedPersonaData.isNotEmpty()) {
                    operationResult = sendSuccessResponse(
                        request = request,
                        persona = persona,
                        selectedAccounts = selectedAccounts,
                        selectedPersonaData = selectedPersonaData,
                        authorizedDapp = authorizedDapp
                    )
                }
            }
            else -> {
                if (selectedAccounts.isNotEmpty()) {
                    operationResult = sendSuccessResponse(
                        request = request,
                        persona = persona,
                        selectedAccounts = selectedAccounts,
                        selectedPersonaData = selectedPersonaData,
                        authorizedDapp = authorizedDapp
                    )
                }
            }
        }
        return operationResult
    }

    private fun updateDappPersonaWithLastUsedTimestamp(
        authorizedDapp: Network.AuthorizedDapp,
        personaAddress: String
    ): Network.AuthorizedDapp {
        val updatedDapp = authorizedDapp.updateAuthorizedDappPersonas(
            authorizedDapp.referencesToAuthorizedPersonas.map { ref ->
                if (ref.identityAddress == personaAddress) {
                    ref.copy(lastUsedOn = LocalDateTime.now().toISO8601String())
                } else {
                    ref
                }
            }
        )
        return updatedDapp
    }

    private suspend fun sendSuccessResponse(
        request: IncomingRequest.AuthorizedRequest,
        persona: Network.Persona,
        selectedAccounts: List<AccountItemUiModel>,
        selectedPersonaData: List<Network.Persona.Field>,
        authorizedDapp: Network.AuthorizedDapp
    ): Result<String> {
        val updatedDapp = updateDappPersonaWithLastUsedTimestamp(authorizedDapp, persona.address)
        val result = dAppMessenger.sendWalletInteractionAuthorizedSuccessResponse(
            interactionId = request.requestId,
            persona = persona,
            usePersona = request.isUsePersonaAuth(),
            ongoingAccounts = selectedAccounts,
            ongoingDataFields = selectedPersonaData
        )
        dAppConnectionRepository.updateOrCreateAuthorizedDApp(updatedDapp)
        return when (result) {
            is Result.Success -> {
                Result.Success(
                    authorizedDapp.displayName
                )
            }
            else -> Result.Error()
        }
    }

    private suspend fun getAlreadyGrantedPersonaData(
        request: IncomingRequest.AuthorizedRequest,
        authorizedDapp: Network.AuthorizedDapp,
        authorizedPersonaSimple: Network.AuthorizedDapp.AuthorizedPersonaSimple
    ): List<Network.Persona.Field> {
        var result: List<Network.Persona.Field> = emptyList()
        val handledRequest = checkNotNull(request.ongoingPersonaDataRequestItem)
        if (personaDataAccessAlreadyGranted(authorizedDapp, handledRequest, authorizedPersonaSimple.identityAddress)) {
            result = personaRepository.getPersonaDataFields(
                address = authorizedPersonaSimple.identityAddress,
                handledRequest.fields.map { it.toKind() }
            )
        }
        return result
    }

    private suspend fun getAccountsWithGrantedAccess(
        request: IncomingRequest.AuthorizedRequest,
        authorizedDapp: Network.AuthorizedDapp,
        authorizedPersonaSimple: Network.AuthorizedDapp.AuthorizedPersonaSimple
    ): List<AccountItemUiModel> {
        var result: List<AccountItemUiModel> = emptyList()
        val handledRequest = checkNotNull(request.ongoingAccountsRequestItem)
        if (request.resetRequestItem?.personaData != true && request.resetRequestItem?.accounts != true) {
            val potentialOngoingAddresses = dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                authorizedDapp.dAppDefinitionAddress,
                authorizedPersonaSimple.identityAddress,
                handledRequest.numberOfAccounts,
                handledRequest.quantifier.toProfileShareAccountsQuantifier()
            )
            if (potentialOngoingAddresses.isNotEmpty()) {
                result = potentialOngoingAddresses
                    .mapNotNull {
                        accountRepository.getAccountByAddress(it)?.toUiModel(true)
                    }
            }
        }
        return result
    }

    private suspend fun personaDataAccessAlreadyGranted(
        dApp: Network.AuthorizedDapp,
        requestItem: IncomingRequest.PersonaRequestItem,
        personaAddress: String
    ): Boolean {
        val requestedFieldsCount = requestItem.fields.size
        val requestedFieldKinds = requestItem.fields.map { it.toKind() }
        val personaFields = personaRepository.getPersonaByAddress(personaAddress)?.fields.orEmpty()
        val requestedFieldsIds = personaFields.filter { requestedFieldKinds.contains(it.kind) }.map { it.id }
        return requestedFieldsCount == requestedFieldsIds.size && dAppConnectionRepository.dAppAuthorizedPersonaHasAllDataFields(
            dApp.dAppDefinitionAddress,
            personaAddress,
            requestedFieldsIds
        )
    }
}
