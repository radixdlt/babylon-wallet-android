package rdx.works.profile.domain.account

import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.AddressHelper
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.extensions.nextAppearanceId
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.addAccounts
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
        factorSourceId: FactorSource.FactorSourceID.FromHash
    ): List<Network.Account> {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val networkId = profile.currentNetwork?.knownNetworkId ?: Radix.Gateway.default.network.networkId()
            val appearanceIdOffset = profile.nextAppearanceId(forNetworkId = networkId)
            val migratedAccounts = olympiaAccounts.mapIndexed { index, olympiaAccount ->
                val babylonAddress = AddressHelper.accountAddressFromOlympia(
                    olympiaAddress = olympiaAccount.address,
                    forNetworkId = networkId.value
                )
                val nextAppearanceId = (appearanceIdOffset + index) % AccountGradientList.size
                Network.Account(
                    displayName = olympiaAccount.accountName.ifEmpty { "Unnamed olympia account ${olympiaAccount.index}" },
                    address = babylonAddress,
                    appearanceID = nextAppearanceId,
                    networkID = networkId.value,
                    securityState = SecurityState.unsecured(
                        publicKey = FactorInstance.PublicKey(olympiaAccount.publicKey, Slip10Curve.SECP_256K1),
                        derivationPath = DerivationPath.forLegacyOlympia(olympiaAccount.index),
                        factorSourceId = factorSourceId
                    ),
                    onLedgerSettings = Network.Account.OnLedgerSettings.init()
                )
            }
            var updatedProfile = profile
            migratedAccounts.forEach { account ->
                updatedProfile = updatedProfile.addAccounts(
                    accounts = listOf(account),
                    onNetwork = networkId
                )
            }
            profileRepository.saveProfile(updatedProfile)
            migratedAccounts
        }
    }
}
