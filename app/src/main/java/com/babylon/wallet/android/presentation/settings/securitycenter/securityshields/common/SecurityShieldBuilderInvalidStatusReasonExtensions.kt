package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.common

import com.radixdlt.sargon.SecurityShieldBuilderStatusInvalidReason

fun SecurityShieldBuilderStatusInvalidReason?.notEnoughFactors(): Boolean {
    this ?: return false
    return isAuthSigningFactorMissing || isPrimaryRoleFactorListEmpty || isRecoveryRoleFactorListEmpty || isConfirmationRoleFactorListEmpty
}
