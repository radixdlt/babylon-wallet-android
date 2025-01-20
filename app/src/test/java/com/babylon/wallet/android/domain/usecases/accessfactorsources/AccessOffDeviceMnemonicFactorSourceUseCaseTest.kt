package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.OffDeviceMnemonicHint
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.factorSourceId
import com.radixdlt.sargon.extensions.sign
import com.radixdlt.sargon.newOffDeviceMnemonicFactorSourceFromMnemonicWithPassphrase
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import com.radixdlt.sargon.os.signing.TransactionSignRequestInput
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import rdx.works.profile.domain.UpdateFactorSourceLastUsedUseCase

class AccessOffDeviceMnemonicFactorSourceUseCaseTest {

    private val updateFactorSourceLastUsedUseCase = mockk<UpdateFactorSourceLastUsedUseCase>()
    private val sut = AccessOffDeviceMnemonicFactorSourceUseCase(
        updateFactorSourceLastUsedUseCase = updateFactorSourceLastUsedUseCase
    )

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testOnSeedPhraseConfirmedValid() = runTest {
        coEvery { updateFactorSourceLastUsedUseCase(factorSourceId = any()) } returns Unit
        val mnemonicWithPassphrase = MnemonicWithPassphrase.sample()
        val offDeviceMnemonicFs = newOffDeviceMnemonicFactorSourceFromMnemonicWithPassphrase(
            mwp = mnemonicWithPassphrase,
            hint = OffDeviceMnemonicHint(
                label = DisplayName("test"),
                wordCount = mnemonicWithPassphrase.mnemonic.wordCount
            )
        )
        val words = mnemonicWithPassphrase.mnemonic.words.map {
            SeedPhraseWord(
                index = it.index.inner.toInt(),
                value = it.word,
                state = SeedPhraseWord.State.Valid
            )
        }

        // Launch a coroutine to expect a result so onSeedPhraseConfirmed can not suspend.
        GlobalScope.launch {
            sut.derivePublicKeys(
                factorSource = offDeviceMnemonicFs.asGeneral(),
                input = KeyDerivationRequestPerFactorSource(
                    factorSourceId = offDeviceMnemonicFs.id,
                    derivationPaths = emptyList()
                )
            )
        }

        val result = sut.onSeedPhraseConfirmed(
            factorSourceId = offDeviceMnemonicFs.id,
            words = words
        )

        assertEquals(
            AccessOffDeviceMnemonicFactorSourceUseCase.SeedPhraseValidity.Valid,
            result
        )
    }

    @Test
    fun testOnSeedPhraseConfirmedInvalid() = runTest {
        val mnemonicWithPassphrase = MnemonicWithPassphrase.sample()
        val words = List(size = 24) {
            SeedPhraseWord(index = it, value = "")
        }
        val validFactorSourceId = mnemonicWithPassphrase.factorSourceId(kind = FactorSourceKind.OFF_DEVICE_MNEMONIC)

        val result = sut.onSeedPhraseConfirmed(
            factorSourceId = validFactorSourceId,
            words = words
        )

        assertEquals(
            AccessOffDeviceMnemonicFactorSourceUseCase.SeedPhraseValidity.InvalidMnemonic,
            result
        )
    }

    @Test
    fun testOnSeedPhraseConfirmedDoesNotDeriveFactorSource() = runTest {
        val mnemonicWithPassphrase = MnemonicWithPassphrase.sample()
        val words = mnemonicWithPassphrase.mnemonic.words.map {
            SeedPhraseWord(
                index = it.index.inner.toInt(),
                value = it.word,
                state = SeedPhraseWord.State.Valid
            )
        }

        val invalidMnemonicWithPassphrase = MnemonicWithPassphrase.sample.other()
        val invalidFactorSourceId = invalidMnemonicWithPassphrase.factorSourceId(kind = FactorSourceKind.OFF_DEVICE_MNEMONIC)

        val result = sut.onSeedPhraseConfirmed(
            factorSourceId = invalidFactorSourceId,
            words = words
        )

        assertEquals(
            AccessOffDeviceMnemonicFactorSourceUseCase.SeedPhraseValidity.DoesNotDeriveFactorSourceId,
            result
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testDerivePublicKeys() = runTest {
        coEvery { updateFactorSourceLastUsedUseCase(factorSourceId = any()) } returns Unit
        val mnemonicWithPassphrase = MnemonicWithPassphrase.sample()
        val offDeviceMnemonicFs = newOffDeviceMnemonicFactorSourceFromMnemonicWithPassphrase(
            mwp = mnemonicWithPassphrase,
            hint = OffDeviceMnemonicHint(
                label = DisplayName("test"),
                wordCount = mnemonicWithPassphrase.mnemonic.wordCount
            )
        )
        val words = mnemonicWithPassphrase.mnemonic.words.map {
            SeedPhraseWord(
                index = it.index.inner.toInt(),
                value = it.word,
                state = SeedPhraseWord.State.Valid
            )
        }

        GlobalScope.launch {
            sut.onSeedPhraseConfirmed(
                factorSourceId = offDeviceMnemonicFs.id,
                words = words
            )
        }

        val path = DerivationPath.sample()
        val result = sut.derivePublicKeys(
            factorSource = offDeviceMnemonicFs.asGeneral(),
            input = KeyDerivationRequestPerFactorSource(
                factorSourceId = offDeviceMnemonicFs.id,
                derivationPaths = listOf(path)
            )
        )

        assertEquals(
            listOf(
                HierarchicalDeterministicFactorInstance(
                    factorSourceId = offDeviceMnemonicFs.id,
                    publicKey = mnemonicWithPassphrase.derivePublicKey(path = path)
                )
            ),
            result.getOrNull()
        )
        coVerify { updateFactorSourceLastUsedUseCase(factorSourceId = offDeviceMnemonicFs.id.asGeneral()) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testSignMono() = runTest {
        coEvery { updateFactorSourceLastUsedUseCase(factorSourceId = any()) } returns Unit
        val mnemonicWithPassphrase = MnemonicWithPassphrase.sample()
        val offDeviceMnemonicFs = newOffDeviceMnemonicFactorSourceFromMnemonicWithPassphrase(
            mwp = mnemonicWithPassphrase,
            hint = OffDeviceMnemonicHint(
                label = DisplayName("test"),
                wordCount = mnemonicWithPassphrase.mnemonic.wordCount
            )
        )
        val words = mnemonicWithPassphrase.mnemonic.words.map {
            SeedPhraseWord(
                index = it.index.inner.toInt(),
                value = it.word,
                state = SeedPhraseWord.State.Valid
            )
        }

        GlobalScope.launch {
            sut.onSeedPhraseConfirmed(
                factorSourceId = offDeviceMnemonicFs.id,
                words = words
            )
        }

        val transaction = TransactionIntent.sample()
        val ownedFactorInstance = OwnedFactorInstance(
            owner = AddressOfAccountOrPersona.sampleMainnet(),
            factorInstance = HierarchicalDeterministicFactorInstance.sample()
        )
        val input = PerFactorSourceInput<Signable.Payload.Transaction, Signable.ID.Transaction>(
            factorSourceId = offDeviceMnemonicFs.id,
            perTransaction = listOf(
                TransactionSignRequestInput(
                    payload = Signable.Payload.Transaction(transaction.compile()),
                    factorSourceId = offDeviceMnemonicFs.id,
                    ownedFactorInstances = listOf(ownedFactorInstance)
                ),
            ),
            invalidTransactionsIfNeglected = emptyList()
        )
        val result = sut.signMono(
            factorSource = offDeviceMnemonicFs.asGeneral(),
            input = input
        )

        assertEquals(
            PerFactorOutcome(
                factorSourceId = offDeviceMnemonicFs.id,
                outcome = FactorOutcome.Signed(
                    producedSignatures = mnemonicWithPassphrase.sign(input)
                )
            ),
            result.getOrNull()
        )
        coVerify { updateFactorSourceLastUsedUseCase(factorSourceId = offDeviceMnemonicFs.id.asGeneral()) }
    }
}