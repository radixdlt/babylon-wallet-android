package rdx.works.profile

import org.junit.Test
import rdx.works.core.HexCoded32Bytes

class HexCoded32BytesTest {

    @Test(expected = IllegalArgumentException::class)
    fun `given a wrong 32 byte hex string, when HexCoded32Bytes, then an error occurs`() {
        val hexString = "5f07ec336e9e7891bff040c817201e73c097b6b1e1b3a26c501e0010196f5"

        HexCoded32Bytes(
            value = hexString
        )
    }
}