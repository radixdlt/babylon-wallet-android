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
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.EntityAddress
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.FactorSourceReference
import rdx.works.profile.data.model.pernetwork.Persona
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

        coEvery { createPersonaUseCase.invoke(any(), any()) } returns Persona(
            entityAddress = EntityAddress(personaId),
            derivationPath = "m/1'/1'/1'/1'/1'/1'",
            displayName = personaName,
            index = 0,
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
    }

    @Test
    fun `when view model init, verify persona info are empty`() = runTest {
        // when
        val viewModel = CreatePersonaViewModel(createPersonaUseCase, deviceSecurityHelper)
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreatePersonaViewModel.CreatePersonaUiState(
                loading = false,
                personaName = "",
                isDeviceSecure = true
            ),
            viewModel.state
        )
    }

    @Test
    fun `given persona data provided, when create button hit, verify complete event sent and persona info shown`() =
        runTest {

            val event = mutableListOf<CreatePersonaEvent>()
            val viewModel = CreatePersonaViewModel(createPersonaUseCase, deviceSecurityHelper)

            viewModel.onPersonaNameChange(personaName)

            // when
            viewModel.onPersonaCreateClick()

            advanceUntilIdle()

            // then
            Assert.assertEquals(
                CreatePersonaViewModel.CreatePersonaUiState(
                    loading = true,
                    personaName = personaName,
                    buttonEnabled = true,
                    isDeviceSecure = true
                ),
                viewModel.state
            )

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
