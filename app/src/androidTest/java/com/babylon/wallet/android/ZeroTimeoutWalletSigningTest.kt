package com.babylon.wallet.android

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.UserNotAuthenticatedException
import android.security.keystore.KeyProperties
import android.util.Log
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.usecases.signing.SignAuthUseCase
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.Bip39Language
import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.DeviceFactorSourceType
import com.radixdlt.sargon.Mnemonic
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.toJson
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Provider
import rdx.works.profile.datastore.EncryptedPreferencesManager

/** Opt-in only: uses the real wallet process and signing UI, but never submits a transaction. */
@RunWith(AndroidJUnit4::class)
class ZeroTimeoutWalletSigningTest {
    @Test
    fun freshWalletKeyRequiresAuthentication() {
        requireDisposableWallet()
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = keyStore.getKey("EncryptedMnemonicAliasV2", null) as SecretKey
        val info = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
            .getKeySpec(key, KeyInfo::class.java) as KeyInfo
        assertTrue(info.isUserAuthenticationRequired)
        assertMigratedKeyPolicy(info)
        Log.i("ZeroTimeoutWalletFlow", "Fresh wallet uses the protected alias and requires authentication")
    }

    @Test
    fun existingLegacySeedCanSignAfterSdkUpdate() = verifySigning("ZeroTimeoutControl", timeoutSeconds = null)

    @Test
    fun freshProtectedSeedCanSign() = verifySigning(
        "ZeroTimeoutFresh", timeoutSeconds = expectedMnemonicTimeout(), keyAlias = "EncryptedMnemonicAliasV2"
    )

    @Test
    fun existingThirtySecondSeedCanSignAfterSdkUpdate() = verifySigning("ZeroTimeoutTimed", timeoutSeconds = 30)

    @Test
    fun migratedLegacySeedCanSignAfterUpdate() = verifySigning(
        "ZeroTimeoutControl", timeoutSeconds = expectedMnemonicTimeout(),
        keyAlias = "EncryptedMnemonicAliasV2", verifyAuthenticationBoundary = true
    )

    /** The mixed fixture separately verifies both key policies before exercising actual account signing. */
    @Test
    fun mixedStorageAccountCanSignAfterRollback() = verifySigning(
        "ZeroTimeoutControl", timeoutSeconds = null, keyAlias = null
    )

    /** Android 8/9 used the protected 30-second path even before the vulnerability fix. */
    @Test
    fun inspectTimedLegacyWalletFixture() = runBlocking<Unit> {
        requireDisposableWallet()
        ActivityScenario.launch(MainActivity::class.java).use {
            val manager = bootedManager()
            assertEquals(fixtureAccountName("ZeroTimeoutTimed"), manager.sargonOs.currentNetwork().accounts.single().displayName.value)
            val store = walletProvider("provideEncryptedDataStoreProvider") as DataStore<Preferences>
            val records = mnemonicRecords(store)
            assertEquals(InstrumentationRegistry.getArguments().getString("expectedMnemonicCount")?.toInt() ?: 1, records.size)
            assertTrue(records.values.none { value -> value.startsWith("sargon:") })
            val info = keyInfo(mnemonicKey("EncryptedMnemonicAlias"))
            assertTrue(info.isUserAuthenticationRequired)
            assertEquals(30, info.userAuthenticationValidityDurationSeconds)
            assertFalse(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias("EncryptedMnemonicAliasV2"))
            val reader = walletProvider("encryptedPreferencesManagerProvider") as EncryptedPreferencesManager
            val fingerprints = records.map { (name, stored) ->
                "$name=${digest(stored)}=${digest(reader.readMnemonic(name).getOrThrow())}"
            }.toSet()
            val referenceKey = stringSetPreferencesKey("qa_timed_mnemonic_fingerprints")
            if (InstrumentationRegistry.getArguments().getString("captureTimedFixture") == "true") {
                store.edit { prefs ->
                    check(prefs[referenceKey] == null) { "Do not replace the original timed-key fingerprints" }
                    prefs[referenceKey] = fingerprints
                }
            } else {
                assertEquals(store.data.first()[referenceKey], fingerprints)
            }
            Log.i("MigrationDeviceQA", "TIMED_LEGACY_WALLET_OK: original ciphertext and full mnemonic retained under the original protected 30-second key")
        }
    }

    /** Prepare only the isolated QA app, running the unchanged SDK, after normal onboarding. */
    @Test
    fun prepareSecondLegacyMnemonic() = runBlocking<Unit> {
        requireDisposableWallet()
        ActivityScenario.launch(MainActivity::class.java).use {
            val manager = bootedManager()
            assertEquals(fixtureAccountName("ZeroTimeoutControl"), manager.sargonOs.currentNetwork().accounts.single().displayName.value)
            val store = walletProvider("provideEncryptedDataStoreProvider") as DataStore<Preferences>
            assertEquals(1, mnemonicRecords(store).size)
            val key = mnemonicKey("EncryptedMnemonicAlias")
            assertFalse(keyInfo(key).isUserAuthenticationRequired)
            val mnemonic = MnemonicWithPassphrase(
                mnemonic = Mnemonic.init(Bip39WordCount.TWENTY_FOUR, Bip39Language.ENGLISH),
                passphrase = "disposable migration QA passphrase π"
            )
            val factor = manager.sargonOs.createDeviceFactorSource(mnemonic, DeviceFactorSourceType.BABYLON)
                .let { value -> value.copy(hint = value.hint.copy(label = "Migration QA second seed")) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            val stored = Base64.encodeToString(iv + cipher.doFinal(mnemonic.toJson().toByteArray(Charsets.UTF_8)), Base64.DEFAULT)
            store.edit { prefs -> prefs[stringPreferencesKey("mnemonic${factor.id.body.hex}")] = stored }
            assertTrue(manager.sargonOs.addFactorSource(factor.asGeneral()))
            assertEquals(2, mnemonicRecords(store).size)
            Log.i("MigrationDeviceQA", "Prepared two genuine mnemonic records under the historical unprotected key; second has a BIP39 passphrase")
        }
    }

    /** Read-only after legacy setup; compares full plaintext hashes without logging seed words. */
    @Test
    fun inspectMigrationFixture() = runBlocking<Unit> {
        requireDisposableWallet()
        val stage = InstrumentationRegistry.getArguments().getString("migrationStage")!!
        ActivityScenario.launch(MainActivity::class.java).use {
            bootedManager()
            val store = walletProvider("provideEncryptedDataStoreProvider") as DataStore<Preferences>
            val records = mnemonicRecords(store)
            assertEquals(2, records.size)
            val versioned = records.values.count { it.startsWith("sargon:mnemonic:v2:") }
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val plaintextHashesKey = stringSetPreferencesKey("qa_migration_plaintext_hashes")
            val ciphertextHashesKey = stringSetPreferencesKey("qa_migration_legacy_ciphertext_hashes")
            if (stage == "legacy") {
                assertEquals(0, versioned)
                val key = mnemonicKey("EncryptedMnemonicAlias")
                assertFalse(keyInfo(key).isUserAuthenticationRequired)
                val hashes = records.map { (name, value) -> "$name=${digest(legacyDecrypt(value, key))}" }.toSet()
                store.edit { prefs ->
                    val previous = prefs[plaintextHashesKey]
                    if (previous != null) assertEquals(previous, hashes)
                    prefs[plaintextHashesKey] = hashes
                    prefs[ciphertextHashesKey] = records.map { (name, value) -> "$name=${digest(value)}" }.toSet()
                }
                Log.i("MigrationDeviceQA", "LEGACY_PRECONDITIONS_OK: two raw ciphertext records; authenticationRequired=false; full plaintext fingerprints recorded")
            } else {
                assertTrue(stage == "partial" || stage == "complete")
                assertEquals(if (stage == "partial") 1 else 2, versioned)
                assertEquals(stage == "partial", keyStore.containsAlias("EncryptedMnemonicAlias"))
                val protectedInfo = keyInfo(mnemonicKey("EncryptedMnemonicAliasV2"))
                assertTrue(protectedInfo.isUserAuthenticationRequired)
                assertMigratedKeyPolicy(protectedInfo)
                val metadata = store.data.first()
                val expected = requireNotNull(metadata[plaintextHashesKey])
                val legacyHashes = requireNotNull(metadata[ciphertextHashesKey])
                val reader = walletProvider("encryptedPreferencesManagerProvider") as EncryptedPreferencesManager
                for ((name, value) in records) {
                    if (!value.startsWith("sargon:")) assertTrue(legacyHashes.contains("$name=${digest(value)}"))
                    assertTrue(expected.contains("$name=${digest(reader.readMnemonic(name).getOrThrow())}"))
                }
                Log.i("MigrationDeviceQA", "${stage.uppercase()}_READS_OK: both complete mnemonic JSON values, including passphrases, match the originals")
            }
        }
    }

    /** Explicit fixture setup/reversal, permitted only for the opted-in disposable wallet. */
    @Test
    fun resetDisposableFixtureToLegacy() = runBlocking<Unit> {
        requireDisposableWallet()
        assertEquals("true", InstrumentationRegistry.getArguments().getString("resetLegacyFixture"))
        ActivityScenario.launch(MainActivity::class.java).use {
            val manager = bootedManager()
            assertEquals(fixtureAccountName("ZeroTimeoutControl"), manager.sargonOs.currentNetwork().accounts.single().displayName.value)
            val store = walletProvider("provideEncryptedDataStoreProvider") as DataStore<Preferences>
            val records = mnemonicRecords(store)
            assertTrue(records.size in 1..2)
            assertTrue(records.values.all { it.startsWith("sargon:mnemonic:v2:") })
            val protectedInfo = keyInfo(mnemonicKey("EncryptedMnemonicAliasV2"))
            assertTrue(protectedInfo.isUserAuthenticationRequired)
            assertMigratedKeyPolicy(protectedInfo)
            val expected = store.data.first()[stringSetPreferencesKey("qa_migration_plaintext_hashes")]
            val reader = walletProvider("encryptedPreferencesManagerProvider") as EncryptedPreferencesManager
            val plaintexts = records.keys.associateWith { name ->
                reader.readMnemonic(name).getOrThrow().also { value ->
                    if (expected != null) assertTrue(expected.contains("$name=${digest(value)}"))
                }
            }.toMutableMap()
            if (InstrumentationRegistry.getArguments().getString("addSecondMnemonic") == "true") {
                assertEquals(1, records.size)
                val mnemonic = MnemonicWithPassphrase(
                    mnemonic = Mnemonic.init(Bip39WordCount.TWENTY_FOUR, Bip39Language.ENGLISH),
                    passphrase = "disposable migration QA passphrase π"
                )
                val factor = manager.sargonOs.createDeviceFactorSource(mnemonic, DeviceFactorSourceType.BABYLON)
                    .let { value -> value.copy(hint = value.hint.copy(label = "Migration QA second seed")) }
                assertTrue(manager.sargonOs.addFactorSource(factor.asGeneral()))
                plaintexts["mnemonic${factor.id.body.hex}"] = mnemonic.toJson()
            }
            val coordinator = walletProvider("provideMnemonicStorageProvider")
            val mutex = coordinator.javaClass.getDeclaredField("mutex").apply { isAccessible = true }.get(coordinator) as Mutex
            mutex.withLock {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            assertFalse("Do not overwrite a retained legacy key", keyStore.containsAlias("EncryptedMnemonicAlias"))
            val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
            val timeout = InstrumentationRegistry.getArguments().getString("legacyFixtureTimeout")?.toInt()
            val builder = KeyGenParameterSpec.Builder("EncryptedMnemonicAlias",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false).setKeySize(256)
            if (timeout != null) {
                require(timeout == 30)
                builder.setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(false)
                    .setUserAuthenticationParameters(timeout,
                        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
            }
            generator.init(builder.build())
            val key = generator.generateKey()
            assertEquals(timeout != null, keyInfo(key).isUserAuthenticationRequired)
            if (timeout != null) {
                // Authorize through the real wallet prompt before preparing timed-key ciphertext.
                val handler = walletProvider("provideBiometricsHandlerProvider") as com.radixdlt.sargon.os.driver.BiometricsHandler
                handler.askForBiometrics().getOrThrow()
            }
            val ciphertexts = plaintexts.mapValues { (_, value) ->
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
                cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
                Base64.encodeToString(iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.DEFAULT)
            }
            store.edit { prefs ->
                check(prefs.asMap().filterKeys { key -> key.name.startsWith("mnemonic") }
                    .map { (key, value) -> key.name to value }.toMap() == records)
                ciphertexts.forEach { (name, value) -> prefs[stringPreferencesKey(name)] = value }
                prefs[stringSetPreferencesKey("qa_migration_plaintext_hashes")] =
                    plaintexts.map { (name, value) -> "$name=${digest(value)}" }.toSet()
                prefs[stringSetPreferencesKey("qa_migration_legacy_ciphertext_hashes")] =
                    ciphertexts.map { (name, value) -> "$name=${digest(value)}" }.toSet()
            }
            assertTrue(mnemonicRecords(store).values.none { it.startsWith("sargon:") })
            keyStore.deleteEntry("EncryptedMnemonicAliasV2")
            assertFalse(keyStore.containsAlias("EncryptedMnemonicAliasV2"))
            ciphertexts.forEach { (name, value) -> assertEquals(plaintexts.getValue(name), legacyDecrypt(value, key)) }
            Log.i("MigrationDeviceQA", "RESET_LEGACY_STATE_OK: complete plaintexts preserved; ${ciphertexts.size} legacy records; timeout=$timeout")
            }
        }
    }

    /** Run only on an empty disposable wallet, before onboarding with the unchanged SDK. */
    @Test
    fun prepareHistoricalThirtySecondKey() {
        requireDisposableWallet()
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

    private fun verifySigning(
        accountName: String,
        timeoutSeconds: Int?,
        keyAlias: String? = "EncryptedMnemonicAlias",
        verifyAuthenticationBoundary: Boolean = false
    ) = runBlocking<Unit> {
        requireDisposableWallet()
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
            assertEquals("Only use the named disposable fixture", fixtureAccountName(accountName), account.displayName.value)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyAlias != null) {
                val key = keyStore.getKey(keyAlias, null) as SecretKey
                if (keyAlias == "EncryptedMnemonicAliasV2") {
                    assertFalse("Migration must finish retiring the vulnerable key", keyStore.containsAlias("EncryptedMnemonicAlias"))
                }
                val info = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
                    .getKeySpec(key, KeyInfo::class.java) as KeyInfo
                assertEquals("SDK updates must retain the existing key's protection", timeoutSeconds != null, info.isUserAuthenticationRequired)
                if (timeoutSeconds == 0) {
                    assertTrue(info.userAuthenticationValidityDurationSeconds in setOf(-1, 0))
                } else if (timeoutSeconds != null) {
                    assertEquals(timeoutSeconds.toInt(), info.userAuthenticationValidityDurationSeconds)
                }
            }

            if (verifyAuthenticationBoundary) {
                val key = mnemonicKey("EncryptedMnemonicAliasV2")
                val timeout = keyInfo(key).userAuthenticationValidityDurationSeconds
                if (timeout > 0) {
                    Log.i("ZeroTimeoutWalletFlow", "WAITING_FOR_AUTH_EXPIRY: ${timeout + 1} seconds")
                    delay((timeout + 1) * 1_000L)
                }
                val store = walletProvider("provideEncryptedDataStoreProvider") as DataStore<Preferences>
                val ciphertext = mnemonicRecords(store).values.first().removePrefix("sargon:mnemonic:v2:")
                val failure = runCatching { legacyDecrypt(ciphertext, key) }.exceptionOrNull()
                assertTrue("Keystore must reject decryption without current authentication: $failure",
                    generateSequence(failure) { it.cause }.any {
                        it is UserNotAuthenticatedException ||
                            (Build.VERSION.SDK_INT >= 33 && it is android.security.KeyStoreException && it.requiresUserAuthentication())
                    })
                Log.i("ZeroTimeoutWalletFlow", "UNAUTHENTICATED_KEY_ACCESS_REJECTED")
            }

            suspend fun sign() = withTimeout(120_000) {
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
                )
            }
            if (verifyAuthenticationBoundary) {
                Log.i("ZeroTimeoutWalletFlow", "CANCEL_NEXT_SIGNING_PROMPT")
                assertTrue("Cancelled authentication must not produce a signature", sign().isFailure)
                Log.i("ZeroTimeoutWalletFlow", "CANCELLED_SIGNING_REJECTED")
            }
            val signatures = sign().getOrThrow()
            assertTrue(signatures.containsKey(account))
            Log.i("ZeroTimeoutWalletFlow", "Key timeout: $timeoutSeconds; actual auth signing returned ${signatures.size} signature")
        }
    }

    private fun requireDisposableWallet() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("disposableWallet") == "true")
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        if (Build.HARDWARE.contains("ranchu")) {
            assertTrue(packageName == QA_PACKAGE || packageName == "com.radixpublishing.radixwallet.android.debug")
        } else {
            assertEquals("Physical-device tests must never use an existing wallet installation", QA_PACKAGE, packageName)
        }
    }

    private fun fixtureAccountName(default: String): String =
        InstrumentationRegistry.getArguments().getString("qaAccountName") ?: default

    private suspend fun bootedManager(): SargonOsManager = withTimeout(30_000) {
        val field = SargonOsManager::class.java.getDeclaredField("instance").apply { isAccessible = true }
        while (field.get(null) == null) delay(100)
        (field.get(null) as SargonOsManager).also { manager ->
            manager.sargonState.first { it is SargonOsState.Booted }
        }
    }

    private fun walletProvider(name: String): Any {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val component = app.javaClass.getMethod("generatedComponent").invoke(app)
        val field = component.javaClass.getDeclaredField(name).apply { isAccessible = true }
        return (field.get(component) as Provider<*>).get()!!
    }

    private suspend fun mnemonicRecords(store: DataStore<Preferences>) = store.data.first().asMap()
        .filterKeys { it.name.startsWith("mnemonic") }.map { (key, value) -> key.name to (value as String) }.toMap()

    private fun mnemonicKey(alias: String) = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        .getKey(alias, null) as SecretKey

    private fun keyInfo(key: SecretKey) = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
        .getKeySpec(key, KeyInfo::class.java) as KeyInfo

    private fun assertMigratedKeyPolicy(info: KeyInfo) {
        val timeout = expectedMnemonicTimeout()
        if (timeout > 0) {
            assertEquals(timeout, info.userAuthenticationValidityDurationSeconds)
        } else {
            assertTrue(info.userAuthenticationValidityDurationSeconds in setOf(-1, 0))
        }
    }

    private fun expectedMnemonicTimeout(): Int = InstrumentationRegistry.getArguments()
        .getString("expectedAuthTimeout")?.toInt()
        ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            BiometricManager.from(InstrumentationRegistry.getInstrumentation().targetContext)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        ) 0 else 30

    private fun legacyDecrypt(value: String, key: SecretKey): String {
        val bytes = Base64.decode(value, Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes, 0, 12))
        return String(cipher.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8)
    }

    private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val QA_PACKAGE = "com.radixpublishing.radixwallet.android.debug.migrationqa"
    }
}
