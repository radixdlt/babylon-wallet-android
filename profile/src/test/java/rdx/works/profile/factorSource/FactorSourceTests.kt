package rdx.works.profile.factorSource

import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.Mnemonic
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.init
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import rdx.works.core.sargon.init
import rdx.works.core.sargon.olympia
import rdx.works.core.sargon.supportsOlympia
import rdx.works.core.sargon.toFactorSourceId
import kotlin.test.Test

class FactorSourceTests {

    @Test
    fun `test factor source id`() {
        val mnemonicWithPassphrase = MnemonicWithPassphrase.init(
            phrase = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                    "humble limb repeat video sudden possible story mask neutral prize goose mandate"
        )

        assertEquals(
            "6facb00a836864511fdf8f181382209e64e83ad462288ea1bc7868f236fb8033",
            mnemonicWithPassphrase.toFactorSourceId().value.body.hex
        )
    }

    @Test
    fun `test factor source id ledger`() {
        val mnemonicWithPassphrase = MnemonicWithPassphrase.init(
            phrase = "equip will roof matter pink blind book anxiety banner elbow sun young"
        )

        assertEquals(
            "41ac202687326a4fc6cb677e9fd92d08b91ce46c669950d58790d4d5e583adc0",
            mnemonicWithPassphrase.toFactorSourceId().value.body.hex
        )
    }

    @Test
    fun `test empty or with passphrase`() {
        val mnemonicOnly = MnemonicWithPassphrase.init(
            phrase = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong"
        )

        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = Mnemonic.init(phrase = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong"),
            passphrase = "foo"
        )

        assertEquals("09a501e4fafc7389202a82a3237a405ed191cdb8a4010124ff8e2c9259af1327", mnemonicOnly.toFactorSourceId().value.body.hex)
        assertEquals(
            "537b56b9881258f08994392e9858962825d92361b6b4775a3bdfeb4eecc0d069",
            mnemonicWithPassphrase.toFactorSourceId().value.body.hex
        )
    }

    @Test
    fun `test on device factor source`() {
        val mnemonicWithPassphrase = MnemonicWithPassphrase.init(
            phrase = "spirit bird issue club alcohol flock skull health lemon judge piece eyebrow"
        )

        val factorSource = DeviceFactorSource.olympia(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            model = "computer",
            name = "unit test"
        )

        assertTrue(factorSource.supportsOlympia)
        assertEquals("c23c47a8a37b79298878506692e42f3a1a11967ff1239bb344ad6ab0c21ddda8", factorSource.id.body.hex)
    }
}
