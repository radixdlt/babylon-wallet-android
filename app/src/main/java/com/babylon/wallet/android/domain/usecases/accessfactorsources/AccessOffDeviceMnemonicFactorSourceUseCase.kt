package com.babylon.wallet.android.domain.usecases.accessfactorsources

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
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import kotlinx.coroutines.channels.Channel
import rdx.works.core.sargon.signInteractorInput
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
        input: PerFactorSourceInput<out Signable.Payload, out Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> {
        val seedPhrase = seedPhraseChannel.receive()
        val outcome = FactorOutcome.Signed(seedPhrase.signInteractorInput(input = input))

        updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)

        return Result.success(
            PerFactorOutcome(
                factorSourceId = input.factorSourceId,
                outcome = outcome
            )
        )
    }

    enum class SeedPhraseValidity {
        Valid,
        InvalidMnemonic,
        WrongMnemonic
    }

    override suspend fun spotCheck(factorSource: FactorSource.OffDeviceMnemonic): Result<Boolean> = runCatching {
        val seedPhrase = seedPhraseChannel.receive()

        factorSource.spotCheck(input = SpotCheckInput.Software(mnemonicWithPassphrase = seedPhrase))
    }
}
