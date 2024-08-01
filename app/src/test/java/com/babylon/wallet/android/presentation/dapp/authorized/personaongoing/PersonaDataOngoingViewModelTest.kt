@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.dapp.authorized.personaongoing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.RequiredPersonaField
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.radixdlt.sargon.CollectionOfEmailAddresses
import com.radixdlt.sargon.CollectionOfPhoneNumbers
import com.radixdlt.sargon.EmailAddress
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.PersonaDataEntryId
import com.radixdlt.sargon.PersonaDataEntryName
import com.radixdlt.sargon.PersonaDataIdentifiedEmailAddress
import com.radixdlt.sargon.PersonaDataIdentifiedName
import com.radixdlt.sargon.PersonaDataNameVariant
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.Personas
import com.radixdlt.sargon.extensions.ProfileNetworks
import com.radixdlt.sargon.extensions.forNetwork
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
import org.junit.Rule
import org.junit.Test
import rdx.works.core.sargon.PersonaDataField
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase

internal class PersonaDataOngoingViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities().let {
        val network = it.networks.asIdentifiable().getBy(NetworkId.MAINNET)!!.let { network ->
            val persona = network.personas.first().copy(
                personaData = PersonaData(
                    name = PersonaDataIdentifiedName(
                        id = PersonaDataEntryId.randomUUID(),
                        value = PersonaDataEntryName(
                            variant = PersonaDataNameVariant.WESTERN,
                            familyName = "",
                            nickname = "",
                            givenNames = "John"
                        )
                    ),
                    emailAddresses = CollectionOfEmailAddresses(
                        listOf(
                            PersonaDataIdentifiedEmailAddress(
                                id = PersonaDataEntryId.randomUUID(),
                                value = EmailAddress("test@test.pl")
                            )
                        )
                    ),
                    phoneNumbers = CollectionOfPhoneNumbers(emptyList())
                )
            )

            network.copy(personas = Personas(persona).asList())
        }
        it.copy(networks = ProfileNetworks(network).asList())
    }
    private val samplePersona = profile.currentNetwork!!.personas.first()

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
                    IncomingMessage.IncomingRequest.NumberOfValues(
                        1,
                        IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.Exactly
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
                    IncomingMessage.IncomingRequest.NumberOfValues(
                        1,
                        IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.Exactly
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
