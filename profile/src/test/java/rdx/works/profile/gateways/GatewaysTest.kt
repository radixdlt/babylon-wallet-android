package rdx.works.profile.gateways

import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.Gateways
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Url
import com.radixdlt.sargon.extensions.all
import com.radixdlt.sargon.extensions.changeCurrent
import com.radixdlt.sargon.extensions.default
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.mainnet
import com.radixdlt.sargon.extensions.size
import com.radixdlt.sargon.extensions.string
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GatewaysTest {

    @Test
    fun `containsGateway extension with Radix mainnet gateway`() {
        var savedGateways = Gateways.default.copy(
            current = Gateway.mainnet.copy(
                url = Url("https://mainnet.radixdlt.com"), // Without slash at the end,
                network = Gateway.mainnet.network.copy(logicalName = "Mainnet") // With uppercase
            )
        )

        assertEquals("https://mainnet.radixdlt.com/", savedGateways.current.string)

        savedGateways = savedGateways.changeCurrent(newCurrent = Gateway.mainnet)

        // Even if new Gateway with different logical name (Lowercase) added, the list remains the same
        assertEquals(NetworkId.MAINNET, savedGateways.current.network.id)
        assertEquals(listOf(NetworkId.STOKENET), savedGateways.other().map { it.network.id })
    }

    @Test
    fun `existing custom gateway with slash and new gateway with same id but without path then containsGateway returns true`() {
        var savedGateways = Gateways.default.changeCurrent(
            newCurrent = Gateway.init("https://my.gateway.com/", NetworkId.NEBUNET)
        )

        assertEquals("https://my.gateway.com/", savedGateways.current.string)

        // Change to a new one without slash at the end
        savedGateways = savedGateways.changeCurrent(newCurrent =  Gateway.init("https://my.gateway.com", NetworkId.NEBUNET))
        assertEquals("https://my.gateway.com/", savedGateways.current.string)
        assertEquals(2, savedGateways.other.size)
    }

    @Test
    fun `existing gateway with path and new gateway with same id but without path then containsGateway returns false`() {
        var savedGateways = Gateways.default.changeCurrent(
            newCurrent = Gateway.init("https://my.gateway.com/path/morepath", NetworkId.NEBUNET)
        )

        assertEquals("https://my.gateway.com/path/morepath", savedGateways.current.string)

        // Change to a new one without slash at the end
        savedGateways = savedGateways.changeCurrent(newCurrent =  Gateway.init("https://my.gateway.com/", NetworkId.NEBUNET))
        assertEquals("https://my.gateway.com/", savedGateways.current.string)
        assertEquals(3, savedGateways.other.size)
    }

    @Test
    fun `existing gateway with path and new gateway with same id and with slash then containsGateway returns true`() {
        var savedGateways = Gateways.default.changeCurrent(
            newCurrent = Gateway.init("https://my.gateway.com/path/morepath", NetworkId.NEBUNET)
        )

        assertEquals("https://my.gateway.com/path/morepath", savedGateways.current.string)

        // Change to a new one without slash at the end
        savedGateways = savedGateways.changeCurrent(newCurrent =  Gateway.init("https://my.gateway.com/path/morepath/", NetworkId.NEBUNET))
        assertEquals("https://my.gateway.com/path/morepath/", savedGateways.current.string)
        assertEquals(3, savedGateways.other.size)
    }

    @Test
    fun `existing gateway with slash and new gateway with same id but without slash then containsGateway returns true`() {
        var savedGateways = Gateways.default.changeCurrent(
            newCurrent = Gateway.init("https://1.1.1.1/", NetworkId.NEBUNET)
        )

        assertEquals("https://1.1.1.1/", savedGateways.current.string)

        // Change to a new one without slash at the end
        savedGateways = savedGateways.changeCurrent(newCurrent =  Gateway.init("https://1.1.1.1", NetworkId.NEBUNET))
        assertEquals("https://1.1.1.1/", savedGateways.current.string)
        assertEquals(2, savedGateways.other.size)
    }

    @Ignore("TODO integration")
    @Test
    fun `existing gateway with slash and new gateway with different id and without slash then containsGateway returns false`() {
        var savedGateways = Gateways.default.changeCurrent(
            newCurrent = Gateway.init("https://1.1.1.1/", NetworkId.NEBUNET)
        )

        assertEquals("https://1.1.1.1/", savedGateways.current.string)
        println(savedGateways.all.map { it.string + " " + it.network.id.value.toString() })

        // Change to a new one without slash at the end
        savedGateways = savedGateways.changeCurrent(newCurrent =  Gateway.init("https://1.1.1.1/", NetworkId.HAMMUNET))
        assertEquals("https://1.1.1.1/", savedGateways.current.string)
        assertEquals(2, savedGateways.other.size)

        println(savedGateways.all.map { it.string + " " + it.network.id.value.toString() })

        assertFalse(savedGateways.all.any { it.network.id == NetworkId.HAMMUNET }) // TODO integration, this is a bug in Sargon
    }
}