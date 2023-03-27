package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.BaseViewModelTest
import com.babylon.wallet.android.presentation.model.encodeToString
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import org.junit.Before
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.PersonaRepository

internal class PersonaDataOnetimeViewModelTest : BaseViewModelTest<PersonaDataOnetimeViewModel>() {

    private val personaRepository = mockk<PersonaRepository>()
    private val savedStateHandle = mockk<SavedStateHandle>()

    private val samplePersona = SampleDataProvider().samplePersona()

    override fun initVM(): PersonaDataOnetimeViewModel {
        return PersonaDataOnetimeViewModel(
            savedStateHandle,
            personaRepository
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_REQUIRED_FIELDS) } returns listOf(Network.Persona.Field.Kind.GivenName).encodeToString()
        coEvery { personaRepository.personas } returns flow {
            emit(listOf(samplePersona))
        }
    }

}