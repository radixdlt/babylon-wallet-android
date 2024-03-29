package rdx.works.profile

import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test
import rdx.works.profile.data.model.DeviceInfo
import rdx.works.profile.data.model.EncryptedProfileSnapshot
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.di.SerializerModule
import java.time.Instant
import kotlin.test.Ignore

class EncryptedProfileTest {

    @Test
    @Ignore("Waiting for Alex Cyon updated encryptedIOSSnapshot")
    fun crossPlatformCompatibility() {
        val encryptedIOSSnapshot = """
            {
              "version": 1,
              "encryptionScheme": {
                "version": 1,
                "description": "AESGCM-256"
              },
              "encryptedSnapshot": "9fc763da7fc5c1cf003f7e3c93b774ec5e64940fad644e99cd0358026b6bd3dea84fc0f52f537d9f40abe5c86c02f86d52bf44bf907a76e130695cf7da7f44133f4456e58edf78ab224b6ca668ec567808b611065d823a361db753c691c243642656e2f648657473660416d7fb0460c073152930aee0e49229c8ed6451be5c3c795ce3ee9216f1b069acb1e2b0396a147a0bc8aebcbe4e274ec49eaf420dad38c0487b4f1402d1aaa5c95fb89aecd4560f1d0864215583b8046e4fd1de8dc019a33f60165c362109bdb7da29718ed2740b9146c45966e2a56c0e06db068fe6e29ac2c29fa8ded623c215cadf66f994e8a661f260edcd8940d3a74b60a39b2b59a0109fb5210d5a2e97a4df7a60af52374584feba0ea897baac6a438555d9fc8cad9bbd20589ca8369341976ef789f6ee00488c898e3161459ec0adf9a1883062720dd087bf5a8bf0fbb9a66b0227748f883bebd4e53c25d366ab47ea3780f0677eee1bb2d728a196fe0bf8104f8cac652f0391662583259fcf416c8de559ec867dcc946ecf733dc77546e38c7475c7ce407d3f4ed13762f675193f8cf5c37a4cac36a747bdbaaa8ae6e652394471a21136d860067160f10e2890484f832931ffb0a9877fc3aa9b2f315f9e4ea8d0f25701a49492e3342fc4f02751f3d529996fab9817fe73af1f1b5e3cc01f8ab50faebdd6299f05c649e85c35cbe7976a17f49e2334f432b9af6f4a69b2e01af76975f65ec832cf9e5df2fb22be79ae48ba84f6de341ba8fe96d371cfbe277a0cc7bdce3ecd8757d3f8e2922602d6f30f5b557e7acbecbaf420ecbf323fefa948c00eef045bf70c75d54a3a3ca3c1826effe00a31ee1c76981a0091bd52bdb873958e48555c745b4186add71a8101af2b1aafa47c51f0233e302919ae43b90fc8495bf5000139cec2fce9cdfa10442d9740aac0a6ef51bc8bcdfa4bc955f4e18733635ed149d36d52a0b4bb97006246d61503e8c64c30a38d2831b913e19862fbc717aaa5e7a47fca12d66d4fee94ef08ee887ec9203f3e4862e04057d7fb912b6a16f5225e776413ee73fde645e5596a8f3b17cc68a6336578295bc524f7101a62c99160f89756b0e2927a3455c4f8e1e0f7f05f76375a694abebfaa6253d07325e2a0e82b77589cdc8bed6155cb2831510777223910c4fc263ad3227b8f00b780ad7172e37f67d078d8213e731886f8e2215368a7584ada2bfbd753516fd0633306986dc1d814a6e61c0666b757336ca97bb5602d4a1b1bbac950785ffdfedd7353389a45bcecf858e8f42bb85ca2cea52ebb9fc7ce7663ad1bac4d4961f0e005c702f0482c4722ae15a9004a793dc8df1c14aa55edfeb77b2a94abef99b14d4e9b19f09f2fd15a713315d9f704d2850f7184e0f7508abc95e730bf04e6fd19b1d03078819d8ab9784f12170f902a11ee990652bc02d1c58949aadb147423b6a5acd310ba72296d1c12b234efe670531deade3cc46fe36332a60a13880297b8394ddcd7725141c5c2e6df7fa5047679f3e40ad84625692e3e9f3359090f9fe0ab0fe409a776ef5ac0ec5f0e78d87b13047c98f1ec6788efcc387a5394df82ea09f9c63e363c6dc8c0de1c8912e6c223d420cd6a028f160b566b2e46ac4094b19e40358d8084d05935d3011ee5179e20ef2356c6b08ca7c73d82fd51043bc982ef8e290bb0090d145d3f8f843a7588998b1f3df11af641781a3f3ab0fac38f2985131a2c064c5c902d1aee30e5f047f5e0aff9e8878aa56b22359b3701debfa3390e5d2c3aa53a58af66790adf5119676eb8d18ec5c76760b6548bf91c964d554ac029bc564befa75620ffb8cce91593a2cd19df20b6873a609e8bc8cf57759c47184392b649833254d04ab9958d1f8f6c1365802cf2a109f934676c769e3d9e61bd5e12d7161a59808360ba5a76a7bdf871e91203318fffc16e79895032d23d94e5377dc1a8b17a7c243eacde352390dfc8a14db1d1a918f99e8194e9bdadade4a329161bdde47efb95cf3ada29a5b7c36f3d1e781281e18d130ff0a36b987723bc85d75ae1a4322b7d8726a7af00bcccc22a7adfe7f31d88fa62501c22c7a3dd4f6649855dc8e7d454f917965b3b83abb6713e1b0e9fdac8cacc54b99eed7edd3e13dbae49e353fe9ffae589c93e4410c78f215ec04a395be15cd2def11804828512ca5f7f0e9ec3af0d6f480d7fb3782c3be1425c69e49e51027642f74eaa80c34a87e7d2403b91ac45906bd2e1259043cbdbd69da4d7f0f4de922b5a33d24fc8df8ece6e636e1850dbcf71920c9b6d0654fd4f1352957ea96ee49a4c7c576f8f13e3068a43ae34a1251a291439bc79102cd6168b24ca800a77eaf41bbbd15631c91bce47bc1aa66adb4c90e529d7769994590f58cd54a78481f1fab1843ee5ff3b49d49e9d585845eab1a9b3307704a9d65a0dab97b5c312e0976362e49c06c6b775d5c1e543a9798381764370b6de968dc17db25917def3de447d3edd557f6c65a4692e2d0db378a204bf0d30c55ad69467aea9eccf9bcdf50c108e93253446bbcd8724b2bd58502ea6de7d4f305ac86b8eb360d77a315d19405b7bc1bc8b4414a2348ff5e20eb36230c9dffdac0321123fd5c5d9dd9c9f739ab1b73c37b66c03b179b6f9551de2f27307461bcfee6b3d508dae15b4bd22274dcccf7c588e422f009d885eab9ae9cc3eca1be3304904cc0b7ae7d83c2bff48b4ac55c7316e7fb06f5f56346bbb210df3ad215642479d99a351f0d7e8d7576ad668ad836f2359fa6197b936d895d075ba385543852f50bc8e1e5ab114200d7cf31ccc504406528d913e474ad902a991a2d3d879701b09aee5e5a674bf12807c2aba61cc64a396959d23eee14673f98babf04ad522a6e6a18f8b58cbe8eb8cf7532dfd095ebee93dc9047a89db5da30ffe33da7ff1a8fcd488053ecce3afa30e3e65c48ed268f48fb904c2e2b2c17b44f90b00a8d34b661de3d26ab933b8522b409dfdc1879bfff6dde972bc07558c5081bc19986e3d44f1cfaba2bf647bb99e829a5421a5803107656caaa64254955e00dbef661366f29893e9a62473905287eda9e05876b29c3ab458ca1dcf847018f4b4695a71cbc0793c6eece2dbc0e0b5d1f73cea9b",
              "keyDerivationScheme": {
                "version": 1,
                "description": "HKDFSHA256-with-UTF8-encoding-of-password-no-salt-no-info"
              }
            }
        """.trimIndent()

        val serializer = SerializerModule.provideProfileSerializer()
        val encryptedSnapshot = serializer.decodeFromString<EncryptedProfileSnapshot>(encryptedIOSSnapshot)
        val decryptedProfile = encryptedSnapshot.decrypt(serializer, "super secret").toProfile()

        assertEquals("computer unit test", decryptedProfile.header.creatingDevice.description)
    }

    @Test
    fun testRoundTrip() {
        val serializer = SerializerModule.provideProfileSerializer()
        val profile = Profile.init(
            id = "BABE1442-3C98-41FF-AFB0-D0F5829B020D",
            deviceInfo = DeviceInfo(
                name = "unit",
                manufacturer = "",
                model = "test"
            ),
            creationDate = Instant.ofEpochSecond(0L),
            gateways = Gateways(currentGatewayUrl = Radix.Gateway.rcnetV3.url, saved = listOf(Radix.Gateway.rcnetV3))
        )

        val encryptedSnapshot = EncryptedProfileSnapshot.from(serializer, profile.snapshot(), "super secret")
        val encryptedSnapshotSerialized = serializer.encodeToString(encryptedSnapshot)

        val decryptedProfile = serializer
            .decodeFromString<EncryptedProfileSnapshot>(encryptedSnapshotSerialized)
            .decrypt(serializer, "super secret")
            .toProfile()

        assertEquals(profile, decryptedProfile)
    }
}
