package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.settings.personas.createpersona.CreatePersonaEvent
import com.babylon.wallet.android.presentation.settings.personas.createpersona.CreatePersonaViewModel
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
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
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.domain.persona.CreatePersonaWithDeviceFactorSourceUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePersonaViewModelTest : StateViewModelTest<CreatePersonaViewModel>() {

    private val deviceCapabilityHelper = mockk<DeviceCapabilityHelper>()
    private val createPersonaWithDeviceFactorSourceUseCase = mockk<CreatePersonaWithDeviceFactorSourceUseCase>()
    private val preferencesManager = mockk<PreferencesManager>()

    private val personaId = "fj3489fj348f"
    private val personaName = PersonaDisplayNameFieldWrapper("My first persona", valid = true, wasEdited = true)

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

        coEvery { createPersonaWithDeviceFactorSourceUseCase.invoke(any(), any()) } returns Network.Persona(
            address = personaId,
            displayName = personaName.value,
            networkID = Radix.Gateway.default.network.id,
            personaData = PersonaData(),
            securityState = SecurityState.Unsecured(
                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                    entityIndex = 0,
                    transactionSigning = FactorInstance(
                        badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
                            derivationPath = DerivationPath.forIdentity(
                                networkId = Radix.Gateway.default.network.networkId(),
                                identityIndex = 0,
                                keyType = KeyType.TRANSACTION_SIGNING
                            ),
                            publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                        ),
                        factorSourceId = FactorSource.FactorSourceID.FromHash(
                            kind = FactorSourceKind.DEVICE,
                            body = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5")
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `when view model init, verify persona info are empty`() = runTest {
        // when
        val viewModel = CreatePersonaViewModel(createPersonaWithDeviceFactorSourceUseCase, preferencesManager)
        advanceUntilIdle()

        // then
        val state = viewModel.state.first()
        Assert.assertEquals(state.loading, false)
        Assert.assertEquals(state.personaDisplayName, PersonaDisplayNameFieldWrapper())
    }

    @Test
    fun `given persona data provided, when create button hit, verify complete event sent and persona info shown`() =
        runTest {

            val event = mutableListOf<CreatePersonaEvent>()
            val viewModel = CreatePersonaViewModel(createPersonaWithDeviceFactorSourceUseCase, preferencesManager)

            viewModel.onDisplayNameChanged(personaName.value)

            // when
            viewModel.onPersonaCreateClick()

            advanceUntilIdle()

            // then
            val state = viewModel.state.first()
            Assert.assertEquals(state.loading, true)
            Assert.assertEquals(state.personaDisplayName, personaName)

            advanceUntilIdle()

            viewModel.oneOffEvent
                .onEach { event.add(it) }
                .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            Assert.assertEquals(event.first(), CreatePersonaEvent.Complete(personaId = personaId))
        }

    override fun initVM(): CreatePersonaViewModel {
        return CreatePersonaViewModel(createPersonaWithDeviceFactorSourceUseCase, preferencesManager)
    }
}
