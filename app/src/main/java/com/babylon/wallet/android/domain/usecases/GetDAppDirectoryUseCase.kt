package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.dapps.DAppDirectoryRepository
import com.babylon.wallet.android.domain.model.DAppDirectory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import rdx.works.core.sargon.isCurrentNetworkMainnet
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class GetDAppDirectoryUseCase @Inject constructor(
    private val repository: DAppDirectoryRepository,
    private val getProfileUseCase: GetProfileUseCase
) {

    operator fun invoke(isRefreshing: Boolean): Flow<Result<DAppDirectory>> = getProfileUseCase.flow
        .distinctUntilChangedBy { it.isCurrentNetworkMainnet }
        .map { profile ->
            if (profile.isCurrentNetworkMainnet) {
                repository.getDirectory(isRefreshing = isRefreshing)
                    .fold(
                        onSuccess = { directory ->
                            if (directory.all.isEmpty()) {
                                Result.failure(IllegalStateException("DApp directory is empty"))
                            } else {
                                Result.success(directory)
                            }
                        },
                        onFailure = { Result.failure(it) }
                    )
            } else {
                Result.success(DAppDirectory())
            }
        }
}
