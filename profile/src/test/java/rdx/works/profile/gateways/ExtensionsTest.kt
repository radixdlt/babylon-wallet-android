package rdx.works.profile.gateways

import org.junit.Assert
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.containsGateway

class ExtensionsTest {

    @Test
    fun `containsGateway extension with Radix mainnet gateway`() {
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

    @Test
    fun `existing custom gateway with slash and new gateway with same id but without path then containsGateway returns true`() {
        val savedGateways = mutableListOf<Radix.Gateway>()

        val myGatewayWithoutPath = Radix.Gateway(
            url = "https://my.gateway.com/",
            network = Radix.Network(
                id = 11,
                name = "mygateway",
                displayDescription = "My gateway"
            )
        )
        savedGateways.add(myGatewayWithoutPath)

        val myGatewayWithoutSlash = Radix.Gateway(
            url = "https://my.gateway.com",
            network = Radix.Network(
                id = 11,
                name = "mygateway",
                displayDescription = "My gateway"
            )
        )
        Assert.assertTrue(savedGateways.containsGateway(myGatewayWithoutSlash))
    }

    @Test
    fun `existing gateway with path and new gateway with same id but without path then containsGateway returns false`() {
        val savedGateways = mutableListOf<Radix.Gateway>()

        val myGatewayWithPath = Radix.Gateway(
            url = "https://my.gateway.com/path/morepath",
            network = Radix.Network(
                id = 11,
                name = "mygateway",
                displayDescription = "Gateway"
            )
        )
        savedGateways.add(myGatewayWithPath)

        val myGatewayWithoutPath = Radix.Gateway(
            url = "https://my.gateway.com/",
            network = Radix.Network(
                id = 11,
                name = "mygateway",
                displayDescription = "My gateway"
            )
        )
        Assert.assertFalse(savedGateways.containsGateway(myGatewayWithoutPath))
    }

    @Test
    fun `existing gateway with path and new gateway with same id and with slash then containsGateway returns true`() {
        val savedGateways = mutableListOf<Radix.Gateway>()

        val myGatewayWithPath = Radix.Gateway(
            url = "https://my.gateway.com/path/morepath",
            network = Radix.Network(
                id = 11,
                name = "mygateway",
                displayDescription = "Gateway"
            )
        )
        savedGateways.add(myGatewayWithPath)

        val myGatewayWithPathAndSlash = Radix.Gateway(
            url = "https://my.gateway.com/path/morepath/",
            network = Radix.Network(
                id = 11,
                name = "mygateway",
                displayDescription = "Gateway"
            )
        )
        Assert.assertTrue(savedGateways.containsGateway(myGatewayWithPathAndSlash))
    }

    @Test
    fun `existing gateway with slash and new gateway with same id but without slash then containsGateway returns true`() {
        val savedGateways = mutableListOf<Radix.Gateway>()

        val mainnet = Radix.Gateway(
            url = "https://1.1.1.1/",
            network = Radix.Network(
                id = 1,
                name = "Mainnet",
                displayDescription = "Mainnet"
            )
        )
        savedGateways.add(mainnet)

        val updatedMainnet = Radix.Gateway(
            url = "https://1.1.1.1",
            network = Radix.Network(
                id = 1,
                name = "mainnet",
                displayDescription = "Mainnet"
            )
        )

        Assert.assertTrue(savedGateways.containsGateway(updatedMainnet))
    }

    @Test
    fun `existing gateway with slash and new gateway with different id and without slash then containsGateway returns false`() {
        val savedGateways = mutableListOf<Radix.Gateway>()

        val mainnet = Radix.Gateway(
            url = "https://1.1.1.1/",
            network = Radix.Network(
                id = 2,
                name = "Mainnet",
                displayDescription = "Mainnet"
            )
        )
        savedGateways.add(mainnet)

        val updatedMainnet = Radix.Gateway(
            url = "https://1.1.1.1",
            network = Radix.Network(
                id = 1,
                name = "mainnet",
                displayDescription = "Mainnet"
            )
        )

        Assert.assertFalse(savedGateways.containsGateway(updatedMainnet))
    }
}