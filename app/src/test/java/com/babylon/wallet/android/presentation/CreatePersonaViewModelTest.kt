package com.babylon.wallet.android.presentation

import rdx.works.core.preferences.PreferencesManager
import com.babylon.wallet.android.presentation.createpersona.CreatePersonaEvent
import com.babylon.wallet.android.presentation.createpersona.CreatePersonaViewModel
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.utils.DeviceSecurityHelper
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
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.domain.persona.CreatePersonaUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePersonaViewModelTest : StateViewModelTest<CreatePersonaViewModel>() {

    private val deviceSecurityHelper = mockk<DeviceSecurityHelper>()
    private val createPersonaUseCase = mockk<CreatePersonaUseCase>()
    private val preferencesManager = mockk<PreferencesManager>()

    private val personaId = "fj3489fj348f"
    private val personaName = PersonaDisplayNameFieldWrapper("My first persona", valid = true, wasEdited = true)

    @Before
    override fun setUp() = runTest {
        super.setUp()

        coEvery {
            deviceSecurityHelper.isDeviceSecure()
        } returns true
        coEvery { preferencesManager.firstPersonaCreated } returns flow {
            emit(true)
        }
        coEvery { preferencesManager.markFirstPersonaCreated() } just Runs

        coEvery { createPersonaUseCase.invoke(any(), any()) } returns Network.Persona(
            address = personaId,
            displayName = personaName.value,
            networkID = 10,
            fields = emptyList(),
            securityState = SecurityState.Unsecured(
                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                    genesisFactorInstance = FactorInstance(
                        derivationPath = DerivationPath.forIdentity("m/1'/1'/1'/1'/1'/1'"),
                        factorSourceId = FactorSource.ID("IDIDDIIDD"),
                        publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                    )
                )
            )
        )
    }

    @Test
    fun `when view model init, verify persona info are empty`() = runTest {
        // when
        val viewModel = CreatePersonaViewModel(createPersonaUseCase, preferencesManager, deviceSecurityHelper)
        advanceUntilIdle()

        // then
        val state = viewModel.state.first()
        Assert.assertEquals(state.loading, false)
        Assert.assertEquals(state.personaDisplayName, PersonaDisplayNameFieldWrapper())
        Assert.assertEquals(state.isDeviceSecure, true)
    }

    @Test
    fun `given persona data provided, when create button hit, verify complete event sent and persona info shown`() =
        runTest {

            val event = mutableListOf<CreatePersonaEvent>()
            val viewModel = CreatePersonaViewModel(createPersonaUseCase, preferencesManager, deviceSecurityHelper)

            viewModel.onDisplayNameChanged(personaName.value)

            // when
            viewModel.onPersonaCreateClick()

            advanceUntilIdle()

            // then
            val state = viewModel.state.first()
            Assert.assertEquals(state.loading, true)
            Assert.assertEquals(state.personaDisplayName, personaName)
            Assert.assertEquals(state.isDeviceSecure, true)

            advanceUntilIdle()

            viewModel.oneOffEvent
                .onEach { event.add(it) }
                .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            Assert.assertEquals(event.first(), CreatePersonaEvent.Complete(personaId = personaId))
        }

    override fun initVM(): CreatePersonaViewModel {
        return CreatePersonaViewModel(createPersonaUseCase, preferencesManager, deviceSecurityHelper)
    }
}
