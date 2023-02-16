package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.createpersona.ARG_PERSONA_ID
import com.babylon.wallet.android.presentation.createpersona.CreatePersonaConfirmationEvent
import com.babylon.wallet.android.presentation.createpersona.CreatePersonaConfirmationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.FactorSourceReference
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.PersonaRepository

@ExperimentalCoroutinesApi
class CreatePersonaConfirmationViewModelTest : BaseViewModelTest<CreatePersonaConfirmationViewModel>() {

    private val savedStateHandle = Mockito.mock(SavedStateHandle::class.java)
    private val personaRepository = Mockito.mock(PersonaRepository::class.java)
    private val personaId = "fj3489fj348f"
    private val personaName = "My first persona"

    private val persona =  OnNetwork.Persona(
        address = personaId,
        derivationPath = "m/1'/1'/1'/1'/1'/1'",
        displayName = personaName,
        index = 0,
        networkID = 10,
        fields = emptyList(),
        securityState = SecurityState.Unsecured(
            discriminator = "dsics",
            unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                genesisFactorInstance = FactorInstance(
                    derivationPath = DerivationPath("few", "disc"),
                    factorInstanceID = "IDIDDIIDD",
                    factorSourceReference = FactorSourceReference(
                        factorSourceID = "f32f3",
                        factorSourceKind = "kind"
                    ),
                    initializationDate = "Date1",
                    publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                )
            )
        )
    )

    @Before
    override fun setUp() = runTest {
        super.setUp()
        whenever(savedStateHandle.get<String>(ARG_PERSONA_ID)).thenReturn(personaId)
        whenever(personaRepository.getPersonaByAddress(any())).thenReturn(persona)
    }

    @Test
    fun `when view model init, verify persona details are fetched and passed to ui`() = runTest {
        // when
        whenever(personaRepository.getPersonas()).thenReturn(
            listOf(persona)
        )
        val viewModel = vm.value
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreatePersonaConfirmationViewModel.PersonaConfirmationUiState(
                isFirstPersona = true
            ),
            viewModel.personaUiState
        )
    }

    @Test
    fun `given view model init, when persona created clicked, verify finish person creation event sent`() = runTest {
        // given
        whenever(personaRepository.getPersonas()).thenReturn(
            listOf(persona, persona)
        )
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
            viewModel.personaUiState
        )
    }

    override fun initVM(): CreatePersonaConfirmationViewModel {
        return CreatePersonaConfirmationViewModel(personaRepository, savedStateHandle)
    }
}