package rdx.works.core.sargon

import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSources
import com.radixdlt.sargon.extensions.invoke

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

val DeviceFactorSource.hasBabylonCryptoParameters: Boolean
    get() = common.cryptoParameters == FactorSourceCryptoParameters.babylon

val DeviceFactorSource.supportsOlympia: Boolean
    get() = common.cryptoParameters.supportsOlympia

val DeviceFactorSource.hasOlympiaSeedPhraseLength: Boolean
    get() = hint.mnemonicWordCount != Bip39WordCount.TWENTY_FOUR

val DeviceFactorSource.hasBabylonSeedPhraseLength: Boolean
    get() = hint.mnemonicWordCount == Bip39WordCount.TWENTY_FOUR

val DeviceFactorSource.isBabylonDeviceFactorSource: Boolean
    get() = supportsBabylon && hasBabylonSeedPhraseLength