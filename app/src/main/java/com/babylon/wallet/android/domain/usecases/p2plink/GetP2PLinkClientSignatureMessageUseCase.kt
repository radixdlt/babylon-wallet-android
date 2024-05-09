package com.babylon.wallet.android.domain.usecases.p2plink

import com.radixdlt.sargon.Hash
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.bytes
import rdx.works.core.hash
import rdx.works.core.toByteArray
import javax.inject.Inject

class GetP2PLinkClientSignatureMessageUseCase @Inject constructor() {

    operator fun invoke(password: RadixConnectPassword): Hash {
        val message = "L".encodeToByteArray() + password.value.bytes.toByteArray()
        return message.hash()
    }
}