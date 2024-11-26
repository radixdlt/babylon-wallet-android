package com.babylon.wallet.android.presentation.dapp.authorized.verifyentities

import androidx.lifecycle.SavedStateHandle
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.SignatureWithPublicKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import rdx.works.core.sargon.serializers.AccountAddressSerializer
import rdx.works.core.sargon.serializers.AddressOfAccountOrPersonaSerializer
import rdx.works.core.sargon.serializers.IdentityAddressSerializer

@Serializable
data class EntitiesForProofWithSignatures(
    @Serializable(with = IdentityAddressSerializer::class)
    val personaAddress: IdentityAddress? = null,
    val accountAddresses: List<
        @Serializable(with = AccountAddressSerializer::class)
        AccountAddress
        > = emptyList(),
    val signatures: Map<
        @Serializable(with = AddressOfAccountOrPersonaSerializer::class)
        AddressOfAccountOrPersona,
        @Serializable(with = rdx.works.core.sargon.serializers.SignatureWithPublicKeySerializer::class)
        SignatureWithPublicKey
        > = emptyMap()
)

internal const val ARG_AUTHORIZED_REQUEST_INTERACTION_ID = "authorized_request_interaction_id_arg"
internal const val ARG_ENTITIES_FOR_PROOF = "entities_for_proof_arg"
internal const val ARG_CAN_NAVIGATE_BACK = "can_navigate_back_arg"

class VerifyEntitiesArgs(
    val authorizedRequestInteractionId: String,
    val entitiesForProofWithSignatures: EntitiesForProofWithSignatures,
    val canNavigateBack: Boolean
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        authorizedRequestInteractionId = checkNotNull(savedStateHandle[ARG_AUTHORIZED_REQUEST_INTERACTION_ID]) as String,
        entitiesForProofWithSignatures = Json.decodeFromString(checkNotNull(savedStateHandle[ARG_ENTITIES_FOR_PROOF]) as String),
        canNavigateBack = checkNotNull(savedStateHandle[ARG_CAN_NAVIGATE_BACK]) as Boolean
    )
}
