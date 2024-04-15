package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class P2PLinkPurpose(val value: String) {

    @SerialName("general")
    General("general");

    companion object {

        fun fromValue(value: String?): P2PLinkPurpose? {
            return entries.firstOrNull { it.value.equals(value, true) }
        }
    }
}