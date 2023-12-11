package rdx.works.profile.domain

import kotlinx.coroutines.flow.first
import rdx.works.core.mapWhen
import rdx.works.core.toIdentifiedArrayList
import rdx.works.profile.data.model.extensions.hasWrongDerivationPathScheme
import rdx.works.profile.data.model.extensions.updateDerivationPathScheme
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.repository.updateProfile
import javax.inject.Inject

class CorrectLegacyAccountsDerivationPathSchemeUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke() {
        if (getProfileUseCase.isInitialized().not()) return
        val profile = profileRepository.profile.first()
        val needUpdate = profile.networks.any { network ->
            network.accounts.any { account ->
                account.hasWrongDerivationPathScheme
            }
        }
        if (needUpdate) {
            profileRepository.updateProfile { p ->
                val updatedNetworks = p.networks.mapWhen(predicate = { network ->
                    network.accounts.any {
                        it.hasWrongDerivationPathScheme
                    }
                }, mutation = { network ->
                    network.copy(
                        accounts = network.accounts.mapWhen(predicate = { account ->
                            account.hasWrongDerivationPathScheme
                        }, mutation = { account ->
                            account.updateDerivationPathScheme(DerivationPathScheme.BIP_44_OLYMPIA)
                        }).toIdentifiedArrayList()
                    )
                })
                p.copy(networks = updatedNetworks)
            }
        }
    }
}
