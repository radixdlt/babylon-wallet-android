package rdx.works.core.sargon

import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.DeviceFactorSourceHint
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceCommon
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.FactorSourceFlag
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.FactorSources
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.LedgerHardwareWalletHint
import com.radixdlt.sargon.LedgerHardwareWalletModel
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.toBagOfBytes
import com.radixdlt.sargon.samples.Sample
import rdx.works.core.TimestampGenerator
import java.time.OffsetDateTime
import kotlin.random.Random

fun FactorSources.getById(id: FactorSourceId) = invoke().firstOrNull { factorSource ->
    when (factorSource) {
        is FactorSource.Device -> factorSource.value.id == (id as? FactorSourceId.Hash)?.value
        is FactorSource.Ledger -> factorSource.value.id == (id as? FactorSourceId.Hash)?.value
    }
}

fun FactorSources.getByIdValue(value: Exactly32Bytes) = invoke().firstOrNull { factorSource ->
    factorSource.idBytes == value
}

val FactorSource.idBytes: Exactly32Bytes
    get() = when (this) {
        is FactorSource.Device -> value.id.body
        is FactorSource.Ledger -> value.id.body
    }

fun List<DeviceFactorSource>.getById(id: FactorSourceId) = firstOrNull { factorSource ->
    factorSource.id == (id as? FactorSourceId.Hash)?.value
}

val DeviceFactorSource.supportsBabylon: Boolean
    get() = common.cryptoParameters.supportsBabylon

val DeviceFactorSource.supportsOlympia: Boolean
    get() = common.cryptoParameters.supportsOlympia

val DeviceFactorSource.hasOlympiaSeedPhraseLength: Boolean
    get() = hint.mnemonicWordCount != Bip39WordCount.TWENTY_FOUR

val DeviceFactorSource.hasBabylonSeedPhraseLength: Boolean
    get() = hint.mnemonicWordCount == Bip39WordCount.TWENTY_FOUR

val FactorSource.Device.isBabylonDeviceFactorSource: Boolean
    get() = value.supportsBabylon && value.hasBabylonSeedPhraseLength

fun DeviceFactorSource.Companion.babylon(
    mnemonicWithPassphrase: MnemonicWithPassphrase,
    model: String = "",
    name: String = "",
    createdAt: Timestamp = OffsetDateTime.now(),
    isMain: Boolean = false
): DeviceFactorSource = device(
    mnemonicWithPassphrase = mnemonicWithPassphrase,
    model = model,
    name = name,
    isOlympia = false,
    createdAt = createdAt,
    isMain = isMain
)

fun DeviceFactorSource.Companion.olympia(
    mnemonicWithPassphrase: MnemonicWithPassphrase,
    model: String = "",
    name: String = "",
    createdAt: Timestamp = OffsetDateTime.now()
): DeviceFactorSource = device(
    mnemonicWithPassphrase = mnemonicWithPassphrase,
    model = model,
    name = name,
    isOlympia = true,
    createdAt = createdAt,
    isMain = false
)

@Suppress("LongParameterList")
fun DeviceFactorSource.Companion.device(
    mnemonicWithPassphrase: MnemonicWithPassphrase,
    model: String = "",
    name: String = "",
    isOlympia: Boolean,
    createdAt: Timestamp,
    isMain: Boolean = false
): DeviceFactorSource {
    require((isMain && isOlympia).not()) {
        "Olympia Device factor source should never be marked 'main'."
    }
    return DeviceFactorSource(
        id = mnemonicWithPassphrase.toFactorSourceId().value,
        common = FactorSourceCommon(
            cryptoParameters = if (isOlympia) {
                FactorSourceCryptoParameters.olympia
            } else {
                FactorSourceCryptoParameters.babylon
            },
            addedOn = createdAt,
            lastUsedOn = createdAt,
            flags = if (isMain) listOf(FactorSourceFlag.MAIN) else emptyList()
        ),
        hint = DeviceFactorSourceHint(
            model = model,
            name = name,
            mnemonicWordCount = mnemonicWithPassphrase.mnemonic.wordCount
        )
    )
}

fun LedgerHardwareWalletFactorSource.Companion.init(
    id: FactorSourceId.Hash,
    name: String,
    model: LedgerHardwareWalletModel,
    createdAt: Timestamp = TimestampGenerator()
): LedgerHardwareWalletFactorSource = LedgerHardwareWalletFactorSource(
    id = id.value,
    common = FactorSourceCommon(
        cryptoParameters = FactorSourceCryptoParameters.olympiaBackwardsCompatible,
        addedOn = createdAt,
        lastUsedOn = createdAt,
        flags = emptyList()
    ),
    hint = LedgerHardwareWalletHint(
        name = name,
        model = model
    )
)

fun LedgerHardwareWalletModel.displayName() = when (this) {
    LedgerHardwareWalletModel.NANO_S -> "Ledger Nano S"
    LedgerHardwareWalletModel.NANO_S_PLUS -> "Ledger Nano S+"
    LedgerHardwareWalletModel.NANO_X -> "Ledger Nano X"
}

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
                    name = "Babylon",
                    model = "Device 1",
                    mnemonicWordCount = Bip39WordCount.TWENTY_FOUR
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
                    name = "Olympia",
                    model = "Device 1",
                    mnemonicWordCount = Bip39WordCount.TWENTY_FOUR
                )
            )
        )
    }

@UsesSampleValues
@Suppress("MagicNumber")
val FactorSource.Ledger.Companion.sample: Sample<FactorSource.Ledger>
    get() = object : Sample<FactorSource.Ledger> {
        override fun invoke(): FactorSource.Ledger = FactorSource.Ledger(
            value = LedgerHardwareWalletFactorSource.init(
                id = FactorSourceId.Hash(
                    value = FactorSourceIdFromHash(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        body = Exactly32Bytes.init(Random.nextBytes(32).toBagOfBytes())
                    )
                ),
                name = "My Nano S",
                model = LedgerHardwareWalletModel.NANO_S
            )
        )

        override fun other(): FactorSource.Ledger = FactorSource.Ledger(
            value = LedgerHardwareWalletFactorSource.init(
                id = FactorSourceId.Hash(
                    value = FactorSourceIdFromHash(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        body = Exactly32Bytes.init(Random.nextBytes(32).toBagOfBytes())
                    )
                ),
                name = "My Nano S+",
                model = LedgerHardwareWalletModel.NANO_S_PLUS
            )
        )
    }
