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
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.extensions.invoke
import java.time.OffsetDateTime

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

val FactorSource.id: FactorSourceId
    get() = when (val factor = this) {
        is FactorSource.Device -> FactorSourceId.Hash(factor.value.id)
        is FactorSource.Ledger -> FactorSourceId.Hash(factor.value.id)
    }

val DeviceFactorSource.supportsBabylon: Boolean
    get() = common.cryptoParameters.supportsBabylon

val DeviceFactorSource.supportsOlympia: Boolean
    get() = common.cryptoParameters.supportsOlympia

val DeviceFactorSource.hasOlympiaSeedPhraseLength: Boolean
    get() = hint.mnemonicWordCount != Bip39WordCount.TWENTY_FOUR

val DeviceFactorSource.hasBabylonSeedPhraseLength: Boolean
    get() = hint.mnemonicWordCount == Bip39WordCount.TWENTY_FOUR

val DeviceFactorSource.isBabylonDeviceFactorSource: Boolean
    get() = supportsBabylon && hasBabylonSeedPhraseLength

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
        id = FactorSourceIdFromHash(
            kind = FactorSourceKind.DEVICE,
            body = mnemonicWithPassphrase.toFactorSourceId(),
        ),
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