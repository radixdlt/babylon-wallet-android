package rdx.works.profile.domain

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class AddLedgerFactorSourceUseCase @Inject constructor(
    private val dataSource: ProfileRepository
) {

    suspend operator fun invoke(id: FactorSource.ID, model: FactorSource.LedgerHardwareWallet.DeviceModel, name: String?): LedgerAddResult {
        val ledgerFactorSource = FactorSource.ledger(
            id = id,
            model = model,
            name = name
        )
        val profile = dataSource.profile.first()
        val existingFactorSource = profile.factorSources.firstOrNull { it.id == id }
        if (existingFactorSource != null) return LedgerAddResult.Exist(existingFactorSource.id)
        val updatedProfile = profile.copy(factorSources = profile.factorSources + listOf(ledgerFactorSource))
        dataSource.saveProfile(updatedProfile)
        return LedgerAddResult.Added(ledgerFactorSource.id)
    }
}

sealed class LedgerAddResult(open val id: FactorSource.ID) {
    data class Added(override val id: FactorSource.ID) : LedgerAddResult(id)
    data class Exist(override val id: FactorSource.ID) : LedgerAddResult(id)
}
