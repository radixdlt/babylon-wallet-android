package com.babylon.wallet.android.presentation.accessfactorsources

import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessArculusFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessDeviceFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessOffDeviceMnemonicFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.DeviceMnemonicLoadError
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HostInfo
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.phrase
import com.radixdlt.sargon.newDeviceFactorSourceBabylon
import com.radixdlt.sargon.newMnemonicSampleDevice
import com.radixdlt.sargon.newMnemonicSampleDeviceOther
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class AccessFactorSourceDelegateTest {

    private val accessDeviceFactorSource = mockk<AccessDeviceFactorSourceUseCase>()
    private val accessOffDeviceMnemonicFactorSource = mockk<AccessOffDeviceMnemonicFactorSourceUseCase>()
    private val accessArculusFactorSource = mockk<AccessArculusFactorSourceUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val mnemonicWithPassphrase = MnemonicWithPassphrase.init(
        phrase = newMnemonicSampleDevice().phrase
    )
    private val factorSource = FactorSource.Device(
        newDeviceFactorSourceBabylon(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            hostInfo = HostInfo.sample()
        )
    )

    @Test
    fun emptySeedPhraseIsNotComplete() {
        assertFalse(SeedPhraseInputDelegate.State().isInputComplete())
    }

    @Test
    fun missingMnemonicIsValidatedSavedAndReturned() = runTest {
        coEvery { accessDeviceFactorSource.loadMnemonic(factorSource) } returns Result.failure(
            DeviceMnemonicLoadError.Missing
        )
        coEvery {
            accessDeviceFactorSource.saveMnemonic(factorSource, mnemonicWithPassphrase)
        } returns Result.success(Unit)
        val accessed = CompletableDeferred<AccessedFactorSource>()
        val delegate = createDelegate(accessed)

        advanceUntilIdle()

        assertEquals(
            AccessFactorSourceDelegate.State.AccessMode.ManualDeviceMnemonic,
            delegate.state.value.accessMode
        )
        assertEquals(
            AccessFactorSourceDelegate.State.DeviceMnemonicPersistence.SaveAfterValidation,
            delegate.state.value.deviceMnemonicPersistence
        )

        enterMnemonic(delegate)
        delegate.onInputConfirmed()
        advanceUntilIdle()

        assertEquals(
            AccessedFactorSource.Device(factorSource, mnemonicWithPassphrase),
            accessed.await()
        )
        coVerify(exactly = 1) {
            accessDeviceFactorSource.saveMnemonic(factorSource, mnemonicWithPassphrase)
        }
    }

    @Test
    fun readFailureUsesValidatedMnemonicWithoutSaving() = runTest {
        coEvery { accessDeviceFactorSource.loadMnemonic(factorSource) } returns Result.failure(
            DeviceMnemonicLoadError.ReadFailure(IllegalStateException("Could not decrypt"))
        )
        val accessed = CompletableDeferred<AccessedFactorSource>()
        val delegate = createDelegate(accessed)

        advanceUntilIdle()

        assertEquals(
            AccessFactorSourceDelegate.State.DeviceMnemonicPersistence.Ephemeral,
            delegate.state.value.deviceMnemonicPersistence
        )

        enterMnemonic(delegate)
        delegate.onInputConfirmed()
        advanceUntilIdle()

        assertTrue(accessed.await() is AccessedFactorSource.Device)
        coVerify(exactly = 0) { accessDeviceFactorSource.saveMnemonic(any(), any()) }
    }

    @Test
    fun mismatchedStoredMnemonicRequiresManualInputWithoutSaving() = runTest {
        val mismatchedMnemonic = MnemonicWithPassphrase.init(
            phrase = newMnemonicSampleDeviceOther().phrase
        )
        coEvery {
            accessDeviceFactorSource.loadMnemonic(factorSource)
        } returns Result.success(mismatchedMnemonic)
        val accessed = CompletableDeferred<AccessedFactorSource>()
        val delegate = createDelegate(accessed)

        advanceUntilIdle()

        assertEquals(
            AccessFactorSourceDelegate.State.AccessMode.ManualDeviceMnemonic,
            delegate.state.value.accessMode
        )
        assertEquals(
            AccessFactorSourceDelegate.State.DeviceMnemonicPersistence.Ephemeral,
            delegate.state.value.deviceMnemonicPersistence
        )
        assertFalse(accessed.isCompleted)
        coVerify(exactly = 0) { accessDeviceFactorSource.saveMnemonic(any(), any()) }
    }

    @Test
    fun cancelledMnemonicLoadResultIsIgnored() = runTest {
        val loadResult = CompletableDeferred<Result<MnemonicWithPassphrase>>()
        coEvery { accessDeviceFactorSource.loadMnemonic(factorSource) } coAnswers {
            try {
                loadResult.await()
            } catch (error: CancellationException) {
                Result.failure(DeviceMnemonicLoadError.ReadFailure(error))
            }
        }
        val accessed = CompletableDeferred<AccessedFactorSource>()
        val delegate = createDelegate(accessed)

        advanceUntilIdle()
        assertEquals(
            AccessFactorSourceDelegate.State.AccessMode.LoadingDeviceMnemonic,
            delegate.state.value.accessMode
        )

        delegate.onCancelAccess()
        advanceUntilIdle()

        assertEquals(
            AccessFactorSourceDelegate.State.AccessMode.Completed,
            delegate.state.value.accessMode
        )
        assertFalse(accessed.isCompleted)
    }

    @Test
    fun cancellingManualAccessClearsSensitiveInput() = runTest {
        coEvery { accessDeviceFactorSource.loadMnemonic(factorSource) } returns Result.failure(
            DeviceMnemonicLoadError.Missing
        )
        val delegate = createDelegate(CompletableDeferred())

        advanceUntilIdle()
        enterMnemonic(delegate)

        delegate.onCancelAccess()
        advanceUntilIdle()

        assertEquals(
            AccessFactorSourceDelegate.State.AccessMode.Completed,
            delegate.state.value.accessMode
        )
        assertTrue(delegate.state.value.seedPhraseInputState.inputWords.isEmpty())
        assertTrue(delegate.state.value.seedPhraseInputState.delegateState.bip39Passphrase.isEmpty())
        assertFalse(delegate.state.value.seedPhraseInputState.isConfirmButtonEnabled)
    }

    @Test
    fun cancellingMnemonicSaveDoesNotReturnAccessedFactorSource() = runTest {
        coEvery { accessDeviceFactorSource.loadMnemonic(factorSource) } returns Result.failure(
            DeviceMnemonicLoadError.Missing
        )
        val saveStarted = CompletableDeferred<Unit>()
        coEvery {
            accessDeviceFactorSource.saveMnemonic(factorSource, mnemonicWithPassphrase)
        } coAnswers {
            saveStarted.complete(Unit)
            try {
                awaitCancellation()
            } catch (error: CancellationException) {
                Result.failure(error)
            }
        }
        val accessed = CompletableDeferred<AccessedFactorSource>()
        val delegate = createDelegate(accessed)

        advanceUntilIdle()
        enterMnemonic(delegate)
        delegate.onInputConfirmed()
        advanceUntilIdle()
        assertTrue(saveStarted.isCompleted)

        delegate.onCancelAccess()
        advanceUntilIdle()

        assertFalse(accessed.isCompleted)
    }

    @Test
    fun confirmingAgainDoesNotCancelMnemonicSave() = runTest {
        coEvery { accessDeviceFactorSource.loadMnemonic(factorSource) } returns Result.failure(
            DeviceMnemonicLoadError.Missing
        )
        val saveStarted = CompletableDeferred<Unit>()
        val finishSave = CompletableDeferred<Unit>()
        coEvery {
            accessDeviceFactorSource.saveMnemonic(factorSource, mnemonicWithPassphrase)
        } coAnswers {
            saveStarted.complete(Unit)
            finishSave.await()
            Result.success(Unit)
        }
        val accessed = CompletableDeferred<AccessedFactorSource>()
        val delegate = createDelegate(accessed)

        advanceUntilIdle()
        enterMnemonic(delegate)
        delegate.onInputConfirmed()
        advanceUntilIdle()
        assertTrue(saveStarted.isCompleted)

        delegate.onInputConfirmed()
        finishSave.complete(Unit)
        advanceUntilIdle()

        assertEquals(
            AccessedFactorSource.Device(factorSource, mnemonicWithPassphrase),
            accessed.await()
        )
        coVerify(exactly = 1) {
            accessDeviceFactorSource.saveMnemonic(factorSource, mnemonicWithPassphrase)
        }
    }

    @Test
    fun confirmationUsesLatestPassphrase() = runTest {
        val latestMnemonic = MnemonicWithPassphrase(
            mnemonic = mnemonicWithPassphrase.mnemonic,
            passphrase = "latest"
        )
        val latestFactorSource = FactorSource.Device(
            newDeviceFactorSourceBabylon(
                mnemonicWithPassphrase = latestMnemonic,
                hostInfo = HostInfo.sample()
            )
        )
        coEvery { accessDeviceFactorSource.loadMnemonic(latestFactorSource) } returns Result.failure(
            DeviceMnemonicLoadError.ReadFailure(IllegalStateException("Could not decrypt"))
        )
        val accessed = CompletableDeferred<AccessedFactorSource>()
        val delegate = createDelegate(
            accessed = accessed,
            factorSource = latestFactorSource
        )

        advanceUntilIdle()
        latestMnemonic.mnemonic.words.forEachIndexed { index, word ->
            delegate.onSeedPhraseWordChanged(index, word.word)
            advanceTimeBy(100)
        }
        advanceUntilIdle()

        delegate.onPassphraseChanged(latestMnemonic.passphrase)
        delegate.onInputConfirmed()
        advanceUntilIdle()

        assertEquals(
            AccessedFactorSource.Device(latestFactorSource, latestMnemonic),
            accessed.await()
        )
    }

    @Test
    fun retryableDeviceAccessResultRestoresAutomaticMode() = runTest {
        coEvery {
            accessDeviceFactorSource.loadMnemonic(factorSource)
        } returns Result.success(mnemonicWithPassphrase)
        val delegate = createDelegate(
            accessed = CompletableDeferred(),
            accessOutcome = AccessFactorSourceDelegate.AccessOutcome.Retryable
        )

        advanceUntilIdle()

        assertEquals(
            AccessFactorSourceDelegate.State.AccessMode.Automatic,
            delegate.state.value.accessMode
        )
        assertTrue(delegate.state.value.isRetryEnabled)

        delegate.onRetry()
        advanceUntilIdle()

        coVerify(exactly = 2) {
            accessDeviceFactorSource.loadMnemonic(factorSource)
        }
    }

    private fun TestScope.createDelegate(
        accessed: CompletableDeferred<AccessedFactorSource>,
        factorSource: FactorSource.Device = this@AccessFactorSourceDelegateTest.factorSource,
        accessOutcome: AccessFactorSourceDelegate.AccessOutcome = AccessFactorSourceDelegate.AccessOutcome.Completed
    ) = AccessFactorSourceDelegate(
        viewModelScope = backgroundScope,
        factorSource = factorSource,
        getProfileUseCase = getProfileUseCase,
        accessDeviceFactorSource = accessDeviceFactorSource,
        accessOffDeviceMnemonicFactorSource = accessOffDeviceMnemonicFactorSource,
        accessArculusFactorSourceUseCase = accessArculusFactorSource,
        onAccessCallback = {
            accessed.complete(it)
            Result.success(accessOutcome)
        },
        onDismissCallback = {},
        onFailCallback = {}
    )

    private fun TestScope.enterMnemonic(delegate: AccessFactorSourceDelegate) {
        mnemonicWithPassphrase.mnemonic.words.forEachIndexed { index, word ->
            delegate.onSeedPhraseWordChanged(index, word.word)
            advanceTimeBy(100)
        }
        delegate.onPassphraseChanged(mnemonicWithPassphrase.passphrase)
        advanceUntilIdle()
        assertTrue(delegate.state.value.seedPhraseInputState.isConfirmButtonEnabled)
    }
}
