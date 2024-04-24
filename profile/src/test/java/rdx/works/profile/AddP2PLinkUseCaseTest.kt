package rdx.works.profile

import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.toBagOfBytes
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.runBlocking
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.sample
import rdx.works.profile.domain.p2plink.AddP2PLinkUseCase
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class AddP2PLinkUseCaseTest {

    private val profileRepository = FakeProfileRepository()
    private val addP2PLinkUseCase = AddP2PLinkUseCase(profileRepository)

    @Test
    fun `given profile exists, when adding p2p client, verify it is added properly`() = runBlocking {
        val browser = "Browser"
        val password = RadixConnectPassword(value = Exactly32Bytes.init(Random.nextBytes(32).toBagOfBytes()))

        val profile = Profile.init(
            deviceFactorSource = DeviceFactorSource.babylon(MnemonicWithPassphrase.sample(), isMain = true).asGeneral(),
            creatingDeviceName = "Unit Test"
        )
        profileRepository.saveProfile(profile)
        addP2PLinkUseCase(displayName = browser, connectionPassword = password)

        assertEquals(
            P2pLink(connectionPassword = password, displayName = browser),
            profileRepository.inMemoryProfileOrNull?.appPreferences?.p2pLinks()?.first()
        )
    }
}
