package com.babylon.wallet.android.data.transaction

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rdx.works.core.blake2Hash
import rdx.works.core.toHexString
import java.io.File

internal class ROLAPayloadTest {

    private lateinit var testVectors: List<TestVector>

    @Before
    fun setUp() {
        val testVectorsContent = File("src/test/resources/raw/rola_challenge_payload_hash_vectors.json").readText()
        testVectors = Json.decodeFromString(testVectorsContent)
    }

    @Test
    fun `run tests for test vector`() {
        testVectors.forEach { testVector ->
            val payloadToHash = payloadToHash(testVector.challenge, testVector.dAppDefinitionAddress, testVector.origin)
            val hexValue = payloadToHash.toHexString()
            Assert.assertEquals(hexValue, testVector.payloadToHash)
            val hashedPayload = payloadToHash.blake2Hash().toHexString()
            Assert.assertEquals(hashedPayload, testVector.blakeHashOfPayload)
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