@file:Suppress("LongParameterList")

package rdx.works.profile.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.IdentifiedArrayList
import rdx.works.core.annotations.DebugOnly
import rdx.works.core.emptyIdentifiedArrayList
import rdx.works.core.identifiedArrayListOf
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.apppreferences.Transaction
import rdx.works.profile.data.model.extensions.isHidden
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.di.SerializerModule
import java.time.Instant

data class Profile(
    val header: Header,

    /**
     * Settings for this profile in the app, contains default security configs as well as display settings.
     */
    val appPreferences: AppPreferences,

    /**
     * The known sources of factors, used for authorization such as spending funds.
     * Always contains at least one DeviceFactorSource.
     */
    val factorSources: IdentifiedArrayList<FactorSource>,

    /**
     * Effectively **per network**: a list of accounts, personas and connected dApps.
     */
    val networks: List<Network>,
) {

    internal fun snapshot(): ProfileSnapshot {
        return ProfileSnapshot(
            header = header,
            appPreferences = appPreferences,
            factorSources = factorSources,
            networks = networks
        )
    }

    internal fun withUpdatedContentHint() = copy(
        header = header.copy(
            contentHint = Header.ContentHint(
                numberOfNetworks = networks.size,
                numberOfAccountsOnAllNetworksInTotal = networks.sumOf { network -> network.accounts.count { it.isHidden().not() } },
                numberOfPersonasOnAllNetworksInTotal = networks.sumOf { network -> network.personas.count { it.isHidden().not() } }
            )
        )
    )

    companion object {
        fun init(
            id: String,
            deviceInfo: DeviceInfo,
            creationDate: Instant,
            gateways: Gateways = Gateways.preset,
        ): Profile {
            val appPreferences = AppPreferences(
                transaction = Transaction.default,
                display = Display.default,
                security = Security.default,
                gateways = gateways,
                p2pLinks = listOf()
            )

            return Profile(
                header = Header.init(
                    id = id,
                    deviceInfo = deviceInfo,
                    creationDate = creationDate,
                    numberOfNetworks = 0
                ),
                appPreferences = appPreferences,
                factorSources = emptyIdentifiedArrayList(),
                networks = emptyList()
            )
        }

        fun initWithFactorSource(
            id: String,
            deviceInfo: DeviceInfo,
            creationDate: Instant,
            gateways: Gateways = Gateways.preset,
            factorSource: FactorSource,
            accounts: IdentifiedArrayList<Network.Account>
        ): Profile {
            val networks = listOf(
                Network(
                    accounts = accounts,
                    authorizedDapps = listOf(),
                    networkID = gateways.current().network.id,
                    personas = emptyIdentifiedArrayList()
                )
            )

            val appPreferences = AppPreferences(
                transaction = Transaction.default,
                display = Display.default,
                security = Security.default,
                gateways = gateways,
                p2pLinks = listOf()
            )

            return Profile(
                header = Header.init(
                    id = id,
                    deviceInfo = deviceInfo,
                    creationDate = creationDate,
                    numberOfNetworks = networks.size
                ),
                appPreferences = appPreferences,
                factorSources = identifiedArrayListOf(factorSource),
                networks = networks
            )
        }
    }
}

val Profile.currentGateway: Radix.Gateway
    get() = appPreferences.gateways.current()

val Profile.currentNetwork: Network
    get() {
        val currentGateway = currentGateway
        return networks.find {
            it.networkID == currentGateway.network.id
        } ?: Network(
            networkID = Radix.Gateway.default.network.id,
            accounts = identifiedArrayListOf(),
            personas = identifiedArrayListOf(),
            authorizedDapps = emptyList()
        )
    }

/**
 * Used only by debug features, like inspect profile, where it is the only place
 * in UI where the profile's json needs to be previewed.
 */
@Suppress("JSON_FORMAT_REDUNDANT")
@DebugOnly
fun Profile.prettyPrinted(): String {
    return Json(from = SerializerModule.provideProfileSerializer()) {
        prettyPrint = true
    }.encodeToString(snapshot())
}
