package rdx.works.peerdroid.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WalletPresenceMessageTest {

    @Test
    fun `the presence message says whether the app is in the background, and names no interaction`() {
        listOf(true, false).forEach { inBackground ->
            val message = Json.parseToJsonElement(walletPresenceMessage(inBackground)).jsonObject

            assertEquals("walletPresence", message.getValue("discriminator").jsonPrimitive.content)
            assertEquals(inBackground, message.getValue("inBackground").jsonPrimitive.boolean)
            // A connector that does not know it never takes it for an answer.
            assertFalse(message.containsKey("interactionId"))
        }
    }
}
