package com.babylon.wallet.android.utils

import org.junit.Test
import org.junit.Assert

class SanitizeAndValidateGatewayUrlTest {

    @Test
    fun `given dev mode is disabled when gateway url is IPv4 then do not accept it`() {
        val inputIP1 = "198.161.9.9"
        val expectedIP1 = "/" // not accepted
        val actualIP1 = inputIP1.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP1, actualIP1)

        val inputIP2 = "198.161.9.9/"
        val expectedIP2 = "/" // not accepted
        val actualIP2 = inputIP2.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP2, actualIP2)

        val inputIP3 = "http://198.161.9.9"
        val expectedIP3 = "/" // not accepted
        val actualIP3 = inputIP3.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP3, actualIP3)

        val inputIP4 = "https://198.161.9.9"
        val expectedIP4 = "/" // not accepted
        val actualIP4 = inputIP4.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP4, actualIP4)

        val inputIP5 = "https://198.161.9.9/"
        val expectedIP5 = "/" // not accepted
        val actualIP5 = inputIP5.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP5, actualIP5)

        val inputIP6 = "https://198.161.9.9:456/test"
        val expectedIP6 = "/" // not accepted
        val actualIP6 = inputIP6.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP6, actualIP6)

        val inputIP7 = "https://198.161.9.9:456/test/test"
        val expectedIP7 = "/" // not accepted
        val actualIP7 = inputIP7.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP7, actualIP7)
    }

    @Test
    fun `given dev mode is disabled when gateway url is IPv6 then do not accept it`() {
        val inputIP1 = "2001:db8:3333:4444:5555:6666:7777:8888"
        val expectedIP1 = "/" // not accepted
        val actualIP1 = inputIP1.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP1, actualIP1)

        val inputIP2 = "[2001:db8:3333:4444:5555:6666:7777:8888]"
        val expectedIP2 = "/" // not accepted
        val actualIP2 = inputIP2.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP2, actualIP2)

        val inputIP3 = "http://2001:db8:3333:4444:5555:6666:7777:8888"
        val expectedIP3 = "/" // not accepted
        val actualIP3 = inputIP3.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP3, actualIP3)

        val inputIP4 = "https://2001:db8:3333:4444:5555:6666:7777:8888"
        val expectedIP4 = "/" // not accepted
        val actualIP4 = inputIP4.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP4, actualIP4)

        val inputIP5 = "https://[2001:db8:3333:4444:5555:6666:7777:8888]/"
        val expectedIP5 = "/" // not accepted
        val actualIP5 = inputIP5.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP5, actualIP5)

        val inputIP6 = "https://[2001:db8:3333:4444:5555:6666:7777:8888]:456/test"
        val expectedIP6 = "/" // not accepted
        val actualIP6 = inputIP6.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedIP6, actualIP6)
    }

    @Test
    fun `given dev mode is disabled when gateway url is a domain url with port then do not accept it`() {
        val inputDomain1 = "www.network.com:456"
        val expectedDomain1 = "/" // not accepted
        val actualDomain1 = inputDomain1.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain1, actualDomain1)

        val inputDomain2 = "https://www.network.com:456"
        val expectedDomain2 = "/" // not accepted
        val actualDomain2 = inputDomain2.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain2, actualDomain2)

        val inputDomain3 = "http://2847478384-network.radixdlt.com:456/test/test"
        val expectedDomain3 = "/" // not accepted
        val actualDomain3 = inputDomain3.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain3, actualDomain3)
    }

    @Test
    fun `given dev mode is disabled when gateway url is a domain url without http or https then convert it to https and accept it`(){
        val inputDomain1 = "www.network.com"
        val expectedDomain1 = "https://www.network.com/"
        val actualDomain1 = inputDomain1.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain1, actualDomain1)

        val inputDomain2 = "www.network.com/"
        val expectedDomain2 = "https://www.network.com/"
        val actualDomain2 = inputDomain2.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain2, actualDomain2)

        val inputDomain3 = "www.network.com/test/"
        val expectedDomain3 = "https://www.network.com/"
        val actualDomain3 = inputDomain3.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain3, actualDomain3)

        val inputDomain4 = "www.network.com/test/"
        val expectedDomain4 = "https://www.network.com/"
        val actualDomain4 = inputDomain4.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain4, actualDomain4)
    }

    @Test
    fun `given dev mode is disabled when gateway url is a domain url with http then convert it to https and accept it`() {
        val inputDomain1 = "http://www.network.com"
        val expectedDomain1 = "https://www.network.com/"
        val actualDomain1 = inputDomain1.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain1, actualDomain1)

        val inputDomain2 = "http://www.network.com/"
        val expectedDomain2 = "https://www.network.com/"
        val actualDomain2 = inputDomain2.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain2, actualDomain2)

        val inputDomain3 = "http://www.network.com/test"
        val expectedDomain3 = "https://www.network.com/"
        val actualDomain3 = inputDomain3.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain3, actualDomain3)
    }

    @Test
    fun `given dev mode is disabled when gateway url is a domain url with https then accept it`() {
        val inputDomain1 = "https://www.network.com"
        val expectedDomain1 = "https://www.network.com/"
        val actualDomain1 = inputDomain1.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain1, actualDomain1)

        val inputDomain2 = "https://2847478384-network.radixdlt.com"
        val expectedDomain2 = "https://2847478384-network.radixdlt.com/"
        val actualDomain2 = inputDomain2.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain2, actualDomain2)

        val inputDomain3 = "https://www.network.com/test"
        val expectedDomain3 = "https://www.network.com/"
        val actualDomain3 = inputDomain3.sanitizeAndValidateGatewayUrl()
        Assert.assertEquals(expectedDomain3, actualDomain3)
    }

    @Test
    fun `given dev mode is enabled when gateway url is IPv4 without http or https then append http and accept it`() {
        val inputIP1 = "198.161.9.9"
        val expectedIP1 = "http://198.161.9.9/"
        val actualIP1 = inputIP1.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIP1, actualIP1)

        val inputIP2 = "198.161.9.9/"
        val expectedIP2 = "http://198.161.9.9/"
        val actualIP2  = inputIP2.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIP2, actualIP2)

        val inputIP3 = "198.161.9.9:456"
        val expectedIP3 = "http://198.161.9.9:456/"
        val actualIP3 = inputIP3.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIP3, actualIP3)

        val inputIP4 = "198.161.9.9:456/"
        val expectedIP4 = "http://198.161.9.9:456/"
        val actualIP4 = inputIP4.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIP4, actualIP4)
    }

    @Test
    fun `given dev mode is enabled when gateway url is IPv4 then accept it`() {
        val inputIP1 = "http://198.161.9.9"
        val expectedIP1 = "http://198.161.9.9/"
        val actualIP1 = inputIP1.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIP1, actualIP1)

        val inputIP2 = "https://198.161.9.9"
        val expectedIP2 = "https://198.161.9.9/"
        val actualIP2 = inputIP2.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIP2, actualIP2)

        val inputIP3 = "https://198.161.9.9/"
        val expectedIP3 = "https://198.161.9.9/"
        val actualIP3 = inputIP3.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIP3, actualIP3)

        val inputIP4 = "http://198.161.9.9:456"
        val expectedIP4 = "http://198.161.9.9:456/"
        val actualIP4 = inputIP4.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIP4, actualIP4)

        val inputIP5 = "https://198.161.9.9:456"
        val expectedIP5 = "https://198.161.9.9:456/"
        val actualIP5 = inputIP5.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIP5, actualIP5)

        val inputIP6 = "http://198.161.9.9:456/"
        val expectedIP6 = "http://198.161.9.9:456/"
        val actualIP6 = inputIP6.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIP6, actualIP6)

        val inputIP7 = "https://198.161.9.9:456/test"
        val expectedIP7 = "https://198.161.9.9:456/test/"
        val actualIP7 = inputIP7.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIP7, actualIP7)
    }

    @Test
    fun `given dev mode is enabled when gateway url is IPv6 without http or https then append http and accept it`() {
        val inputIPv6_1 = "2001:db8:3333:4444:5555:6666:7777:8888"
        val expectedIPv6_1 = "http://[2001:db8:3333:4444:5555:6666:7777:8888]/"
        val actualIPv6_1 = inputIPv6_1.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIPv6_1, actualIPv6_1)

        val inputIPv6_2 = "[2001:db8:3333:4444:5555:6666:7777:8888]"
        val expectedIPv6_2 = "http://[2001:db8:3333:4444:5555:6666:7777:8888]/"
        val actualIPv6_2 = inputIPv6_2.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIPv6_2, actualIPv6_2)

        val inputIPv6_3 = "2001:db8:3333:4444:5555:6666:7777:8888/"
        val expectedIPv6_3 = "http://[2001:db8:3333:4444:5555:6666:7777:8888]/"
        val actualIPv6_3 = inputIPv6_3.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIPv6_3, actualIPv6_3)

        val inputIPv6_4 = "[1fff:0:a88:85a3::ac1f]:8001/"
        val expectedIPv6_4 = "http://[1fff:0:a88:85a3::ac1f]:8001/"
        val actualIPv6_4 = inputIPv6_4.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIPv6_4, actualIPv6_4)

        val inputIPv6_5 = "[1fff:0:a88:85a3::ac1f]:8001/test"
        val expectedIPv6_5 = "http://[1fff:0:a88:85a3::ac1f]:8001/test/"
        val actualIPv6_5 = inputIPv6_5.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIPv6_5, actualIPv6_5)
    }

    @Test
    fun `given dev mode is enabled when gateway url is IPv6 then accept it`() {
        val inputIPv6_1 = "http://2001:db8:1111:2222:3333:4444:5555:6666"
        val expectedIPv6_1 = "http://[2001:db8:1111:2222:3333:4444:5555:6666]/"
        val actualIPv6_1 = inputIPv6_1.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIPv6_1, actualIPv6_1)

        val inputIPv6_2 = "https://2001:db8:8888:7777:6666:5555:4444:3333"
        val expectedIPv6_2 = "https://[2001:db8:8888:7777:6666:5555:4444:3333]/"
        val actualIPv6_2 = inputIPv6_2.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIPv6_2, actualIPv6_2)

        val inputIPv6_3 = "http://[1fff:0:a88:85a3::ac1f]:8001/"
        val expectedIPv6_3 = "http://[1fff:0:a88:85a3::ac1f]:8001/"
        val actualIPv6_3 = inputIPv6_3.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIPv6_3, actualIPv6_3)

        val inputIPv6_4 = "https://[2001:db8:3333:4444:5555:6666:7777:8888]/"
        val expectedIPv6_4 = "https://[2001:db8:3333:4444:5555:6666:7777:8888]/"
        val actualIPv6_4 = inputIPv6_4.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIPv6_4, actualIPv6_4)

        val inputIPv6_5 = "https://[2001:db8:3333:4444:5555:6666:7777:8888]:453"
        val expectedIPv6_5 = "https://[2001:db8:3333:4444:5555:6666:7777:8888]:453/"
        val actualIPv6_5 = inputIPv6_5.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIPv6_5, actualIPv6_5)

        val inputIPv6_6 = "https://[2001:db8:3333:4444:5555:6666:7777:8888]:453/test"
        val expectedIPv6_6 = "https://[2001:db8:3333:4444:5555:6666:7777:8888]:453/test/"
        val actualIPv6_6 = inputIPv6_6.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedIPv6_6, actualIPv6_6)
    }

    @Test
    fun `given dev mode is enabled when gateway url is a domain url without http or https then append http and accept it`() {
        val inputDomain1 = "www.network.com"
        val expectedDomain1 = "http://www.network.com/"
        val actualIDomain1 = inputDomain1.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedDomain1, actualIDomain1)

        val inputDomain2 = "www.network.com/"
        val expectedDomain2 = "http://www.network.com/"
        val actualIDomain2 = inputDomain2.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedDomain2, actualIDomain2)

        val inputDomain3 = "www.network.com:456"
        val expectedDomain3 = "http://www.network.com:456/"
        val actualIDomain3 = inputDomain3.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedDomain3, actualIDomain3)

        val inputDomain4 = "www.network.com:456/test"
        val expectedDomain4 = "http://www.network.com:456/test/"
        val actualIDomain4 = inputDomain4.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedDomain4, actualIDomain4)
    }

    @Test
    fun `given dev mode is enabled when gateway url is a domain url then accept it`() {
        val inputDomain1 = "http://www.network.com"
        val expectedDomain1 = "http://www.network.com/"
        val actualIDomain1 = inputDomain1.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedDomain1, actualIDomain1)

        val inputDomain2 = "https://www.network.com"
        val expectedDomain2 = "https://www.network.com/"
        val actualIDomain2 = inputDomain2.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedDomain2, actualIDomain2)

        val inputDomain3 = "https://www.network.com:456"
        val expectedDomain3 = "https://www.network.com:456/"
        val actualIDomain3 = inputDomain3.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedDomain3, actualIDomain3)

        val inputDomain4 = "https://2847478384-network.radixdlt.com"
        val expectedDomain4 = "https://2847478384-network.radixdlt.com/"
        val actualIDomain4 = inputDomain4.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedDomain4, actualIDomain4)

        val inputDomain5 = "http://2847478384-network.radixdlt.com/test"
        val expectedDomain5 = "http://2847478384-network.radixdlt.com/test/"
        val actualIDomain5 = inputDomain5.sanitizeAndValidateGatewayUrl(isDevModeEnabled = true)
        Assert.assertEquals(expectedDomain5, actualIDomain5)
    }
}