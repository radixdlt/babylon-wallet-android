package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.dapp.DAppConnectionRepository
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
                dAppAccount = DAppLoginData(
                    accountName = "Account name",
                    name = "Name",
                    emailAddress = "test@gmail.com"
                )
            )
        }
    }

    override suspend fun getChooseDAppAccountsData(): DAppAccountsData {
        return withContext(ioDispatcher) {
            delay(Random.nextLong(500, 1500))
            DAppAccountsData(
                imageUrl = "INVALID_URL",
                dAppAccounts = listOf(
                    DAppAccountData(
                        accountName = "Account name1",
                        accountValue = "1000",
                        accountCurrency = "$",
                        accountHash = "43432423rf43g32g34"
                    ),
                    DAppAccountData(
                        accountName = "Account name2",
                        accountValue = "2000",
                        accountCurrency = "$",
                        accountHash = "d12j392dk02g43g43"
                    ),
                    DAppAccountData(
                        accountName = "Account name3",
                        accountValue = "3000",
                        accountCurrency = "$",
                        accountHash = "dj39f322dk02g43g43"
                    ),
                    DAppAccountData(
                        accountName = "Account name4",
                        accountValue = "4000",
                        accountCurrency = "$",
                        accountHash = "dj392dkg4302g2g42"
                    )
                )
            )
        }
    }
}
