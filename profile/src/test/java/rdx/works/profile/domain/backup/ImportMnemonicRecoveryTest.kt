package rdx.works.profile.domain.backup

import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.ProfileException
import java.io.IOException

class ImportMnemonicRecoveryTest {
    private val mnemonic = MnemonicWithPassphrase.sample()
    private val id = FactorSourceId.Hash.init(FactorSourceKind.DEVICE, mnemonic)
    private val mnemonics = mockk<MnemonicRepository>()
    private val preferences = mockk<PreferencesManager>(relaxed = true)
    private val sut = ImportMnemonicUseCase(mnemonics, preferences)

    @Test
    fun `backup import recovers retained invalidated key before saving the seed`() = runTest {
        var invalidatedKeyRetained = true
        coEvery { mnemonics.clearInvalidatedMnemonics() } coAnswers {
            invalidatedKeyRetained = false
            Result.success(Unit)
        }
        coEvery { mnemonics.saveMnemonic(id, mnemonic) } coAnswers {
            if (invalidatedKeyRetained) Result.failure(IOException("Retained V2 key is invalidated"))
            else Result.success(Unit)
        }

        assertTrue(sut(id, mnemonic).isSuccess)

        coVerifyOrder {
            mnemonics.clearInvalidatedMnemonics()
            mnemonics.saveMnemonic(id, mnemonic)
            preferences.markFactorSourceBackedUp(id)
        }
    }

    @Test
    fun `failed recovery prevents saving and is propagated so import can be retried`() = runTest {
        val failure = IOException("Keystore recovery failed")
        coEvery { mnemonics.clearInvalidatedMnemonics() } returnsMany listOf(Result.failure(failure), Result.success(Unit))
        coEvery { mnemonics.saveMnemonic(id, mnemonic) } returns Result.success(Unit)

        assertSame(failure, sut(id, mnemonic).exceptionOrNull())
        coVerify(exactly = 0) { mnemonics.saveMnemonic(any(), any()) }
        coVerify(exactly = 0) { preferences.markFactorSourceBackedUp(any()) }

        assertTrue(sut(id, mnemonic).isSuccess)
        coVerify(exactly = 2) { mnemonics.clearInvalidatedMnemonics() }
        coVerify(exactly = 1) { mnemonics.saveMnemonic(id, mnemonic) }
        coVerify(exactly = 1) { preferences.markFactorSourceBackedUp(id) }
    }

    @Test
    fun `incorrect seed does not reset keys or write any data`() = runTest {
        assertEquals(ProfileException.InvalidMnemonic, sut(id, mnemonic.copy(passphrase = "wrong passphrase")).exceptionOrNull())
        coVerify(exactly = 0) { mnemonics.clearInvalidatedMnemonics() }
        coVerify(exactly = 0) { mnemonics.saveMnemonic(any(), any()) }
        coVerify(exactly = 0) { preferences.markFactorSourceBackedUp(any()) }
    }

    @Test
    fun `failed save after recovery does not mark the seed as backed up`() = runTest {
        coEvery { mnemonics.clearInvalidatedMnemonics() } returns Result.success(Unit)
        coEvery { mnemonics.saveMnemonic(id, mnemonic) } returns Result.failure(IOException("Write failed"))

        assertEquals(ProfileException.SecureStorageAccess, sut(id, mnemonic).exceptionOrNull())
        coVerify(exactly = 0) { preferences.markFactorSourceBackedUp(any()) }
    }
}
