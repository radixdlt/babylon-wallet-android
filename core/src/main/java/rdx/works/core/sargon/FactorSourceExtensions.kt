package rdx.works.core.sargon

import com.radixdlt.sargon.ArculusCardFactorSource
import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.DeviceFactorSourceHint
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceCommon
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HostInfo
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.LedgerHardwareWalletHint
import com.radixdlt.sargon.LedgerHardwareWalletModel
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.OffDeviceMnemonicFactorSource
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toBagOfBytes
import com.radixdlt.sargon.samples.Sample
import rdx.works.core.TimestampGenerator
import java.time.OffsetDateTime
import kotlin.random.Random

val FactorSource.Device.supportsBabylon: Boolean
    get() = value.common.cryptoParameters.supportsBabylon

val FactorSource.Device.supportsOlympia: Boolean
    get() = value.common.cryptoParameters.supportsOlympia

val FactorSource.Device.hasOlympiaSeedPhraseLength: Boolean
    get() = value.hint.mnemonicWordCount != Bip39WordCount.TWENTY_FOUR

val FactorSource.Device.hasBabylonSeedPhraseLength: Boolean
    get() = value.hint.mnemonicWordCount == Bip39WordCount.TWENTY_FOUR

val FactorSource.Device.isBabylonDeviceFactorSource: Boolean
    get() = supportsBabylon && hasBabylonSeedPhraseLength

val FactorSource.lastUsedOn: OffsetDateTime
    get() = when (this) {
        is FactorSource.ArculusCard -> this.value.common.lastUsedOn
        is FactorSource.Device -> this.value.common.lastUsedOn
        is FactorSource.Ledger -> this.value.common.lastUsedOn
        is FactorSource.OffDeviceMnemonic -> this.value.common.lastUsedOn
        is FactorSource.Password -> this.value.common.lastUsedOn
    }

fun FactorSource.Device.Companion.babylon(
    mnemonicWithPassphrase: MnemonicWithPassphrase,
    hostInfo: HostInfo,
    createdAt: Timestamp = OffsetDateTime.now(),
    isMain: Boolean = false
): FactorSource.Device = FactorSource.Device.babylon(
    mnemonicWithPassphrase = mnemonicWithPassphrase,
    hostInfo = hostInfo,
    createdAt = createdAt,
    isMain = isMain
)

fun FactorSource.Device.Companion.olympia(
    mnemonicWithPassphrase: MnemonicWithPassphrase,
    hostInfo: HostInfo,
    createdAt: Timestamp = OffsetDateTime.now()
): FactorSource.Device = FactorSource.Device.olympia(
    mnemonicWithPassphrase = mnemonicWithPassphrase,
    hostInfo = hostInfo,
    createdAt = createdAt
)

fun FactorSource.Ledger.Companion.init(
    id: FactorSourceId.Hash,
    name: String,
    model: LedgerHardwareWalletModel,
    createdAt: Timestamp = TimestampGenerator()
): FactorSource.Ledger = LedgerHardwareWalletFactorSource(
    id = id.value,
    common = FactorSourceCommon(
        cryptoParameters = FactorSourceCryptoParameters.olympiaBackwardsCompatible,
        addedOn = createdAt,
        lastUsedOn = createdAt,
        flags = emptyList()
    ),
    hint = LedgerHardwareWalletHint(
        label = name,
        model = model
    )
).asGeneral()

fun LedgerHardwareWalletModel.displayName() = when (this) {
    LedgerHardwareWalletModel.NANO_S -> "Ledger Nano S"
    LedgerHardwareWalletModel.NANO_S_PLUS -> "Ledger Nano S+"
    LedgerHardwareWalletModel.NANO_X -> "Ledger Nano X"
}

// TODO move to sargon
fun ArculusCardFactorSource.asGeneral() = FactorSource.ArculusCard(value = this)
fun OffDeviceMnemonicFactorSource.asGeneral() = FactorSource.OffDeviceMnemonic(value = this)

@UsesSampleValues
@Suppress("MagicNumber")
val FactorSource.Device.Companion.sample: Sample<FactorSource.Device>
    get() = object : Sample<FactorSource.Device> {
        override fun invoke(): FactorSource.Device = FactorSource.Device(
            value = DeviceFactorSource(
                id = FactorSourceIdFromHash(
                    kind = FactorSourceKind.DEVICE,
                    body = Exactly32Bytes.init(Random.nextBytes(32).toBagOfBytes())
                ),
                common = FactorSourceCommon(
                    cryptoParameters = FactorSourceCryptoParameters.babylon,
                    addedOn = TimestampGenerator(),
                    lastUsedOn = TimestampGenerator(),
                    flags = emptyList()
                ),
                hint = DeviceFactorSourceHint(
                    deviceName = "Babylon",
                    model = "Device 1",
                    label = "My Phone",
                    mnemonicWordCount = Bip39WordCount.TWENTY_FOUR,
                    systemVersion = "Android 14 (API 34)",
                    hostAppVersion = "1.0.0",
                    hostVendor = "Android"
                )
            )
        )

        override fun other(): FactorSource.Device = FactorSource.Device(
            value = DeviceFactorSource(
                id = FactorSourceIdFromHash(
                    kind = FactorSourceKind.DEVICE,
                    body = Exactly32Bytes.init(Random.nextBytes(32).toBagOfBytes())
                ),
                common = FactorSourceCommon(
                    cryptoParameters = FactorSourceCryptoParameters.olympiaBackwardsCompatible,
                    addedOn = TimestampGenerator(),
                    lastUsedOn = TimestampGenerator(),
                    flags = emptyList()
                ),
                hint = DeviceFactorSourceHint(
                    deviceName = "Olympia",
                    model = "Device 1",
                    label = "My Phone",
                    mnemonicWordCount = Bip39WordCount.TWENTY_FOUR,
                    systemVersion = "Android 14 (API 34)",
                    hostAppVersion = "1.0.0",
                    hostVendor = "Android"
                )
            )
        )
    }

@UsesSampleValues
@Suppress("MagicNumber")
val FactorSource.Ledger.Companion.sample: Sample<FactorSource.Ledger>
    get() = object : Sample<FactorSource.Ledger> {
        override fun invoke(): FactorSource.Ledger = FactorSource.Ledger.init(
            id = FactorSourceId.Hash(
                value = FactorSourceIdFromHash(
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    body = Exactly32Bytes.init(Random.nextBytes(32).toBagOfBytes())
                )
            ),
            name = "My Nano S",
            model = LedgerHardwareWalletModel.NANO_S
        )

        override fun other(): FactorSource.Ledger = FactorSource.Ledger.init(
            id = FactorSourceId.Hash(
                value = FactorSourceIdFromHash(
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    body = Exactly32Bytes.init(Random.nextBytes(32).toBagOfBytes())
                )
            ),
            name = "My Nano S+",
            model = LedgerHardwareWalletModel.NANO_S_PLUS
        )
    }
