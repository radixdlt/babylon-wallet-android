package com.babylon.wallet.android.presentation.accessfactorsources

enum class AccessFactorSourcePurpose {
    SignatureRequest,
    ProvingOwnership,
    DerivingAccounts,
    UpdatingFactorConfig,
    SpotCheck
}

enum class AccessFactorSourceSkipOption {
    CanSkipFactor,
    CanIgnoreFactor,
    None
}
