package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.common.seedphrase.toMnemonicWithPassphraseOrNull
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.SpotCheckInput
import com.radixdlt.sargon.extensions.derivePublicKeys
import com.radixdlt.sargon.extensions.factorSourceId
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.spotCheck
import kotlinx.coroutines.channels.Channel
import rdx.works.profile.domain.UpdateFactorSourceLastUsedUseCase
import javax.inject.Inject

class AccessOffDeviceMnemonicFactorSourceUseCase @Inject constructor(
    private val updateFactorSourceLastUsedUseCase: UpdateFactorSourceLastUsedUseCase
) : AccessFactorSource<FactorSource.OffDeviceMnemonic> {

    private val seedPhraseChannel = Channel<MnemonicWithPassphrase>()

    suspend fun onSeedPhraseConfirmed(
        factorSourceId: FactorSourceIdFromHash,
        words: List<SeedPhraseWord>
    ): SeedPhraseValidity {
        val seedPhrase = words.toMnemonicWithPassphraseOrNull() ?: return SeedPhraseValidity.InvalidMnemonic

        val generatedId = seedPhrase.factorSourceId(kind = FactorSourceKind.OFF_DEVICE_MNEMONIC)

        return if (generatedId != factorSourceId) {
            SeedPhraseValidity.WrongMnemonic
        } else {
            seedPhraseChannel.send(seedPhrase)
            SeedPhraseValidity.Valid
        }
    }

    override suspend fun derivePublicKeys(
        factorSource: FactorSource.OffDeviceMnemonic,
        input: KeyDerivationRequestPerFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> {
        val seedPhrase = seedPhraseChannel.receive()
        val hdInstances = seedPhrase.derivePublicKeys(input.derivationPaths).map {
            HierarchicalDeterministicFactorInstance(
                factorSourceId = factorSource.value.id,
                publicKey = it
            )
        }

        updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)

        return Result.success(hdInstances)
    }

    override suspend fun signMono(
        factorSource: FactorSource.OffDeviceMnemonic,
        input: AccessFactorSourcesInput.Sign
    ): Result<AccessFactorSourcesOutput.Sign> {
        val mnemonic = seedPhraseChannel.receive()
        return runCatching {
            when (input) {
                is AccessFactorSourcesInput.SignTransaction -> mnemonic.signTransaction(input.input)
                is AccessFactorSourcesInput.SignSubintent -> mnemonic.signSubintent(input.input)
                is AccessFactorSourcesInput.SignAuth -> mnemonic.signAuth(input.input)
            }
        }.onSuccess {
            updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
        }
    }

    override suspend fun spotCheck(factorSource: FactorSource.OffDeviceMnemonic): Result<Boolean> = runCatching {
        val seedPhrase = seedPhraseChannel.receive()

        factorSource.spotCheck(input = SpotCheckInput.Software(mnemonicWithPassphrase = seedPhrase))
    }.onSuccess {
        updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
    }

    enum class SeedPhraseValidity {
        Valid,
        InvalidMnemonic,
        WrongMnemonic
    }
}
