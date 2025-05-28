package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.rnsDomainValidated
import javax.inject.Inject

class IsValidRadixDomainUseCase @Inject constructor() {

    operator fun invoke(domain: String) = runCatching {
        rnsDomainValidated(raw = domain)
    }.map { true }.getOrNull() ?: false

}