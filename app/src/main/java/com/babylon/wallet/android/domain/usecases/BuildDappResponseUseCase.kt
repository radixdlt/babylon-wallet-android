package com.babylon.wallet.android.domain.usecases

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
import com.babylon.wallet.android.data.dapp.model.WalletUnauthorizedRequestResponseItems
import com.babylon.wallet.android.data.dapp.model.toProof
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.ROLAClient
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AuthorizedRequest
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.babylon.wallet.android.presentation.model.toPersonaDataRequestResponseItem
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData
import javax.inject.Inject

open class BuildDappResponseUseCase(private val rolaClient: ROLAClient) {

    val signingState = rolaClient.signingState

    protected suspend fun buildAccountsResponseItem(
        request: MessageFromDataChannel.IncomingRequest,
        accounts: List<Network.Account>,
        challengeHex: String?,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean,
    ): Result<AccountsRequestResponseItem?> {
        if (accounts.isEmpty()) {
            return Result.success(null)
        }
        var accountProofs: List<AccountProof>? = null
        if (challengeHex != null) {
            accountProofs = accounts.mapIndexed { index, account ->
                // TODO this is hacky workaround to only ask for biometrics once - this will go away with MFA refactor
                val biometricAuthProvider: suspend () -> Boolean = if (index == 0) {
                    deviceBiometricAuthenticationProvider
                } else {
                    { true }
                }
                val signRequest = SignRequest.SignAuthChallengeRequest(
                    challengeHex,
                    request.metadata.origin,
                    request.metadata.dAppDefinitionAddress
                )
                val signatureWithPublicKey = rolaClient.signAuthChallenge(account, signRequest, biometricAuthProvider)
                if (signatureWithPublicKey.isFailure) {
                    return Result.failure(
                        signatureWithPublicKey.exceptionOrNull() ?: DappRequestFailure.FailedToSignAuthChallenge()
                    )
                }
                AccountProof(
                    account.address,
                    signatureWithPublicKey.getOrThrow().toProof(signRequest.dataToSign)
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

class BuildAuthorizedDappResponseUseCase @Inject constructor(
    private val rolaClient: ROLAClient
) : BuildDappResponseUseCase(rolaClient) {

    @Suppress("LongParameterList", "ReturnCount")
    suspend operator fun invoke(
        request: AuthorizedRequest,
        selectedPersona: Network.Persona,
        oneTimeAccounts: List<Network.Account>,
        ongoingAccounts: List<Network.Account>,
        ongoingSharedPersonaData: PersonaData? = null,
        onetimeSharedPersonaData: PersonaData? = null,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean = { true }
    ): Result<WalletInteractionResponse> {
        val loginWithChallenge = request.authRequest is AuthorizedRequest.AuthRequest.LoginRequest.WithChallenge
        val authResponse: Result<AuthRequestResponseItem> =
            buildAuthResponseItem(request, selectedPersona, deviceBiometricAuthenticationProvider)
        if (authResponse.isSuccess) {
            val authProvider = if (loginWithChallenge) {
                { true } // TODO this will go away with MFA, don't ask for biometrics twice
            } else {
                deviceBiometricAuthenticationProvider
            }
            val oneTimeAccountsResponseItem =
                buildAccountsResponseItem(
                    request,
                    oneTimeAccounts,
                    request.oneTimeAccountsRequestItem?.challenge?.value,
                    authProvider
                )
            if (oneTimeAccountsResponseItem.isFailure) {
                return Result.failure(oneTimeAccountsResponseItem.exceptionOrNull() ?: DappRequestFailure.FailedToSignAuthChallenge())
            }
            val ongoingAccountsResponseItem =
                buildAccountsResponseItem(
                    request,
                    ongoingAccounts,
                    request.ongoingAccountsRequestItem?.challenge?.value,
                    authProvider
                )
            if (ongoingAccountsResponseItem.isFailure) {
                return Result.failure(ongoingAccountsResponseItem.exceptionOrNull() ?: DappRequestFailure.FailedToSignAuthChallenge())
            }
            return Result.success(
                WalletInteractionSuccessResponse(
                    interactionId = request.interactionId,
                    items = WalletAuthorizedRequestResponseItems(
                        auth = authResponse.getOrThrow(),
                        oneTimeAccounts = oneTimeAccountsResponseItem.getOrNull(),
                        ongoingAccounts = ongoingAccountsResponseItem.getOrNull(),
                        ongoingPersonaData = ongoingSharedPersonaData?.toPersonaDataRequestResponseItem(),
                        oneTimePersonaData = onetimeSharedPersonaData?.toPersonaDataRequestResponseItem()
                    )
                )
            )
        } else {
            return Result.failure(authResponse.exceptionOrNull() ?: RadixWalletException.DappRequestException.FailedToSignAuthChallenge())
        }
    }

    private suspend fun buildAuthResponseItem(
        request: AuthorizedRequest,
        selectedPersona: Network.Persona,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ): Result<AuthRequestResponseItem> {
        val authResponse: Result<AuthRequestResponseItem> = when (val authRequest = request.authRequest) {
            is AuthorizedRequest.AuthRequest.LoginRequest.WithChallenge -> {
                var response: Result<AuthRequestResponseItem> = Result.failure(
                    RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
                )
                val signRequest = SignRequest.SignAuthChallengeRequest(
                    challengeHex = authRequest.challenge.value,
                    origin = request.metadata.origin,
                    dAppDefinitionAddress = request.metadata.dAppDefinitionAddress
                )
                rolaClient.signAuthChallenge(
                    selectedPersona,
                    signRequest,
                    deviceBiometricAuthenticationProvider
                ).onSuccess { signature ->
                    response = Result.success(
                        AuthLoginWithChallengeRequestResponseItem(
                            Persona(
                                selectedPersona.address,
                                selectedPersona.displayName
                            ),
                            authRequest.challenge.value,
                            signature.toProof(signRequest.dataToSign)
                        )
                    )
                }.onFailure {
                    response = Result.failure(it)
                }
                response
            }

            AuthorizedRequest.AuthRequest.LoginRequest.WithoutChallenge -> {
                Result.success(
                    AuthLoginWithoutChallengeRequestResponseItem(
                        Persona(
                            selectedPersona.address,
                            selectedPersona.displayName
                        )
                    )
                )
            }

            is AuthorizedRequest.AuthRequest.UsePersonaRequest -> {
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
}

class BuildUnauthorizedDappResponseUseCase @Inject constructor(
    rolaClient: ROLAClient
) : BuildDappResponseUseCase(rolaClient) {

    @Suppress("LongParameterList", "ReturnCount")
    suspend operator fun invoke(
        request: MessageFromDataChannel.IncomingRequest.UnauthorizedRequest,
        oneTimeAccounts: List<Network.Account> = emptyList(),
        onetimeSharedPersonaData: PersonaData? = null,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean = { true },
    ): Result<WalletInteractionResponse> {
        val oneTimeAccountsResponseItem =
            buildAccountsResponseItem(
                request = request,
                accounts = oneTimeAccounts,
                challengeHex = request.oneTimeAccountsRequestItem?.challenge?.value,
                deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
            )
        if (oneTimeAccountsResponseItem.isFailure) {
            return Result.failure(oneTimeAccountsResponseItem.exceptionOrNull() ?: DappRequestFailure.FailedToSignAuthChallenge())
        }
        return Result.success(
            WalletInteractionSuccessResponse(
                interactionId = request.interactionId,
                items = WalletUnauthorizedRequestResponseItems(
                    oneTimeAccounts = oneTimeAccountsResponseItem.getOrNull(),
                    oneTimePersonaData = onetimeSharedPersonaData?.toPersonaDataRequestResponseItem()
                )
            )
        )
    }
}
