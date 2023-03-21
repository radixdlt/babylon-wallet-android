package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.presentation.createpersona.CreatePersonaEvent
import com.babylon.wallet.android.presentation.createpersona.CreatePersonaViewModel
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import io.mockk.coEvery
import io.mockk.mockk
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
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.domain.CreatePersonaUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePersonaViewModelTest : BaseViewModelTest<CreatePersonaViewModel>() {

    private val deviceSecurityHelper = mockk<DeviceSecurityHelper>()
    private val createPersonaUseCase = mockk<CreatePersonaUseCase>()

    private val personaId = "fj3489fj348f"
    private val personaName = "My first persona"

    @Before
    override fun setUp() = runTest {
        super.setUp()

        coEvery {
            deviceSecurityHelper.isDeviceSecure()
        } returns true

        coEvery { createPersonaUseCase.invoke(any(), any()) } returns Network.Persona(
            address = personaId,
            displayName = personaName,
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
        val viewModel = CreatePersonaViewModel(createPersonaUseCase, deviceSecurityHelper)
        advanceUntilIdle()

        // then
        Assert.assertEquals(viewModel.state.loading, false)
        Assert.assertEquals(viewModel.state.personaDisplayName, "")
        Assert.assertEquals(viewModel.state.isDeviceSecure, true)
    }

    @Test
    fun `given persona data provided, when create button hit, verify complete event sent and persona info shown`() =
        runTest {

            val event = mutableListOf<CreatePersonaEvent>()
            val viewModel = CreatePersonaViewModel(createPersonaUseCase, deviceSecurityHelper)

            viewModel.onDisplayNameChanged(personaName)

            // when
            viewModel.onPersonaCreateClick()

            advanceUntilIdle()

            // then
            Assert.assertEquals(viewModel.state.loading, true)
            Assert.assertEquals(viewModel.state.personaDisplayName, personaName)
            Assert.assertEquals(viewModel.state.isDeviceSecure, true)

            advanceUntilIdle()

            viewModel.oneOffEvent
                .onEach { event.add(it) }
                .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            Assert.assertEquals(event.first(), CreatePersonaEvent.Complete(personaId = personaId))
        }

    override fun initVM(): CreatePersonaViewModel {
        return CreatePersonaViewModel(createPersonaUseCase, deviceSecurityHelper)
    }
}
