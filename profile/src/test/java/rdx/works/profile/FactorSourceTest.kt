package rdx.works.profile

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.factorsources.OffDeviceMnemonicFactorSource
import rdx.works.profile.data.model.factorsources.TrustedContactFactorSource
import rdx.works.profile.domain.TestData
import java.io.File
import kotlin.test.Test


class FactorSourceTest {

    @Test
    fun `test generate vector`() {
        val mnemonic =
            MnemonicWords("zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote")
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = mnemonic.toString(),
            bip39Passphrase = "Radix"
        )

        val babylon = DeviceFactorSource.babylon(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            model = "Pixel 8",
            name = "new phone"
        )
        val olympia = DeviceFactorSource.olympia(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            model = "Nokia 3310",
            name = "old phone"
        )
        val ledger = LedgerHardwareWalletFactorSource.newSource(
            model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
            name = "my ledger",
            deviceID = TestData.ledgerFactorSource.id.body
        )

        val offDeviceMnemonicFactorSource = OffDeviceMnemonicFactorSource.newSource(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            label = "off device"
        )

        val trustedContactFactorSource = TrustedContactFactorSource.newSource(
            accountAddress = FactorSource.AccountAddress("account_tdx_c_1px0jul7a44s65568d32f82f0lkssjwx6f5t5e44yl6csqurxw3"),
            emailAddress = "panathinaikos@fc.com",
            name = "Alafouzos"
        )

        val listOfFactorSources = listOf(
            babylon,
            olympia,
            ledger,
            offDeviceMnemonicFactorSource,
            trustedContactFactorSource
        )

        val result = Json.encodeToString(listOfFactorSources)
        println(result)
    }

    @Test
    fun `test factor sources`() {
        val factorSourcesJson = File("src/test/resources/raw/factor_sources.json").readText()
        val factorSources = Json.decodeFromString<List<FactorSource>>(factorSourcesJson)
        assert(factorSources.count() == 5)

        val babylon = factorSources[0] as DeviceFactorSource
        assert(babylon.id.kind == FactorSourceKind.DEVICE)

        val olympia = factorSources[1] as DeviceFactorSource
        assert(olympia.id.kind == FactorSourceKind.DEVICE)

        val ledger = factorSources[2] as LedgerHardwareWalletFactorSource
        assert(ledger.id.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET)

        val offDeviceMnemonic = factorSources[3] as OffDeviceMnemonicFactorSource
        assert(offDeviceMnemonic.id.kind == FactorSourceKind.OFF_DEVICE_MNEMONIC)

        val trustedContact = factorSources[4] as TrustedContactFactorSource
        assert(trustedContact.id.kind == FactorSourceKind.TRUSTED_CONTACT)
    }
}
