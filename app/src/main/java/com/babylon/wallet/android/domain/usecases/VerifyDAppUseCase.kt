package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepository
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.asRadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.domain.toConnectorExtensionError
import com.babylon.wallet.android.utils.isValidHttpsUrl
import com.radixdlt.ret.Address
import kotlinx.coroutines.flow.first
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.security
import javax.inject.Inject

class VerifyDAppUseCase @Inject constructor(
    private val stateRepository: StateRepository,
    private val wellKnownDAppDefinitionRepository: WellKnownDAppDefinitionRepository,
    private val dAppMessenger: DappMessenger,
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(request: IncomingRequest): Result<Boolean> {
        val developerMode = getProfileUseCase.security.first().isDeveloperModeEnabled
        val decodeResult = runCatching { Address(request.metadata.dAppDefinitionAddress) }
        if (decodeResult.isFailure) {
            dAppMessenger.sendWalletInteractionResponseFailure(
                remoteConnectorId = request.remoteConnectorId,
                requestId = request.id,
                error = WalletErrorType.InvalidRequest
            )
            return Result.failure(RadixWalletException.DappRequestException.InvalidRequest)
        }
        return if (developerMode) {
            Result.success(true)
        } else {
            validateTwoWayLink(
                origin = request.metadata.origin,
                dAppDefinitionAddress = request.metadata.dAppDefinitionAddress
            ).onFailure { error ->
                error.asRadixWalletException()?.let { radixWalletException ->
                    val walletErrorType = radixWalletException.toConnectorExtensionError() ?: return@let
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        remoteConnectorId = request.remoteConnectorId,
                        requestId = request.id,
                        error = walletErrorType,
                        message = radixWalletException.getDappMessage()
                    )
                }
            }
        }
    }

    private suspend fun validateTwoWayLink(
        origin: String,
        dAppDefinitionAddress: String
    ): Result<Boolean> = if (origin.isValidHttpsUrl()) {
        stateRepository.getDAppsDetails(
            definitionAddresses = listOf(dAppDefinitionAddress),
            skipCache = true
        ).mapCatching { dApps ->
            dApps.first()
        }.then {
            it.verify(origin)
        }
    } else {
        Result.failure(RadixWalletException.DappVerificationException.UnknownWebsite)
    }

    private suspend fun DApp.verify(origin: String) = when {
        !isDappDefinition -> Result.failure(RadixWalletException.DappVerificationException.WrongAccountType)
        !isRelatedWith(origin) -> Result.failure(RadixWalletException.DappVerificationException.UnknownWebsite)
        else ->
            wellKnownDAppDefinitionRepository
                .getWellKnownDAppDefinitions(origin)
                .map { dAppDefinitions -> dAppDefinitions.contains(dAppAddress) }
    }
}
