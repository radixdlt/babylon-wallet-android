package rdx.works.core.sargon

import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.SargonException
import com.radixdlt.sargon.extensions.string

@Throws(SargonException::class)
fun NetworkId.Companion.init(name: String) = NetworkId.entries.find { it.string == name } ?: throw CommonException.UnknownNetworkWithName(
    badValue = name
)

fun NetworkId?.orDefault() = this ?: Gateway.default.network.id

fun NetworkId.dashboardUrl() = when (this) {
    NetworkId.MAINNET -> {
        DASHBOARD_MAINNET_URL
    }

    NetworkId.ZABANET -> {
        DASHBOARD_RCNET_V3_URL
    }

    NetworkId.STOKENET -> {
        DASHBOARD_STOKENET_URL
    }

    NetworkId.HAMMUNET -> {
        DASHBOARD_HAMMUNET_URL
    }

    NetworkId.ENKINET -> {
        DASHBOARD_ENKINET_URL
    }

    NetworkId.MARDUNET -> {
        DASHBOARD_MARDUNET_URL
    }

    NetworkId.KISHARNET -> {
        DASHBOARD_KISHARNET_URL
    }

    else -> {
        DASHBOARD_MAINNET_URL
    }
}

private const val DASHBOARD_MAINNET_URL = "https://dashboard.radixdlt.com"
private const val DASHBOARD_RCNET_V3_URL = "https://rcnet-v3-dashboard.radixdlt.com"
private const val DASHBOARD_STOKENET_URL = "https://stokenet-dashboard.radixdlt.com"
private const val DASHBOARD_HAMMUNET_URL = "https://hammunet-dashboard.rdx-works-main.extratools.works"
private const val DASHBOARD_ENKINET_URL = "https://enkinet-dashboard.rdx-works-main.extratools.works"
private const val DASHBOARD_MARDUNET_URL = "https://mardunet-dashboard.rdx-works-main.extratools.works"
private const val DASHBOARD_KISHARNET_URL = "https://kisharnet-dashboard.radixdlt.com"
