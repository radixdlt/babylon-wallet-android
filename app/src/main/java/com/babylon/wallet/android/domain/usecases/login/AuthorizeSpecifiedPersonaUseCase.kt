package com.babylon.wallet.android.domain.usecases.login

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.presentation.model.getPersonaDataForFieldKinds
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.AuthorizedPersonaSimple
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import rdx.works.core.TimestampGenerator
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.updateAuthorizedDAppPersonas
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

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
    private val respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val buildAuthorizedDappResponseUseCase: BuildAuthorizedDappResponseUseCase
) {

    @Suppress("ReturnCount", "NestedBlockDepth", "LongMethod")
    suspend operator fun invoke(dappToWalletInteraction: DappToWalletInteraction): Result<DAppData> {
        var operationResult: Result<DAppData> = Result.failure(
            RadixWalletException.DappRequestException.NotPossibleToAuthenticateAutomatically
        )

        (dappToWalletInteraction as? WalletAuthorizedRequest)?.let { request ->
            (request.authRequestItem as? WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest)?.let {
                val authorizedDapp = dAppConnectionRepository.getAuthorizedDApp(
                    dAppDefinitionAddress = AccountAddress.init(request.metadata.dAppDefinitionAddress)
                )
                if (authorizedDapp == null) {
                    respondWithInvalidPersona(dappToWalletInteraction)
                    return Result.failure(RadixWalletException.DappRequestException.InvalidPersona)
                }
                if (dappToWalletInteraction.needSignatures() || dappToWalletInteraction.isMobileConnectRequest) {
                    return operationResult
                }
                val authorizedPersonaSimple = authorizedDapp
                    .referencesToAuthorizedPersonas
                    .firstOrNull { authorizedPersonaSimple ->
                        authorizedPersonaSimple.identityAddress.string ==
                            (request.authRequestItem as? WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest)?.identityAddress?.string
                    }
                if (authorizedPersonaSimple == null) {
                    respondWithInvalidPersona(dappToWalletInteraction)
                    return Result.failure(RadixWalletException.DappRequestException.InvalidPersona)
                }

                val persona = getProfileUseCase().activePersonaOnCurrentNetwork(
                    withAddress = authorizedPersonaSimple.identityAddress
                )
                if (persona == null) {
                    respondWithInvalidPersona(dappToWalletInteraction)
                    return Result.failure(RadixWalletException.DappRequestException.InvalidPersona)
                }

                if (request.hasOngoingRequestItemsOnly()) {
                    if (request.needSignatures()) {
                        return@let
                    }
                    val hasOngoingAccountsRequest = request.ongoingAccountsRequestItem != null
                    val hasOngoingPersonaDataRequest = request.ongoingPersonaDataRequestItem != null
                    val selectedAccounts: List<Selectable<Account>> = emptyList()
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
                                DAppData(interactionId = request.interactionId, name = dAppName)
                            }
                        }

                        hasOngoingPersonaDataRequest -> {
                            selectedPersonaData = getAlreadyGrantedPersonaData(
                                request = request,
                                authorizedDApp = authorizedDapp,
                                authorizedPersonaSimple = authorizedPersonaSimple
                            )
                            if (selectedPersonaData != null) {
                                val result = sendSuccessResponse(
                                    request = request,
                                    persona = persona,
                                    selectedAccounts = selectedAccounts,
                                    selectedPersonaData = selectedPersonaData,
                                    authorizedDApp = authorizedDapp
                                )
                                operationResult = result.map { dAppName ->
                                    DAppData(interactionId = request.interactionId, name = dAppName)
                                }
                            }
                        }
                    }
                }
            }
        }
        return operationResult
    }

    private suspend fun respondWithInvalidPersona(walletAuthorizedRequest: WalletAuthorizedRequest) {
        respondToIncomingRequestUseCase.respondWithFailure(
            request = walletAuthorizedRequest,
            dappWalletInteractionErrorType = DappWalletInteractionErrorType.INVALID_PERSONA
        )
    }

    private suspend fun handleOngoingAccountsRequest(
        request: WalletAuthorizedRequest,
        authorizedDapp: AuthorizedDapp,
        authorizedPersonaSimple: AuthorizedPersonaSimple,
        hasOngoingPersonaDataRequest: Boolean,
        persona: Persona
    ): Result<String?> {
        var operationResult: Result<String?> = Result.failure(
            exception = RadixWalletException.DappRequestException.NotPossibleToAuthenticateAutomatically
        )
        val selectedAccounts: List<Selectable<Account>> = getAccountsWithGrantedAccess(
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
                        authorizedDApp = authorizedDapp
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
                        authorizedDApp = authorizedDapp
                    )
                }
            }
        }
        return operationResult
    }

    private fun updateDAppPersonaWithLastUsedTimestamp(
        authorizedDApp: AuthorizedDapp,
        personaAddress: IdentityAddress
    ): AuthorizedDapp {
        val updatedDapp = authorizedDApp.updateAuthorizedDAppPersonas(
            authorizedDApp.referencesToAuthorizedPersonas.map { ref ->
                if (ref.identityAddress == personaAddress) {
                    ref.copy(lastLogin = TimestampGenerator())
                } else {
                    ref
                }
            }
        )
        return updatedDapp
    }

    private suspend fun sendSuccessResponse(
        request: WalletAuthorizedRequest,
        persona: Persona,
        selectedAccounts: List<Selectable<Account>>,
        selectedPersonaData: PersonaData?,
        authorizedDApp: AuthorizedDapp
    ): Result<String?> {
        return buildAuthorizedDappResponseUseCase(
            request = request,
            authorizedPersona = persona.asProfileEntity() to null,
            oneTimeAccountsWithSignatures = emptyMap(),
            ongoingAccountsWithSignatures = selectedAccounts.map { it.data }.associate { it.asProfileEntity() to null },
            ongoingSharedPersonaData = selectedPersonaData
        ).mapCatching { walletToDappInteractionResponse ->
            return respondToIncomingRequestUseCase.respondWithSuccess(
                request = request,
                response = walletToDappInteractionResponse
            ).getOrNull()?.let {
                val updatedDApp = updateDAppPersonaWithLastUsedTimestamp(authorizedDApp, persona.address)
                dAppConnectionRepository.updateOrCreateAuthorizedDApp(updatedDApp)
                Result.success(authorizedDApp.displayName)
            } ?: Result.failure(RadixWalletException.DappRequestException.InvalidRequest)
        }
    }

    private suspend fun getAlreadyGrantedPersonaData(
        request: WalletAuthorizedRequest,
        authorizedDApp: AuthorizedDapp,
        authorizedPersonaSimple: AuthorizedPersonaSimple
    ): PersonaData? {
        var result: PersonaData? = null
        val handledRequest = checkNotNull(request.ongoingPersonaDataRequestItem)
        val requiredFieldKinds = handledRequest.toRequiredFields()
        if (personaDataAccessAlreadyGranted(authorizedDApp, handledRequest, authorizedPersonaSimple.identityAddress)) {
            result = getProfileUseCase().activePersonaOnCurrentNetwork(
                authorizedPersonaSimple.identityAddress
            )?.getPersonaDataForFieldKinds(requiredFieldKinds.fields)
        }
        return result
    }

    private suspend fun getAccountsWithGrantedAccess(
        request: WalletAuthorizedRequest,
        authorizedDApp: AuthorizedDapp,
        authorizedPersonaSimple: AuthorizedPersonaSimple
    ): List<Selectable<Account>> {
        var result: List<Selectable<Account>> = emptyList()
        val handledRequest = checkNotNull(request.ongoingAccountsRequestItem)
        if (request.resetRequestItem?.personaData != true && request.resetRequestItem?.accounts != true) {
            val potentialOngoingAddresses = dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                authorizedDApp.dappDefinitionAddress,
                authorizedPersonaSimple.identityAddress,
                handledRequest.numberOfValues.quantity,
                handledRequest.numberOfValues.toRequestedNumberQuantifier()
            )
            if (potentialOngoingAddresses.isNotEmpty()) {
                result = potentialOngoingAddresses.mapNotNull { address ->
                    getProfileUseCase().activeAccountOnCurrentNetwork(withAddress = address)?.let { Selectable(it) }
                }
            }
        }
        return result
    }

    private suspend fun personaDataAccessAlreadyGranted(
        dApp: AuthorizedDapp,
        requestItem: DappToWalletInteraction.PersonaDataRequestItem,
        personaAddress: IdentityAddress
    ): Boolean {
        val requestedFieldKinds = requestItem.toRequiredFields()
        return dAppConnectionRepository.dAppAuthorizedPersonaHasAllDataFields(
            dApp.dappDefinitionAddress,
            personaAddress,
            requestedFieldKinds.fields.associate { it.kind to it.numberOfValues.quantity }
        )
    }
}

data class DAppData(
    val interactionId: WalletInteractionId,
    val name: String?
)
