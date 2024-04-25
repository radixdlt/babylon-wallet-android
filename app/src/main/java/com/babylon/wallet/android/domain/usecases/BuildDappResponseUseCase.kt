package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.model.AccountProof
import com.babylon.wallet.android.data.dapp.model.AccountsRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.AuthLoginWithChallengeRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.AuthLoginWithoutChallengeRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.AuthRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.AuthUsePersonaRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.WalletAuthorizedRequestResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.dapp.model.WalletUnauthorizedRequestResponseItems
import com.babylon.wallet.android.data.dapp.model.toProof
import com.babylon.wallet.android.data.transaction.ROLAClient
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AuthorizedRequest
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.babylon.wallet.android.presentation.model.toPersonaDataRequestResponseItem
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.string
import javax.inject.Inject

private typealias DAppPersona = com.babylon.wallet.android.data.dapp.model.Persona

open class BuildDappResponseUseCase(private val rolaClient: ROLAClient) {

    val signingState = rolaClient.signingState

    protected suspend fun buildAccountsResponseItem(
        request: MessageFromDataChannel.IncomingRequest,
        accounts: List<Account>,
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
                val signatureWithPublicKey =
                    rolaClient.signAuthChallenge(account.asProfileEntity(), signRequest, biometricAuthProvider)
                if (signatureWithPublicKey.isFailure) {
                    return Result.failure(
                        signatureWithPublicKey.exceptionOrNull() ?: RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
                    )
                }
                AccountProof(
                    account.address.string,
                    signatureWithPublicKey.getOrThrow().toProof()
                )
            }
        }

        val accountsResponses = accounts.map { account ->
            AccountsRequestResponseItem.Account(
                address = account.address.string,
                label = account.displayName.value,
                appearanceId = account.appearanceId.value.toInt()
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
        selectedPersona: Persona,
        oneTimeAccounts: List<Account>,
        ongoingAccounts: List<Account>,
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
                    request = request,
                    accounts = oneTimeAccounts,
                    challengeHex = request.oneTimeAccountsRequestItem?.challenge?.hex,
                    deviceBiometricAuthenticationProvider = authProvider
                )
            if (oneTimeAccountsResponseItem.isFailure) {
                return Result.failure(
                    oneTimeAccountsResponseItem.exceptionOrNull() ?: RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
                )
            }
            val ongoingAccountsResponseItem =
                buildAccountsResponseItem(
                    request = request,
                    accounts = ongoingAccounts,
                    challengeHex = request.ongoingAccountsRequestItem?.challenge?.hex,
                    deviceBiometricAuthenticationProvider = authProvider
                )
            if (ongoingAccountsResponseItem.isFailure) {
                return Result.failure(
                    ongoingAccountsResponseItem.exceptionOrNull() ?: RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
                )
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
        selectedPersona: Persona,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ): Result<AuthRequestResponseItem> {
        val authResponse: Result<AuthRequestResponseItem> = when (val authRequest = request.authRequest) {
            is AuthorizedRequest.AuthRequest.LoginRequest.WithChallenge -> {
                var response: Result<AuthRequestResponseItem> = Result.failure(
                    RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
                )
                val signRequest = SignRequest.SignAuthChallengeRequest(
                    challengeHex = authRequest.challenge.hex,
                    origin = request.metadata.origin,
                    dAppDefinitionAddress = request.metadata.dAppDefinitionAddress
                )
                rolaClient.signAuthChallenge(
                    selectedPersona.asProfileEntity(),
                    signRequest,
                    deviceBiometricAuthenticationProvider
                ).onSuccess { signature ->
                    response = Result.success(
                        AuthLoginWithChallengeRequestResponseItem(
                            persona = DAppPersona(
                                identityAddress = selectedPersona.address.string,
                                label = selectedPersona.displayName.value
                            ),
                            challenge = authRequest.challenge.hex,
                            proof = signature.toProof()
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
                        persona = DAppPersona(
                            identityAddress = selectedPersona.address.string,
                            label = selectedPersona.displayName.value
                        )
                    )
                )
            }

            is AuthorizedRequest.AuthRequest.UsePersonaRequest -> {
                Result.success(
                    AuthUsePersonaRequestResponseItem(
                        persona = DAppPersona(
                            identityAddress = selectedPersona.address.string,
                            label = selectedPersona.displayName.value
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
        oneTimeAccounts: List<Account> = emptyList(),
        onetimeSharedPersonaData: PersonaData? = null,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean = { true },
    ): Result<WalletInteractionResponse> {
        val oneTimeAccountsResponseItem =
            buildAccountsResponseItem(
                request = request,
                accounts = oneTimeAccounts,
                challengeHex = request.oneTimeAccountsRequestItem?.challenge?.hex,
                deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
            )
        if (oneTimeAccountsResponseItem.isFailure) {
            return Result.failure(
                oneTimeAccountsResponseItem.exceptionOrNull() ?: RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
            )
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
