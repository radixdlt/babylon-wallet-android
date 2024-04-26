package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSources
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.LedgerHardwareWalletModel
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.factorSourceById
import rdx.works.core.sargon.init
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class AddLedgerFactorSourceUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        ledgerId: FactorSourceId.Hash,
        model: LedgerHardwareWalletModel,
        name: String?
    ): AddLedgerFactorSourceResult {
        val profile = profileRepository.profile.first()
        val existingLedgerFactorSource = profile.factorSourceById(id = ledgerId) as? FactorSource.Ledger

        if (existingLedgerFactorSource != null) {
            return AddLedgerFactorSourceResult.AlreadyExist(existingLedgerFactorSource)
        }

        val ledgerFactorSource = FactorSource.Ledger.init(
            id = ledgerId,
            model = model,
            name = name.orEmpty() // it should not be null
        )

        val updatedProfile = profile.copy(factorSources = FactorSources.init(profile.factorSources() + ledgerFactorSource))
        profileRepository.saveProfile(updatedProfile)
        return AddLedgerFactorSourceResult.Added(ledgerFactorSource)
    }
}

sealed class AddLedgerFactorSourceResult(open val ledgerFactorSource: FactorSource.Ledger) {

    data class Added(
        override val ledgerFactorSource: FactorSource.Ledger
    ) : AddLedgerFactorSourceResult(ledgerFactorSource)

    data class AlreadyExist(
        override val ledgerFactorSource: FactorSource.Ledger
    ) : AddLedgerFactorSourceResult(ledgerFactorSource)
}
