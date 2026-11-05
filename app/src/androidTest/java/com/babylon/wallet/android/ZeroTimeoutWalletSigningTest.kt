package com.babylon.wallet.android

import android.os.Build
import android.security.keystore.KeyInfo
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.usecases.signing.SignAuthUseCase
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toBagOfBytes
import com.radixdlt.sargon.newWalletInteractionVersionCurrent
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.os.SargonOsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import javax.crypto.SecretKey
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory

/** Opt-in only: uses the real wallet process and signing UI, but never submits a transaction. */
@RunWith(AndroidJUnit4::class)
class ZeroTimeoutWalletSigningTest {
    @Test
    fun freshWalletKeyRequiresPerOperationAuthentication() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("disposableWallet") == "true")
        assumeTrue(Build.HARDWARE.contains("ranchu"))
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = keyStore.getKey("EncryptedMnemonicAlias", null) as SecretKey
        val info = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
            .getKeySpec(key, KeyInfo::class.java) as KeyInfo
        assertTrue(info.isUserAuthenticationRequired)
        assertEquals(0, info.userAuthenticationValidityDurationSeconds)
        Log.i("ZeroTimeoutWalletFlow", "Fresh wallet key requires authentication with timeout zero")
    }

    @Test
    fun existingLegacySeedCanSignAfterSdkUpdate() = verifySigning("ZeroTimeoutControl", timeoutSeconds = null)

    @Test
    fun freshProtectedSeedCanSign() = verifySigning("ZeroTimeoutFresh", timeoutSeconds = 0)

    @Test
    fun existingThirtySecondSeedCanSignAfterSdkUpdate() = verifySigning("ZeroTimeoutTimed", timeoutSeconds = 30)

    /** Run only on an empty disposable wallet, before onboarding with the unchanged SDK. */
    @Test
    fun prepareHistoricalThirtySecondKey() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("disposableWallet") == "true")
        assumeTrue(Build.HARDWARE.contains("ranchu"))
        assertEquals("com.radixpublishing.radixwallet.android.debug",
            InstrumentationRegistry.getInstrumentation().targetContext.packageName)
        val alias = "EncryptedMnemonicAlias"
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertFalse("Never replace a wallet key", keyStore.containsAlias(alias))
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(false)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(false)
            .setUserAuthenticationParameters(30, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
            .build())
        generator.generateKey()
    }

    private fun verifySigning(accountName: String, timeoutSeconds: Int?) = runBlocking<Unit> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("disposableWallet") == "true")
        assumeTrue(Build.HARDWARE.contains("ranchu"))
        ActivityScenario.launch(MainActivity::class.java).use {
            // Observe the already-injected production singleton, without changing the app's DI graph.
            val field = SargonOsManager::class.java.getDeclaredField("instance").apply { isAccessible = true }
            val manager = withTimeout(30_000) {
                while (field.get(null) == null) delay(100)
                (field.get(null) as SargonOsManager).also { manager ->
                    manager.sargonState.first { state -> state is SargonOsState.Booted }
                }
            }
            val network = manager.sargonOs.currentNetwork()
            val account = network.accounts.single()
            assertEquals("Only use the named disposable fixture", accountName, account.displayName.value)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key = keyStore.getKey("EncryptedMnemonicAlias", null) as SecretKey
            val info = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
                .getKeySpec(key, KeyInfo::class.java) as KeyInfo
            assertEquals("SDK updates must retain the existing key's protection", timeoutSeconds != null, info.isUserAuthenticationRequired)
            if (timeoutSeconds != null) assertEquals(timeoutSeconds.toInt(), info.userAuthenticationValidityDurationSeconds)

            val signatures = withTimeout(120_000) {
                SignAuthUseCase(manager).accounts(
                    challenge = Exactly32Bytes.init(ByteArray(32) { 7 }.toBagOfBytes()),
                    accounts = listOf(account),
                    metadata = DappToWalletInteraction.RequestMetadata(
                        version = newWalletInteractionVersionCurrent(),
                        networkId = network.id,
                        origin = "https://example.com",
                        dAppDefinitionAddress = account.address.string,
                        isInternal = true
                    )
                ).getOrThrow()
            }
            assertTrue(signatures.containsKey(account))
            Log.i("ZeroTimeoutWalletFlow", "Key timeout: $timeoutSeconds; actual auth signing returned ${signatures.size} signature")
        }
    }
}
