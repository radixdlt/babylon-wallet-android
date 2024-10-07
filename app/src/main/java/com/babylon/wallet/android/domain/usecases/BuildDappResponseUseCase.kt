package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.model.toWalletToDappInteractionPersonaDataRequestResponseItem
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.model.messages.WalletUnauthorizedRequest
import com.babylon.wallet.android.domain.model.signing.SignPurpose
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.babylon.wallet.android.domain.usecases.signing.ROLAClient
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DappWalletInteractionPersona
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.Slip10Curve
import com.radixdlt.sargon.WalletInteractionWalletAccount
import com.radixdlt.sargon.WalletToDappInteractionAccountProof
import com.radixdlt.sargon.WalletToDappInteractionAccountsRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionAuthLoginWithChallengeRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionAuthLoginWithoutChallengeRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionAuthProof
import com.radixdlt.sargon.WalletToDappInteractionAuthRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionAuthUsePersonaRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionAuthorizedRequestResponseItems
import com.radixdlt.sargon.WalletToDappInteractionResponse
import com.radixdlt.sargon.WalletToDappInteractionResponseItems
import com.radixdlt.sargon.WalletToDappInteractionSuccessResponse
import com.radixdlt.sargon.WalletToDappInteractionUnauthorizedRequestResponseItems
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.publicKey
import com.radixdlt.sargon.extensions.signature
import javax.inject.Inject

open class BuildDappResponseUseCase(private val accessFactorSourcesProxy: AccessFactorSourcesProxy) {

    protected suspend fun buildAccountsResponseItem(
        request: DappToWalletInteraction,
        accounts: List<Account>,
        challenge: Exactly32Bytes?,
        entitiesWithSignatures: Map<ProfileEntity, SignatureWithPublicKey>,
    ): Result<WalletToDappInteractionAccountsRequestResponseItem?> {
        if (accounts.isEmpty()) {
            return Result.success(null)
        }

        var allEntitiesWithSignatures: Map<ProfileEntity, SignatureWithPublicKey> = entitiesWithSignatures
        if (challenge != null && entitiesWithSignatures.isEmpty()) {
            val signRequest = SignRequest.SignAuthChallengeRequest(
                challengeHex = challenge.hex,
                origin = request.metadata.origin,
                dAppDefinitionAddress = request.metadata.dAppDefinitionAddress
            )

            accessFactorSourcesProxy.getSignatures(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToGetSignatures(
                    signPurpose = SignPurpose.SignAuth,
                    signers = accounts.map { it.asProfileEntity() },
                    signRequest = signRequest
                )
            ).onSuccess {
                allEntitiesWithSignatures = allEntitiesWithSignatures.toMutableMap().apply {
                    putAll(it.signersWithSignatures)
                }
            }
        }

        var accountProofs: List<WalletToDappInteractionAccountProof>? = null
        if (challenge != null) {
            accountProofs = accounts.map { account ->
                val signatureForAccount = allEntitiesWithSignatures[account.asProfileEntity()]
                    ?: return Result.failure(RadixWalletException.DappRequestException.FailedToSignAuthChallenge())

                WalletToDappInteractionAccountProof(
                    accountAddress = account.address,
                    proof = WalletToDappInteractionAuthProof(
                        publicKey = signatureForAccount.publicKey,
                        curve = when (signatureForAccount.publicKey) {
                            is PublicKey.Ed25519 -> Slip10Curve.CURVE25519
                            is PublicKey.Secp256k1 -> Slip10Curve.SECP256K1
                        },
                        signature = signatureForAccount.signature
                    )
                )
            }
        }

        val accountsResponses = accounts.map { account ->
            WalletInteractionWalletAccount(
                address = account.address,
                label = account.displayName,
                appearanceId = account.appearanceId
            )
        }

        return Result.success(
            WalletToDappInteractionAccountsRequestResponseItem(
                accounts = accountsResponses,
                challenge = challenge,
                proofs = accountProofs
            )
        )
    }
}

class BuildAuthorizedDappResponseUseCase @Inject constructor(
    private val rolaClient: ROLAClient,
    accessFactorSourcesProxy: AccessFactorSourcesProxy
) : BuildDappResponseUseCase(accessFactorSourcesProxy = accessFactorSourcesProxy) {

    @Suppress("LongParameterList", "ReturnCount", "LongMethod")
    suspend operator fun invoke(
        request: WalletAuthorizedRequest,
        selectedPersona: Persona,
        oneTimeAccounts: List<Account>,
        ongoingAccounts: List<Account>,
        ongoingSharedPersonaData: PersonaData? = null,
        onetimeSharedPersonaData: PersonaData? = null
    ): Result<WalletToDappInteractionResponse> {
        var entitiesWithSignatures: Map<ProfileEntity, SignatureWithPublicKey> = emptyMap()

        val dappInteractionPersona = DappWalletInteractionPersona(
            identityAddress = selectedPersona.address,
            label = selectedPersona.displayName.value
        )

        val authResponseItem: Result<WalletToDappInteractionAuthRequestResponseItem> =
            when (val authRequest = request.authRequestItem) {
                is WalletAuthorizedRequest.AuthRequestItem.LoginRequest.WithChallenge -> {
                    var response: Result<WalletToDappInteractionAuthRequestResponseItem> = Result.failure(
                        RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
                    )
                    getSignaturesForAllEntities(
                        challenge = authRequest.challenge,
                        metadata = request.metadata,
                        entities = getAccountsWithSignatureRequired(
                            request = request,
                            ongoingAccounts = ongoingAccounts,
                            oneTimeAccounts = oneTimeAccounts
                        ) + selectedPersona.asProfileEntity()
                    ).onSuccess { entitiesWithSignaturesResult ->
                        entitiesWithSignatures = entitiesWithSignaturesResult

                        val signatureForPersona = entitiesWithSignatures[selectedPersona.asProfileEntity()]
                            ?: return Result.failure(RadixWalletException.DappRequestException.FailedToSignAuthChallenge())

                        response = Result.success(
                            WalletToDappInteractionAuthRequestResponseItem.LoginWithChallenge(
                                v1 = WalletToDappInteractionAuthLoginWithChallengeRequestResponseItem(
                                    persona = dappInteractionPersona,
                                    challenge = authRequest.challenge,
                                    proof = WalletToDappInteractionAuthProof(
                                        publicKey = signatureForPersona.publicKey,
                                        curve = when (signatureForPersona.publicKey) {
                                            is PublicKey.Ed25519 -> Slip10Curve.CURVE25519
                                            is PublicKey.Secp256k1 -> Slip10Curve.SECP256K1
                                        },
                                        signature = signatureForPersona.signature
                                    )
                                )
                            )
                        )
                    }.onFailure {
                        response = Result.failure(RadixWalletException.DappRequestException.FailedToSignAuthChallenge(it))
                    }
                    response
                }

                WalletAuthorizedRequest.AuthRequestItem.LoginRequest.WithoutChallenge -> {
                    Result.success(
                        WalletToDappInteractionAuthRequestResponseItem.LoginWithoutChallenge(
                            v1 = WalletToDappInteractionAuthLoginWithoutChallengeRequestResponseItem(
                                persona = dappInteractionPersona
                            )
                        )
                    )
                }

                is WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest -> {
                    Result.success(
                        WalletToDappInteractionAuthRequestResponseItem.UsePersona(
                            v1 = WalletToDappInteractionAuthUsePersonaRequestResponseItem(
                                persona = dappInteractionPersona
                            )
                        )
                    )
                }
            }

        if (authResponseItem.isSuccess) {
            val oneTimeAccountsResponseItem = buildAccountsResponseItem(
                request = request,
                accounts = oneTimeAccounts,
                challenge = request.oneTimeAccountsRequestItem?.challenge,
                entitiesWithSignatures = entitiesWithSignatures
            )
            if (oneTimeAccountsResponseItem.isFailure) {
                return Result.failure(
                    exception = oneTimeAccountsResponseItem.exceptionOrNull()
                        ?: RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
                )
            }

            val ongoingAccountsResponseItem = buildAccountsResponseItem(
                request = request,
                accounts = ongoingAccounts,
                challenge = request.ongoingAccountsRequestItem?.challenge,
                entitiesWithSignatures = entitiesWithSignatures
            )
            if (ongoingAccountsResponseItem.isFailure) {
                return Result.failure(
                    exception = ongoingAccountsResponseItem.exceptionOrNull()
                        ?: RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
                )
            }

            return Result.success(
                WalletToDappInteractionResponse.Success(
                    v1 = WalletToDappInteractionSuccessResponse(
                        interactionId = request.interactionId,
                        items = WalletToDappInteractionResponseItems.AuthorizedRequest(
                            v1 = WalletToDappInteractionAuthorizedRequestResponseItems(
                                auth = authResponseItem.getOrThrow(),
                                oneTimeAccounts = oneTimeAccountsResponseItem.getOrNull(),
                                ongoingAccounts = ongoingAccountsResponseItem.getOrNull(),
                                ongoingPersonaData = ongoingSharedPersonaData?.toWalletToDappInteractionPersonaDataRequestResponseItem(),
                                oneTimePersonaData = onetimeSharedPersonaData?.toWalletToDappInteractionPersonaDataRequestResponseItem()
                            ),
                        )
                    )
                )
            )
        } else {
            return Result.failure(
                exception = authResponseItem.exceptionOrNull()
                    ?: RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
            )
        }
    }

    private fun getAccountsWithSignatureRequired(
        request: WalletAuthorizedRequest,
        ongoingAccounts: List<Account>,
        oneTimeAccounts: List<Account>
    ): List<ProfileEntity.AccountEntity> {
        val ongoingAccountsToSign = request.ongoingAccountsRequestItem?.challenge?.let {
            ongoingAccounts.map { account -> account.asProfileEntity() }
        }.orEmpty()

        val onetimeAccountsToSign = request.ongoingAccountsRequestItem?.challenge?.let {
            oneTimeAccounts.map { account -> account.asProfileEntity() }
        }.orEmpty()

        return onetimeAccountsToSign + ongoingAccountsToSign
    }

    private suspend fun getSignaturesForAllEntities(
        challenge: Exactly32Bytes,
        metadata: DappToWalletInteraction.RequestMetadata,
        entities: List<ProfileEntity>
    ): Result<Map<ProfileEntity, SignatureWithPublicKey>> {
        val signRequest = SignRequest.SignAuthChallengeRequest(
            challengeHex = challenge.hex,
            origin = metadata.origin,
            dAppDefinitionAddress = metadata.dAppDefinitionAddress
        )

        return rolaClient.signAuthChallenge(
            signRequest = signRequest,
            entities = entities
        )
    }
}

class BuildUnauthorizedDappResponseUseCase @Inject constructor(
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy
) : BuildDappResponseUseCase(accessFactorSourcesProxy = accessFactorSourcesProxy) {

    @Suppress("LongParameterList", "ReturnCount")
    suspend operator fun invoke(
        request: WalletUnauthorizedRequest,
        oneTimeAccounts: List<Account> = emptyList(),
        oneTimePersonaData: PersonaData? = null
    ): Result<WalletToDappInteractionResponse> {
        var entitiesWithSignatures: Map<ProfileEntity, SignatureWithPublicKey> = emptyMap()

        if (request.oneTimeAccountsRequestItem?.challenge != null) {
            val signRequest = SignRequest.SignAuthChallengeRequest(
                challengeHex = request.oneTimeAccountsRequestItem.challenge.hex,
                origin = request.metadata.origin,
                dAppDefinitionAddress = request.metadata.dAppDefinitionAddress
            )

            accessFactorSourcesProxy.getSignatures(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToGetSignatures(
                    signPurpose = SignPurpose.SignAuth,
                    signers = oneTimeAccounts.map { it.asProfileEntity() },
                    signRequest = signRequest
                )
            ).onSuccess { result ->
                entitiesWithSignatures = result.signersWithSignatures
            }.onFailure { error ->
                return Result.failure(RadixWalletException.DappRequestException.FailedToSignAuthChallenge(error))
            }
        }

        val oneTimeAccountsResponseItem = buildAccountsResponseItem(
            request = request,
            accounts = oneTimeAccounts,
            challenge = request.oneTimeAccountsRequestItem?.challenge,
            entitiesWithSignatures = entitiesWithSignatures
        )
        if (oneTimeAccountsResponseItem.isFailure) {
            return Result.failure(
                exception = oneTimeAccountsResponseItem.exceptionOrNull()
                    ?: RadixWalletException.DappRequestException.FailedToSignAuthChallenge()
            )
        }
        return Result.success(
            WalletToDappInteractionResponse.Success(
                v1 = WalletToDappInteractionSuccessResponse(
                    interactionId = request.interactionId,
                    items = WalletToDappInteractionResponseItems.UnauthorizedRequest(
                        v1 = WalletToDappInteractionUnauthorizedRequestResponseItems(
                            oneTimeAccounts = oneTimeAccountsResponseItem.getOrNull(),
                            oneTimePersonaData = oneTimePersonaData?.toWalletToDappInteractionPersonaDataRequestResponseItem()
                        ),
                    )
                )
            )
        )
    }
}
