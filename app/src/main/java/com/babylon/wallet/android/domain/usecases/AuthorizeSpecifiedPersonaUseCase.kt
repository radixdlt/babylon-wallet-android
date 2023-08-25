package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AuthorizedRequest
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.PersonaRequestItem
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.model.toProfileShareAccountsQuantifier
import com.babylon.wallet.android.domain.model.toRequiredFields
import com.babylon.wallet.android.presentation.model.getPersonaDataForFieldKinds
import com.babylon.wallet.android.utils.toISO8601String
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.updateAuthorizedDappPersonas
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.personaOnCurrentNetwork
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.Result
import com.babylon.wallet.android.domain.common.Result as ResultInternal

/**
 * Purpose of this use case is to respond to dApp login request silently without showing dApp login flow.
 * This can happen if those are satisfied:
 * - request is of type AuthorizedRequest
 * - auth type is of type UsePersonaRequest
 * - there are only ongoing request items within that request, no reset item
 * - we have all the data already granted for ongoing request items that are part this request
 */
class AuthorizeSpecifiedPersonaUseCase @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val dAppMessenger: DappMessenger,
    private val getProfileUseCase: GetProfileUseCase,
    private val buildAuthorizedDappResponseUseCase: BuildAuthorizedDappResponseUseCase
) {

    @Suppress("ReturnCount", "NestedBlockDepth", "LongMethod")
    suspend operator fun invoke(incomingRequest: IncomingRequest): Result<DAppData> {
        var operationResult: Result<DAppData> = Result.failure(DappRequestFailure.InvalidRequest)
        (incomingRequest as? AuthorizedRequest)?.let { request ->
            (request.authRequest as? AuthorizedRequest.AuthRequest.UsePersonaRequest)?.let {
                val authorizedDapp = dAppConnectionRepository.getAuthorizedDapp(
                    dAppDefinitionAddress = request.metadata.dAppDefinitionAddress
                ) ?: return Result.failure(DappRequestFailure.InvalidRequest)

                val authorizedPersonaSimple = authorizedDapp
                    .referencesToAuthorizedPersonas
                    .firstOrNull { authorizedPersonaSimple ->
                        authorizedPersonaSimple.identityAddress ==
                            (request.authRequest as? AuthorizedRequest.AuthRequest.UsePersonaRequest)?.personaAddress
                    } ?: return Result.failure(DappRequestException(DappRequestFailure.InvalidPersona))

                val persona = getProfileUseCase.personaOnCurrentNetwork(
                    withAddress = authorizedPersonaSimple.identityAddress
                ) ?: return Result.failure(DappRequestFailure.InvalidRequest)

                if (request.hasOngoingRequestItemsOnly()) {
                    val hasOngoingAccountsRequest = request.ongoingAccountsRequestItem != null
                    val hasOngoingPersonaDataRequest = request.ongoingPersonaDataRequestItem != null
                    val selectedAccounts: List<Selectable<Network.Account>> = emptyList()
                    val selectedPersonaData: PersonaData?
                    when {
                        hasOngoingAccountsRequest -> {
                            val result = handleOngoingAccountsRequest(
                                request,
                                authorizedDapp,
                                authorizedPersonaSimple,
                                hasOngoingPersonaDataRequest,
                                persona
                            )
                            operationResult = result.map { dAppName ->
                                DAppData(requestId = request.id, name = dAppName)
                            }
                        }

                        hasOngoingPersonaDataRequest -> {
                            selectedPersonaData = getAlreadyGrantedPersonaData(
                                request = request,
                                authorizedDapp = authorizedDapp,
                                authorizedPersonaSimple = authorizedPersonaSimple
                            )
                            if (selectedPersonaData != null) {
                                val result = sendSuccessResponse(
                                    request = request,
                                    persona = persona,
                                    selectedAccounts = selectedAccounts,
                                    selectedPersonaData = selectedPersonaData,
                                    authorizedDapp = authorizedDapp
                                )
                                operationResult = result.map { dAppName ->
                                    DAppData(requestId = request.id, name = dAppName)
                                }
                            }
                        }
                    }
                }
            }
        }
        return operationResult
    }

    private suspend fun handleOngoingAccountsRequest(
        request: AuthorizedRequest,
        authorizedDapp: Network.AuthorizedDapp,
        authorizedPersonaSimple: Network.AuthorizedDapp.AuthorizedPersonaSimple,
        hasOngoingPersonaDataRequest: Boolean,
        persona: Network.Persona
    ): Result<String> {
        var operationResult: Result<String> = Result.failure(DappRequestFailure.InvalidRequest)
        val selectedAccounts: List<Selectable<Network.Account>> = getAccountsWithGrantedAccess(
            request,
            authorizedDapp,
            authorizedPersonaSimple
        )
        var selectedPersonaData: PersonaData? = null
        when {
            hasOngoingPersonaDataRequest -> {
                selectedPersonaData = getAlreadyGrantedPersonaData(request, authorizedDapp, authorizedPersonaSimple)
                if (selectedAccounts.isNotEmpty() && selectedPersonaData != null) {
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
                    ref.copy(lastLogin = LocalDateTime.now().toISO8601String())
                } else {
                    ref
                }
            }
        )
        return updatedDapp
    }

    private suspend fun sendSuccessResponse(
        request: AuthorizedRequest,
        persona: Network.Persona,
        selectedAccounts: List<Selectable<Network.Account>>,
        selectedPersonaData: PersonaData?,
        authorizedDapp: Network.AuthorizedDapp
    ): Result<String> {
        return buildAuthorizedDappResponseUseCase(
            request = request,
            selectedPersona = persona,
            oneTimeAccounts = emptyList(),
            ongoingAccounts = selectedAccounts.map { it.data },
            ongoingSharedPersonaData = selectedPersonaData
        ).mapCatching { response ->
            return when (
                dAppMessenger.sendWalletInteractionSuccessResponse(
                    remoteConnectorId = request.remoteConnectorId,
                    response = response
                )
            ) {
                is ResultInternal.Success -> {
                    val updatedDapp = updateDappPersonaWithLastUsedTimestamp(authorizedDapp, persona.address)
                    dAppConnectionRepository.updateOrCreateAuthorizedDApp(updatedDapp)
                    Result.success(
                        authorizedDapp.displayName
                    )
                }

                else -> Result.failure(DappRequestFailure.InvalidRequest)
            }
        }
    }

    private suspend fun getAlreadyGrantedPersonaData(
        request: AuthorizedRequest,
        authorizedDapp: Network.AuthorizedDapp,
        authorizedPersonaSimple: Network.AuthorizedDapp.AuthorizedPersonaSimple
    ): PersonaData? {
        var result: PersonaData? = null
        val handledRequest = checkNotNull(request.ongoingPersonaDataRequestItem)
        val requiredFieldKinds = handledRequest.toRequiredFields()
        if (personaDataAccessAlreadyGranted(authorizedDapp, handledRequest, authorizedPersonaSimple.identityAddress)) {
            result = getProfileUseCase.personaOnCurrentNetwork(
                authorizedPersonaSimple.identityAddress
            )?.getPersonaDataForFieldKinds(requiredFieldKinds.fields)
        }
        return result
    }

    private suspend fun getAccountsWithGrantedAccess(
        request: AuthorizedRequest,
        authorizedDapp: Network.AuthorizedDapp,
        authorizedPersonaSimple: Network.AuthorizedDapp.AuthorizedPersonaSimple
    ): List<Selectable<Network.Account>> {
        var result: List<Selectable<Network.Account>> = emptyList()
        val handledRequest = checkNotNull(request.ongoingAccountsRequestItem)
        if (request.resetRequestItem?.personaData != true && request.resetRequestItem?.accounts != true) {
            val potentialOngoingAddresses = dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                authorizedDapp.dAppDefinitionAddress,
                authorizedPersonaSimple.identityAddress,
                handledRequest.numberOfValues.quantity,
                handledRequest.numberOfValues.toProfileShareAccountsQuantifier()
            )
            if (potentialOngoingAddresses.isNotEmpty()) {
                result = potentialOngoingAddresses.mapNotNull { address ->
                    getProfileUseCase.accountOnCurrentNetwork(withAddress = address)?.let { Selectable(it) }
                }
            }
        }
        return result
    }

    private suspend fun personaDataAccessAlreadyGranted(
        dApp: Network.AuthorizedDapp,
        requestItem: PersonaRequestItem,
        personaAddress: String
    ): Boolean {
        val requestedFieldKinds = requestItem.toRequiredFields()
        return dAppConnectionRepository.dAppAuthorizedPersonaHasAllDataFields(
            dApp.dAppDefinitionAddress,
            personaAddress,
            requestedFieldKinds.fields.associate { it.kind to it.numberOfValues.quantity }
        )
    }
}

data class DAppData(
    val requestId: String,
    val name: String
)
