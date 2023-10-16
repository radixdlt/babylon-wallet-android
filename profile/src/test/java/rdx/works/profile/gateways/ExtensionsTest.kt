package rdx.works.profile.gateways

import org.junit.Assert
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.containsGateway

class ExtensionsTest {

    @Test
    fun `containsGateway extension`() {
        val savedGateways = mutableListOf<Radix.Gateway>()
        val mainnet = Radix.Gateway(
            url = "https://mainnet.radixdlt.com",
            network = Radix.Network(
                id = 1,
                name = "Mainnet",
                displayDescription = "Mainnet"
            )
        )
        savedGateways.add(mainnet)

        // a later app version fixed the properties of this object
        val updatedMainnet = Radix.Gateway(
            url = "https://mainnet.radixdlt.com/", // added a slash in the end
            network = Radix.Network(
                id = 1,
                name = "mainnet", // name fixed
                displayDescription = "Mainnet"
            )
        )

        Assert.assertTrue(savedGateways.containsGateway(updatedMainnet))
    }
}