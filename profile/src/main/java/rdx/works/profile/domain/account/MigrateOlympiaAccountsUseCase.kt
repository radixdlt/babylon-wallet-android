package rdx.works.profile.domain.account

import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.request.DeriveBabylonAddressFromOlympiaAddressRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.addAccount
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import javax.inject.Inject

class MigrateOlympiaAccountsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        olympiaAccounts: List<OlympiaAccountDetails>,
        factorSourceId: FactorSource.ID
    ): List<Network.Account> {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val networkId = profile.currentNetwork.knownNetworkId ?: Radix.Gateway.default.network.networkId()
            val accountOffset = profile.currentNetwork.accounts.size
            val migratedAccounts = olympiaAccounts.map { olympiaAccount ->
                val babylonAddress = RadixEngineToolkit.deriveBabylonAddressFromOlympiaAddress(
                    DeriveBabylonAddressFromOlympiaAddressRequest(
                        networkId.value.toUByte(),
                        olympiaAccount.address
                    )
                ).getOrThrow().babylonAccountAddress.address
                Network.Account(
                    displayName = olympiaAccount.accountName.ifEmpty { "Unnamed olympia account ${olympiaAccount.index}" },
                    address = babylonAddress,
                    appearanceID = accountOffset + olympiaAccount.index,
                    networkID = networkId.value,
                    securityState = SecurityState.unsecured(
                        publicKey = FactorInstance.PublicKey(olympiaAccount.publicKey, Slip10Curve.SECP_256K1),
                        derivationPath = DerivationPath.forLegacyOlympia(olympiaAccount.index),
                        factorSourceId = factorSourceId
                    )
                )
            }
            var updatedProfile = profile
            migratedAccounts.forEach { account ->
                updatedProfile = updatedProfile.addAccount(
                    account = account,
                    withFactorSourceId = factorSourceId,
                    onNetwork = networkId,
                    shouldUpdateFactorSourceNextDerivationIndex = false
                )
            }
            profileRepository.saveProfile(updatedProfile)
            migratedAccounts
        }
    }
}
