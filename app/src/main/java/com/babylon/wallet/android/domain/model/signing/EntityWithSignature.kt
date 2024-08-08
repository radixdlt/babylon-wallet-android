package com.babylon.wallet.android.domain.model.signing

import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity

/**
 * A data class that holds an entity and its signature
 *
 */
data class EntityWithSignature(
    val entity: ProfileEntity,
    val signatureWithPublicKey: SignatureWithPublicKey
)
