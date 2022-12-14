package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.wallet.WalletData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

@Suppress("MagicNumber") // TODO this is temporarily here.
class MainViewRepositoryImpl(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MainViewRepository {

    override suspend fun getWallet(): WalletData {
        return withContext(ioDispatcher) {
            delay(Random.nextLong(500, 1500))
            WalletData(
                "$",
                Random.nextDouble(9999.999, 99999.999).toString()
            )
        }
    }
}
