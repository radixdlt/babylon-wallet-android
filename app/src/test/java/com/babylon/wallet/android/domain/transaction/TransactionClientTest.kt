package com.babylon.wallet.android.domain.transaction

import builders.ManifestBuilder
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.presentation.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import models.crypto.PublicKey
import models.transaction.TransactionHeader
import models.transaction.TransactionIntent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import rdx.works.profile.data.repository.ProfileRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class TransactionClientTest {

    val testT = TransactionIntent(
        TransactionHeader(
            1.toUByte(),
            34.toUByte(),
            10u,
            90u,
            100u,
            PublicKey.EcdsaSecp256k1.fromString("02ed3bace23c5e17652e174c835fb72bf53ee306b3406a26890221b4cef7500f88"),
            false,
            100_000_000u,
            0u
        ),
        ManifestBuilder().dropAllProofs().build()
    )

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val transactionRepository = Mockito.mock(TransactionRepository::class.java)

    private val profileRepository = Mockito.mock(ProfileRepository::class.java)

    private lateinit var client: TransactionClient

    @Before
    fun setUp() = runBlocking {
        client = TransactionClient(transactionRepository, profileRepository)
    }

    @Test
    fun test1() {

    }

}