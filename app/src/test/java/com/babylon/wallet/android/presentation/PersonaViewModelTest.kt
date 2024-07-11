package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.fakes.FakeAppEventBus
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.settings.personas.createpersona.CreatePersonaViewModel
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.core.sargon.sample
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.persona.CreatePersonaUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class PersonaViewModelTest : StateViewModelTest<CreatePersonaViewModel>() {

    private val deviceCapabilityHelper = mockk<DeviceCapabilityHelper>()
    private val accessFactorSourceProxy = mockk<AccessFactorSourcesProxy>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val createPersonaUseCase = mockk<CreatePersonaUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()

    private val persona = Persona.sampleMainnet()

    @Before
    override fun setUp() = runTest {
        coEvery {
            deviceCapabilityHelper.isDeviceSecure()
        } returns true
        coEvery { preferencesManager.firstPersonaCreated } returns flow {
            emit(true)
        }
        coEvery { preferencesManager.markFirstPersonaCreated() } just Runs
        coEvery { createPersonaUseCase(any(), any(), any(), any()) } returns Result.success(persona)

        coEvery { accessFactorSourceProxy.getPublicKeyAndDerivationPathForFactorSource(any()) } returns Result.success(
            AccessFactorSourcesOutput.HDPublicKey(HierarchicalDeterministicPublicKey.sample())
        )
        coEvery { getProfileUseCase() } returns Profile.sample()
        mockkStatic("rdx.works.core.sargon.ProfileExtensionsKt")
        every { any<Profile>().mainBabylonFactorSource } returns FactorSource.Device.sample()
        super.setUp()
    }

    @Test
    fun `when view model init, verify persona info are empty`() = runTest {
        // then
        val state = vm.value.state.first()
        Assert.assertEquals(state.personaDisplayName, PersonaDisplayNameFieldWrapper())
    }

    @Test
    fun `given persona data provided, when create button hit, verify complete event sent and persona info shown`() = runTest {
        vm.value.onDisplayNameChanged(persona.displayName.value)

        // when
        vm.value.onPersonaCreateClick()

        advanceUntilIdle()

        // then
        val state = vm.value.state.first()
        Assert.assertEquals(state.personaDisplayName.value, persona.displayName.value)

        advanceUntilIdle()
        coVerify(exactly = 1) { accessFactorSourceProxy.getPublicKeyAndDerivationPathForFactorSource(any()) }
    }

    override fun initVM(): CreatePersonaViewModel {
        return CreatePersonaViewModel(createPersonaUseCase, getProfileUseCase, accessFactorSourceProxy, FakeAppEventBus())
    }
}
