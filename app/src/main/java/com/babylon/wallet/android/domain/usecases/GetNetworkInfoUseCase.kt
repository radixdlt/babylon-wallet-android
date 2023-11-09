package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.domain.model.NetworkInfo
import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetNetworkInfoUseCase @Inject constructor(
    private val repository: NetworkInfoRepository,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(url: String) = repository.getNetworkInfo(url)

    suspend operator fun invoke(): Result<NetworkInfo> = runCatching {
        profileRepository.profile.first().currentGateway.url
    }.fold(
        onSuccess = { invoke(it) },
        onFailure = { Result.failure(it) }
    )

}
