package rdx.works.core.sargon.drivers

import com.radixdlt.sargon.ArculusCsdkDriver
import com.radixdlt.sargon.ArculusVerifyPinResponse
import com.radixdlt.sargon.ArculusWalletPointer
import com.radixdlt.sargon.BagOfBytes

@Suppress("TooManyFunctions")
class ArculusCSDKDriver : ArculusCsdkDriver {

    override fun walletInit(): ArculusWalletPointer? {
        TODO("Not yet implemented")
    }

    override fun walletFree(wallet: ArculusWalletPointer) {
        TODO("Not yet implemented")
    }

    override fun selectWalletRequest(
        wallet: ArculusWalletPointer,
        aid: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun selectWalletResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun createWalletSeedRequest(
        wallet: ArculusWalletPointer,
        wordCount: Long
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun createWalletSeedResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun seedPhraseFromMnemonicSentence(
        wallet: ArculusWalletPointer,
        mnemonicSentence: BagOfBytes,
        passphrase: BagOfBytes?
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun initRecoverWalletRequest(
        wallet: ArculusWalletPointer,
        wordCount: Long
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun initRecoverWalletResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        TODO("Not yet implemented")
    }

    override fun finishRecoverWalletRequest(
        wallet: ArculusWalletPointer,
        seed: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun finishRecoverWalletResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        TODO("Not yet implemented")
    }

    override fun resetWalletRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun resetWalletResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        TODO("Not yet implemented")
    }

    override fun getGguidRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun getGguidResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun getFirmwareVersionRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun getFirmwareVersionResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun storeDataPinRequest(
        wallet: ArculusWalletPointer,
        pin: String
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun storeDataPinResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        TODO("Not yet implemented")
    }

    override fun verifyPinRequest(
        wallet: ArculusWalletPointer,
        pin: String
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun verifyPinResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): ArculusVerifyPinResponse {
        TODO("Not yet implemented")
    }

    override fun initEncryptedSessionRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun initEncryptedSessionResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        TODO("Not yet implemented")
    }

    override fun getPublicKeyByPathRequest(
        wallet: ArculusWalletPointer,
        path: BagOfBytes,
        curve: UShort
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun getPublicKeyByPathResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun signHashPathRequest(
        wallet: ArculusWalletPointer,
        path: BagOfBytes,
        curve: UShort,
        algorithm: UByte,
        hash: BagOfBytes
    ): List<BagOfBytes>? {
        TODO("Not yet implemented")
    }

    override fun signHashPathResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }
}
