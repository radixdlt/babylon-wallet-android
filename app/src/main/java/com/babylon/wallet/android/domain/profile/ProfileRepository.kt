package com.babylon.wallet.android.domain.profile

import com.babylon.wallet.android.data.profile.Account
import com.babylon.wallet.android.data.profile.PersonaEntity

interface ProfileRepository {
    suspend fun getAccounts(): List<Account>
    suspend fun getPersonas(): List<PersonaEntity>
}