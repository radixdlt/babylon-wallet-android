package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.domain.model.toProfileShareAccountsQuantifier
import com.babylon.wallet.android.presentation.dapp.account.toUiModel
import com.babylon.wallet.android.utils.toISO8601String
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.data.repository.AccountRepository
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.PersonaRepository
import java.time.LocalDateTime
import javax.inject.Inject

class AuthorizeSpecifiedPersonaUseCase @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val dAppMessenger: DAppMessenger,
    private val accountRepository: AccountRepository,
    private val personaRepository: PersonaRepository
) {

    @Suppress("LongMethod")
    suspend operator fun invoke(request: IncomingRequest): Result<String> =
        coroutineScope {
            var operationResult: Result<String> =
                Result.Error()
            if (request is IncomingRequest.AuthorizedRequest &&
                request.isUsePersonaWithOngoingAccountsOnly()
            ) {
                val ongoingRequest = checkNotNull(request.ongoingAccountsRequestItem)
                val dappDefinitionAddress = request.metadata.dAppDefinitionAddress
                val authRequest = request.authRequest as IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest
                val connectedDapp = dAppConnectionRepository.getConnectedDapp(
                    dappDefinitionAddress
                )
                val authorizedPersonaSimple =
                    connectedDapp?.referencesToAuthorizedPersonas?.firstOrNull {
                        it.identityAddress == authRequest.personaAddress
                    }
                if (connectedDapp != null && authorizedPersonaSimple != null) {
                    val potentialOngoingAddresses = dAppConnectionRepository.dAppConnectedPersonaAccountAddresses(
                        connectedDapp.dAppDefinitionAddress,
                        authorizedPersonaSimple.identityAddress,
                        ongoingRequest.numberOfAccounts,
                        ongoingRequest.quantifier.toProfileShareAccountsQuantifier()
                    )
                    if (potentialOngoingAddresses.isNotEmpty()) {
                        val selectedAccounts = potentialOngoingAddresses
                            .mapNotNull {
                                accountRepository.getAccountByAddress(it)?.toUiModel(true)
                            }
                        dAppConnectionRepository.updateConnectedDappPersonas(
                            connectedDapp.dAppDefinitionAddress,
                            connectedDapp.referencesToAuthorizedPersonas.map { ref ->
                                if (ref.identityAddress == authorizedPersonaSimple.identityAddress) {
                                    ref.copy(lastUsedOn = LocalDateTime.now().toISO8601String())
                                } else {
                                    ref
                                }
                            }
                        )
                        val persona = checkNotNull(
                            personaRepository.getPersonaByAddress(authorizedPersonaSimple.identityAddress)
                        )
                        val result = dAppMessenger.sendWalletInteractionSuccessResponse(
                            interactionId = request.requestId,
                            persona = persona,
                            usePersona = request.isUsePersonaAuth(),
                            ongoingAccounts = selectedAccounts
                        )
                        when (result) {
                            is Result.Success -> {
                                operationResult = Result.Success(
                                    connectedDapp.displayName
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
            operationResult
        }
}
