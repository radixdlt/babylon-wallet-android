package com.babylon.wallet.android.data.transaction

import android.util.Log
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rdx.works.core.blake2Hash
import rdx.works.core.decodeHex
import rdx.works.core.toHexString
import java.io.File

internal class ROLAPayloadTest {

    private lateinit var testVectors: List<TestVector>
    private val json = Json {
        prettyPrint = true
    }

    /**
     * make sure our code generates identical test vectors to iOS, see @see ROLAClientTests.omit_test_generate_rola_payload_hash_vectors
     */
    @Before
    fun setUp() {
        val testVectorsFile = File("src/test/resources/raw/rola_challenge_payload_hash_vectors.json")
        if (testVectorsFile.exists()) {
            testVectors = json.decodeFromString(testVectorsFile.readText())
        } else {
            val origins = listOf("https://dashboard.rdx.works", "https://stella.swap", "https://rola.xrd")
            val accounts = listOf(
                "account_sim1cyvgx33089ukm2pl97pv4max0x40ruvfy4lt60yvya744cve475w0q",
                "account_sim1cyzfj6p254jy6lhr237s7pcp8qqz6c8ahq9mn6nkdjxxxat5syrgz9",
                "account_sim168gge5mvjmkc7q4suyt3yddgk0c7yd5z6g662z4yc548cumw8nztch"
            )
            testVectors = origins.flatMap { origin ->
                accounts.flatMap { accountAddress ->
                    (0 until 10).map { seed ->
                        val challenge = (origin.toByteArray() + accountAddress.toByteArray() + seed.toUByte().toByte()).blake2Hash()
                        val payloadHex = SignRequest.SignAuthChallengeRequest(challenge.toHexString(), origin, accountAddress).payloadHex
                        val blakeHashOfPayload = payloadHex.decodeHex().blake2Hash().toHexString()
                        TestVector(payloadHex, blakeHashOfPayload, accountAddress, origin, challenge.toHexString())
                    }
                }
            }
            testVectorsFile.writeText(json.encodeToString(testVectors))
        }
    }

    @Test
    fun `run tests for test vectors`() {
        testVectors.forEach { testVector ->
            Log.d("Test vector", testVector.toString())
            val signRequest = SignRequest.SignAuthChallengeRequest(
                challengeHex = testVector.challenge,
                dAppDefinitionAddress = testVector.dAppDefinitionAddress,
                origin = testVector.origin
            )

            Assert.assertEquals(testVector.payloadToHash, signRequest.payloadHex)
            Assert.assertEquals(testVector.blakeHashOfPayload, signRequest.dataToSign.blake2Hash().toHexString())
        }
    }

}

@kotlinx.serialization.Serializable
data class TestVector(
    @kotlinx.serialization.SerialName("payloadToHash")
    val payloadToHash: String,
    @kotlinx.serialization.SerialName("blakeHashOfPayload")
    val blakeHashOfPayload: String,
    @kotlinx.serialization.SerialName("dAppDefinitionAddress")
    val dAppDefinitionAddress: String,
    @kotlinx.serialization.SerialName("origin")
    val origin: String,
    @kotlinx.serialization.SerialName("challenge")
    val challenge: String
)
