package rdx.works.core.sargon.drivers

import co.arculus.csdknative.CSDKLibraryDirect
import co.arculus.csdknative.CSDKLibraryDirect.CSDK_OK
import co.arculus.csdknative.SizeTByReference
import com.google.common.primitives.UnsignedInts
import com.radixdlt.sargon.ArculusVerifyPinResponse
import com.radixdlt.sargon.ArculusWalletPointer
import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.extensions.toBagOfBytes
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import rdx.works.core.toByteArray
import com.radixdlt.sargon.ArculusCsdkDriver as SargonArculusCSDKDriver

@Suppress("TooManyFunctions")
class ArculusCSDKDriver : SargonArculusCSDKDriver {
    override fun createWalletSeedRequest(
        wallet: ArculusWalletPointer,
        wordCount: Long
    ): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletCreateWalletRequest(
                wallet.asJnaPointer(),
                it,
                wordCount.toInt()
            )
        }
    }

    override fun createWalletSeedResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletCreateWalletResponse(
                wallet.asJnaPointer(),
                response.toByteArray(),
                response.size,
                it
            )
        }
    }

    override fun finishRecoverWalletRequest(
        wallet: ArculusWalletPointer,
        seed: BagOfBytes
    ): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletSeedFinishRecoverWalletRequest(
                wallet.asJnaPointer(),
                seed.toByteArray(),
                seed.size,
                it
            )
        }
    }

    override fun finishRecoverWalletResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        return CSDKLibraryDirect.WalletFinishRecoverWalletResponse(
            wallet.asJnaPointer(),
            response.toByteArray(),
            response.size
        )
    }

    override fun getFirmwareVersionRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletGetFirmwareVersionRequest(
                wallet.asJnaPointer(),
                it
            )
        }
    }

    override fun getFirmwareVersionResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletGetFirmwareVersionResponse(
                wallet.asJnaPointer(),
                response.toByteArray(),
                response.size,
                it
            )
        }
    }

    override fun getGguidRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletGetGGUIDRequest(
                wallet.asJnaPointer(),
                it
            )
        }
    }

    override fun getGguidResponse(wallet: ArculusWalletPointer, response: BagOfBytes): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletGetGGUIDResponse(
                wallet.asJnaPointer(),
                response.toByteArray(),
                response.size,
                it
            )
        }
    }

    override fun getPublicKeyByPathRequest(
        wallet: ArculusWalletPointer,
        path: BagOfBytes,
        curve: UShort
    ): BagOfBytes? {
        val rLen = SizeTByReference()
        val keyPointer: Pointer = CSDKLibraryDirect.WalletGetPublicKeyFromPathRequest(
            wallet.asJnaPointer(),
            path.toByteArray(),
            path.size,
            curve.toShort(),
            rLen
        ) ?: return null

        val rPub = CSDKLibraryDirect.ExtendedKey_getPubKey(keyPointer, rLen)
        val bytes = rPub.getByteArray(0, UnsignedInts.checkedCast(rLen.value.toLong()))
        return bytes.toBagOfBytes()
    }

    override fun getPublicKeyByPathResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletGetPublicKeyFromPathResponse(
                wallet.asJnaPointer(),
                response.toByteArray(),
                response.size
            )
        }
    }

    override fun initEncryptedSessionRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletInitSessionRequest(
                wallet.asJnaPointer(),
                it
            )
        }
    }

    override fun initEncryptedSessionResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        val ptr: Pointer = CSDKLibraryDirect.WalletInitSessionResponse(
            wallet.asJnaPointer(),
            response.toByteArray(),
            response.size
        ) ?: return -100

        return 0
    }

    override fun initRecoverWalletRequest(
        wallet: ArculusWalletPointer,
        wordCount: Long
    ): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletInitRecoverWalletRequest(
                wallet.asJnaPointer(),
                wordCount.toInt(),
                it
            )
        }
    }

    override fun initRecoverWalletResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        return CSDKLibraryDirect.WalletInitRecoverWalletResponse(
            wallet.asJnaPointer(),
            response.toByteArray(),
            response.size
        )
    }

    override fun resetWalletRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletResetWalletRequest(
                wallet.asJnaPointer(),
                it
            )
        }
    }

    override fun resetWalletResponse(wallet: ArculusWalletPointer, response: BagOfBytes): Int {
        return CSDKLibraryDirect.WalletResetWalletResponse(
            wallet.asJnaPointer(),
            response.toByteArray(),
            response.size
        )
    }

    override fun seedPhraseFromMnemonicSentence(
        wallet: ArculusWalletPointer,
        mnemonicSentence: BagOfBytes,
        passphrase: BagOfBytes?
    ): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletSeedFromMnemonicSentence(
                wallet.asJnaPointer(),
                mnemonicSentence.toByteArray(),
                mnemonicSentence.size,
                passphrase?.toByteArray(),
                passphrase?.size ?: 0,
                it
            )
        }
    }

    override fun selectWalletRequest(wallet: ArculusWalletPointer, aid: BagOfBytes): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletSelectWalletRequest(
                wallet.asJnaPointer(),
                aid.toByteArray(),
                it
            )
        }
    }

    override fun selectWalletResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        val ptr: Pointer = CSDKLibraryDirect.WalletSelectWalletResponse(
            wallet.asJnaPointer(),
            response.toByteArray(),
            response.size
        ) ?: return null

        return withParsingByteResult {
            CSDKLibraryDirect.WalletGetApplicationAID(ptr, it)
        }
    }

    override fun signHashPathRequest(
        wallet: ArculusWalletPointer,
        path: BagOfBytes,
        curve: UShort,
        algorithm: UByte,
        hash: BagOfBytes
    ): List<BagOfBytes>? {
        val rLen = SizeTByReference()

        val bPath = path.toByteArray()
        val bHash = hash.toByteArray()
        val bPathPtr: Pointer = CSDKLibraryDirect.CreateByteVector(bPath, bPath.size)
        val hashPtr: Pointer = CSDKLibraryDirect.CreateByteVector(bHash, bHash.size)
        val apduChainPtr: PointerByReference? = PointerByReference()

        val status: Int = CSDKLibraryDirect.WalletSignRequest(
            wallet.asJnaPointer(),
            bPathPtr,
            curve.toShort(),
            algorithm.toByte(),
            hashPtr,
            apduChainPtr
        )

        if (status != CSDK_OK) {
            return null
        }

        val apduChain = CSDKLibraryDirect.GetAPDUSequenceFromResult(apduChainPtr)
        val apduChainBytes = apduChain.GetChain()

        return apduChainBytes.map { it.toBagOfBytes() }
    }

    override fun signHashPathResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletSignHashResponse(
                wallet.asJnaPointer(),
                response.toByteArray(),
                response.size,
                it
            )
        }
    }

    override fun storeDataPinRequest(wallet: ArculusWalletPointer, pin: String): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletStoreDataPINRequest(
                wallet.asJnaPointer(),
                pin.toByteArray(),
                pin.length,
                it
            )
        }
    }

    override fun storeDataPinResponse(wallet: ArculusWalletPointer, response: BagOfBytes): Int {
        return CSDKLibraryDirect.WalletStoreDataPINResponse(
            wallet.asJnaPointer(),
            response.toByteArray(),
            response.size
        )
    }

    override fun verifyPinRequest(wallet: ArculusWalletPointer, pin: String): BagOfBytes? {
        return withParsingByteResult {
            CSDKLibraryDirect.WalletVerifyPINRequest(
                wallet.asJnaPointer(),
                pin.toByteArray(),
                pin.length,
                it
            )
        }
    }

    override fun verifyPinResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): ArculusVerifyPinResponse {
        val nbrOfTries = SizeTByReference()
        val status = CSDKLibraryDirect.WalletVerifyPINResponse(
            wallet.asJnaPointer(),
            response.toByteArray(),
            response.size,
            nbrOfTries
        )

        return ArculusVerifyPinResponse(status, nbrOfTries.value.toByte())
    }

    override fun walletFree(wallet: ArculusWalletPointer) {
        CSDKLibraryDirect.WalletFree(wallet.asJnaPointer())
    }

    override fun walletInit(): ArculusWalletPointer? {
        val pointer: Pointer = CSDKLibraryDirect.WalletInit() ?: return null
        return pointer.asArculusWalletPointer()
    }

    private fun withParsingByteResult(op: (SizeTByReference) -> Pointer?): BagOfBytes? {
        val rLen = SizeTByReference()
        val ptr: Pointer = op(rLen) ?: return null
        val bytes = ptr.getByteArray(0, UnsignedInts.checkedCast(rLen.value.toLong()))
        return bytes.toBagOfBytes()
    }
}

private fun Pointer.asArculusWalletPointer(): ArculusWalletPointer {
    val address = Pointer.nativeValue(this)
    return ArculusWalletPointer(address.toULong())
}

private fun ArculusWalletPointer.asJnaPointer(): Pointer {
    return Pointer(this.pointer.toLong())
}
