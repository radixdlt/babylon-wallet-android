package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    @SerialName("defaultDepositGuarantee")
    val defaultDepositGuarantee: String
) {
    companion object {
        val default = Transaction(
            defaultDepositGuarantee = "1"
        )
    }
}
