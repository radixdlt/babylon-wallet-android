package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.rns.RNSRepository
import javax.inject.Inject

class ResolveRadixDomainUseCase @Inject constructor(
   private val rnsRepository: RNSRepository
) {

    suspend operator fun invoke(domain: String) = rnsRepository.resolveReceiver(domain)

}