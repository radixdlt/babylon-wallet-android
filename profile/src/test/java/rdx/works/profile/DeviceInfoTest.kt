package rdx.works.profile

import org.junit.Test
import rdx.works.core.domain.DeviceInfo
import java.util.UUID
import kotlin.test.assertEquals

class DeviceInfoTest {

    @Test
    fun `given device info with no name, when display name is invoked, then only the manufacturer with the model are present`() {
        val deviceInfo = DeviceInfo(
            id = UUID.randomUUID(),
            name = "",
            manufacturer = "Samsung",
            model = "SM-A536B"
        )

        assertEquals("Samsung SM-A536B", deviceInfo.displayName)
    }

}
