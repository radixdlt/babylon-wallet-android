package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.os.driver.BiometricsHandler
import timber.log.Timber
import javax.inject.Inject

/**
 * This use case provides the view models a way to ask for biometrics without having to reference android context
 * Uses [BiometricsHandler] which is injected in [MainActivity] and handles authentication asynchronously.
 */
class BiometricsAuthenticateUseCase @Inject constructor(
    private val biometricsHandler: BiometricsHandler
) {

    suspend operator fun invoke() = asResult().isSuccess

    suspend fun asResult() = biometricsHandler.askForBiometrics().onFailure { error ->
        Timber.tag(TAG).w(error)
    }

    companion object {
        private const val TAG = "BiometricsHandler"
    }
}
