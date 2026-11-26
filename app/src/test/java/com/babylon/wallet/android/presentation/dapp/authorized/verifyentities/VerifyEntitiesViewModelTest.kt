package com.babylon.wallet.android.presentation.dapp.authorized.verifyentities

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.usecases.signing.SignAuthUseCase
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.newWalletInteractionVersionCurrent
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
internal class VerifyEntitiesViewModelTest : StateViewModelTest<VerifyEntitiesViewModel>() {

    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val signAuthUseCase = mockk<SignAuthUseCase>()

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities()
    private val persona = profile.currentNetwork!!.personas.first()
    private val personaSignature = mockk<SignatureWithPublicKey>()

    // A proof of ownership of the persona alone: no account to verify after it.
    private val personaOnlyProofRequest = WalletAuthorizedRequest(
        remoteEntityId = RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = DappToWalletInteraction.RequestMetadata(
            version = newWalletInteractionVersionCurrent(),
            networkId = NetworkId.MAINNET,
            origin = "https://example.com",
            dAppDefinitionAddress = AccountAddress.sampleMainnet().string,
            isInternal = false
        ),
        authRequestItem = WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest(persona.address),
        proofOfOwnershipRequestItem = WalletAuthorizedRequest.ProofOfOwnershipRequestItem(
            challenge = Exactly32Bytes.sample(),
            personaAddress = persona.address
        )
    )

    override fun initVM(): VerifyEntitiesViewModel = VerifyEntitiesViewModel(
        savedStateHandle = savedStateHandle,
        getProfileUseCase = getProfileUseCase,
        incomingRequestRepository = incomingRequestRepository,
        signAuthUseCase = signAuthUseCase,
        defaultDispatcher = coroutineRule.dispatcher
    )

    @Before
    override fun setUp() {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_AUTHORIZED_REQUEST_INTERACTION_ID) } returns personaOnlyProofRequest.interactionId
        every { savedStateHandle.get<String>(ARG_ENTITIES_FOR_PROOF) } returns Json.encodeToString(
            EntitiesForProofWithSignatures(personaAddress = persona.address)
        )
        every { savedStateHandle.get<Boolean>(ARG_CAN_NAVIGATE_BACK) } returns false
        coEvery { getProfileUseCase() } returns profile
        every { incomingRequestRepository.getRequest(personaOnlyProofRequest.interactionId) } returns personaOnlyProofRequest
        coEvery { signAuthUseCase.persona(any(), any(), any()) } returns Result.success(personaSignature)
    }

    @Test
    fun `given a proof of the persona alone, when the persona signs, then the verification completes`() = runTest {
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.oneOffEvent.test {
            viewModel.onContinueClick()
            advanceUntilIdle()

            assertEquals(
                VerifyEntitiesViewModel.Event.EntitiesVerified(
                    mapOf(AddressOfAccountOrPersona.Identity(persona.address) to personaSignature)
                ),
                awaitItem()
            )
        }
        coVerify(exactly = 1) { signAuthUseCase.persona(any(), any(), any()) }
    }
}
