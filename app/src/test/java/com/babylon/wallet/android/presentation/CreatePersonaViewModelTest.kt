package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.fakes.FakeAppEventBus
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.settings.personas.createpersona.CreatePersonaEvent
import com.babylon.wallet.android.presentation.settings.personas.createpersona.CreatePersonaViewModel
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.persona.CreatePersonaWithDeviceFactorSourceUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePersonaViewModelTest : StateViewModelTest<CreatePersonaViewModel>() {

    private val deviceCapabilityHelper = mockk<DeviceCapabilityHelper>()
    private val createPersonaWithDeviceFactorSourceUseCase = mockk<CreatePersonaWithDeviceFactorSourceUseCase>()
    private val preferencesManager = mockk<PreferencesManager>()

    private val persona = Persona.sampleMainnet()

    @Before
    override fun setUp() = runTest {
        super.setUp()

        coEvery {
            deviceCapabilityHelper.isDeviceSecure()
        } returns true
        coEvery { preferencesManager.firstPersonaCreated } returns flow {
            emit(true)
        }
        coEvery { preferencesManager.markFirstPersonaCreated() } just Runs

        coEvery { createPersonaWithDeviceFactorSourceUseCase.invoke(any(), any()) } returns Result.success(persona)
    }

    @Test
    fun `when view model init, verify persona info are empty`() = runTest {
        // when
        val viewModel = CreatePersonaViewModel(createPersonaWithDeviceFactorSourceUseCase, preferencesManager, FakeAppEventBus())
        advanceUntilIdle()

        // then
        val state = viewModel.state.first()
        Assert.assertEquals(state.loading, false)
        Assert.assertEquals(state.personaDisplayName, PersonaDisplayNameFieldWrapper())
    }

    @Test
    fun `given persona data provided, when create button hit, verify complete event sent and persona info shown`() = runTest {
            val event = mutableListOf<CreatePersonaEvent>()
            val viewModel = CreatePersonaViewModel(createPersonaWithDeviceFactorSourceUseCase, preferencesManager, FakeAppEventBus())

            viewModel.onDisplayNameChanged(persona.displayName.value)

            // when
            viewModel.onPersonaCreateClick()

            advanceUntilIdle()

            // then
            val state = viewModel.state.first()
            Assert.assertEquals(state.loading, true)
            Assert.assertEquals(state.personaDisplayName.value, persona.displayName.value)

            advanceUntilIdle()

            viewModel.oneOffEvent
                .onEach { event.add(it) }
                .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            Assert.assertEquals(event.first(), CreatePersonaEvent.Complete(personaId = persona.address))
        }

    override fun initVM(): CreatePersonaViewModel {
        return CreatePersonaViewModel(createPersonaWithDeviceFactorSourceUseCase, preferencesManager, FakeAppEventBus())
    }
}
