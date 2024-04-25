package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.settings.personas.createpersona.ARG_PERSONA_ID
import com.babylon.wallet.android.presentation.settings.personas.createpersona.CreatePersonaConfirmationEvent
import com.babylon.wallet.android.presentation.settings.personas.createpersona.CreatePersonaConfirmationViewModel
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.getBy
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import rdx.works.core.sargon.changeGateway
import rdx.works.profile.domain.GetProfileUseCase

@ExperimentalCoroutinesApi
class CreatePersonaConfirmationViewModelTest : StateViewModelTest<CreatePersonaConfirmationViewModel>() {

    private val savedStateHandle = Mockito.mock(SavedStateHandle::class.java)
    private val getProfileUseCase = Mockito.mock(GetProfileUseCase::class.java)

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET))
    private val persona = profile.networks.getBy(NetworkId.MAINNET)?.personas?.invoke()?.first()!!

    @Before
    override fun setUp() = runTest {
        super.setUp()
        whenever(savedStateHandle.get<String>(ARG_PERSONA_ID)).thenReturn(persona.address.string)
        whenever(getProfileUseCase()).thenReturn(profile)
    }

    @Test
    fun `when view model init, verify persona details are fetched and passed to ui`() = runTest {
        // when
        val viewModel = vm.value
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreatePersonaConfirmationViewModel.PersonaConfirmationUiState(isFirstPersona = true),
            viewModel.state.first()
        )
    }

    @Test
    fun `given view model init, when persona created clicked, verify finish person creation event sent`() = runTest {
        // given
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

        // TODO integration why was this true?
//        // then
//        Assert.assertEquals(
//            CreatePersonaConfirmationViewModel.PersonaConfirmationUiState(
//                isFirstPersona = false
//            ),
//            viewModel.state.first()
//        )
    }

    override fun initVM(): CreatePersonaConfirmationViewModel {
        return CreatePersonaConfirmationViewModel(getProfileUseCase)
    }
}
