package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.dapps.DAppDirectoryRepository
import javax.inject.Inject

class GetDAppDirectoryUseCase @Inject constructor(
    private val repository: DAppDirectoryRepository
) {

    suspend operator fun invoke(isRefreshing: Boolean) =
        repository.getDirectory(isRefreshing = isRefreshing)

}