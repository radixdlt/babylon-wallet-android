package com.babylon.wallet.android.domain.usecases.login

import com.babylon.wallet.android.data.dapp.model.toWalletToDappInteractionAuthProof
import com.babylon.wallet.android.data.dapp.model.toWalletToDappInteractionPersonaDataRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.toWalletToDappInteractionProofOfOwnershipRequestResponseItem
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.model.messages.WalletUnauthorizedRequest
import com.radixdlt.sargon.DappWalletInteractionPersona
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.WalletInteractionWalletAccount
import com.radixdlt.sargon.WalletToDappInteractionAccountProof
import com.radixdlt.sargon.WalletToDappInteractionAccountsRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionAuthLoginWithChallengeRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionAuthLoginWithoutChallengeRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionAuthRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionAuthUsePersonaRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionAuthorizedRequestResponseItems
import com.radixdlt.sargon.WalletToDappInteractionResponse
import com.radixdlt.sargon.WalletToDappInteractionResponseItems
import com.radixdlt.sargon.WalletToDappInteractionSuccessResponse
import com.radixdlt.sargon.WalletToDappInteractionUnauthorizedRequestResponseItems
import com.radixdlt.sargon.extensions.ProfileEntity
import javax.inject.Inject

class BuildAuthorizedDappResponseUseCase @Inject constructor() {

    @Suppress("LongParameterList", "LongMethod")
    operator fun invoke(
        request: WalletAuthorizedRequest,
        authorizedPersona: Pair<ProfileEntity.PersonaEntity, SignatureWithPublicKey?>,
        oneTimeAccountsWithSignatures: Map<ProfileEntity.AccountEntity, SignatureWithPublicKey?> = emptyMap(),
        ongoingAccountsWithSignatures: Map<ProfileEntity.AccountEntity, SignatureWithPublicKey?> = emptyMap(),
        ongoingSharedPersonaData: PersonaData? = null,
        onetimeSharedPersonaData: PersonaData? = null,
        verifiedEntities: Map<ProfileEntity, SignatureWithPublicKey> = emptyMap()
    ): Result<WalletToDappInteractionResponse> {
        val dappWalletInteractionPersona = DappWalletInteractionPersona(
            identityAddress = authorizedPersona.first.identityAddress,
            label = authorizedPersona.first.persona.displayName.value
        )

        val authResponseItem = when (val authRequest = request.authRequestItem) {
            is WalletAuthorizedRequest.AuthRequestItem.LoginRequest.WithChallenge -> {
                val signatureForAuthorizedPersona = authorizedPersona.second

                if (signatureForAuthorizedPersona == null) {
                    return Result.failure(RadixWalletException.DappRequestException.FailedToSignAuthChallenge())
                } else {
                    WalletToDappInteractionAuthRequestResponseItem.LoginWithChallenge(
                        v1 = WalletToDappInteractionAuthLoginWithChallengeRequestResponseItem(
                            persona = dappWalletInteractionPersona,
                            challenge = authRequest.challenge,
                            proof = signatureForAuthorizedPersona.toWalletToDappInteractionAuthProof()
                        )
                    )
                }
            }

            WalletAuthorizedRequest.AuthRequestItem.LoginRequest.WithoutChallenge -> {
                WalletToDappInteractionAuthRequestResponseItem.LoginWithoutChallenge(
                    v1 = WalletToDappInteractionAuthLoginWithoutChallengeRequestResponseItem(
                        persona = dappWalletInteractionPersona
                    )
                )
            }

            is WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest -> {
                WalletToDappInteractionAuthRequestResponseItem.UsePersona(
                    v1 = WalletToDappInteractionAuthUsePersonaRequestResponseItem(
                        persona = dappWalletInteractionPersona
                    )
                )
            }
        }

        val oneTimeAccountsResponseItem = buildAccountsRequestResponseItem(
            accountsWithSignatures = oneTimeAccountsWithSignatures,
            challenge = request.oneTimeAccountsRequestItem?.challenge
        )

        val ongoingAccountsResponseItem = buildAccountsRequestResponseItem(
            accountsWithSignatures = ongoingAccountsWithSignatures,
            challenge = request.ongoingAccountsRequestItem?.challenge
        )

        return Result.success(
            WalletToDappInteractionResponse.Success(
                v1 = WalletToDappInteractionSuccessResponse(
                    interactionId = request.interactionId,
                    items = WalletToDappInteractionResponseItems.AuthorizedRequest(
                        v1 = WalletToDappInteractionAuthorizedRequestResponseItems(
                            auth = authResponseItem,
                            oneTimeAccounts = oneTimeAccountsResponseItem,
                            ongoingAccounts = ongoingAccountsResponseItem,
                            ongoingPersonaData = ongoingSharedPersonaData?.toWalletToDappInteractionPersonaDataRequestResponseItem(),
                            oneTimePersonaData = onetimeSharedPersonaData?.toWalletToDappInteractionPersonaDataRequestResponseItem(),
                            proofOfOwnership = request.proofOfOwnershipRequestItem?.let {
                                verifiedEntities.toWalletToDappInteractionProofOfOwnershipRequestResponseItem(it.challenge)
                            }
                        ),
                    )
                )
            )
        )
    }
}

class BuildUnauthorizedDappResponseUseCase @Inject constructor() {

    operator fun invoke(
        request: WalletUnauthorizedRequest,
        oneTimeAccountsWithSignatures: Map<ProfileEntity.AccountEntity, SignatureWithPublicKey?> = emptyMap(),
        oneTimePersonaData: PersonaData? = null
    ): WalletToDappInteractionResponse {
        val oneTimeAccountsResponseItem = buildAccountsRequestResponseItem(
            accountsWithSignatures = oneTimeAccountsWithSignatures,
            challenge = request.oneTimeAccountsRequestItem?.challenge
        )

        return WalletToDappInteractionResponse.Success(
            v1 = WalletToDappInteractionSuccessResponse(
                interactionId = request.interactionId,
                items = WalletToDappInteractionResponseItems.UnauthorizedRequest(
                    v1 = WalletToDappInteractionUnauthorizedRequestResponseItems(
                        oneTimeAccounts = oneTimeAccountsResponseItem,
                        oneTimePersonaData = oneTimePersonaData?.toWalletToDappInteractionPersonaDataRequestResponseItem()
                    ),
                )
            )
        )
    }
}

private fun buildAccountsRequestResponseItem(
    accountsWithSignatures: Map<ProfileEntity.AccountEntity, SignatureWithPublicKey?>,
    challenge: Exactly32Bytes?
): WalletToDappInteractionAccountsRequestResponseItem? {
    @Suppress("UnsafeCallOnNullableType")
    return if (accountsWithSignatures.isNotEmpty()) {
        val accountsWithProofs = if (accountsWithSignatures.all { it.value != null }) {
            accountsWithSignatures.map { (accountEntity, signatureWithPublicKey) ->
                WalletToDappInteractionAccountProof(
                    accountAddress = accountEntity.accountAddress,
                    proof = signatureWithPublicKey!!.toWalletToDappInteractionAuthProof()
                )
            }
        } else {
            null
        }

        val walletAccounts = accountsWithSignatures.keys.map { accountEntity ->
            WalletInteractionWalletAccount(
                address = accountEntity.accountAddress,
                label = accountEntity.account.displayName,
                appearanceId = accountEntity.account.appearanceId
            )
        }

        WalletToDappInteractionAccountsRequestResponseItem(
            accounts = walletAccounts,
            challenge = challenge,
            proofs = accountsWithProofs
        )
    } else {
        null
    }
}
