package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.dapp.model.AccountProof
import com.babylon.wallet.android.data.dapp.model.AccountsRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.AuthLoginWithChallengeRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.AuthLoginWithoutChallengeRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.AuthRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.AuthUsePersonaRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.Persona
import com.babylon.wallet.android.data.dapp.model.WalletAuthorizedRequestResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.dapp.model.toDataModel
import com.babylon.wallet.android.data.dapp.model.toProof
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

class BuildAuthorizedDappResponseUseCase @Inject constructor(
    private val rolaClient: ROLAClient
) {

    @Suppress("LongParameterList", "ReturnCount")
    suspend operator fun invoke(
        request: MessageFromDataChannel.IncomingRequest.AuthorizedRequest,
        selectedPersona: Network.Persona,
        oneTimeAccounts: List<Network.Account>,
        ongoingAccounts: List<Network.Account>,
        ongoingDataFields: List<Network.Persona.Field>,
        onetimeDataFields: List<Network.Persona.Field>
    ): Result<WalletInteractionResponse> {
        val authResponse: Result<AuthRequestResponseItem> = buildAuthResponseItem(request, selectedPersona)
        if (authResponse.isSuccess) {
            val oneTimeAccountsResponseItem =
                buildAccountsResponseItem(request, oneTimeAccounts, request.oneTimeAccountsRequestItem?.challenge)
            if (oneTimeAccountsResponseItem.isFailure) {
                return Result.failure(authResponse.exceptionOrNull() ?: DappRequestFailure.FailedToSignAuthChallenge())
            }
            val ongoingAccountsResponseItem =
                buildAccountsResponseItem(request, ongoingAccounts, request.ongoingAccountsRequestItem?.challenge)
            if (ongoingAccountsResponseItem.isFailure) {
                return Result.failure(authResponse.exceptionOrNull() ?: DappRequestFailure.FailedToSignAuthChallenge())
            }
            return Result.success(
                WalletInteractionSuccessResponse(
                    interactionId = request.interactionId,
                    items = WalletAuthorizedRequestResponseItems(
                        auth = authResponse.getOrThrow(),
                        oneTimeAccounts = oneTimeAccountsResponseItem.getOrNull(),
                        ongoingAccounts = ongoingAccountsResponseItem.getOrNull(),
                        ongoingPersonaData = ongoingDataFields.toDataModel(),
                        oneTimePersonaData = onetimeDataFields.toDataModel()
                    )
                )
            )
        } else {
            return Result.failure(authResponse.exceptionOrNull() ?: DappRequestFailure.FailedToSignAuthChallenge())
        }
    }

    private suspend fun buildAuthResponseItem(
        request: MessageFromDataChannel.IncomingRequest.AuthorizedRequest,
        selectedPersona: Network.Persona
    ): Result<AuthRequestResponseItem> {
        val authResponse: Result<AuthRequestResponseItem> = when (val authRequest = request.authRequest) {
            is MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest.WithChallenge -> {
                var response: Result<AuthRequestResponseItem> = Result.failure(DappRequestFailure.FailedToSignAuthChallenge())
                rolaClient.signAuthChallenge(
                    selectedPersona,
                    authRequest.challenge,
                    request.metadata.dAppDefinitionAddress,
                    request.metadata.origin
                ).onSuccess { signature ->
                    response = Result.success(
                        AuthLoginWithChallengeRequestResponseItem(
                            Persona(
                                selectedPersona.address,
                                selectedPersona.displayName
                            ),
                            authRequest.challenge,
                            signature.toProof(authRequest.challenge)
                        )
                    )
                }
                response
            }
            MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest.WithoutChallenge -> {
                Result.success(
                    AuthLoginWithoutChallengeRequestResponseItem(
                        Persona(
                            selectedPersona.address,
                            selectedPersona.displayName
                        )
                    )
                )
            }
            is MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest -> {
                Result.success(
                    AuthUsePersonaRequestResponseItem(
                        Persona(
                            selectedPersona.address,
                            selectedPersona.displayName
                        )
                    )
                )
            }
        }
        return authResponse
    }

    private suspend fun buildAccountsResponseItem(
        request: MessageFromDataChannel.IncomingRequest.AuthorizedRequest,
        accounts: List<Network.Account>,
        challengeHex: String?
    ): Result<AccountsRequestResponseItem?> {
        if (accounts.isEmpty()) {
            return Result.success(null)
        }
        var accountProofs: List<AccountProof>? = null
        if (challengeHex != null) {
            accountProofs = accounts.map { account ->
                val signatureWithPublicKey = rolaClient.signAuthChallenge(
                    account,
                    challengeHex,
                    request.metadata.dAppDefinitionAddress,
                    request.metadata.origin
                )
                if (signatureWithPublicKey.isFailure) return Result.failure(DappRequestFailure.FailedToSignAuthChallenge())
                AccountProof(
                    account.address,
                    signatureWithPublicKey.getOrThrow().toProof(challengeHex)
                )
            }
        }

        val accountsResponses = accounts.map { account ->
            AccountsRequestResponseItem.Account(
                address = account.address,
                label = account.displayName,
                appearanceId = account.appearanceID
            )
        }

        return Result.success(
            AccountsRequestResponseItem(
                accounts = accountsResponses,
                challenge = challengeHex,
                proofs = accountProofs
            )
        )
    }
}
