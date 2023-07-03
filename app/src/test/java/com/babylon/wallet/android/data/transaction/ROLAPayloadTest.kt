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

    @Before
    fun setUp() {
        val testVectorsFile = File("src/test/resources/raw/rola_challenge_payload_hash_vectors.json")
        if (testVectorsFile.exists()) {
            testVectors = Json.decodeFromString(testVectorsFile.readText())
        } else {
            val origins = listOf("https://dashboard.rdx.works", "https://stella.swap", "https://rola.xrd")
            val accounts = listOf(
                "account_tdx_b_1p9dkged3rpzy860ampt5jpmvv3yl4y6f5yppp4tnscdslvt9v3",
                "account_tdx_b_1p8ahenyznrqy2w0tyg00r82rwuxys6z8kmrhh37c7maqpydx7p",
                "account_tdx_b_1p95nal0nmrqyl5r4phcspg8ahwnamaduzdd3kaklw3vqeavrwa"
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
            testVectorsFile.writeText(Json.encodeToString(testVectors))
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
            Assert.assertEquals(testVector.blakeHashOfPayload, signRequest.hashedDataToSign.toHexString())
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
