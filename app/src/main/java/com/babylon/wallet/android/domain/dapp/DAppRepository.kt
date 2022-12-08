package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.DAppResult
import com.babylon.wallet.android.domain.common.Result

interface DAppRepository {

    suspend fun verifyDApp(): Result<DAppResult>
}
