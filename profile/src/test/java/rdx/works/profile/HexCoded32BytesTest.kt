package rdx.works.profile

import org.junit.Test
import rdx.works.profile.data.model.factorsources.FactorSource
import java.lang.IllegalArgumentException
import kotlin.test.assertEquals

class HexCoded32BytesTest {

    @Test
    fun `given a correct 32 byte hex string, when HexCoded32Bytes, then no error occurs`() {
        val hexString = "5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5"

        val hexCoded32Bytes = FactorSource.HexCoded32Bytes(
            value = hexString
        )

        assertEquals(hexString, hexCoded32Bytes.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given a wrong 32 byte hex string, when HexCoded32Bytes, then an error occurs`() {
        val hexString = "5f07ec336e9e7891bff040c817201e73c097b6b1e1b3a26c501e0010196f5"

        FactorSource.HexCoded32Bytes(
            value = hexString
        )
    }
}