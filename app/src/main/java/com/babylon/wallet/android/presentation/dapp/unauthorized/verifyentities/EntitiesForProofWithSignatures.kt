package com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.SignatureWithPublicKey
import kotlinx.serialization.Serializable
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
