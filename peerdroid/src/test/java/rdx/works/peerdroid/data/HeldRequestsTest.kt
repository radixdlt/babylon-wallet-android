package rdx.works.peerdroid.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import rdx.works.peerdroid.domain.ConnectionIdHolder

class HeldRequestsTest {

    private val link = ConnectionIdHolder(id = "link-1")
    private val otherLink = ConnectionIdHolder(id = "link-2")

    private fun request(id: String) = """{"interactionId":"$id","items":{"discriminator":"transaction"},"metadata":{}}"""
    private fun cancel(id: String) = """{"interactionId":"$id","items":{"discriminator":"cancelRequest"},"metadata":{}}"""
    private fun answer(id: String) = """{"discriminator":"success","interactionId":"$id","items":{}}"""

    private fun HeldRequests.listed(link: ConnectionIdHolder): List<String> {
        val message = Json.parseToJsonElement(inventoryMessage(link)).jsonObject
        assertEquals("walletRequests", message.getValue("discriminator").jsonPrimitive.content)
        return message.getValue("interactionIds").jsonArray.map { it.jsonPrimitive.content }
    }

    @Test
    fun `holds the requests the app took until it answers them or the dApp cancels them`() {
        val held = HeldRequests()
        held.took(link, request("a"))
        held.took(link, request("b"))
        held.took(link, request("c"))
        held.took(link, request("a"))
        assertEquals(listOf("a", "b", "c"), held.listed(link))

        held.sent(link, answer("a"))
        held.took(link, cancel("b"))

        assertEquals(listOf("c"), held.listed(link))
    }

    @Test
    fun `each link lists only its own requests, and messages that are not requests change nothing`() {
        val held = HeldRequests()
        held.took(link, request("a"))
        held.took(otherLink, request("b"))
        held.took(link, """{"discriminator":"accountList","accounts":[]}""")
        held.sent(link, """{"discriminator":"walletPresence","inBackground":true}""")
        held.took(link, "not json")

        assertEquals(listOf("a"), held.listed(link))
        assertEquals(listOf("b"), held.listed(otherLink))
    }

    @Test
    fun `a link the app took nothing from lists nothing`() {
        assertEquals(emptyList<String>(), HeldRequests().listed(link))
    }
}
