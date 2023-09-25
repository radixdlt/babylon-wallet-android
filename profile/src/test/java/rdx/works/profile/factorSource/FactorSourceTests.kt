package rdx.works.profile.factorSource

import org.junit.Test
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource.Companion.factorSourceId
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FactorSourceTests {

    @Test
    fun `test factor source id`() {
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                    "humble limb repeat video sudden possible story mask neutral prize goose mandate",
            bip39Passphrase = ""
        )

        assertEquals("6facb00a836864511fdf8f181382209e64e83ad462288ea1bc7868f236fb8033", factorSourceId(mnemonicWithPassphrase))
    }

    @Test
    fun `test factor source id ledger`() {
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "equip will roof matter pink blind book anxiety banner elbow sun young",
            bip39Passphrase = ""
        )

        assertEquals("41ac202687326a4fc6cb677e9fd92d08b91ce46c669950d58790d4d5e583adc0", factorSourceId(mnemonicWithPassphrase))
    }

    @Test
    fun `test empty or with passphrase`() {
        val mnemonicOnly = MnemonicWithPassphrase(
            mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong",
            bip39Passphrase = ""
        )

        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong",
            bip39Passphrase = "foo"
        )

        assertEquals("09a501e4fafc7389202a82a3237a405ed191cdb8a4010124ff8e2c9259af1327", factorSourceId(mnemonicOnly))
        assertEquals("537b56b9881258f08994392e9858962825d92361b6b4775a3bdfeb4eecc0d069", factorSourceId(mnemonicWithPassphrase))
    }

    @Test
    fun `test on device factor source`() {
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "spirit bird issue club alcohol flock skull health lemon judge piece eyebrow",
            bip39Passphrase = ""
        )

        val factorSource = DeviceFactorSource.olympia(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            model = "computer",
            name = "unit test",
            createdAt = Instant.ofEpochSecond(0)
        )

        assertTrue(factorSource.isOlympia)
        assertEquals("c23c47a8a37b79298878506692e42f3a1a11967ff1239bb344ad6ab0c21ddda8", factorSource.id.body.value)
    }
}
