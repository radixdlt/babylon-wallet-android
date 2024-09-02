package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceAccessControllerRecoveryBadgeValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceAccountOwnerBadgeValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceEd25519SignatureResourceValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceGlobalCallerResourceValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceIdentityOwnerBadgeValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceKind
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceMultiResourcePoolUnitValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceOneResourcePoolUnitValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourcePackageOfDirectCallerResourceValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourcePackageOwnerBadgeValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceSecp256k1SignatureResourceValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceSystemExecutionResourceValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceTwoResourcePoolUnitValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceValidatorClaimNftValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceValidatorLiquidStakeUnitValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceValidatorOwnerBadgeValue
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceXrdValue
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object NativeResourceDetailsSerializer : JsonContentPolymorphicSerializer<NativeResourceDetails>(
    NativeResourceDetails::class
) {
    @Suppress("CyclomaticComplexMethod")
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<NativeResourceDetails> {
        return when (element.jsonObject["kind"]?.jsonPrimitive?.content) {
            NativeResourceKind.Xrd.value -> NativeResourceXrdValue.serializer()
            NativeResourceKind.PackageOwnerBadge.value -> NativeResourcePackageOwnerBadgeValue.serializer()
            NativeResourceKind.AccountOwnerBadge.value -> NativeResourceAccountOwnerBadgeValue.serializer()
            NativeResourceKind.IdentityOwnerBadge.value -> NativeResourceIdentityOwnerBadgeValue.serializer()
            NativeResourceKind.ValidatorOwnerBadge.value -> NativeResourceValidatorOwnerBadgeValue.serializer()
            NativeResourceKind.Secp256k1SignatureResource.value -> NativeResourceSecp256k1SignatureResourceValue.serializer()
            NativeResourceKind.Ed25519SignatureResource.value -> NativeResourceEd25519SignatureResourceValue.serializer()
            NativeResourceKind.GlobalCallerResource.value -> NativeResourceGlobalCallerResourceValue.serializer()
            NativeResourceKind.PackageOfDirectCallerResource.value -> NativeResourcePackageOfDirectCallerResourceValue.serializer()
            NativeResourceKind.SystemExecutionResource.value -> NativeResourceSystemExecutionResourceValue.serializer()
            NativeResourceKind.ValidatorLiquidStakeUnit.value -> NativeResourceValidatorLiquidStakeUnitValue.serializer()
            NativeResourceKind.ValidatorClaimNft.value -> NativeResourceValidatorClaimNftValue.serializer()
            NativeResourceKind.OneResourcePoolUnit.value -> NativeResourceOneResourcePoolUnitValue.serializer()
            NativeResourceKind.TwoResourcePoolUnit.value -> NativeResourceTwoResourcePoolUnitValue.serializer()
            NativeResourceKind.MultiResourcePoolUnit.value -> NativeResourceMultiResourcePoolUnitValue.serializer()
            NativeResourceKind.AccessControllerRecoveryBadge.value -> NativeResourceAccessControllerRecoveryBadgeValue.serializer()
            else -> error("")
        }
    }
}
