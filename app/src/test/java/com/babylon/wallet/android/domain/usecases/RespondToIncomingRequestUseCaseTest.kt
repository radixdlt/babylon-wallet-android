package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.radixdlt.sargon.IntentSignatures
import com.radixdlt.sargon.SignedSubintent
import com.radixdlt.sargon.Subintent
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, UsesSampleValues::class)
class RespondToIncomingRequestUseCaseTest {

    private val dappMessenger = mockk<DappMessenger> {
        coEvery { sendTransactionWriteResponseSuccess(any(), any(), any()) } returns Result.success(Unit)
    }
    private val request = mockk<DappToWalletInteraction> {
        every { remoteEntityId } returns RemoteEntityID.ConnectorId("connector")
        every { interactionId } returns "1"
    }

    @Test
    fun `the answer to a submitted transaction may be sent again, a signed subintent may not`() = runTest {
        val sut = RespondToIncomingRequestUseCase(mockk(relaxed = true), dappMessenger, UnconfinedTestDispatcher(testScheduler))

        sut.respondWithSuccessTransactionIntent(
            request = request,
            intentHash = TransactionIntentHash.init("txid_rdx1frcm6zzyfd08z0deu9x24sh64eccxeux4j2dv3dsqeuh9qsz4y6szm3ltd")
        )
        coVerify { dappMessenger.sendTransactionWriteResponseSuccess("connector", any(), resendIfNotSent = true) }

        sut.respondWithSuccessSubintent(request = request, signedSubintent = SignedSubintent(Subintent.sample(), IntentSignatures(emptyList())))
        coVerify { dappMessenger.sendTransactionWriteResponseSuccess("connector", any(), resendIfNotSent = false) }
    }
}
