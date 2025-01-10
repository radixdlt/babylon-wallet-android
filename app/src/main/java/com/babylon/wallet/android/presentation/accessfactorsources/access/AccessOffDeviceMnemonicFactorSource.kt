package com.babylon.wallet.android.presentation.accessfactorsources.access

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import kotlinx.coroutines.channels.Channel
import rdx.works.core.sargon.signInteractorInput

class AccessOffDeviceMnemonicFactorSource: AccessFactorSource<FactorSource.OffDeviceMnemonic> {

    private val seedPhraseChannel = Channel<MnemonicWithPassphrase>()

    suspend fun onSeedPhraseProvided(
        seedPhrase: MnemonicWithPassphrase
    ) {
        seedPhraseChannel.send(seedPhrase)
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
}