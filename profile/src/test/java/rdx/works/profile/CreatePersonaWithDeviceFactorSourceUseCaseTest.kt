package rdx.works.profile

import com.radixdlt.sargon.CollectionOfEmailAddresses
import com.radixdlt.sargon.CollectionOfPhoneNumbers
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.init
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import rdx.works.core.sargon.addNetworkIfDoesNotExist
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.babylon
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.persona.CreatePersonaWithDeviceFactorSourceUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class CreatePersonaWithDeviceFactorSourceUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mnemonicWithPassphrase = MnemonicWithPassphrase.init(
        phrase = "travel organ kick vote head divide express recall oblige foster banner spin shield " +
                "stone scan pretty sort skate knock kangaroo pill test belt father"
    )
    private val mnemonicRepository = mockk<MnemonicRepository>().apply {
        coEvery {
            readMnemonic(FactorSourceId.Hash.init(kind = FactorSourceKind.DEVICE, mnemonicWithPassphrase = mnemonicWithPassphrase))
        } returns Result.success(mnemonicWithPassphrase)
    }
    private val profileRepository = FakeProfileRepository()

    private val createPersonaWithDeviceFactorSourceUseCase = CreatePersonaWithDeviceFactorSourceUseCase(
        mnemonicRepository = mnemonicRepository,
        profileRepository = profileRepository,
        ensureBabylonFactorSourceExistUseCase = EnsureBabylonFactorSourceExistUseCase(
            mnemonicRepository = mnemonicRepository,
            profileRepository = profileRepository,
            preferencesManager = mockk(),
            deviceInfoRepository = mockk()
        ),
        defaultDispatcher = testDispatcher
    )

    @Test
    fun `given profile already exists, when creating new persona, verify its returned and persisted to the profile`() = testScope.runTest {
        // given
        val profile = Profile.init(
            deviceFactorSource = FactorSource.Device.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase, isMain = true),
            creatingDeviceName = "Unit Test"
        ).addNetworkIfDoesNotExist(onNetwork = NetworkId.MAINNET)
        profileRepository.saveProfile(profile)

        val newPersona = createPersonaWithDeviceFactorSourceUseCase(
            displayName = DisplayName("Michael"),
            personaData = PersonaData(
                name = null,
                phoneNumbers = CollectionOfPhoneNumbers(emptyList()),
                emailAddresses = CollectionOfEmailAddresses(emptyList())
            )
        ).getOrNull()

        assertEquals(newPersona, profileRepository.inMemoryProfileOrNull?.networks?.asIdentifiable()?.getBy(NetworkId.MAINNET)?.personas?.first())
    }
}
