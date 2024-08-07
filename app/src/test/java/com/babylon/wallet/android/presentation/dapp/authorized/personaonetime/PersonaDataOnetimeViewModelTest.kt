package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.RequiredPersonaField
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.Personas
import com.radixdlt.sargon.extensions.ProfileNetworks
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.PersonaDataField
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersonaDataOnetimeViewModelTest : StateViewModelTest<PersonaDataOnetimeViewModel>() {

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val preferencesManager = mockk<PreferencesManager>()

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities().let { profile ->
        val network = profile.networks.asIdentifiable().getBy(NetworkId.MAINNET)!!.let {
            it.copy(personas = Personas(it.personas.first()).asList())
        }
        profile.copy(networks = ProfileNetworks(network).asList())
    }
    private val samplePersona = profile.currentNetwork!!.personas.first()

    override fun initVM(): PersonaDataOnetimeViewModel {
        return PersonaDataOnetimeViewModel(
            savedStateHandle,
            getProfileUseCase,
            preferencesManager
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { savedStateHandle.get<RequiredPersonaFields>(ARG_REQUIRED_FIELDS) } returns RequiredPersonaFields(
            fields = listOf(
                RequiredPersonaField(
                    PersonaDataField.Kind.Name,
                    IncomingMessage.IncomingRequest.NumberOfValues(
                        1,
                        IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.Exactly
                    )
                )
            )
        )
        every { savedStateHandle.get<Boolean>(ARG_SHOW_BACK) } returns true
        coEvery { preferencesManager.firstPersonaCreated } returns flowOf(true)
        coEvery { getProfileUseCase() } returns profile
        coEvery { getProfileUseCase.flow } returns flowOf(profile)
    }

    @Test
    fun `initial state is set up properly`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.personaListToDisplay.size == 1)
            assert(item.personaListToDisplay.first().missingFieldKinds().size == 0)
        }
    }

    @Test
    fun `selecting persona enables continue button`() = runTest {
        val vm = vm.value
        vm.onSelectPersona(samplePersona)
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.continueButtonEnabled)
        }
    }

    @Test
    fun `edit click triggers edit action with proper required fields`() = runTest {
        val vm = vm.value
        vm.onEditClick(samplePersona)
        advanceUntilIdle()
        val item = vm.oneOffEvent.first()
        assert(item is PersonaDataOnetimeEvent.OnEditPersona && item.requiredPersonaFields.fields.any { it.kind == PersonaDataField.Kind.Name })
    }

}
