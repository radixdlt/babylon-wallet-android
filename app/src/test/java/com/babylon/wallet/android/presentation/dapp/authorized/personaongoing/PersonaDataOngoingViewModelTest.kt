@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.dapp.authorized.personaongoing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.RequiredPersonaField
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import rdx.works.core.sargon.PersonaDataField
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase

@Ignore("TODO Integration")
internal class PersonaDataOngoingViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities()
    private val samplePersona = profile.currentNetwork!!.personas().first()

    fun initVM(): PersonaDataOngoingViewModel {
        return PersonaDataOngoingViewModel(
            savedStateHandle,
            getProfileUseCase
        )
    }

    @Before
    fun setUp() {
        every { savedStateHandle.get<String>(ARG_PERSONA_ID) } returns samplePersona.address.string
        every { savedStateHandle.get<RequiredPersonaFields>(ARG_REQUIRED_FIELDS) } returns RequiredPersonaFields(
            fields = listOf(
                RequiredPersonaField(
                    PersonaDataField.Kind.Name,
                    MessageFromDataChannel.IncomingRequest.NumberOfValues(
                        1,
                        MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
                    )
                )
            )
        )
        coEvery { getProfileUseCase() } returns profile
        coEvery { getProfileUseCase.flow } returns flowOf(profile)
    }

    @Test
    fun `initial state is set up properly when fields are not missing`() = runTest {
        val vm = initVM()
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.persona?.persona?.address == samplePersona.address)
            assert(item.continueButtonEnabled)
        }
    }

    @Test
    fun `initial state is set up properly when fields are missing`() = runTest {
        every { savedStateHandle.get<RequiredPersonaFields>(ARG_REQUIRED_FIELDS) } returns RequiredPersonaFields(
            fields = listOf(
                RequiredPersonaField(
                    PersonaDataField.Kind.PhoneNumber,
                    MessageFromDataChannel.IncomingRequest.NumberOfValues(
                        1,
                        MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
                    )
                )
            )
        )
        val vm = initVM()
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.persona?.persona?.address == samplePersona.address)
            assert(!item.continueButtonEnabled)
        }
    }

}
