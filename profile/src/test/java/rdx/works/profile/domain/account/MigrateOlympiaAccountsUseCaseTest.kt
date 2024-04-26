package rdx.works.profile.domain.account

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.LegacyOlympiaAccountAddress
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Slip10Curve
import com.radixdlt.sargon.extensions.account
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.size
import com.radixdlt.sargon.extensions.toBabylonAddress
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import rdx.works.core.domain.DeviceInfo
import rdx.works.core.domain.ProfileState
import rdx.works.core.sargon.addAccounts
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.derivePublicKey
import rdx.works.core.sargon.init
import rdx.works.core.sargon.initBabylon
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import rdx.works.profile.olympiaimport.OlympiaAccountType
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MigrateOlympiaAccountsUseCaseTest {

    private val profileRepository = mockk<ProfileRepository>()
    private val mnemonicRepository = mockk<MnemonicRepository>()
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `migrate and add accounts to profile`() = testScope.runTest {
        val babylonMnemonic = MnemonicWithPassphrase.init(
            phrase = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote"
        )

        val factorSource = FactorSource.Device.babylon(
            mnemonicWithPassphrase = babylonMnemonic,
            isMain = true
        )
        val derivationPath = DerivationPath.Cap26.account(
            networkId = NetworkId.MAINNET,
            keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
            index = 0u
        )
        val profile = Profile.init(
            deviceFactorSource = factorSource,
            creatingDeviceName = DeviceInfo.sample().displayName
        ).addAccounts(
            accounts = listOf(
                Account.initBabylon(
                    networkId = NetworkId.MAINNET,
                    displayName = DisplayName("my account"),
                    publicKey = babylonMnemonic.derivePublicKey(derivationPath = derivationPath),
                    derivationPath = derivationPath,
                    factorSourceId = factorSource.value.id.asGeneral()
                )
            ),
            onNetwork = NetworkId.MAINNET
        )

        coEvery { mnemonicRepository.readMnemonic(any()) } returns Result.success(babylonMnemonic)
        coEvery { mnemonicRepository.saveMnemonic(any(), any()) } just Runs
        coEvery { profileRepository.profileState } returns flowOf(ProfileState.Restored(profile))
        coEvery { profileRepository.saveProfile(any()) } just Runs

        val usecase = MigrateOlympiaAccountsUseCase(profileRepository, testDispatcher)
        val capturedProfile = slot<Profile>()
        usecase(getOlympiaTestAccounts(), factorSource.value.id.asGeneral())
        coVerify(exactly = 1) { profileRepository.saveProfile(capture(capturedProfile)) }
        assertEquals(12, capturedProfile.captured.currentNetwork!!.accounts.size)
    }

    private fun getOlympiaTestAccounts(): List<OlympiaAccountDetails> {
        val olympiaMnemonic = MnemonicWithPassphrase.init(
            phrase = "bridge easily outer film record undo turtle method knife quarter promote arch"
        )
        val accounts = List(11) { index ->
            val derivationPath = DerivationPath.Bip44Like.init(index = index.toUInt())
            val publicKey = olympiaMnemonic.derivePublicKey(
                derivationPath = derivationPath,
                curve = Slip10Curve.SECP256K1
            ) as PublicKey.Secp256k1

            val olympiaAddress = LegacyOlympiaAccountAddress.init(publicKey)
            OlympiaAccountDetails(
                index = index,
                type = if (index % 2 == 0) OlympiaAccountType.Software else OlympiaAccountType.Hardware,
                address = olympiaAddress,
                publicKey = publicKey,
                accountName = "Olympia $index",
                derivationPath = derivationPath,
                newBabylonAddress = olympiaAddress.toBabylonAddress(),
                appearanceId = AppearanceId.init(index.toUByte())
            )
        }
        return accounts
    }
}
