package com.babylon.wallet.android.presentation.accessfactorsources.access

import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.common.seedphrase.toMnemonicWithPassphraseOrNull
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.newFactorSourceIdFromHashFromMnemonicWithPassphrase
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import kotlinx.coroutines.channels.Channel
import rdx.works.core.sargon.signInteractorInput
import javax.inject.Inject

class AccessOffDeviceMnemonicFactorSource @Inject constructor(): AccessFactorSource<FactorSource.OffDeviceMnemonic> {

    private val seedPhraseChannel = Channel<MnemonicWithPassphrase>()

    suspend fun onSeedPhraseConfirmed(
        factorSourceId: FactorSourceIdFromHash,
        words: List<SeedPhraseWord>
    ): SeedPhraseValidity {
        val seedPhrase = words.toMnemonicWithPassphraseOrNull() ?: return SeedPhraseValidity.InvalidMnemonic

        val generatedId = newFactorSourceIdFromHashFromMnemonicWithPassphrase(
            factorSourceKind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
            mnemonicWithPassphrase = seedPhrase
        )

        return if (generatedId != factorSourceId) {
            SeedPhraseValidity.DoesNotDeriveFactorSourceId
        } else {
            seedPhraseChannel.send(seedPhrase)
            SeedPhraseValidity.Valid
        }
    }

    override suspend fun signMono(
        factorSource: FactorSource.OffDeviceMnemonic,
        input: PerFactorSourceInput<Signable.Payload, Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> {
        val seedPhrase = seedPhraseChannel.receive()

        return Result.success(
            PerFactorOutcome(
                factorSourceId = input.factorSourceId,
                outcome = FactorOutcome.Signed(seedPhrase.signInteractorInput(input = input))
            )
        )
    }

    enum class SeedPhraseValidity {
        Valid,
        InvalidMnemonic,
        DoesNotDeriveFactorSourceId;

        fun isIncorrect(): Boolean = this != Valid
    }
}