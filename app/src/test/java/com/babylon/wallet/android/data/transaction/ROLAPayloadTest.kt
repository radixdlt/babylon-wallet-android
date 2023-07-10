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
                "account_rdx168fghy4kapzfnwpmq7t7753425lwklk65r82ys7pz2xzleehk2ap0k",
                "account_rdx12xsvygvltz4uhsht6tdrfxktzpmnl77r0d40j8agmujgdj022sudkk",
                "account_rdx168e8u653alt59xm8ple6khu6cgce9cfx9mlza6wxf7qs3wwdh0pwrf"
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
