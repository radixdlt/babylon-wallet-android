package rdx.works.profile

import com.radixdlt.sargon.CollectionOfEmailAddresses
import com.radixdlt.sargon.CollectionOfPhoneNumbers
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import rdx.works.core.domain.DeviceInfo
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.addNetworkIfDoesNotExist
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.babylon
import rdx.works.profile.domain.persona.CreatePersonaUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class CreatePersonaUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mnemonicWithPassphrase = MnemonicWithPassphrase.init(
        phrase = "travel organ kick vote head divide express recall oblige foster banner spin shield " +
                "stone scan pretty sort skate knock kangaroo pill test belt father"
    )
    private val profileRepository = FakeProfileRepository()

    private val preferencesManager = mockk<PreferencesManager>().apply {
        coEvery { markFirstPersonaCreated() } just Runs
    }

    private val createPersonaUseCase = CreatePersonaUseCase(
        preferencesManager = preferencesManager,
        profileRepository = profileRepository,
        defaultDispatcher = testDispatcher
    )

    @Test
    fun `given profile already exists, when creating new persona, verify its returned and persisted to the profile`() = testScope.runTest {
        // given
        val deviceInfo = DeviceInfo.sample()
        val profile = Profile.init(
            deviceFactorSource = FactorSource.Device.babylon(
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                deviceInfo = deviceInfo,
                isMain = true
            ),
            deviceInfo = deviceInfo.toSargonDeviceInfo()
        ).addNetworkIfDoesNotExist(onNetwork = NetworkId.MAINNET)
        profileRepository.saveProfile(profile)

        val newPersona = createPersonaUseCase(
            displayName = DisplayName("Michael"),
            personaData = PersonaData(
                name = null,
                phoneNumbers = CollectionOfPhoneNumbers(emptyList()),
                emailAddresses = CollectionOfEmailAddresses(emptyList())
            ),
            hdPublicKey = HierarchicalDeterministicPublicKey.sample(),
            factorSourceId = FactorSourceId.Hash.init(kind = FactorSourceKind.DEVICE, mnemonicWithPassphrase = mnemonicWithPassphrase)
        ).getOrNull()

        assertEquals(
            newPersona,
            profileRepository.inMemoryProfileOrNull?.networks?.asIdentifiable()?.getBy(NetworkId.MAINNET)?.personas?.first()
        )
    }
}
