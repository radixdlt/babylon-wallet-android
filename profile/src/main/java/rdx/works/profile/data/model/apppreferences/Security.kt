package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Security(
    @SerialName("isDeveloperModeEnabled")
    val isDeveloperModeEnabled: Boolean
) {

    companion object {
        val default = Security(isDeveloperModeEnabled = true)
    }
}
