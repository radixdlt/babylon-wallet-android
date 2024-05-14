package com.babylon.wallet.android.domain.usecases.p2plink

import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionPayload
import com.radixdlt.sargon.LinkConnectionQrData
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.RadixConnectPurpose
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toJson
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class ParseLinkConnectionDetailsUseCaseTest {

    companion object {

        private lateinit var sut: ParseLinkConnectionDetailsUseCase

        private val p2pLinksRepositoryMock: P2PLinksRepository = mock()
    }

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        sut = ParseLinkConnectionDetailsUseCase(
            p2PLinksRepository = p2pLinksRepositoryMock
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

            val result = sut(raw)

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

            val result = sut(raw)

            verifyNoInteractions(p2pLinksRepositoryMock)
            assertEquals(
                Result.failure<LinkConnectionPayload>(RadixWalletException.LinkConnectionException.InvalidQR),
                result
            )
        }
    }

    @Test
    fun `given a purpose other than General, the result is failure with unknown purpose exception`() {
        testScope.runTest {
            val rawInput = LinkConnectionQrData.sample.invoke().copy(
                purpose = RadixConnectPurpose.UNKNOWN
            ).toJson()
            whenever(p2pLinksRepositoryMock.getP2PLinks()).doReturn(emptyList<P2pLink>().asIdentifiable())

            val result = sut(rawInput)

            assertEquals(
                Result.failure<LinkConnectionPayload>(RadixWalletException.LinkConnectionException.UnknownPurpose),
                result
            )
        }
    }

    @Test
    fun `given a valid link data with no existing p2p link for the same public key, the result is success with null existingLink`() {
        testScope.runTest {
            val qrData = LinkConnectionQrData.sample.invoke()
            val expectedResult = LinkConnectionPayload(
                password = RadixConnectPassword.sample.invoke(),
                publicKey = PublicKey.Ed25519.init(qrData.publicKeyOfOtherParty.asGeneral().hex),
                purpose = RadixConnectPurpose.GENERAL,
                existingP2PLink = null
            )

            whenever(p2pLinksRepositoryMock.getP2PLinks()).doReturn(emptyList<P2pLink>().asIdentifiable())

            val result = sut(qrData.toJson())

            assertEquals(
                Result.success(expectedResult),
                result
            )
        }
    }

    @Test
    fun `given a valid link data with an already existing p2p link for the same public key, the result is success`() {
        testScope.runTest {
            val publicKey = PublicKey.Ed25519.sample.invoke()
            val qrData = LinkConnectionQrData.sample.invoke().copy(
                publicKeyOfOtherParty = publicKey.v1
            )

            val existingP2PLink = P2pLink.sample.invoke().copy(
                publicKey = publicKey.v1
            )
            val expectedResult = LinkConnectionPayload(
                password = qrData.password,
                publicKey = qrData.publicKeyOfOtherParty.asGeneral(),
                purpose = qrData.purpose,
                existingP2PLink = existingP2PLink
            )

            whenever(p2pLinksRepositoryMock.getP2PLinks()).doReturn(listOf(existingP2PLink).asIdentifiable())

            val result = sut(qrData.toJson())

            assertEquals(
                Result.success(expectedResult),
                result
            )
        }
    }
}