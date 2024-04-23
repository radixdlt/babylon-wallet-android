package rdx.works.profile.factorSource

//class FactorSourceEncodingTests {
//
//    @Test
//    fun `test factor sources round-trip`() {
//        val json = SerializerModule.provideProfileSerializer()
//        val vector = json.encodeToString(factorSources)
//
//        val factorSources = json.decodeFromString<List<FactorSource>>(vector)
//
//        assertEquals(5, factorSources.size)
//
//        assertEquals(FactorSourceKind.DEVICE, factorSources[0].id.kind)
//        assertIs<DeviceFactorSource>(factorSources[0])
//
//        assertEquals(FactorSourceKind.DEVICE, factorSources[1].id.kind)
//        assertIs<DeviceFactorSource>(factorSources[1])
//
//        assertEquals(FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET, factorSources[2].id.kind)
//        assertIs<LedgerHardwareWalletFactorSource>(factorSources[2])
//
//        assertEquals(FactorSourceKind.OFF_DEVICE_MNEMONIC, factorSources[3].id.kind)
//        assertIs<OffDeviceMnemonicFactorSource>(factorSources[3])
//
//        assertEquals(FactorSourceKind.TRUSTED_CONTACT, factorSources[4].id.kind)
//        assertIs<TrustedContactFactorSource>(factorSources[4])
//    }
//
//    companion object {
//        private val mnemonic = MnemonicWithPassphrase(
//            mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote",
//            bip39Passphrase = "Radix"
//        )
//        private val date = Instant.ofEpochSecond(1_690_801_871)
//        private val factorSources = listOf(
//            DeviceFactorSource.babylon(
//                mnemonicWithPassphrase = mnemonic,
//                model = "A22",
//                name = "Galaxy",
//                createdAt = date
//            ),
//            DeviceFactorSource.olympia(
//                mnemonicWithPassphrase = mnemonic,
//                model = "A18",
//                name = "Galaxy",
//                createdAt = date
//            ),
//            LedgerHardwareWalletFactorSource.newSource(
//                model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
//                name = "Orange",
//                deviceID = HexCoded32Bytes("deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"),
//                createdAt = date
//            ),
//            OffDeviceMnemonicFactorSource.newSource(
//                mnemonicWithPassphrase = mnemonic,
//                label = "Test",
//                createdAt = date
//            ),
//            TrustedContactFactorSource.newSource(
//                accountAddress = FactorSource.AccountAddress("account_rdx1283u6e8r2jnz4a3jwv0hnrqfr5aq50yc9ts523sd96hzfjxqqcs89q"),
//                emailAddress = "hi@rdx.works",
//                name = "My Friend"
//            )
//        )
//    }
//}
