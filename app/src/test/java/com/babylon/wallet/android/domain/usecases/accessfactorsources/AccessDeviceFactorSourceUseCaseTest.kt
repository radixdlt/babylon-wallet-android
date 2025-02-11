package com.babylon.wallet.android.domain.usecases.accessfactorsources

import androidx.biometric.BiometricPrompt
import com.babylon.wallet.android.domain.usecases.BiometricsAuthenticateUseCase
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HostInfo
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.newDeviceFactorSourceBabylon
import com.radixdlt.sargon.os.driver.BiometricsFailure
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import com.radixdlt.sargon.os.signing.TransactionSignRequestInput
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import rdx.works.core.sargon.signInteractorInput
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.UpdateFactorSourceLastUsedUseCase

class AccessDeviceFactorSourceUseCaseTest {

    private val biometricsAuthenticateUseCase = mockk<BiometricsAuthenticateUseCase>()
    private val mnemonicRepository = mockk<MnemonicRepository>()
    private val updateFactorSourceLastUsedUseCase = mockk<UpdateFactorSourceLastUsedUseCase>()

    private val mnemonicWithPassphrase = MnemonicWithPassphrase.sample()
    private val device = newDeviceFactorSourceBabylon(
        isMain = true,
        mnemonicWithPassphrase = mnemonicWithPassphrase,
        hostInfo = HostInfo.sample()
    )

    val sut = AccessDeviceFactorSourceUseCase(
        biometricsAuthenticateUseCase = biometricsAuthenticateUseCase,
        mnemonicRepository = mnemonicRepository,
        updateFactorSourceLastUsedUseCase = updateFactorSourceLastUsedUseCase
    )

    @Test
    fun derivePublicKeysFailsDueToNoBiometrics() = runTest {
        mockMnemonicAccess(
            id = device.id,
            keyExists = true,
            biometricsSucceeds = false
        )

        val result = sut.derivePublicKeys(
            factorSource = device.asGeneral(),
            input = KeyDerivationRequestPerFactorSource(
                factorSourceId = device.id,
                derivationPaths = listOf(DerivationPath.sample())
            )
        )

        assertTrue(result.exceptionOrNull() is CommonException.SecureStorageAccessException)
    }

    @Test
    fun signMonoFailsDueToNoBiometrics() = runTest {
        mockMnemonicAccess(
            id = device.id,
            keyExists = true,
            biometricsSucceeds = false
        )

        val result = sut.signMono(
            factorSource = device.asGeneral(),
            input = PerFactorSourceInput(
                factorSourceId = device.id,
                perTransaction = emptyList(),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertTrue(result.exceptionOrNull() is CommonException.SecureStorageAccessException)
    }

    @Test
    fun spotCheckFailsDueToNoBiometrics() = runTest {
        mockMnemonicAccess(
            id = device.id,
            keyExists = true,
            biometricsSucceeds = false
        )

        val result = sut.spotCheck(factorSource = device.asGeneral())

        assertTrue(result.exceptionOrNull() is CommonException.SecureStorageAccessException)
    }

    @Test
    fun derivePublicKeysFailsDueToNoMnemonic() = runTest {
        mockMnemonicAccess(
            id = device.id,
            keyExists = false
        )

        val result = sut.derivePublicKeys(
            factorSource = device.asGeneral(),
            input = KeyDerivationRequestPerFactorSource(
                factorSourceId = device.id,
                derivationPaths = listOf(DerivationPath.sample())
            )
        )

        assertTrue(result.exceptionOrNull() is CommonException.UnableToLoadMnemonicFromSecureStorage)
    }

    @Test
    fun signMonoFailsDueToNoMnemonic() = runTest {
        coEvery { updateFactorSourceLastUsedUseCase.invoke(factorSourceId = device.id.asGeneral()) } just Runs
        mockMnemonicAccess(
            id = device.id,
            keyExists = false
        )

        val result = sut.signMono(
            factorSource = device.asGeneral(),
            input = PerFactorSourceInput(
                factorSourceId = device.id,
                perTransaction = emptyList(),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertTrue(result.exceptionOrNull() is CommonException.UnableToLoadMnemonicFromSecureStorage)
    }

    @Test
    fun spotCheckFailsDueToNoMnemonic() = runTest {
        coEvery { updateFactorSourceLastUsedUseCase.invoke(factorSourceId = device.id.asGeneral()) } just Runs
        mockMnemonicAccess(
            id = device.id,
            keyExists = false
        )

        val result = sut.spotCheck(factorSource = device.asGeneral())

        assertTrue(result.exceptionOrNull() is CommonException.UnableToLoadMnemonicFromSecureStorage)
    }

    @Test
    fun derivePublicKeysFailsWhenMnemonicShouldExistButFailsToLoad() = runTest {
        mockMnemonicAccess(
            id = device.id,
            mnemonicInRepository = null
        )

        val result = sut.derivePublicKeys(
            factorSource = device.asGeneral(),
            input = KeyDerivationRequestPerFactorSource(
                factorSourceId = device.id,
                derivationPaths = listOf(DerivationPath.sample())
            )
        )

        assertTrue(result.exceptionOrNull() is CommonException.UnableToLoadMnemonicFromSecureStorage)
    }

    @Test
    fun signMonoFailsWhenMnemonicShouldExistButFailsToLoad() = runTest {
        mockMnemonicAccess(
            id = device.id,
            mnemonicInRepository = null
        )

        val result = sut.signMono(
            factorSource = device.asGeneral(),
            input = PerFactorSourceInput(
                factorSourceId = device.id,
                perTransaction = emptyList(),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertTrue(result.exceptionOrNull() is CommonException.UnableToLoadMnemonicFromSecureStorage)
    }

    @Test
    fun spotCheckFailsWhenMnemonicShouldExistButFailsToLoad() = runTest {
        mockMnemonicAccess(
            id = device.id,
            mnemonicInRepository = null
        )

        val result = sut.spotCheck(factorSource = device.asGeneral())

        assertTrue(result.exceptionOrNull() is CommonException.UnableToLoadMnemonicFromSecureStorage)
    }

    @Test
    fun derivePublicKeysFailsWhenBiometricsSucceedButCouldNotDecrypt() = runTest {
        mockMnemonicAccess(
            id = device.id,
            mnemonicInRepository = mnemonicWithPassphrase,
            failsToAccessMnemonicEvenIfBiometricsProvided = true
        )

        val result = sut.derivePublicKeys(
            factorSource = device.asGeneral(),
            input = KeyDerivationRequestPerFactorSource(
                factorSourceId = device.id,
                derivationPaths = listOf(DerivationPath.sample())
            )
        )

        assertTrue(result.exceptionOrNull() is CommonException.SecureStorageReadException)
    }

    @Test
    fun signMonoFailsWhenBiometricsSucceedButCouldNotDecrypt() = runTest {
        mockMnemonicAccess(
            id = device.id,
            mnemonicInRepository = mnemonicWithPassphrase,
            failsToAccessMnemonicEvenIfBiometricsProvided = true
        )

        val result = sut.signMono(
            factorSource = device.asGeneral(),
            input = PerFactorSourceInput(
                factorSourceId = device.id,
                perTransaction = emptyList(),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertTrue(result.exceptionOrNull() is CommonException.SecureStorageReadException)
    }

    @Test
    fun spotCheckFailsWhenBiometricsSucceedButCouldNotDecrypt() = runTest {
        mockMnemonicAccess(
            id = device.id,
            mnemonicInRepository = mnemonicWithPassphrase,
            failsToAccessMnemonicEvenIfBiometricsProvided = true
        )

        val result = sut.spotCheck(factorSource = device.asGeneral())

        assertTrue(result.exceptionOrNull() is CommonException.SecureStorageReadException)
    }

    @Test
    fun testDerivePublicKeySucceeds() = runTest {
        coEvery { updateFactorSourceLastUsedUseCase.invoke(factorSourceId = device.id.asGeneral()) } just Runs
        mockMnemonicAccess(
            id = device.id,
            mnemonicInRepository = mnemonicWithPassphrase
        )

        val derivationPath = DerivationPath.sample()
        val result = sut.derivePublicKeys(
            factorSource = device.asGeneral(),
            input = KeyDerivationRequestPerFactorSource(
                factorSourceId = device.id,
                derivationPaths = listOf(derivationPath)
            )
        )

        assertEquals(
            listOf(
                HierarchicalDeterministicFactorInstance(
                    factorSourceId = device.id,
                    publicKey = mnemonicWithPassphrase.derivePublicKey(path = derivationPath)
                )
            ),
            result.getOrNull()
        )
        coVerify { updateFactorSourceLastUsedUseCase(factorSourceId = device.id.asGeneral()) }
    }

    @Test
    fun testSignMonoSucceeds() = runTest {
        coEvery { updateFactorSourceLastUsedUseCase.invoke(factorSourceId = device.id.asGeneral()) } just Runs
        mockMnemonicAccess(
            id = device.id,
            mnemonicInRepository = mnemonicWithPassphrase
        )

        val input = PerFactorSourceInput<Signable.Payload.Transaction, Signable.ID.Transaction>(
            factorSourceId = device.id,
            perTransaction = listOf(
                TransactionSignRequestInput(
                    payload = Signable.Payload.Transaction(TransactionIntent.sample().compile()),
                    factorSourceId = device.id,
                    ownedFactorInstances = listOf(
                        OwnedFactorInstance(
                            owner = AddressOfAccountOrPersona.sampleMainnet(),
                            factorInstance = HierarchicalDeterministicFactorInstance.sample()
                        )
                    )
                )
            ),
            invalidTransactionsIfNeglected = emptyList()
        )
        val result = sut.signMono(
            factorSource = device.asGeneral(),
            input = input
        )

        assertEquals(
            PerFactorOutcome(
                factorSourceId = device.id,
                outcome = FactorOutcome.Signed(
                    producedSignatures = mnemonicWithPassphrase.signInteractorInput(input = input)
                )
            ),
            result.getOrNull()
        )
        coVerify { updateFactorSourceLastUsedUseCase(factorSourceId = device.id.asGeneral()) }
    }

    @Test
    fun testSpotCheckSucceeds() = runTest {
//        coEvery { updateFactorSourceLastUsedUseCase.invoke(factorSourceId = device.id.asGeneral()) } just Runs
        mockMnemonicAccess(
            id = device.id,
            mnemonicInRepository = mnemonicWithPassphrase
        )

        val result = sut.spotCheck(factorSource = device.asGeneral())

        assertEquals(
            true,
            result.getOrNull()
        )
//        coVerify { updateFactorSourceLastUsedUseCase(factorSourceId = device.id.asGeneral()) }
    }

    private suspend fun mockMnemonicAccess(
        id: FactorSourceIdFromHash,
        keyExists: Boolean = true,
        biometricsSucceeds: Boolean = true,
        mnemonicInRepository: MnemonicWithPassphrase? = null,
        failsToAccessMnemonicEvenIfBiometricsProvided: Boolean = false,
    ) {
        coEvery { mnemonicRepository.mnemonicExist(key = id.asGeneral()) } returns keyExists
        coEvery { biometricsAuthenticateUseCase.asResult() } answers {
            if (biometricsSucceeds) {
                Result.success(Unit)
            } else {
                Result.failure(
                    BiometricsFailure(
                        errorCode = BiometricPrompt.ERROR_USER_CANCELED,
                        errorMessage = "User cancelled"
                    )
                )
            }
        }
        coEvery { mnemonicRepository.readMnemonic(key = id.asGeneral()) } answers {
            if (failsToAccessMnemonicEvenIfBiometricsProvided) {
                Result.failure(ProfileException.SecureStorageAccess)
            } else if (mnemonicInRepository != null) {
                Result.success(mnemonicInRepository)
            } else {
                Result.failure(ProfileException.NoMnemonic)
            }
        }
    }
}