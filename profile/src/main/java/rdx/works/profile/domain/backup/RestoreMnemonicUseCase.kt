package rdx.works.profile.domain.backup

import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.repository.MnemonicRepository
import javax.inject.Inject

class RestoreMnemonicUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository
) {

    suspend operator fun invoke(
        factorSourceId: FactorSource.ID,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ) = mnemonicRepository.saveMnemonic(factorSourceId, mnemonicWithPassphrase)
}
