package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.fakes.FakeAppEventBus
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.settings.personas.createpersona.CreatePersonaViewModel
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rdx.works.core.preferences.PreferencesManager

@OptIn(ExperimentalCoroutinesApi::class)
class PersonaViewModelTest : StateViewModelTest<CreatePersonaViewModel>() {

    private val deviceCapabilityHelper = mockk<DeviceCapabilityHelper>()
    private val accessFactorSourceProxy = mockk<AccessFactorSourcesProxy>()
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

        coEvery { accessFactorSourceProxy.createPersona(any()) } returns Result.success(AccessFactorSourcesOutput.CreatedPersona(persona))
    }

    @Test
    fun `when view model init, verify persona info are empty`() = runTest {
        // when
        val viewModel = CreatePersonaViewModel(accessFactorSourceProxy, FakeAppEventBus())
        advanceUntilIdle()

        // then
        val state = viewModel.state.first()
        Assert.assertEquals(state.personaDisplayName, PersonaDisplayNameFieldWrapper())
    }

    @Test
    fun `given persona data provided, when create button hit, verify complete event sent and persona info shown`() = runTest {
        val viewModel = CreatePersonaViewModel(accessFactorSourceProxy, FakeAppEventBus())

        viewModel.onDisplayNameChanged(persona.displayName.value)

        // when
        viewModel.onPersonaCreateClick()

        advanceUntilIdle()

        // then
        val state = viewModel.state.first()
        Assert.assertEquals(state.personaDisplayName.value, persona.displayName.value)

        advanceUntilIdle()
        coVerify(exactly = 1) { accessFactorSourceProxy.createPersona(any()) }
    }

    override fun initVM(): CreatePersonaViewModel {
        return CreatePersonaViewModel(accessFactorSourceProxy, FakeAppEventBus())
    }
}
