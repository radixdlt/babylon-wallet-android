package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
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

    suspend operator fun invoke(request: MessageFromDataChannel.IncomingRequest): com.babylon.wallet.android.domain.common.Result<String> =
        coroutineScope {
            var operationResult: com.babylon.wallet.android.domain.common.Result<String> =
                com.babylon.wallet.android.domain.common.Result.Error()
            if (request is MessageFromDataChannel.IncomingRequest.AuthorizedRequest
                && request.authRequest is MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest
            ) {
                val dappDefinitionAddress = request.metadata.dAppDefinitionAddress
                val authRequest = request.authRequest
                val connectedDapp = dAppConnectionRepository.getConnectedDapp(dappDefinitionAddress)
                val authorizedPersonaSimple =
                    connectedDapp?.referencesToAuthorizedPersonas?.firstOrNull {
                        it.identityAddress == authRequest.personaAddress
                    }
                if (connectedDapp != null && authorizedPersonaSimple != null) {
                    val selectedAccounts = authorizedPersonaSimple
                        .sharedAccounts
                        .accountsReferencedByAddress
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
                        ongoingAccounts = selectedAccounts

                    )
                    when (result) {
                        is com.babylon.wallet.android.domain.common.Result.Success -> {
                            operationResult = com.babylon.wallet.android.domain.common.Result.Success(
                                connectedDapp.displayName
                            )
                        }
                        else -> {}
                    }
                } else {
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        request.requestId,
                        error = WalletErrorType.RejectedByUser
                    )
                }
            }
            operationResult
        }
}
