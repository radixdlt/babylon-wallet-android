package rdx.works.profile

import kotlin.test.assertEquals
import org.junit.Test
import rdx.works.profile.data.model.DeviceInfo

class DeviceInfoTest {

    @Test
    fun `given device info with no name, when display name is invoked, then only the manufacturer with the model are present`() {
        val deviceInfo = DeviceInfo(
            name = "",
            manufacturer = "samsung",
            model = "SM-A536B"
        )

        assertEquals("Samsung SM-A536B", deviceInfo.displayName)
    }

    @Test
    fun `given device info with name, when display name is invoked, then only the whole name is present`() {
        val deviceInfo = DeviceInfo(
            name = "Galaxy A53 5G",
            manufacturer = "samsung",
            model = "SM-A536B"
        )

        assertEquals("Galaxy A53 5G (Samsung SM-A536B)", deviceInfo.displayName)
    }

}
