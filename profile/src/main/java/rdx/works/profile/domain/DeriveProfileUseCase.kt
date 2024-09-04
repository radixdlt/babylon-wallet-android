package rdx.works.profile.domain

import com.radixdlt.sargon.AppPreferences
import com.radixdlt.sargon.ContentHint
import com.radixdlt.sargon.DeviceInfo
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Header
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileNetwork
import com.radixdlt.sargon.ProfileSnapshotVersion
import com.radixdlt.sargon.extensions.Accounts
import com.radixdlt.sargon.extensions.FactorSources
import com.radixdlt.sargon.extensions.ProfileNetworks
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.default
import com.radixdlt.sargon.extensions.from
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import rdx.works.core.TimestampGenerator
import rdx.works.core.UUIDGenerator
import rdx.works.core.di.DefaultDispatcher
import rdx.works.core.domain.ProfileState
import rdx.works.core.mapError
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.os.SargonOsManager
import rdx.works.core.then
import rdx.works.profile.data.repository.HostInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class DeriveProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mnemonicRepository: MnemonicRepository,
    private val hostInfoRepository: HostInfoRepository,
    private val preferencesManager: PreferencesManager,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        deviceFactorSource: FactorSource.Device,
        mnemonicWithPassphrase: MnemonicWithPassphrase,
        accounts: Accounts
    ): Result<Unit> = runCatching {
        when (profileRepository.profileState.first()) {
            is ProfileState.Restored -> return@runCatching
            else -> withContext(defaultDispatcher) {
                val sargonOs = sargonOsManager.sargonOs.firstOrNull() ?: return@withContext
                val accountsList = accounts.asList()
                val networkId = accountsList.firstOrNull()?.networkId ?: NetworkId.MAINNET

                val hostId = hostInfoRepository.getHostId().getOrThrow()
                val hostInfo = hostInfoRepository.getHostInfo().getOrThrow()
                val creatingDevice = DeviceInfo.from(hostId, hostInfo)

                val header = Header(
                    snapshotVersion = ProfileSnapshotVersion.V100,
                    id = UUIDGenerator.uuid(),
                    creatingDevice = creatingDevice,
                    lastUsedOnDevice = creatingDevice,
                    lastModified = TimestampGenerator(),
                    contentHint = ContentHint(
                        numberOfAccountsOnAllNetworksInTotal = accounts.size.toUShort(),
                        numberOfPersonasOnAllNetworksInTotal = 0u,
                        numberOfNetworks = 1u
                    )
                )

                val network = ProfileNetwork(
                    id = networkId,
                    accounts = accounts.asList(),
                    personas = emptyList(),
                    authorizedDapps = emptyList()
                )

                val profile = Profile(
                    header = header,
                    appPreferences = AppPreferences.default(),
                    factorSources = FactorSources(deviceFactorSource).asList(),
                    networks = ProfileNetworks(network).asList()
                )

                mnemonicRepository.saveMnemonic(
                    deviceFactorSource.value.id.asGeneral(),
                    mnemonicWithPassphrase
                ).mapError {
                    ProfileException.SecureStorageAccess
                }.mapCatching {
                    sargonOs.importWallet(profile = profile, bdfsSkipped = false)
                }.onSuccess {
                    preferencesManager.markFactorSourceBackedUp(deviceFactorSource.value.id.asGeneral())
                }.getOrThrow()
            }
        }
    }
}
