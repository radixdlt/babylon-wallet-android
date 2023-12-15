package rdx.works.profile.snapshot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.skyscreamer.jsonassert.JSONAssert
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
class SnapshotEncodingTests(private val input: SnapshotTestInput) {

    private val serializer = SerializerModule.provideProfileSerializer()

    @Test
    fun `test round-trip for multiple `() {
        val multiple = input.multiSnapshot.readText()
        val testVector = serializer.decodeFromString<TestVector>(multiple)

        val decryptedSnapshots = testVector.validate(serializer)
        // Validate all decrypted snapshots have the correct version
        assertEquals(
            List(size = decryptedSnapshots.size) { input.snapshotVersion }.toSet(),
            decryptedSnapshots.map { it.toProfile().header.snapshotVersion }.toSet()
        )
        // Validate plaintext snapshot has the correct version
        assertEquals(input.snapshotVersion, testVector.plaintext.toProfile().header.snapshotVersion)

        decryptedSnapshots.forEach { snapshot ->
            val actual = serializer.encodeToString(snapshot)

            if (input.isMostRecentIteration) {
                // Most recent iterations for the specific version should do a JSON assert round-trip
                // which is strict regarding fields that are marked as null.
                val expected = input.baseSnapshot.readText()
                JSONAssert.assertEquals(expected, actual, false)
            }
        }
    }

    @Test
    fun `test round-trip for snapshot `() {
        val expected = input.baseSnapshot.readText()
        val snapshot = serializer.decodeFromString<ProfileSnapshot>(expected)

        val actual = serializer.encodeToString(snapshot)
        if (input.isMostRecentIteration) {
            JSONAssert.assertEquals(expected, actual, false)
        }
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
                network.accounts.forEach { it.validateWithMnemonic(plaintextProfile.factorSources.toList()) }
                network.personas.forEach { it.validateWithMnemonic(plaintextProfile.factorSources.toList()) }
            }

            return decrypted
        }

        private fun Entity.validateWithMnemonic(factorSources: List<FactorSource>) {
            when (val state = securityState) {
                is SecurityState.Unsecured -> {
                    val instance = state.unsecuredEntityControl.transactionSigning

                    if (instance.factorSourceId.kind != FactorSourceKind.DEVICE) return

                    val factorSource = factorSources.find { it.id == instance.factorSourceId } as? DeviceFactorSource
                        ?: error("Factor source not found ${instance.factorSourceId}")
                    val mnemonic = mnemonics.find { it.factorSourceID == instance.factorSourceId }
                        ?: error("Mnemonic not found ${instance.factorSourceId}")

                    assertTrue(
                        factorSource.validateAgainst(mnemonic.mnemonicWithPassphrase.toDomain()),
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
    }

    companion object {
        private const val TEST_VECTOR_DIR = "src/test/resources/version"
        private const val MULTI_SNAPSHOT_VECTOR_FILE = "multi_profile_snapshots.json"
        private const val BASE_SNAPSHOT = "base_profile_snapshot.json"

        data class SnapshotTestInput(
            val snapshotVersion: Int,
            val iteration: Int?,
            val latestIteration: Int?
        ) {
            val isMostRecentIteration: Boolean
                get() = iteration == latestIteration

            val multiSnapshot: File
                get() = if (iteration != null) {
                    File("$TEST_VECTOR_DIR/$snapshotVersion/$iteration/$MULTI_SNAPSHOT_VECTOR_FILE")
                } else {
                    File("$TEST_VECTOR_DIR/$snapshotVersion/$MULTI_SNAPSHOT_VECTOR_FILE")
                }

            val baseSnapshot: File
                get() = if (iteration != null) {
                    File("$TEST_VECTOR_DIR/$snapshotVersion/$iteration/$BASE_SNAPSHOT")
                } else {
                    File("$TEST_VECTOR_DIR/$snapshotVersion/$BASE_SNAPSHOT")
                }

            val latestBaseSnapshot: File
                get() = if (latestIteration != null) {
                    File("$TEST_VECTOR_DIR/$snapshotVersion/$latestIteration/$BASE_SNAPSHOT")
                } else {
                    File("$TEST_VECTOR_DIR/$snapshotVersion/$BASE_SNAPSHOT")
                }

        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<SnapshotTestInput> {
            return File(TEST_VECTOR_DIR)
                .listFiles()
                ?.filter { it.name.toIntOrNull() != null }
                ?.sortedBy { it.name.toInt() }
                ?.map { versionDirectory ->
                    val snapshotVersion = versionDirectory.name.toInt()
                    if (versionDirectory.isDirectory) {
                        val iterations = versionDirectory.listFiles()?.map { it.name.toInt() }?.sorted().orEmpty()
                        iterations.mapIndexed { index, iteration ->
                            SnapshotTestInput(
                                snapshotVersion = snapshotVersion,
                                iteration = iteration,
                                latestIteration = iterations.max()
                            )
                        }
                    } else {
                        listOf(
                            SnapshotTestInput(
                                snapshotVersion = snapshotVersion,
                                iteration = null,
                                latestIteration = null
                            )
                        )
                    }
                }.orEmpty().flatten()
        }
    }

}
