package com.babylon.wallet.android.presentation.settings.personaedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.RequiredPersonaField
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.settings.personas.personaedit.ARG_PERSONA_ADDRESS
import com.babylon.wallet.android.presentation.settings.personas.personaedit.ARG_REQUIRED_FIELDS
import com.babylon.wallet.android.presentation.settings.personas.personaedit.PersonaEditViewModel
import com.babylon.wallet.android.utils.isValidEmail
import com.radixdlt.sargon.CollectionOfEmailAddresses
import com.radixdlt.sargon.CollectionOfPhoneNumbers
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.PersonaDataEntryEmailAddress
import com.radixdlt.sargon.PersonaDataEntryId
import com.radixdlt.sargon.PersonaDataEntryName
import com.radixdlt.sargon.PersonaDataIdentifiedEmailAddress
import com.radixdlt.sargon.PersonaDataIdentifiedName
import com.radixdlt.sargon.PersonaDataNameVariant
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import rdx.works.core.sargon.PersonaDataField
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.persona.UpdatePersonaUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersonaEditViewModelTest : StateViewModelTest<PersonaEditViewModel>() {

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val updatePersonaUseCase = mockk<UpdatePersonaUseCase>()

    private val nameFieldId = PersonaDataEntryId.randomUUID()
    private val emailFieldId = PersonaDataEntryId.randomUUID()
    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities().let {
        val mainnetNetwork = it.networks.asIdentifiable().getBy(NetworkId.MAINNET)!!
        val firstPersona = mainnetNetwork.personas.first().copy(
            personaData = PersonaData(
                name = PersonaDataIdentifiedName(
                    id = nameFieldId, value = PersonaDataEntryName(
                        variant = PersonaDataNameVariant.WESTERN,
                        givenNames = "John",
                        familyName = "",
                        nickname = ""
                    )
                ),
                emailAddresses = CollectionOfEmailAddresses(
                    listOf(PersonaDataIdentifiedEmailAddress(id = emailFieldId, value = PersonaDataEntryEmailAddress("test@test.pl")))
                ),
                phoneNumbers = CollectionOfPhoneNumbers(emptyList())
            )
        )
        it.copy(
            networks = it.networks.asIdentifiable().updateOrAppend(
                mainnetNetwork.copy(
                    personas = mainnetNetwork.personas.asIdentifiable().updateOrAppend(firstPersona).asList()
                )
            ).asList()
        )
    }
    private val persona = profile.currentNetwork?.personas?.first()!!

    override fun initVM(): PersonaEditViewModel {
        return PersonaEditViewModel(getProfileUseCase, updatePersonaUseCase, savedStateHandle)
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_PERSONA_ADDRESS) } returns persona.address.string
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
        mockkStatic("com.babylon.wallet.android.utils.StringExtensionsKt")
        every { any<String>().isValidEmail() } returns true
        coEvery { updatePersonaUseCase(any()) } just Runs
        every { getProfileUseCase.flow } returns flowOf(profile)
    }

    private fun <T> StateFlow<T>.whileCollecting(action: suspend () -> Unit) = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { collect {} }
        action()
        collectJob.cancel()
    }

    @Test
    fun `init load persona and it's fields`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assertEquals(persona.address, item.persona?.address)
                assertEquals(2, item.currentFields.size)
                assertEquals(1, item.fieldsToAdd.size)
            }
        }
    }

    @Test
    fun `save triggers proper useCase`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onSave()
            advanceUntilIdle()
            val persona = slot<Persona>()
            coVerify(exactly = 1) { updatePersonaUseCase(capture(persona)) }
            assert(persona.captured.personaData.name != null)
        }
    }

    @Test
    fun `delete field updates state`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onDeleteField(emailFieldId)
            advanceUntilIdle()
            vm.onSave()
            advanceUntilIdle()
            val persona = slot<Persona>()
            coVerify(exactly = 1) { updatePersonaUseCase(capture(persona)) }
            assert(persona.captured.personaData.name != null)
            assert(persona.captured.personaData.emailAddresses.collection.isEmpty())
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.currentFields.size == 1)
                assert(item.fieldsToAdd.size == 2)
            }

        }
    }

    @Test
    fun `field value change update state`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onFieldValueChanged(emailFieldId, PersonaDataField.Email("jakub@jakub.pl"))
            advanceUntilIdle()
            vm.onFieldValueChanged(
                nameFieldId,
                PersonaDataField.Name(
                    variant = PersonaDataField.Name.Variant.Western,
                    given = "jakub",
                    family = "",
                    nickname = ""
                )
            )
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(
                    item.currentFields.map {
                        it.entry
                    }.filter {
                        it.value.kind == PersonaDataField.Kind.EmailAddress
                    }.map { it.value as PersonaDataField.Email }.first().value == "jakub@jakub.pl"
                )
                assert(
                    item.currentFields.map {
                        it.entry
                    }.filter {
                        it.value.kind == PersonaDataField.Kind.Name
                    }.map { it.value as PersonaDataField.Name }.first().given == "jakub"
                )
            }
        }
    }

}