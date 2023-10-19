package rdx.works.profile.domain

import kotlinx.coroutines.flow.first
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.toDistinctList
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class AddLedgerFactorSourceUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(
        ledgerId: HexCoded32Bytes,
        model: LedgerHardwareWalletFactorSource.DeviceModel,
        name: String?
    ): AddLedgerFactorSourceResult {
        val ledgerFactorSource = LedgerHardwareWalletFactorSource.newSource(
            deviceID = ledgerId,
            model = model,
            name = name.orEmpty() // it should not be null
        )

        val profile = profileRepository.profile.first()
        val existingLedgerFactorSource = getProfileUseCase.factorSourceById(
            FactorSource.FactorSourceID.FromHash(
                kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                body = ledgerId
            )
        )

        if (existingLedgerFactorSource != null) {
            return AddLedgerFactorSourceResult.AlreadyExist(existingLedgerFactorSource as LedgerHardwareWalletFactorSource)
        }

        val updatedProfile = profile.copy(factorSources = (profile.factorSources + listOf(ledgerFactorSource)).toDistinctList())
        profileRepository.saveProfile(updatedProfile)
        return AddLedgerFactorSourceResult.Added(ledgerFactorSource)
    }
}

sealed class AddLedgerFactorSourceResult(open val ledgerFactorSource: LedgerHardwareWalletFactorSource) {

    data class Added(
        override val ledgerFactorSource: LedgerHardwareWalletFactorSource
    ) : AddLedgerFactorSourceResult(ledgerFactorSource)

    data class AlreadyExist(
        override val ledgerFactorSource: LedgerHardwareWalletFactorSource
    ) : AddLedgerFactorSourceResult(ledgerFactorSource)
}
