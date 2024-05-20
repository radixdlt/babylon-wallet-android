package com.babylon.wallet.android.presentation.ui.composables.actionableaddress

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.usecases.VerifyAddressOnLedgerUseCase
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.Profile
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.domain.ProfileState
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ActionableAddressViewEntryPointI {

    fun profileUseCase(): GetProfileUseCase

    fun verifyAddressOnLedgerUseCase(): VerifyAddressOnLedgerUseCase

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}

@ActivityRetainedScoped
class ActionableAddressViewEntryPoint @Inject constructor(
    @ApplicationContext context: Context
) : ActionableAddressViewEntryPointI {

    private val useCaseProvider = EntryPoints.get(context, ActionableAddressViewEntryPointI::class.java)

    override fun profileUseCase(): GetProfileUseCase {
        return useCaseProvider.profileUseCase()
    }

    override fun verifyAddressOnLedgerUseCase(): VerifyAddressOnLedgerUseCase {
        return useCaseProvider.verifyAddressOnLedgerUseCase()
    }

    override fun applicationScope(): CoroutineScope {
        return useCaseProvider.applicationScope()
    }
}

val actionableAddressViewEntryPointMock = object : ActionableAddressViewEntryPointI {
    override fun profileUseCase(): GetProfileUseCase {
        return fakeGetProfileUseCase()
    }

    override fun verifyAddressOnLedgerUseCase(): VerifyAddressOnLedgerUseCase {
        return VerifyAddressOnLedgerUseCase(fakeGetProfileUseCase(), ledgerMessengerFake())
    }

    override fun applicationScope(): CoroutineScope {
        return MainScope()
    }

    private fun fakeGetProfileUseCase(
        initialProfileState: ProfileState = ProfileState.NotInitialised
    ) = GetProfileUseCase(
        profileRepository = object : ProfileRepository {

            private val profileStateSource: MutableStateFlow<ProfileState> = MutableStateFlow(
                initialProfileState
            )

            override val profileState: Flow<ProfileState> = profileStateSource

            override val inMemoryProfileOrNull: Profile?
                get() = (profileStateSource.value as? ProfileState.Restored)?.profile

            override suspend fun saveProfile(profile: Profile) {
                profileStateSource.update { ProfileState.Restored(profile) }
            }

            override suspend fun clearProfileDataOnly() {
                profileStateSource.update { ProfileState.None }
            }

            override suspend fun clearAllWalletData() {
                profileStateSource.update { ProfileState.None }
            }

            override fun deriveProfileState(content: String): ProfileState {
                error("Not needed")
            }
        }
    )

    private fun ledgerMessengerFake() = object : LedgerMessenger {

        override suspend fun sendDeviceInfoRequest(
            interactionId: String
        ): Result<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse> {
            TODO("Not yet implemented")
        }

        override suspend fun signTransactionRequest(
            interactionId: String,
            hdPublicKeys: List<HierarchicalDeterministicPublicKey>,
            compiledTransactionIntent: String,
            ledgerDevice: LedgerInteractionRequest.LedgerDevice,
            displayHashOnLedgerDisplay: Boolean
        ): Result<MessageFromDataChannel.LedgerResponse.SignTransactionResponse> {
            TODO("Not yet implemented")
        }

        override suspend fun sendDerivePublicKeyRequest(
            interactionId: String,
            keyParameters: List<LedgerInteractionRequest.KeyParameters>,
            ledgerDevice: LedgerInteractionRequest.LedgerDevice
        ): Result<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse> {
            TODO("Not yet implemented")
        }

        override suspend fun signChallengeRequest(
            interactionId: String,
            hdPublicKeys: List<HierarchicalDeterministicPublicKey>,
            ledgerDevice: LedgerInteractionRequest.LedgerDevice,
            challengeHex: String,
            origin: String,
            dAppDefinitionAddress: String
        ): Result<MessageFromDataChannel.LedgerResponse.SignChallengeResponse> {
            TODO("Not yet implemented")
        }

        override suspend fun deriveAndDisplayAddressRequest(
            interactionId: String,
            keyParameters: LedgerInteractionRequest.KeyParameters,
            ledgerDevice: LedgerInteractionRequest.LedgerDevice
        ): Result<MessageFromDataChannel.LedgerResponse.DeriveAndDisplayAddressResponse> {
            TODO("Not yet implemented")
        }
    }
}

@Suppress("CompositionLocalAllowlist")
val LocalActionableAddressViewEntryPoint = compositionLocalOf<ActionableAddressViewEntryPointI> {
    error("No ActionableAddressViewEntryPoint provided")
}

@Composable
fun ProvideMockActionableAddressViewEntryPoint(
    entryPoint: ActionableAddressViewEntryPointI = actionableAddressViewEntryPointMock,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalActionableAddressViewEntryPoint provides entryPoint) {
        content()
    }
}
