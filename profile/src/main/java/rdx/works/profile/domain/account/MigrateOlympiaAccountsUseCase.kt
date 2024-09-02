package rdx.works.profile.domain.account

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSourceId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.addAccounts
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.from
import rdx.works.core.sargon.initOlympia
import rdx.works.core.sargon.nextAppearanceId
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.core.di.DefaultDispatcher
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import javax.inject.Inject

class MigrateOlympiaAccountsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        olympiaAccounts: List<OlympiaAccountDetails>,
        factorSourceId: FactorSourceId.Hash
    ): List<Account> {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val networkId = profile.currentGateway.network.id
            val appearanceIdOffset = profile.nextAppearanceId(forNetworkId = networkId)
            val migratedAccounts = olympiaAccounts.mapIndexed { index, olympiaAccount ->
                val nextAppearanceId = AppearanceId.from(offset = appearanceIdOffset.value + index.toUInt())

                Account.initOlympia(
                    networkId = networkId,
                    displayName = olympiaAccount.accountName.ifEmpty { "Unnamed olympia account ${olympiaAccount.index}" }.let {
                        DisplayName(it)
                    },
                    publicKey = olympiaAccount.publicKey,
                    derivationPath = olympiaAccount.derivationPath,
                    factorSourceId = factorSourceId,
                    customAppearanceId = nextAppearanceId
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
