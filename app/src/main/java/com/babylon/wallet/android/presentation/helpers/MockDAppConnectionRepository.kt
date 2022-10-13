package com.babylon.wallet.android.presentation.helpers

import com.babylon.wallet.android.presentation.dapp.DAppAccount
import com.babylon.wallet.android.presentation.dapp.DAppConnectionData
import com.babylon.wallet.android.presentation.dapp.DAppConnectionRepository

class MockDAppConnectionRepository : DAppConnectionRepository {
    override suspend fun getDAppConnectionData(): DAppConnectionData {
        return DAppConnectionData(
            labels = listOf(
                "• A dApp Login, including the following information:\n" +
                    "        • Name\n" +
                    "        • Email address",
                "• Permission to view at least one account"
            ),
            imageUrl = "INVALID_URL"
        )
    }

    override suspend fun getChooseDAppLoginData(): DAppConnectionData {
        return DAppConnectionData(
            imageUrl = "INVALID_URL",
            dAppAccount = DAppAccount(
                accountName = "Account name",
                name = "Name",
                emailAddress = "test@gmail.com"
            )
        )
    }
}
