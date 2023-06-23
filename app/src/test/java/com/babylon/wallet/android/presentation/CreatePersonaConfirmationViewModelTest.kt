package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.createpersona.ARG_PERSONA_ID
import com.babylon.wallet.android.presentation.createpersona.CreatePersonaConfirmationEvent
import com.babylon.wallet.android.presentation.createpersona.CreatePersonaConfirmationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.domain.GetProfileUseCase

@ExperimentalCoroutinesApi
class CreatePersonaConfirmationViewModelTest : StateViewModelTest<CreatePersonaConfirmationViewModel>() {

    private val savedStateHandle = Mockito.mock(SavedStateHandle::class.java)
    private val getProfileUseCase = Mockito.mock(GetProfileUseCase::class.java)
    private val personaId = "fj3489fj348f"
    private val personaName = "My first persona"

    private val persona =  Network.Persona(
        address = personaId,
        displayName = personaName,
        networkID = Radix.Gateway.default.network.id,
        fields = emptyList(),
        securityState = SecurityState.Unsecured(
            unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                transactionSigning = FactorInstance(
                    derivationPath = DerivationPath.forIdentity(
                        networkId = Radix.Gateway.default.network.networkId(),
                        identityIndex = 0,
                        keyType = KeyType.TRANSACTION_SIGNING
                    ),
                    factorSourceId = FactorSource.FactorSourceID.FromHash(
                        kind = FactorSourceKind.DEVICE,
                        body = FactorSource.HexCoded32Bytes("IDIDDIIDD")
                    ),
                    publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                )
            )
        )
    )

    @Before
    override fun setUp() = runTest {
        super.setUp()
        whenever(savedStateHandle.get<String>(ARG_PERSONA_ID)).thenReturn(personaId)
        whenever(getProfileUseCase()).thenReturn(flowOf(
            profile(personas = listOf(persona))
        ))
    }

    @Test
    fun `when view model init, verify persona details are fetched and passed to ui`() = runTest {
        // when
        val viewModel = vm.value
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreatePersonaConfirmationViewModel.PersonaConfirmationUiState(
                isFirstPersona = true
            ),
            viewModel.state.first()
        )
    }

    @Test
    fun `given view model init, when persona created clicked, verify finish person creation event sent`() = runTest {
        // given
        whenever(getProfileUseCase()).thenReturn(flowOf(
            profile(personas = listOf(persona, persona))
        ))
        val viewModel = vm.value
        val event = mutableListOf<CreatePersonaConfirmationEvent>()

        // when
        viewModel.personaConfirmed()

        viewModel.oneOffEvent
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.first(), CreatePersonaConfirmationEvent.FinishPersonaCreation)

        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreatePersonaConfirmationViewModel.PersonaConfirmationUiState(
                isFirstPersona = false
            ),
            viewModel.state.first()
        )
    }

    override fun initVM(): CreatePersonaConfirmationViewModel {
        return CreatePersonaConfirmationViewModel(getProfileUseCase)
    }
}
