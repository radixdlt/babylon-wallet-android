package rdx.works.profile.snapshot

//class SnapshotHeaderTests {
//
//    @Test
//    fun `test version compatibility too low`() {
//        val version = ProfileSnapshot.MINIMUM - 1
//        val header = Header(
//            creatingDevice = device,
//            lastUsedOnDevice = device,
//            id = id,
//            lastModified = date,
//            snapshotVersion = version,
//            contentHint = Header.ContentHint(
//                numberOfNetworks = 1,
//                numberOfAccountsOnAllNetworksInTotal = 1,
//                numberOfPersonasOnAllNetworksInTotal = 0
//            )
//        )
//
//        assertFalse(header.isCompatible)
//    }
//
//    @Test
//    fun `test version compatibility ok`() {
//        val header = Header(
//            creatingDevice = device,
//            lastUsedOnDevice = device,
//            id = id,
//            lastModified = date,
//            snapshotVersion = ProfileSnapshot.MINIMUM,
//            contentHint = Header.ContentHint(
//                numberOfNetworks = 1,
//                numberOfAccountsOnAllNetworksInTotal = 1,
//                numberOfPersonasOnAllNetworksInTotal = 0
//            )
//        )
//
//        assertTrue(header.isCompatible)
//    }
//
//    companion object {
//        private val deviceInfo = DeviceInfo(name = "unit test", manufacturer = "computer", model = "computer")
//        private val date = Instant.ofEpochSecond(0)
//        private const val id = "BABE1442-3C98-41FF-AFB0-D0F5829B020D"
//        private val device = Header.Device(
//            description = deviceInfo.displayName,
//            id = id,
//            date = date
//        )
//    }
//}
