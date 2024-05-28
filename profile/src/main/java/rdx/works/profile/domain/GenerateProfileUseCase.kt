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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.TimestampGenerator
import rdx.works.core.UUIDGenerator
import rdx.works.core.domain.ProfileState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.babylon
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class GenerateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mnemonicRepository: MnemonicRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val preferencesManager: PreferencesManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(mnemonicWithPassphrase: MnemonicWithPassphrase): Profile {
        val device = deviceInfoRepository.getDeviceInfo()

        val date = TimestampGenerator()
        val creatingDevice = DeviceInfo(
            id = device.id,
            date = date,
            description = device.displayName
        )

        val header = Header(
            snapshotVersion = ProfileSnapshotVersion.V100,
            id = UUIDGenerator.uuid(),
            creatingDevice = creatingDevice,
            lastUsedOnDevice = creatingDevice,
            lastModified = date,
            contentHint = ContentHint(
                numberOfAccountsOnAllNetworksInTotal = 0u,
                numberOfPersonasOnAllNetworksInTotal = 0u,
                numberOfNetworks = 0u
            )
        )

        val bdfs = FactorSource.Device.babylon(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            model = device.model,
            name = device.name,
            createdAt = date,
            isMain = true
        )

        return Profile(
            header = header,
            appPreferences = AppPreferences.default(),
            factorSources = listOf(bdfs),
            networks = emptyList()
        ).also {
            profileRepository.saveProfile(it)
        }
    }

    suspend fun derived(
        deviceFactorSource: FactorSource.Device,
        mnemonicWithPassphrase: MnemonicWithPassphrase,
        accounts: Accounts
    ): Result<Profile> {
        return runCatching {
            val accountsList = accounts.asList()
            val networkId = accountsList.firstOrNull()?.networkId ?: NetworkId.MAINNET
            when (val state = profileRepository.profileState.first()) {
                is ProfileState.Restored -> state.profile
                else -> withContext(defaultDispatcher) {
                    val device = deviceInfoRepository.getDeviceInfo()

                    val date = TimestampGenerator()
                    val creatingDevice = DeviceInfo(
                        id = device.id,
                        date = date,
                        description = device.displayName
                    )

                    val header = Header(
                        snapshotVersion = ProfileSnapshotVersion.V100,
                        id = UUIDGenerator.uuid(),
                        creatingDevice = creatingDevice,
                        lastUsedOnDevice = creatingDevice,
                        lastModified = date,
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

                    mnemonicRepository.saveMnemonic(deviceFactorSource.value.id.asGeneral(), mnemonicWithPassphrase).fold(onSuccess = {
                        profileRepository.saveProfile(profile)
                        preferencesManager.markFactorSourceBackedUp(deviceFactorSource.value.id.asGeneral())
                        Result.success(profile)
                    }, onFailure = {
                        Result.failure(ProfileException.SecureStorageAccess)
                    }).getOrThrow()
                }
            }
        }
    }
}
