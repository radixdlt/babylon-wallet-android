package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepository
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.asRadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.domain.model.IncomingMessage.IncomingRequest
import com.babylon.wallet.android.domain.toConnectorExtensionError
import com.babylon.wallet.android.utils.isValidHttpsUrl
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import kotlinx.coroutines.flow.first
import rdx.works.core.domain.DApp
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class VerifyDAppUseCase @Inject constructor(
    private val stateRepository: StateRepository,
    private val wellKnownDAppDefinitionRepository: WellKnownDAppDefinitionRepository,
    private val respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(request: IncomingRequest): Result<Boolean> {
        val dAppDefinitionAddress = runCatching { AccountAddress.init(request.metadata.dAppDefinitionAddress) }.getOrElse {
            respondToIncomingRequestUseCase.respondWithFailure(
                request = request,
                error = WalletErrorType.InvalidRequest
            )
            return Result.failure(RadixWalletException.DappRequestException.InvalidRequest)
        }

        val developerMode = getProfileUseCase().appPreferences.security.isDeveloperModeEnabled
        return if (developerMode) {
            Result.success(true)
        } else {
            validateTwoWayLink(
                origin = request.metadata.origin,
                dAppDefinitionAddress = dAppDefinitionAddress
            ).onFailure { error ->
                error.asRadixWalletException()?.let { radixWalletException ->
                    val walletErrorType = radixWalletException.toConnectorExtensionError() ?: return@let
                    respondToIncomingRequestUseCase.respondWithFailure(request, walletErrorType, radixWalletException.getDappMessage())
                }
            }
        }
    }

    private suspend fun validateTwoWayLink(
        origin: String,
        dAppDefinitionAddress: AccountAddress
    ): Result<Boolean> = if (origin.isValidHttpsUrl()) {
        stateRepository.getDAppsDetails(
            definitionAddresses = listOf(dAppDefinitionAddress),
            isRefreshing = true
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
                .getWellKnownDAppDefinitionAddresses(origin)
                .map { dAppDefinitions -> dAppDefinitions.contains(dAppAddress) }
    }
}
