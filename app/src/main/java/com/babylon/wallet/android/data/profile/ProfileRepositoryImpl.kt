package com.babylon.wallet.android.data.profile

import com.babylon.wallet.android.data.AccountDto.Companion.toDAppUiModel
import com.babylon.wallet.android.data.mockdata.mockAccountDtoList
import com.babylon.wallet.android.domain.profile.ProfileRepository

class ProfileRepositoryImpl : ProfileRepository {

    override suspend fun getAccounts(): List<Account> {
        return mockAccountDtoList
            .shuffled()
            .map { accountDto ->
                accountDto.toDAppUiModel()
            }
    }

    override suspend fun getPersonas(): List<PersonaEntity> {
        return listOf(
            PersonaEntity(
                accountName = "My main",
                name = "John Smith",
                emailAddress = "smith@gmail.com"
            ),
            PersonaEntity(
                accountName = "My second",
                name = "James Bond",
                emailAddress = "jim@gmail.com"
            ),
            PersonaEntity(
                accountName = "My third",
                name = "Tom tom",
                emailAddress = "tom@gmail.com"
            )
        )
    }
}