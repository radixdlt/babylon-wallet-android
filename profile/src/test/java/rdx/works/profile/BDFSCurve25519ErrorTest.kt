package rdx.works.profile

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.PublicKeySurrogate
import java.io.File
import kotlin.test.assertEquals

class BDFSCurve25519ErrorTest {

    @Test
    fun `test secp256k1 public key with curve25519 decoding`() {
        val publicKey = Json.decodeFromString<PublicKeySurrogate>(File(FILE_CURVE_25519).readText())
        assertEquals(publicKey.curve, Slip10Curve.CURVE_25519) // error state
        val publicKeyCorrect = Json.decodeFromString<FactorInstance.PublicKey>(File(FILE_CURVE_25519).readText())
        assertEquals(publicKeyCorrect.curve, Slip10Curve.SECP_256K1) // correct state after decoding fix
    }

    @Test
    fun `test secp256k1 public key with curve25519 encoding`() {
        val publicKeyWithWrongCurve =
            FactorInstance.PublicKey("023a41f437972033fa83c3c4df08dc7d68212ccac07396a29aca971ad5ba3c27c8", Slip10Curve.CURVE_25519)
        val json = Json.encodeToString<FactorInstance.PublicKey>(publicKeyWithWrongCurve) // we encode with custom parser
        val publicKeySurrogate = Json.decodeFromString<PublicKeySurrogate>(json) // decode with surrogate as is, no correction
        assertEquals(publicKeySurrogate.curve, Slip10Curve.SECP_256K1) // error state
    }

    companion object {
        private const val FILE_CURVE_25519 = "src/test/resources/raw/secp256k1_public_key_with_curve25519_error.json"
    }
}
