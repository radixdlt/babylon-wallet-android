package com.babylon.wallet.android.data.gateway.model

import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccessRule(
    @SerialName("type") val type: Type,
    @SerialName("access_rule") val accessRule: AccessRuleDetail? = null
) {
    @Serializable
    enum class Type {
        @SerialName("DenyAll")
        DenyAll,

        @SerialName("AllowAll")
        AllowAll,

        @SerialName("Protected")
        Protected
    }

    @Serializable
    data class AccessRuleDetail(
        @SerialName("type") val type: String,
        @SerialName("proof_rule") val proofRule: ProofRule? = null,
        @SerialName("rules") val rules: List<AccessRuleDetail>? = null
    )

    @Serializable
    data class ProofRule(
        @SerialName("type") val type: String,
        @SerialName("requirement") val requirement: Requirement? = null
    )

    @Serializable
    data class Requirement(
        @SerialName("type") val type: String,
        @SerialName("resource") val resource: String? = null,
        @SerialName("resource_address") val resourceAddress: String? = null,
        @SerialName("fungible") val fungible: FungibleRequirement? = null,
        @SerialName("non_fungible") val nonFungible: NonFungibleRequirement? = null
    )

    @Serializable
    data class FungibleRequirement(
        @SerialName("resource_address") val resourceAddress: String
    )

    @Serializable
    data class NonFungibleRequirement(
        @SerialName("resource_address") val resourceAddress: String,
        @SerialName("local_id") val localId: LocalIdRequirement? = null
    )

    @Serializable
    data class LocalIdRequirement(
        @SerialName("id_type") val idType: String? = null,
        @SerialName("simple_rep") val simpleRep: String? = null
    )

    fun getRequiredBadgeAddress(): String? {
        val req = accessRule?.proofRule?.requirement ?: return null
        return req.resource
            ?: req.fungible?.resourceAddress
            ?: req.nonFungible?.resourceAddress
    }

    fun getRequiredBadgeNonFungibleLocalId(): String? {
        return accessRule?.proofRule?.requirement?.nonFungible?.localId?.simpleRep
    }
}

sealed class RequiredBadgeSpecification {
    abstract val resourceAddress: ResourceAddress

    data class Fungible(override val resourceAddress: ResourceAddress) : RequiredBadgeSpecification()
    data class NonFungible(
        override val resourceAddress: ResourceAddress,
        val localId: NonFungibleLocalId? = null
    ) : RequiredBadgeSpecification()
}

fun AccessRule.extractRequiredBadges(): List<RequiredBadgeSpecification> {
    if (type != AccessRule.Type.Protected) return emptyList()
    return accessRule?.extractRequiredBadges().orEmpty()
}

fun AccessRule.AccessRuleDetail.extractRequiredBadges(): List<RequiredBadgeSpecification> {
    return when (type) {
        "ProofRule" -> {
            val proof = proofRule ?: return emptyList()
            if (proof.type == "Require") {
                val req = proof.requirement ?: return emptyList()
                when (req.type) {
                    "Resource" -> {
                        val addr = req.resourceAddress ?: req.resource ?: req.fungible?.resourceAddress ?: return emptyList()
                        runCatching { ResourceAddress.init(addr) }.getOrNull()?.let {
                            listOf(RequiredBadgeSpecification.Fungible(it))
                        }.orEmpty()
                    }
                    "NonFungible" -> {
                        val nf = req.nonFungible ?: return emptyList()
                        val addr = runCatching { ResourceAddress.init(nf.resourceAddress) }.getOrNull() ?: return emptyList()
                        val localId = nf.localId?.simpleRep?.let { runCatching { NonFungibleLocalId.init(it) }.getOrNull() }
                        listOf(RequiredBadgeSpecification.NonFungible(addr, localId))
                    }
                    else -> emptyList()
                }
            } else {
                emptyList()
            }
        }
        "AllOf", "AnyOf" -> {
            rules?.flatMap { it.extractRequiredBadges() }.orEmpty()
        }
        else -> emptyList()
    }
}

