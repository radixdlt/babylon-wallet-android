package com.babylon.wallet.android.domain.usecases.p2plink

import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionPayload
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionQRContent
import com.radixdlt.sargon.Ed25519PublicKey
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.P2PLinkPurpose

class ParseLinkConnectionDetailsUseCaseTest {

    companion object {

        private lateinit var objectUnderTest: ParseLinkConnectionDetailsUseCase

        private val p2pLinksRepositoryMock: P2PLinksRepository = mock()
        private val getP2PLinkClientSignatureMessageUseCase = GetP2PLinkClientSignatureMessageUseCase()
        private val json = Json
    }

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        objectUnderTest = ParseLinkConnectionDetailsUseCase(
            p2PLinksRepository = p2pLinksRepositoryMock,
            getP2PLinkClientSignatureMessageUseCase = getP2PLinkClientSignatureMessageUseCase,
            json = json
        )
    }

    @After
    fun tearDown() {
        reset(p2pLinksRepositoryMock)
    }

    @Test
    fun `given an old version of link details, the result is failure with old qr version exception`() {
        testScope.runTest {
            val raw = "eb71c52ec7c61cea3791835d84c1a851d2a70c19149da71fc4852fd5ff585fbb"

            val result = objectUnderTest(raw)

            verifyNoInteractions(p2pLinksRepositoryMock)
            assertEquals(
                Result.failure<LinkConnectionPayload>(RadixWalletException.LinkConnectionException.OldQRVersion),
                result
            )
        }
    }

    @Test
    fun `given a malformed json, the result is failure with invalid qr exception`() {
        testScope.runTest {
            val raw = "Test"

            val result = objectUnderTest(raw)

            verifyNoInteractions(p2pLinksRepositoryMock)
            assertEquals(
                Result.failure<LinkConnectionPayload>(RadixWalletException.LinkConnectionException.InvalidQR),
                result
            )
        }
    }

    @Test
    fun `given a malformed connection password, the result is failure with invalid qr exception`() {
        testScope.runTest {
            val rawInput = toRawInput(buildQRContent(password = "Test"))

            val result = objectUnderTest(rawInput)

            verifyNoInteractions(p2pLinksRepositoryMock)
            assertEquals(
                Result.failure<LinkConnectionPayload>(RadixWalletException.LinkConnectionException.InvalidQR),
                result
            )
        }
    }

    @Test
    fun `given a malformed signature, the result is failure with invalid signature exception`() {
        testScope.runTest {
            val rawInput = toRawInput(buildQRContent(signature = "Test"))

            val result = objectUnderTest(rawInput)

            verifyNoInteractions(p2pLinksRepositoryMock)
            assertEquals(
                Result.failure<LinkConnectionPayload>(RadixWalletException.LinkConnectionException.InvalidSignature),
                result
            )
        }
    }

    @Test
    fun `given a purpose other than General, the result is failure with unknown purpose exception`() {
        testScope.runTest {
            val rawInput = toRawInput(buildQRContent(purpose = "Test"))
            whenever(p2pLinksRepositoryMock.getP2PLinks()).doReturn(emptyList())

            val result = objectUnderTest(rawInput)

            assertEquals(
                Result.failure<LinkConnectionPayload>(RadixWalletException.LinkConnectionException.UnknownPurpose),
                result
            )
        }
    }

    @Test
    fun `given a valid link content with no existing p2p link for the same public key, the result is success with null existingLink`() {
        testScope.runTest {
            val qrContent = buildQRContent()
            val rawInput = toRawInput(qrContent)
            val expectedResult = LinkConnectionPayload(
                password = RadixConnectPassword(Exactly32Bytes.init(qrContent.password.hexToBagOfBytes())),
                publicKey = PublicKey.Ed25519.init(qrContent.publicKey),
                purpose = P2PLinkPurpose.General,
                existingP2PLink = null
            )

            whenever(p2pLinksRepositoryMock.getP2PLinks()).doReturn(emptyList())

            val result = objectUnderTest(rawInput)

            assertEquals(
                Result.success(expectedResult),
                result
            )
        }
    }

    @Test
    fun `given a valid link content with an already existing p2p link for the same public key, the result is success`() {
        testScope.runTest {
            val qrContent = buildQRContent()
            val rawInput = toRawInput(qrContent)

            val existingP2PLink = buildP2PLink()
            val expectedResult = LinkConnectionPayload(
                password = RadixConnectPassword(Exactly32Bytes.init(qrContent.password.hexToBagOfBytes())),
                publicKey = PublicKey.Ed25519.init(qrContent.publicKey),
                purpose = P2PLinkPurpose.General,
                existingP2PLink = existingP2PLink
            )

            whenever(p2pLinksRepositoryMock.getP2PLinks()).doReturn(listOf(existingP2PLink))

            val result = objectUnderTest(rawInput)

            assertEquals(
                Result.success(expectedResult),
                result
            )
        }
    }

    private fun toRawInput(content: LinkConnectionQRContent): String {
        return json.encodeToString(content)
    }

    private fun buildQRContent(
        password: String = "eb71c52ec7c61cea3791835d84c1a851d2a70c19149da71fc4852fd5ff585fbb",
        publicKey: String = "8020845f1d7d1fe45e3e4e4e5ac5484e8650a1151adc7fe38283af9d0bbef2ac",
        purpose: String = "general",
        signature: String = "f31b0494ce9c4a31521f91e7f8d2a0f2ea6925ac71e413f899c25ac00930044de67975b0f05b89bff1c50ade1cbab5b954a54e83863ee64729fbd72d029fb50d"
    ): LinkConnectionQRContent {
        return LinkConnectionQRContent(
            password = password,
            publicKey = publicKey,
            purpose = purpose,
            signature = signature
        )
    }

    private fun buildP2PLink(
        password: String = "eb71c52ec7c61cea3791835d84c1a851d2a70c19149da71fc4852fd5ff585fbb",
        name: String = "Test Name",
        publicKey: String = "8020845f1d7d1fe45e3e4e4e5ac5484e8650a1151adc7fe38283af9d0bbef2ac",
        purpose: P2PLinkPurpose = P2PLinkPurpose.General
    ): P2PLink {
        return P2PLink(
            connectionPassword = RadixConnectPassword(Exactly32Bytes.init(password.hexToBagOfBytes())),
            displayName = name,
            publicKey = PublicKey.Ed25519.init(publicKey.hexToBagOfBytes()),
            purpose = purpose
        )
    }
}