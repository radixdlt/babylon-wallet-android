package com.babylon.wallet.android.domain.transaction

import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.presentation.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import rdx.works.profile.data.repository.ProfileRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class TransactionClientTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val transactionRepository = Mockito.mock(TransactionRepository::class.java)

    private val profileRepository = Mockito.mock(ProfileRepository::class.java)
    private val preferencesManager = Mockito.mock(PreferencesManager::class.java)

    private lateinit var client: TransactionClient

    @Before
    fun setUp() = runBlocking {
        client = TransactionClient(transactionRepository, profileRepository, preferencesManager, StandardTestDispatcher())
    }

    @Test
    fun test1() {

    }

}