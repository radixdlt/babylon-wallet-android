package rdx.works.profile.snapshot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.skyscreamer.jsonassert.JSONAssert
import rdx.works.core.HexCoded32Bytes
import rdx.works.profile.data.model.EncryptedProfileSnapshot
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.validateAgainst
import rdx.works.profile.di.SerializerModule
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class SnapshotEncodingTests(private val versionUnderTest: Int) {

    private val serializer = SerializerModule.provideProfileSerializer()

    @Test
    fun `test round-trip for multiple `() {
        val multiple = File("$TEST_VECTOR_DIR/$versionUnderTest/$MULTI_SNAPSHOT_VECTOR_FILE").readText()
        val testVector = serializer.decodeFromString<TestVector>(multiple)

        val decryptedSnapshots = testVector.validate(serializer)
        // Validate all decrypted snapshots have the correct version
        assertEquals(
            List(size = decryptedSnapshots.size) { versionUnderTest }.toSet(),
            decryptedSnapshots.map { it.toProfile().header.snapshotVersion }.toSet()
        )
        // Validate plaintext snapshot has the correct version
        assertEquals(versionUnderTest, testVector.plaintext.toProfile().header.snapshotVersion)

        val expected = File("$TEST_VECTOR_DIR/$versionUnderTest/$BASE_SNAPSHOT").readText()
        decryptedSnapshots.forEach { snapshot ->
            val actual = serializer.encodeToString(snapshot)

            JSONAssert.assertEquals(expected, actual, false)
        }
    }

    @Test
    fun `test round-trip for snapshot `() {
        val expected = File("$TEST_VECTOR_DIR/$versionUnderTest/$BASE_SNAPSHOT").readText()
        val snapshot = serializer.decodeFromString<ProfileSnapshot>(expected)

        val actual = serializer.encodeToString(snapshot)
        JSONAssert.assertEquals(expected, actual, false)
    }

    @Serializable
    private data class TestVector(
        @SerialName("_snapshotVersion")
        val snapshotVersion: Int,
        val encryptedSnapshots: List<EncryptedMnemonicWithPassword>,
        val mnemonics: List<MnemonicWithFactorSourceId>,
        val plaintext: ProfileSnapshot
    ) {

        fun validate(serializer: Json): List<ProfileSnapshot> {
            // Validate that decryption of encrypted snapshots derive the plaintext
            val decrypted = encryptedSnapshots.map { it.decrypted(serializer) }.onEach {
                assertEquals(plaintext, it)
            }

            // Validate that all mnemonics match the plaintext device factor source ids
            assertEquals(
                mnemonics.map { it.factorSourceID as FactorSource.FactorSourceID.FromHash }.toSet(),
                plaintext.toProfile().factorSources.filterIsInstance<DeviceFactorSource>().map { it.id }.toSet()
            )

            // Validate that all device backed accounts/personas use the same id as the one derived from mnemonics
            val plaintextProfile = plaintext.toProfile()
            plaintextProfile.networks.forEach { network ->
                network.accounts.forEach { it.validateWithMnemonic(plaintextProfile.factorSources) }
                network.personas.forEach { it.validateWithMnemonic(plaintextProfile.factorSources) }
            }

            return decrypted
        }

        private fun Entity.validateWithMnemonic(factorSources: List<FactorSource>) {
            when (val state = securityState) {
                is SecurityState.Unsecured -> {
                    val instance = state.unsecuredEntityControl.transactionSigning

                    if (instance.factorSourceId.kind != FactorSourceKind.DEVICE) return

                    factorSources.find { it.id == instance.factorSourceId }
                        ?: error("Factor source not found ${instance.factorSourceId}")
                    val mnemonic = mnemonics.find { it.factorSourceID == instance.factorSourceId }
                        ?: error("Mnemonic not found ${instance.factorSourceId}")

                    assertTrue(
                        instance.validateAgainst(mnemonic.mnemonicWithPassphrase.toDomain()),
                        "FactorInstance: ${instance.factorSourceId} cannot be derived from mnemonic"
                    )
                }
            }
        }

        @Serializable
        private data class EncryptedMnemonicWithPassword(
            val password: String,
            val snapshot: EncryptedProfileSnapshot
        ) {

            fun decrypted(serializer: Json): ProfileSnapshot = snapshot.decrypt(serializer, password)

        }

        @Serializable
        private data class MnemonicWithFactorSourceId(
            val factorSourceID: FactorSource.FactorSourceID,
            val mnemonicWithPassphrase: Mnemonic
        ) {
            @Serializable
            data class Mnemonic(
                val mnemonic: String,
                val passphrase: String
            ) {
                fun toDomain() = MnemonicWithPassphrase(mnemonic, passphrase)
            }
        }

        companion object {

            fun from(
                serializer: Json,
                snapshot: ProfileSnapshot,
                mnemonics: List<MnemonicWithPassphrase>,
                passwords: List<String>
            ): TestVector {
                val profile = snapshot.toProfile()

                return TestVector(
                    snapshotVersion = profile.header.snapshotVersion,
                    encryptedSnapshots = passwords.map {
                        EncryptedMnemonicWithPassword(
                            password = it,
                            snapshot = EncryptedProfileSnapshot.from(
                                serializer = serializer,
                                snapshot = snapshot,
                                password = it
                            )
                        )
                    },
                    mnemonics = mnemonics.map {
                        MnemonicWithFactorSourceId(
                            factorSourceID = FactorSource.FactorSourceID.FromHash(
                                kind = FactorSourceKind.DEVICE,
                                body = HexCoded32Bytes(FactorSource.factorSourceId(mnemonicWithPassphrase = it))
                            ),
                            mnemonicWithPassphrase = MnemonicWithFactorSourceId.Mnemonic(
                                mnemonic = it.mnemonic,
                                passphrase = it.bip39Passphrase
                            )
                        )
                    },
                    plaintext = snapshot
                )
            }

        }
    }

    companion object {
        private const val TEST_VECTOR_DIR = "src/test/resources/version"
        private const val MULTI_SNAPSHOT_VECTOR_FILE = "multi_profile_snapshots.json"
        private const val BASE_SNAPSHOT = "base_profile_snapshot.json"

        private val MNEMONICS = listOf(
            MnemonicWithPassphrase(
                mnemonic = "alley urge tag valid execute hat little funny armed salute orient hurt balcony urban found clip tennis wrong " +
                        "turtle canoe castle exist pledge test",
                bip39Passphrase = ""
            ),
            MnemonicWithPassphrase(
                mnemonic = "gentle hawk winner rain embrace erosion call update photo frost fatal wrestle",
                bip39Passphrase = ""
            ),
            MnemonicWithPassphrase(
                mnemonic = "smile entry satisfy shed margin rubber disorder hungry foot error ribbon cradle aim round october blind " +
                        "lab spend",
                bip39Passphrase = ""
            )
        )

        private val PASSWORDS = listOf(
            "",
            "Radix... just imagine!",
            "babylon"
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Int> {
            return File(TEST_VECTOR_DIR)
                .listFiles()
                ?.filter { it.name.toIntOrNull() != null }
                ?.sortedBy { it.name.toInt() }
                ?.map { vectorDirectory ->
                    vectorDirectory.name.toInt()
                }.orEmpty()
        }
    }

}
