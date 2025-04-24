package com.babylon.wallet.android.presentation.accessfactorsources

enum class AccessFactorSourcePurpose {
    SignatureRequest,
    ProvingOwnership,
    DerivingAccounts,
    UpdatingFactorConfig,
    CreatingAccount,
    CreatingPersona,
    SpotCheck
}

enum class AccessFactorSourceSkipOption {
    CanSkipFactor,
    CanIgnoreFactor,
    None
}
