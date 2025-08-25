package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.selectfactorsource.SelectFactorSourceProxy
import com.babylon.wallet.android.presentation.settings.personas.createpersona.CreatePersonaViewModel
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.SargonOs
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePersonaViewModelTest : StateViewModelTest<CreatePersonaViewModel>() {

    private val preferencesManager = mockk<PreferencesManager>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val sargonOs = mockk<SargonOs>()
    private val sargonOsManager = mockk<SargonOsManager>().also {
        every { it.sargonOs } returns sargonOs
    }
    private val selectFactorSourceProxy = mockk<SelectFactorSourceProxy>()

    private val persona = Persona.sampleMainnet()

    @Before
    override fun setUp() = runTest {
        coEvery { preferencesManager.firstPersonaCreated } returns flow {
            emit(true)
        }
        coEvery { preferencesManager.markFirstPersonaCreated() } just Runs
        coEvery {
            sargonOs.createAndSaveNewPersonaWithFactorSource(
                factorSource = DeviceFactorSource.sample().asGeneral(),
                networkId = any(),
                name = any(),
                personaData = any()
            )
        } returns persona

        coEvery { getProfileUseCase() } returns Profile.sample().changeGatewayToNetworkId(NetworkId.MAINNET)
        super.setUp()
    }

    @Test
    fun `when view model init, verify persona info are empty`() = runTest {
        // then
        val state = vm.value.state.first()
        Assert.assertEquals(state.personaDisplayName, PersonaDisplayNameFieldWrapper())
    }

    override fun initVM(): CreatePersonaViewModel {
        return CreatePersonaViewModel(getProfileUseCase, sargonOsManager, preferencesManager, selectFactorSourceProxy)
    }
}
