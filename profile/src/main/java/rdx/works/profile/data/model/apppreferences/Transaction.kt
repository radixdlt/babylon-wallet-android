package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.serializers.DoubleAsStringSerializer

@Serializable
data class Transaction(
    @Serializable(with = DoubleAsStringSerializer::class)
    @SerialName("defaultDepositGuarantee")
    val defaultDepositGuarantee: Double
) {
    companion object {
        val default = Transaction(
            defaultDepositGuarantee = 1.0
        )
    }
}
