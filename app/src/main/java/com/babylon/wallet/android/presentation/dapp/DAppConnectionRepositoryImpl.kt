package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.di.coroutines.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

@Suppress("MagicNumber") // TODO this is temporarily here.
class DAppConnectionRepositoryImpl(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DAppConnectionRepository {
    override suspend fun getDAppConnectionData(): DAppConnectionData {
        return withContext(ioDispatcher) {
            delay(Random.nextLong(500, 1500))
            DAppConnectionData(
                labels = listOf(
                    "• A dApp Login, including the following information:\n" +
                        "        • Name\n" +
                        "        • Email address",
                    "• Permission to view at least one account"
                ),
                imageUrl = "INVALID_URL"
            )
        }
    }

    override suspend fun getChooseDAppLoginData(): DAppConnectionData {
        return withContext(ioDispatcher) {
            delay(Random.nextLong(500, 1500))
            DAppConnectionData(
                imageUrl = "INVALID_URL",
                dAppAccount = DAppAccount(
                    accountName = "Account name",
                    name = "Name",
                    emailAddress = "test@gmail.com"
                )
            )
        }
    }
}
