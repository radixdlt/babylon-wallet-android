package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HostId
import com.radixdlt.sargon.HostInfo
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.core.sargon.olympia
import rdx.works.profile.FakeProfileRepository
import rdx.works.profile.data.repository.HostInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class EnsureBabylonFactorSourceExistUseCaseTest {
    private val profileRepository = FakeProfileRepository()
    private val mnemonicRepository = mockk<MnemonicRepository>()
    private val hostInfoRepository = mockk<HostInfoRepository>()

    private val ensureBabylonFactorSourceExistUseCase =
        EnsureBabylonFactorSourceExistUseCase(mnemonicRepository, profileRepository, hostInfoRepository)

    private val hostId = HostId.sample()
    private val hostInfo = HostInfo.sample.other()
    private val mnemonic = MnemonicWithPassphrase.init(
        phrase = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                "humble limb repeat video sudden possible story mask neutral prize goose mandate"
    )
    // Sargon does not allow init without main, so the only possible test case here is to create a new profile,
    // remove main factor source, serialize, deserialize and provide it to repository
    private val deviceFactorSource = FactorSource.Device.babylon(
        mnemonicWithPassphrase = mnemonic,
        hostInfo = hostInfo,
        isMain = true
    )
    private val profileWithoutMain = Profile.init(
        deviceFactorSource = deviceFactorSource,
        hostId = hostId,
        hostInfo = hostInfo
    ).let {
        it.copy(
            factorSources = it.factorSources.asIdentifiable().append(
                element = FactorSource.Device.olympia(
                    mnemonicWithPassphrase = MnemonicWithPassphrase.sample.other(),
                    hostInfo = hostInfo
                )
            ).removeBy(deviceFactorSource.id).asList()
        )
    }

    @Test
    fun `babylon factor source is added to profile if it does not exist`() = runTest {
        profileRepository.saveProfile(profileWithoutMain)
        coEvery { hostInfoRepository.getHostId() } returns hostId
        coEvery { hostInfoRepository.getHostInfo() } returns hostInfo
        coEvery { mnemonicRepository.createNew() } returns Result.success(mnemonic)

        val profile = ensureBabylonFactorSourceExistUseCase().getOrThrow()
        assertEquals(2, profile.factorSources.size)
        assertEquals(deviceFactorSource.id, profile.deviceFactorSources.first().id)

        // Run use case again, factor source is in place so no need to add it again
        val profileAgain = ensureBabylonFactorSourceExistUseCase().getOrThrow()
        assertEquals(2, profileAgain.factorSources.size)
    }
}
